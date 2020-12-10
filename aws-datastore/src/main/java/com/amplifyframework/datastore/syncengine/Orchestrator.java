/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.datastore.syncengine;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.util.Supplier;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.datastore.AWSDataStorePlugin;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.DataStoreConfigurationProvider;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;

import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Synchronizes changed data between the {@link LocalStorageAdapter} and {@link AppSync}.
 */
public final class Orchestrator {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private static final long TIMEOUT_SECONDS_PER_MODEL = 2;
    private static final long NETWORK_OP_TIMEOUT_SECONDS = 10;
    private static final long LOCAL_OP_TIMEOUT_SECONDS = 2;

    private final SubscriptionProcessor subscriptionProcessor;
    private final SyncProcessor syncProcessor;
    private final MutationProcessor mutationProcessor;
    private final QueryPredicateProvider queryPredicateProvider;
    private final StorageObserver storageObserver;
    private final Supplier<State> targetState;
    private final AtomicReference<State> currentState;
    private final MutationOutbox mutationOutbox;
    private final CompositeDisposable disposables;
    private final Scheduler startStopScheduler;
    private final long adjustedTimeoutSeconds;
    private final Semaphore startStopSemaphore;

    /**
     * Constructs a new Orchestrator.
     * The Orchestrator will synchronize data between the {@link AppSync}
     * and the {@link LocalStorageAdapter}.
     * @param modelProvider A provider of the models to be synchronized
     * @param modelSchemaRegistry A registry of model schema
     * @param localStorageAdapter
     *        used to durably store offline changes until they can be written to the network
     * @param appSync An AppSync Endpoint
     * @param dataStoreConfigurationProvider
     *        A {@link DataStoreConfigurationProvider}; Note that the provider-style interface
     *        is needed because at the time this constructor is called from the
     *        {@link AWSDataStorePlugin}'s constructor, the plugin is not fully configured yet.
     *        The reference to the variable returned by the provider only get set after the plugin's
     *        {@link AWSDataStorePlugin#configure(JSONObject, Context)} is invoked by Amplify.
     * @param targetState The desired state of operation - online, or offline
     */
    public Orchestrator(
            @NonNull final ModelProvider modelProvider,
            @NonNull final ModelSchemaRegistry modelSchemaRegistry,
            @NonNull final LocalStorageAdapter localStorageAdapter,
            @NonNull final AppSync appSync,
            @NonNull final DataStoreConfigurationProvider dataStoreConfigurationProvider,
            @NonNull final Supplier<State> targetState) {
        Objects.requireNonNull(modelSchemaRegistry);
        Objects.requireNonNull(modelProvider);
        Objects.requireNonNull(appSync);
        Objects.requireNonNull(localStorageAdapter);

        this.mutationOutbox = new PersistentMutationOutbox(localStorageAdapter);
        VersionRepository versionRepository = new VersionRepository(localStorageAdapter);
        Merger merger = new Merger(mutationOutbox, versionRepository, localStorageAdapter);
        SyncTimeRegistry syncTimeRegistry = new SyncTimeRegistry(localStorageAdapter);
        ConflictResolver conflictResolver = new ConflictResolver(dataStoreConfigurationProvider, appSync);
        this.queryPredicateProvider = new QueryPredicateProvider(dataStoreConfigurationProvider);

        this.mutationProcessor = MutationProcessor.builder()
            .merger(merger)
            .versionRepository(versionRepository)
            .modelSchemaRegistry(modelSchemaRegistry)
            .mutationOutbox(mutationOutbox)
            .appSync(appSync)
            .conflictResolver(conflictResolver)
            .build();
        this.syncProcessor = SyncProcessor.builder()
            .modelProvider(modelProvider)
            .modelSchemaRegistry(modelSchemaRegistry)
            .syncTimeRegistry(syncTimeRegistry)
            .appSync(appSync)
            .merger(merger)
            .dataStoreConfigurationProvider(dataStoreConfigurationProvider)
            .queryPredicateProvider(queryPredicateProvider)
            .build();
        this.subscriptionProcessor = SubscriptionProcessor.builder()
                .appSync(appSync)
                .modelProvider(modelProvider)
                .merger(merger)
                .queryPredicateProvider(queryPredicateProvider)
                .build();
        this.storageObserver = new StorageObserver(localStorageAdapter, mutationOutbox);
        this.currentState = new AtomicReference<>(State.STOPPED);
        this.targetState = targetState;
        this.disposables = new CompositeDisposable();
        this.startStopScheduler = Schedulers.single();

        // Operation times out after 10 seconds. If there are more than 5 models,
        // then 2 seconds are added to the timer per additional model count.
        this.adjustedTimeoutSeconds = Math.max(
            NETWORK_OP_TIMEOUT_SECONDS,
            TIMEOUT_SECONDS_PER_MODEL * modelProvider.models().size()
        );
        this.startStopSemaphore = new Semaphore(1);

    }

