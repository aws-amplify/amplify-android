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
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.SchemaRegistry;
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
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Synchronizes changed data between the {@link LocalStorageAdapter} and {@link AppSync}.
 */
public final class Orchestrator {
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");
    private static final long LOCAL_OP_TIMEOUT_SECONDS = 7;

    private final SubscriptionProcessor subscriptionProcessor;
    private final SyncProcessor syncProcessor;
    private final MutationProcessor mutationProcessor;
    private final QueryPredicateProvider queryPredicateProvider;
    private final StorageObserver storageObserver;
    private final Supplier<State> targetState;
    private final AtomicReference<State> currentState;
    private final MutationOutbox mutationOutbox;
    private final CompositeDisposable disposables;
    private final Semaphore startStopSemaphore;
    private final ReachabilityMonitor reachabilityMonitor;
    private Disposable monitorNetworkChangesDisposable;

    // This lock is used to synchronize on any state changes made to current stae
    private final Object transitionLock = new Object();

    /*
    This lock is used to syncrhonize on the startApi block. clearing the disposable is not always enough to stop
    the completable. This block is an additional safety net to ensure that startApi does not run multiple times.
     */
    private final Object startApiLock = new Object();

    /**
     * Constructs a new Orchestrator.
     * The Orchestrator will synchronize data between the {@link AppSync}
     * and the {@link LocalStorageAdapter}.
     * @param modelProvider A provider of the models to be synchronized
     * @param schemaRegistry A registry of model schema and customType schema
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
     * @param reachabilityMonitor The reachability monitor to use.
     * @param isSyncRetryEnabled enable or disable the SyncProcessor retry
     */
    public Orchestrator(
            @NonNull final ModelProvider modelProvider,
            @NonNull final SchemaRegistry schemaRegistry,
            @NonNull final LocalStorageAdapter localStorageAdapter,
            @NonNull final AppSync appSync,
            @NonNull final DataStoreConfigurationProvider dataStoreConfigurationProvider,
            @NonNull final Supplier<State> targetState,
            @NonNull final ReachabilityMonitor reachabilityMonitor,
            final boolean isSyncRetryEnabled) {
        Objects.requireNonNull(schemaRegistry);
        Objects.requireNonNull(modelProvider);
        Objects.requireNonNull(appSync);
        Objects.requireNonNull(localStorageAdapter);

        this.mutationOutbox = new PersistentMutationOutbox(localStorageAdapter);
        VersionRepository versionRepository = new VersionRepository(localStorageAdapter);
        Merger merger = new Merger(mutationOutbox, versionRepository, localStorageAdapter);
        SyncTimeRegistry syncTimeRegistry = new SyncTimeRegistry(localStorageAdapter);
        this.queryPredicateProvider = new QueryPredicateProvider(dataStoreConfigurationProvider);

        this.mutationProcessor = MutationProcessor.builder()
            .merger(merger)
            .versionRepository(versionRepository)
            .schemaRegistry(schemaRegistry)
            .mutationOutbox(mutationOutbox)
            .appSync(appSync)
            .dataStoreConfigurationProvider(dataStoreConfigurationProvider)
            .retryHandler(new RetryHandler())
            .build();
        this.syncProcessor = SyncProcessor.builder()
            .modelProvider(modelProvider)
            .schemaRegistry(schemaRegistry)
            .syncTimeRegistry(syncTimeRegistry)
            .appSync(appSync)
            .merger(merger)
            .dataStoreConfigurationProvider(dataStoreConfigurationProvider)
            .queryPredicateProvider(queryPredicateProvider)
            .retryHandler(new RetryHandler())
                .isSyncRetryEnabled(isSyncRetryEnabled)
            .build();
        this.subscriptionProcessor = SubscriptionProcessor.builder()
                .appSync(appSync)
                .modelProvider(modelProvider)
                .schemaRegistry(schemaRegistry)
                .merger(merger)
                .queryPredicateProvider(queryPredicateProvider)
                .onFailure(this::onApiSyncFailure)
                .build();
        this.storageObserver = new StorageObserver(localStorageAdapter, mutationOutbox);
        this.currentState = new AtomicReference<>(State.STOPPED);
        this.targetState = targetState;
        this.reachabilityMonitor = reachabilityMonitor;
        this.disposables = new CompositeDisposable();

        this.startStopSemaphore = new Semaphore(1);

    }

