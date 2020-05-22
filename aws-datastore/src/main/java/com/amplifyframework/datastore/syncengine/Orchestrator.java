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

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.datastore.DataStoreConfigurationProvider;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.logging.Logger;

import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Completable;

/**
 * Synchronizes changed data between the {@link LocalStorageAdapter}
 * and {@link AppSync}.
 */
public final class Orchestrator {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final SubscriptionProcessor subscriptionProcessor;
    private final SyncProcessor syncProcessor;
    private final MutationOutbox mutationOutbox;
    private final MutationProcessor mutationProcessor;
    private final StorageObserver storageObserver;
    private final AtomicReference<OrchestratorStatus> status;
    private Completable initializationCompletable;

    /**
     * Constructs a new Orchestrator.
     * The Orchestrator will synchronize data between the {@link AppSync}
     * and the {@link LocalStorageAdapter}.
     * @param modelProvider A provider of the models to be synchronized
     * @param modelSchemaRegistry A registry of model schema
     * @param localStorageAdapter Interface to local storage, used to
     *                       durably store offline changes until
     *                       then can be written to the network
     * @param appSync An AppSync Endpoint
     * @param dataStoreConfigurationProvider An instance that implements {@link DataStoreConfigurationProvider}
     *                       Note that the provider-style interface is needed because
     *                       at the time this constructor is called from the
     *                       {@link com.amplifyframework.datastore.AWSDataStorePlugin}'s constructor,
     *                       the plugin is not fully configured yet. The reference to the
     *                       variable returned by the provider only get set after the plugin's
     *                       #{@link com.amplifyframework.datastore.AWSDataStorePlugin#configure(JSONObject, Context)}
     *                       is invoked by Amplify.
     *
     */
    public Orchestrator(
            @NonNull final ModelProvider modelProvider,
            @NonNull final ModelSchemaRegistry modelSchemaRegistry,
            @NonNull final LocalStorageAdapter localStorageAdapter,
            @NonNull final AppSync appSync,
            @NonNull final DataStoreConfigurationProvider dataStoreConfigurationProvider) {
        Objects.requireNonNull(modelSchemaRegistry);
        Objects.requireNonNull(modelProvider);
        Objects.requireNonNull(appSync);
        Objects.requireNonNull(localStorageAdapter);
        this.status = new AtomicReference<>(OrchestratorStatus.STOPPED);
        this.mutationOutbox = new PersistentMutationOutbox(localStorageAdapter);
        VersionRepository versionRepository = new VersionRepository(localStorageAdapter);
        Merger merger = new Merger(mutationOutbox, versionRepository, localStorageAdapter);
        SyncTimeRegistry syncTimeRegistry = new SyncTimeRegistry(localStorageAdapter);

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
    }

    /**
     * Checks whether the orchestrator is {@link OrchestratorStatus#STARTED}.
     * @return True if the orchestrator is started, false otherwise.
     */
    public boolean isStarted() {
        return OrchestratorStatus.STARTED.equals(status.get());
    }

    /**
     * Start performing sync operations between the local storage adapter
     * and the remote GraphQL endpoint.
     * @return A Completable operation to start the sync engine orchestrator
     */
    @NonNull
    public Completable start() {
        // Only start if it's stopped.
        if (!OrchestratorStatus.STOPPED.equals(status.get())) {
            return initializationCompletable;
        }
        LOG.debug("Starting the orchestrator.");
        status.compareAndSet(OrchestratorStatus.STOPPED, OrchestratorStatus.STARTING);
        initializationCompletable = mutationOutbox.load().andThen(
            Completable.fromAction(() -> {
                if (!storageObserver.isObservingStorageChanges()) {
                    LOG.debug("Starting local storage observer.");
                    storageObserver.startObservingStorageChanges();
                }
                if (!subscriptionProcessor.isObservingSubscriptionEvents()) {
                    LOG.debug("Starting subscription processor.");
                    subscriptionProcessor.startSubscriptions();
                }
                syncProcessor.hydrate().blockingAwait();
                if (!mutationProcessor.isDrainingMutationOutbox()) {
                    LOG.debug("Starting mutation processor.");
                    mutationProcessor.startDrainingMutationOutbox();
                }
                if (!subscriptionProcessor.isDrainingMutationBuffer()) {
                    LOG.debug("Starting draining mutation buffer.");
                    subscriptionProcessor.startDrainingMutationBuffer();
                }
                status.compareAndSet(OrchestratorStatus.STARTING, OrchestratorStatus.STARTED);
            })
        );
        return initializationCompletable;
    }

    /**
     * Stop all model synchronization.
     */
    public void stop() {
        if (isStarted()) {
            LOG.info("Intentionally stopping cloud synchronization, now.");
            status.compareAndSet(OrchestratorStatus.STARTED, OrchestratorStatus.STOPPING);
            subscriptionProcessor.stopAllSubscriptionActivity();
            storageObserver.stopObservingStorageChanges();
            mutationProcessor.stopDrainingMutationOutbox();
            status.compareAndSet(OrchestratorStatus.STOPPING, OrchestratorStatus.STOPPED);
            LOG.debug("Stopped remote synchronization.");
        }

    }

    /**
     * Represents possible status of the orchestrator.
     */
    enum OrchestratorStatus {
        /**
         * The orchestrator is in the process of shutting down all the necessary components. Any requests to
         * start it will be ignored.
         *
         * Upon completion, the state should be changed to {@link #STOPPED}.
         */
        STOPPING,
        /**
         * The orchestrator is stopped and it is currently not performing any background processing. At this point
         * it is safe to start it. Only possible transition from this state should be to {@link #STARTING}
         * which happens by invoking {@link #start()}
         */
        STOPPED,
        /**
         * The orchestrator is bootstrapping all the components needed to perform the different background
         * processes it orchestrates.
         *
         * Upon completion, the state should be changed to {@link #STARTED}
         */
        STARTING,
        /**
         * The orchestrator is started. Only possible transition from this state should be to {@link #STOPPING}
         * which happens by invoking {@link #stop()}
         */
        STARTED
    }
}

