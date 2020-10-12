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
import androidx.core.util.ObjectsCompat;
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

import java.util.Locale;
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
    private static final long NO_WAIT_TIMEOUT = 0;

    private final SubscriptionProcessor subscriptionProcessor;
    private final SyncProcessor syncProcessor;
    private final MutationProcessor mutationProcessor;
    private final StorageObserver storageObserver;
    private final Supplier<Mode> targetMode;
    private final AtomicReference<Mode> currentMode;
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
     * @param targetMode The desired mode of operation - online, or offline
     */
    public Orchestrator(
            @NonNull final ModelProvider modelProvider,
            @NonNull final ModelSchemaRegistry modelSchemaRegistry,
            @NonNull final LocalStorageAdapter localStorageAdapter,
            @NonNull final AppSync appSync,
            @NonNull final DataStoreConfigurationProvider dataStoreConfigurationProvider,
            @NonNull final Supplier<Mode> targetMode) {
        Objects.requireNonNull(modelSchemaRegistry);
        Objects.requireNonNull(modelProvider);
        Objects.requireNonNull(appSync);
        Objects.requireNonNull(localStorageAdapter);

        this.mutationOutbox = new PersistentMutationOutbox(localStorageAdapter);
        VersionRepository versionRepository = new VersionRepository(localStorageAdapter);
        Merger merger = new Merger(mutationOutbox, versionRepository, localStorageAdapter);
        SyncTimeRegistry syncTimeRegistry = new SyncTimeRegistry(localStorageAdapter);

        this.mutationProcessor = MutationProcessor.builder()
            .merger(merger)
            .versionRepository(versionRepository)
            .syncTimeRegistry(syncTimeRegistry)
            .mutationOutbox(mutationOutbox)
            .appSync(appSync)
            .dataStoreConfigurationProvider(dataStoreConfigurationProvider)
            .build();
        this.syncProcessor = SyncProcessor.builder()
            .modelProvider(modelProvider)
            .modelSchemaRegistry(modelSchemaRegistry)
            .syncTimeRegistry(syncTimeRegistry)
            .appSync(appSync)
            .merger(merger)
            .dataStoreConfigurationProvider(dataStoreConfigurationProvider)
            .build();
        this.subscriptionProcessor = new SubscriptionProcessor(appSync, modelProvider, merger);
        this.storageObserver = new StorageObserver(localStorageAdapter, mutationOutbox);
        this.currentMode = new AtomicReference<>(Mode.STOPPED);
        this.targetMode = targetMode;
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
     * Checks if the orchestrator is running in the desired target state.
     * @return true if so, false otherwise.
     */
    public boolean isStarted() {
        return ObjectsCompat.equals(targetMode.get(), currentMode.get());
    }

    /**
     * Checks if the orchestrator is stopped.
     * @return true if so, false otherwise.
     */
    @SuppressWarnings("unused")
    public boolean isStopped() {
        return Mode.STOPPED.equals(currentMode.get());
    }

    /**
     * Start performing sync operations between the local storage adapter
     * and the remote GraphQL endpoint.
     *
     * If locked, waits for 2 seconds to acquire a lock before exiting
     */
    public void start() {
        attemptStart(LOCAL_OP_TIMEOUT_SECONDS);
    }

    /**
     * Attempts to start orchestrator however immediately exists if another process has a lock.
     */
    public void startWithBehaviorExitImmediatelyIfTransitioning() {
        attemptStart(NO_WAIT_TIMEOUT);
    }

    /**
     * Start performing sync operations between the local storage adapter
     * and the remote GraphQL endpoint.
     */
    private void attemptStart(@NonNull final long lockAcquireTimeoutSeconds) {
        if (tryAcquireStartStopLock(lockAcquireTimeoutSeconds, TimeUnit.SECONDS)) {
            disposables.add(transitionCompletable()
                .doOnSubscribe(subscriber -> {
                    LOG.info("Starting the orchestrator.");
                })
                .doOnComplete(() -> {
                    LOG.info("Orchestrator completed a transition");
                    if (isStarted()) {
                        Amplify.Hub.publish(HubChannel.DATASTORE,
                            HubEvent.create(DataStoreChannelEventName.READY));
                    }
                })
                .doOnError(failure -> {
                    LOG.warn("Unable to acquire orchestrator lock. Transition currently in progress.", failure);
                })
                .doOnDispose(() -> LOG.debug("Orchestrator disposed a transition."))
                .doFinally(startStopSemaphore::release)
                .subscribeOn(startStopScheduler)
                .subscribe()
            );
        } else {
            LOG.warn("Unable to acquire orchestrator lock. Transition currently in progress.");
        }
    }

    private boolean tryAcquireStartStopLock(long opTimeout, TimeUnit timeUnit) {
        boolean permitAvailable = startStopSemaphore.availablePermits() > 0;
        LOG.debug("Attempting to acquire lock. Permits available = " + permitAvailable);
        try {
            if (!startStopSemaphore.tryAcquire(opTimeout, timeUnit)) {
                LOG.warn("Unable to acquire orchestrator lock. Transition currently in progress.");
                return false;
            }
        } catch (InterruptedException exception) {
            return false;
        }
        LOG.debug("Lock acquired.");
        return true;
    }

    private Completable transitionCompletable() {
        Mode current = currentMode.get();
        Mode target = targetMode.get();
        if (ObjectsCompat.equals(current, target)) {
            return Completable.complete();
        }
        LOG.info(String.format(Locale.US,
            "DataStore orchestrator transitioning states. " +
                "Current mode = %s, target mode = %s.", current, target
        ));

        switch (target) {
            case STOPPED:
                return transitionToStopped(current);
            case LOCAL_ONLY:
                return transitionToLocalOnly(current);
            case SYNC_VIA_API:
                return transitionToApiSync(current);
            default:
                return unknownMode(target);
        }
    }

    /**
     * Stop the orchestrator.
     * @return A completable which emits success when orchestrator stops
     */
    public synchronized Completable stop() {
        LOG.info("DataStore orchestrator stopping. Current mode = " + currentMode.get().name());
        if (tryAcquireStartStopLock(LOCAL_OP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            disposables.clear();
            return transitionToStopped(currentMode.get())
                .subscribeOn(startStopScheduler)
                .doFinally(startStopSemaphore::release);
        } else {
            return Completable.error(new DataStoreException("Unable to acquire orchestrator lock. " +
                                                                "Transition currently in progress.",
                                                            "Retry your operation"));
        }

    }

    private static Completable unknownMode(Mode mode) {
        return Completable.error(new DataStoreException(
            "Orchestrator state machine made reference to unknown mode = " + mode.name(),
            AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
        ));
    }

    private Completable transitionToStopped(Mode current) {
        switch (current) {
            case SYNC_VIA_API:
                return stopApiSync().doFinally(this::stopObservingStorageChanges);
            case LOCAL_ONLY:
                stopObservingStorageChanges();
                return Completable.complete();
            case STOPPED:
                return Completable.complete();
            default:
                return unknownMode(current);
        }
    }

    private Completable transitionToLocalOnly(Mode current) {
        switch (current) {
            case STOPPED:
                startObservingStorageChanges();
                return Completable.complete();
            case LOCAL_ONLY:
                return Completable.complete();
            case SYNC_VIA_API:
                return stopApiSync();
            default:
                return unknownMode(current);
        }
    }

    private Completable transitionToApiSync(Mode current) {
        switch (current) {
            case SYNC_VIA_API:
                return Completable.complete();
            case LOCAL_ONLY:
                return startApiSync();
            case STOPPED:
                startObservingStorageChanges();
                return startApiSync();
            default:
                return unknownMode(current);
        }
    }

    /**
     * Start observing the local storage adapter for changes;
     * enqueue them into the mutation outbox.
     */
    private void startObservingStorageChanges() {
        LOG.info("Starting to observe local storage changes.");
        try {
            boolean subscribed = mutationOutbox.load()
                .andThen(Completable.create(emitter -> {
                    storageObserver.startObservingStorageChanges(emitter::onComplete);
                    currentMode.set(Mode.LOCAL_ONLY);
                })).blockingAwait(LOCAL_OP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!subscribed) {
                throw new TimeoutException("Timed out while preparing local-only mode.");
            }
        } catch (Throwable throwable) {
            LOG.warn("Failed to start observing storage changes.", throwable);
        }
    }

    /**
     * Stop observing the local storage. Do not enqueue changes to the outbox.
     */
    private void stopObservingStorageChanges() {
        LOG.info("Stopping observation of local storage changes.");
        storageObserver.stopObservingStorageChanges();
        currentMode.set(Mode.STOPPED);
    }

    /**
     * Start syncing models to and from a remote API.
     * @return A Completable that succeeds when API sync is enabled.
     */
    private Completable startApiSync() {
        return Completable.create(emitter -> {
            LOG.info("Starting API synchronization mode.");

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

            currentMode.set(Mode.SYNC_VIA_API);
            emitter.onComplete();
        })
        .doOnError(error -> {
            LOG.error("Failure encountered while attempting to start API sync.", error);
            stopApiSyncBlocking();
        })
        .onErrorComplete();
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
        .doOnComplete(() -> currentMode.set(Mode.LOCAL_ONLY));
    }

    /**
     * The mode of operation for the Orchestrator's synchronization logic.
     */
    public enum Mode {
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