    /**
     * Start the orchestrator.
     * @return A completable which emits success when the orchestrator has transitioned to LOCAL_ONLY (synchronously)
     *      and started (asynchronously) the transition to SYNC_VIA_API, if an API is available.
     */
    public synchronized Completable start() {
        return performSynchronized(Completable.fromAction(() -> {
            switch (targetState.get()) {
                case LOCAL_ONLY:
                    transitionToLocalOnly();
                    break;
                case SYNC_VIA_API:
                    transitionToApiSync();
                    break;
                case STOPPED:
                default:
                    break;
            }
        }));
    }

    /**
     * Stop the orchestrator.
     * @return A completable which emits success when orchestrator stops
     */
    public synchronized Completable stop() {
        return performSynchronized(transitionToStopped());
    }

    private Completable performSynchronized(Completable completable) {
        boolean permitAvailable = startStopSemaphore.availablePermits() > 0;
        LOG.debug("Attempting to acquire lock. Permits available = " + permitAvailable);
        try {
            if (!startStopSemaphore.tryAcquire(LOCAL_OP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                return Completable.error(new DataStoreException("Timed out acquiring orchestrator lock.",
                        "Retry your request."));
            }
        } catch (InterruptedException exception) {
            return Completable.error(new DataStoreException("Interrupted while acquiring orchestrator lock.",
                    "Retry your request."));
        }
        LOG.info("Orchestrator lock acquired.");
        return completable.doFinally(() -> {
            startStopSemaphore.release();
            LOG.info("Orchestrator lock released.");
        });
    }

    private DataStoreException unknownStateError(State state) {
        return new DataStoreException(
                "Orchestrator state machine made reference to unknown state = " + state.name(),
                AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
        );
    }

    private Completable transitionToStopped() {
        LOG.info("DataStore orchestrator stopping. Current mode = " + currentState.get().name());
        disposables.clear();
        switch (currentState.get()) {
            case SYNC_VIA_API:
                return stopApiSync().doFinally(this::stopObservingStorageChanges);
            case LOCAL_ONLY:
                stopObservingStorageChanges();
                return Completable.complete();
            case STOPPED:
                return Completable.complete();
            default:
                return Completable.error(unknownStateError(currentState.get()));

        }
    }

    private void transitionToLocalOnly() throws DataStoreException {
        switch (currentState.get()) {
            case STOPPED:
                LOG.info("Starting the orchestrator.");
                startObservingStorageChanges();
                publishReadyEvent();
                break;
            case LOCAL_ONLY:
                break;
            case SYNC_VIA_API:
                stopApiSyncBlocking();
                break;
            default:
                throw unknownStateError(currentState.get());
        }
    }

    private void transitionToApiSync() throws DataStoreException {
        switch (currentState.get()) {
            case SYNC_VIA_API:
                break;
            case LOCAL_ONLY:
                startApiSync();
                break;
            case STOPPED:
                LOG.info("Starting the orchestrator.");
                startObservingStorageChanges();
                startApiSync();
                break;
            default:
                throw unknownStateError(currentState.get());
        }
    }

    /**
     * Start observing the local storage adapter for changes;
     * enqueue them into the mutation outbox.
     */
    private void startObservingStorageChanges() throws DataStoreException {
        LOG.info("Starting to observe local storage changes.");
        try {
            boolean subscribed = mutationOutbox.load()
                .andThen(Completable.create(emitter -> {
                    storageObserver.startObservingStorageChanges(emitter::onComplete);
                    LOG.info("Setting currentState to LOCAL_ONLY");
                    currentState.set(State.LOCAL_ONLY);
                })).blockingAwait(LOCAL_OP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!subscribed) {
                throw new TimeoutException("Timed out while preparing local-only mode.");
            }
        } catch (Throwable throwable) {
            throw new DataStoreException("Timed out while starting to observe storage changes.",
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION);
        }
    }

    /**
     * Stop observing the local storage. Do not enqueue changes to the outbox.
     */
    private void stopObservingStorageChanges() {
        LOG.info("Stopping observation of local storage changes.");
        storageObserver.stopObservingStorageChanges();
        LOG.info("Setting currentState to STOPPED");
        currentState.set(State.STOPPED);
    }

    /**
     * Start syncing models to and from a remote API.
     * @return A Completable that succeeds when API sync is enabled.
     */
    private void startApiSync() {
        LOG.info("Setting currentState to SYNC_VIA_API");
        currentState.set(State.SYNC_VIA_API);
        disposables.add(startApiSyncCompletable()
            .doOnComplete(() -> {
                LOG.info("Started the orchestrator in API sync mode.");
                publishReadyEvent();
            })
            .doOnDispose(() -> LOG.debug("Orchestrator disposed the API sync"))
            .subscribeOn(Schedulers.io())
            .subscribe()
        );
    }

    private Completable startApiSyncCompletable() {
        return Completable.create(emitter -> {
            LOG.info("Starting API synchronization mode.");

            // Resolve any client provided DataStoreSyncExpressions, before starting sync and subscriptions, once each
            // time DataStore starts.  The QueryPredicateProvider caches the resolved QueryPredicates, which are then
            // used to filter data received from AppSync.
            queryPredicateProvider.resolvePredicates();

            subscriptionProcessor.startSubscriptions();

            LOG.debug("About to hydrate...");
            try {
                boolean subscribed = syncProcessor.hydrate()
                    .blockingAwait(adjustedTimeoutSeconds, TimeUnit.SECONDS);
                if (!subscribed) {
                    throw new TimeoutException("Timed out while performing initial model sync.");
                }
            } catch (Throwable failure) {
                if (!emitter.isDisposed()) {
                    emitter.onError(new DataStoreException(
                        "Initial sync during DataStore initialization failed.", failure,
                        AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
                    ));
                } else {
                    LOG.warn("Initial sync during DataStore initialization failed.", failure);
                    emitter.onComplete();
                }
                return;
            }

            LOG.debug("Draining outbox...");
            mutationProcessor.startDrainingMutationOutbox();

            LOG.debug("Draining subscription buffer...");
            subscriptionProcessor.startDrainingMutationBuffer(this::stopApiSyncBlocking);
            emitter.onComplete();
        })
        .doOnError(error -> {
            LOG.error("Failure encountered while attempting to start API sync.", error);
            stopApiSyncBlocking();
        })
        .onErrorComplete();
    }

    private void publishReadyEvent() {
        Amplify.Hub.publish(HubChannel.DATASTORE, HubEvent.create(DataStoreChannelEventName.READY));
    }

    private void stopApiSyncBlocking() {
        try {
            boolean stopped = stopApiSync()
                .subscribeOn(startStopScheduler)
                .blockingAwait(NETWORK_OP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!stopped) {
                throw new TimeoutException("Timed out while waiting for API synchronization to end.");
            }
        } catch (Throwable failure) {
            LOG.warn("Failed to stop API sync.", failure);
        }
    }

    /**
     * Stop all model synchronization with the remote API.
     * A Completable that ends when API sync is stopped.
     */
    private Completable stopApiSync() {
        return Completable.fromAction(() -> {
            LOG.info("Stopping synchronization with remote API.");
            subscriptionProcessor.stopAllSubscriptionActivity();
            mutationProcessor.stopDrainingMutationOutbox();
        })
        .onErrorComplete()
        .doOnComplete(() -> currentState.set(State.LOCAL_ONLY));
    }

    /**
     * The current state of the Orchestrator.
     */
    public enum State {
        /**
         * The sync orchestrator is fully stopped.
         */
        STOPPED,

        /**
         * The orchestrator will enqueue mutations into a holding pen, to sync with server, later.
         */
        LOCAL_ONLY,

        /**
         * The orchestrator maintains components to actively sync data up and down.
         */
        SYNC_VIA_API
    }
}