    /**
     * Start the orchestrator.
     * @return A completable which emits success when the orchestrator has transitioned to LOCAL_ONLY (synchronously)
     *      and started (asynchronously) the transition to SYNC_VIA_API, if an API is available.
     */
    public synchronized Completable start() {
        return performSynchronized(() -> {
            switch (targetState.get()) {
                case LOCAL_ONLY:
                    disposeNetworkChanges();
                    transitionToLocalOnly();
                    break;
                case SYNC_VIA_API:
                    boolean isOnline = reachabilityMonitor.getObservable().blockingFirst();
                    if (isOnline) {
                        transitionToApiSync();
                    } else {
                        transitionToLocalOnly();
                    }
                    break;
                case STOPPED:
                default:
                    break;
            }
        });
    }

    /**
     * Stop the orchestrator.
     * @return A completable which emits success when orchestrator stops
     */
    public synchronized Completable stop() {
        return performSynchronized(this::transitionToStopped);
    }

    private Completable performSynchronized(Action action) {
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
        return Completable.fromAction(action).doOnError((e) -> {
            startStopSemaphore.release();
            LOG.info("Orchestrator lock released.");
        }).andThen(Completable.fromAction(() -> {
            startStopSemaphore.release();
            LOG.info("Orchestrator lock released.");
        }));
    }

    private void unknownState(State state) throws DataStoreException {
        throw new DataStoreException(
                "Orchestrator state machine made reference to unknown state = " + state.name(),
                AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
        );
    }

    private void transitionToStopped() throws DataStoreException {
        synchronized (transitionLock) {
            switch (currentState.get()) {
                case SYNC_VIA_API:
                    LOG.info("Orchestrator transitioning from SYNC_VIA_API to STOPPED");
                    stopApiSync();
                    LOG.info("Setting currentState to LOCAL_ONLY");
                    currentState.set(State.LOCAL_ONLY);
                    stopObservingStorageChanges();
                    LOG.info("Setting currentState to STOPPED");
                    currentState.set(State.STOPPED);
                    break;
                case LOCAL_ONLY:
                    LOG.info("Orchestrator transitioning from LOCAL_ONLY to STOPPED");
                    stopObservingStorageChanges();
                    LOG.info("Setting currentState to STOPPED");
                    currentState.set(State.STOPPED);
                    break;
                case STOPPED:
                    break;
                default:
                    unknownState(currentState.get());
                    break;
            }
        }
    }

    private void transitionToLocalOnly() throws DataStoreException {
        synchronized (transitionLock) {
            switch (currentState.get()) {
                case STOPPED:
                    LOG.info("Orchestrator transitioning from STOPPED to LOCAL_ONLY");
                    startObservingStorageChanges();
                    LOG.info("Setting currentState to LOCAL_ONLY");
                    currentState.set(State.LOCAL_ONLY);
                    publishReadyEvent();
                    break;
                case LOCAL_ONLY:
                    break;
                case SYNC_VIA_API:
                    LOG.info("Orchestrator transitioning from SYNC_VIA_API to LOCAL_ONLY");
                    stopApiSync();
                    monitorNetworkChanges();
                    LOG.info("Setting currentState to LOCAL_ONLY");
                    currentState.set(State.LOCAL_ONLY);
                    break;
                default:
                    unknownState(currentState.get());
                    break;
            }
        }
    }

    private void transitionToApiSync() throws DataStoreException {
        synchronized (transitionLock) {
            switch (currentState.get()) {
                case SYNC_VIA_API:
                    break;
                case LOCAL_ONLY:
                    LOG.info("Orchestrator transitioning from LOCAL_ONLY to SYNC_VIA_API");
                    LOG.info("Setting currentState to SYNC_VIA_API");
                    currentState.set(State.SYNC_VIA_API);
                    startApiSync();
                    break;
                case STOPPED:
                    LOG.info("Orchestrator transitioning from STOPPED to SYNC_VIA_API");
                    startObservingStorageChanges();
                    LOG.info("Setting currentState to LOCAL_ONLY");
                    currentState.set(State.LOCAL_ONLY);
                    LOG.info("Setting currentState to SYNC_VIA_API");
                    currentState.set(State.SYNC_VIA_API);
                    startApiSync();
                    break;
                default:
                    unknownState(currentState.get());
                    break;
            }
        }
    }

