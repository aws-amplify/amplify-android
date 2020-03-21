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

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;

import java.util.Objects;

import io.reactivex.Completable;

/**
 * Synchronizes changed data between the {@link LocalStorageAdapter}
 * and {@link AppSync}.
 */
public final class Orchestrator {
    private final SubscriptionProcessor subscriptionProcessor;
    private final SyncProcessor syncProcessor;
    private final MutationProcessor mutationProcessor;
    private final StorageObserver storageObserver;

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
     */
    public Orchestrator(
            @NonNull final ModelProvider modelProvider,
            @NonNull final ModelSchemaRegistry modelSchemaRegistry,
            @NonNull final LocalStorageAdapter localStorageAdapter,
            @NonNull final AppSync appSync) {
        Objects.requireNonNull(modelSchemaRegistry);
        Objects.requireNonNull(modelProvider);
        Objects.requireNonNull(appSync);
        Objects.requireNonNull(localStorageAdapter);

        RemoteModelMutations remoteModelMutations = new RemoteModelMutations(appSync, modelProvider);
        RemoteModelState remoteModelState = new RemoteModelState(appSync, modelProvider);
        MutationOutbox mutationOutbox = new MutationOutbox(localStorageAdapter);
        Merger merger = new Merger(localStorageAdapter);
        VersionRepository versionRepository = new VersionRepository(localStorageAdapter);

        this.mutationProcessor = new MutationProcessor(merger, versionRepository, mutationOutbox, appSync);
        this.syncProcessor = new SyncProcessor(remoteModelState, merger, modelProvider, modelSchemaRegistry);
        this.subscriptionProcessor = new SubscriptionProcessor(remoteModelMutations, merger);
        this.storageObserver = new StorageObserver(localStorageAdapter, mutationOutbox);
    }

    /**
     * Start performing sync operations between the local storage adapter
     * and the remote GraphQL endpoint.
     * @return A Completable operation to start the sync engine orchestrator
     */
    @NonNull
    public Completable start() {
        return Completable.fromAction(() -> {
            storageObserver.startObservingStorageChanges();
            subscriptionProcessor.startSubscriptions();
            syncProcessor.hydrate().blockingAwait();
            mutationProcessor.startDrainingMutationOutbox();
            subscriptionProcessor.startDrainingMutationBuffer();
        });
    }

    /**
     * Stop all model synchronization.
     */
    public void stop() {
        storageObserver.stopObservingStorageChanges();
        subscriptionProcessor.stopAllSubscriptionActivity();
        mutationProcessor.stopDrainingMutationOutbox();
    }
}
