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
import com.amplifyframework.core.model.Model;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Synchronizes changed data between the {@link LocalStorageAdapter} and {@link AppSync}.
 */
public final class Orchestrator {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private static final long OP_TIMEOUT_SECONDS = 10;

    private final SubscriptionProcessor subscriptionProcessor;
    private final SyncProcessor syncProcessor;
    private final MutationProcessor mutationProcessor;
    private final StorageObserver storageObserver;
    private final Supplier<Mode> targetMode;
    private final AtomicReference<Mode> currentMode;
    private final MutationOutbox mutationOutbox;
    private final CompositeDisposable disposables;
    private final Scheduler startStopScheduler;
    private final LocalStorageAdapter localStorageAdapter;
    private final Set<Class<? extends Model>> syncableModels;

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

        this.localStorageAdapter = localStorageAdapter;
        syncableModels = modelProvider.models();

        this.mutationProcessor = new MutationProcessor(merger, versionRepository, mutationOutbox, appSync);
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
     */
    public void start() {
        disposables.add(transitionCompletable()
            .subscribeOn(startStopScheduler)
            .doOnDispose(() -> LOG.debug("Orchestrator disposed a transition."))
            .subscribe(
                () -> {
                    LOG.debug("Orchestrator completed a transition");
                    if (isStarted()) {
                        Amplify.Hub.publish(HubChannel.DATASTORE,
                            HubEvent.create(DataStoreChannelEventName.READY));
                    }
                },
                failure -> LOG.warn("Orchestrator failed to transition.")
            ));
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
    public Completable stop() {
        LOG.info("DataStore orchestrator stopping. Current mode = " + currentMode.get().name());
        disposables.clear();
        return transitionToStopped(currentMode.get())
            .subscribeOn(startStopScheduler);
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
        Throwable throwable = mutationOutbox.load()
            .andThen(Completable.create(emitter -> {
                storageObserver.startObservingStorageChanges(emitter::onComplete);
                currentMode.set(Mode.LOCAL_ONLY);
            })).blockingGet(OP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (throwable != null) {
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
            Throwable failure = syncProcessor.hydrate()
                .blockingGet(OP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (failure != null) {
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
        Throwable failure = stopApiSync()
            .subscribeOn(startStopScheduler)
            .blockingGet(OP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (failure != null) {
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