    /**
     * Start observing the local storage adapter for changes;
     * enqueue them into the mutation outbox.
     */
    private void startObservingStorageChanges() throws DataStoreException {
        LOG.info("Starting to observe local storage changes.");
        try {
            mutationOutbox.load()
                .andThen(Completable.create(emitter -> {
                    storageObserver.startObservingStorageChanges(emitter::onComplete);
                })).blockingAwait();
        } catch (Throwable throwable) {
            throw new DataStoreException("Timed out while starting to observe storage changes.",
                throwable,
                AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION);
        }
    }

    /**
     * Stop observing the local storage. Do not enqueue changes to the outbox.
     */
    private void stopObservingStorageChanges() {
        LOG.info("Stopping observation of local storage changes.");
        storageObserver.stopObservingStorageChanges();
    }

    /**
     * Start syncing models to and from a remote API.
     */
    private void startApiSync() {
        monitorNetworkChanges();
        disposables.add(
            Completable.create(emitter -> {
                synchronized (startApiLock) {
                    LOG.info("Starting API synchronization mode.");

                    // Resolve any client provided DataStoreSyncExpressions, before starting sync and subscriptions,
                    // once each time DataStore starts.  The QueryPredicateProvider caches the resolved QueryPredicates,
                    // which are then used to filter data received from AppSync.
                    queryPredicateProvider.resolvePredicates();

                    try {
                        subscriptionProcessor.startSubscriptions();
                    } catch (Throwable failure) {
                        if (!emitter.tryOnError(
                            new DataStoreException("DataStore subscriptionProcessor failed to start.",
                                failure, "Check your internet."))) {
                            LOG.warn("DataStore failed to start after emitter was disposed.",
                                failure);
                            emitter.onComplete();
                        }
                        return;
                    }

                    long startTime = System.currentTimeMillis();
                    LOG.debug("About to hydrate...");
                    try {
                        syncProcessor.hydrate()
                            .blockingAwait();
                        LOG.debug("Hydration complete in " + (System.currentTimeMillis() - startTime) + "ms");
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

                    if (!emitter.isDisposed()) {
                        LOG.debug("Draining outbox...");
                        mutationProcessor.startDrainingMutationOutbox();
                    }

                    if (!emitter.isDisposed()) {
                        LOG.debug("Draining mutation buffer...");
                        subscriptionProcessor.startDrainingMutationBuffer();
                    }

                    emitter.onComplete();
                }
            })
            .doOnError(error -> LOG.error("Failure encountered while attempting to start API sync.", error))
            .doOnComplete(() -> LOG.info("Started the orchestrator in API sync mode."))
            .doOnDispose(() -> LOG.debug("Orchestrator disposed the API sync"))
            .subscribeOn(Schedulers.io())
            .subscribe(
                    this::publishReadyEvent,
                    this::onApiSyncFailure
            )
        );
    }

    private void publishReadyEvent() {
        Amplify.Hub.publish(HubChannel.DATASTORE, HubEvent.create(DataStoreChannelEventName.READY));
    }

    private void onApiSyncFailure(Throwable exception) {
        // Don't transition to LOCAL_ONLY, if it's already in progress.
        if (!State.SYNC_VIA_API.equals(currentState.get())) {
            return;
        }
        LOG.warn("API sync failed - transitioning to LOCAL_ONLY.", exception);
        Completable.fromAction(this::transitionToLocalOnly)
            .doOnError(error -> LOG.warn("Transition to LOCAL_ONLY failed.", error))
            .subscribe();
    }

    private void disposeNetworkChanges() {
        if (monitorNetworkChangesDisposable != null) {
            monitorNetworkChangesDisposable.dispose();
            monitorNetworkChangesDisposable = null;
        }
    }

    private void monitorNetworkChanges() {
        disposeNetworkChanges();

        monitorNetworkChangesDisposable = reachabilityMonitor.getObservable()
            .skip(1) // We skip the current online state, we only care about transitions
            .filter(ignore -> !State.STOPPED.equals(currentState.get()))
            .subscribe(isOnline -> {
                if (isOnline) {
                    transitionToApiSync();
                } else {
                    transitionToLocalOnly();
                }
            });
    }

    /**
     * Stop all model synchronization with the remote API.
     */
    private void stopApiSync() {
        disposeNetworkChanges();
        disposables.clear();
        subscriptionProcessor.stopAllSubscriptionActivity();
        mutationProcessor.stopDrainingMutationOutbox();
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
