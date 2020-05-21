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

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.core.reachability.Reachability;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreConfigurationProvider;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.logging.Logger;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Orchestrates the various components of the Sync Engine. The Sync Engine
 * is responsible for synchronizing data back and forth between the device and an
 * AppSync endpoint.
 */
public final class Orchestrator {
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final SubscriptionProcessor subscriptionProcessor;
    private final SyncProcessor syncProcessor;
    private final MutationOutbox mutationOutbox;
    private final MutationProcessor mutationProcessor;
    private final StorageObserver storageObserver;
    private final CompositeDisposable onlineDisposable;
    private final OnlineState onlineState;

    /**
     * Constructs a new Orchestrator.
     * The Orchestrator will synchronize data between {@link AppSync} and the {@link LocalStorageAdapter}.
     * @param modelProvider A provider of the models to be synchronized
     * @param modelSchemaRegistry A registry of model schema
     * @param localStorageAdapter Local storage, to manage models as well as system metadata
     * @param appSync An AppSync Endpoint
     * @param dataStoreConfigurationProvider A provider of {@link DataStoreConfiguration}
     * @param reachability A way to determine if there is an internet connection
     */
    public Orchestrator(
            @NonNull final ModelProvider modelProvider,
            @NonNull final ModelSchemaRegistry modelSchemaRegistry,
            @NonNull final LocalStorageAdapter localStorageAdapter,
            @NonNull final AppSync appSync,
            @NonNull final DataStoreConfigurationProvider dataStoreConfigurationProvider,
            @NonNull final Reachability reachability) {
        Objects.requireNonNull(modelProvider);
        Objects.requireNonNull(modelSchemaRegistry);
        Objects.requireNonNull(localStorageAdapter);
        Objects.requireNonNull(appSync);
        Objects.requireNonNull(dataStoreConfigurationProvider);
        Objects.requireNonNull(reachability);

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
            .hub(Amplify.Hub)
            .build();
        this.subscriptionProcessor = new SubscriptionProcessor(appSync, modelProvider, merger);
        this.storageObserver = new StorageObserver(localStorageAdapter, mutationOutbox);

        this.onlineDisposable = new CompositeDisposable();
        this.onlineState = new OnlineState(Amplify.Hub, reachability);
    }

    /**
     * Start performing sync operations between the local storage adapter
     * and the remote GraphQL endpoint.
     * @param syncMode The syncrhonization mode for DataStore, e.g. {@link SyncMode#SYNC_VIA_API}
     * @return A Completable operation to start the sync engine orchestrator
     */
    @NonNull
    public Completable start(SyncMode syncMode) {
        return mutationOutbox.load()
            .andThen(Completable.fromAction(storageObserver::startObservingStorageChanges))
            .andThen(Completable.fromAction(() -> {
                // If we're not sync'ing, be done with this.
                if (!SyncMode.SYNC_VIA_API.equals(syncMode)) {
                    return;
                }
                // If we are, setup a reaction to changes in the online/offline state.
                onlineDisposable.add(onlineState.observe()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe(this::toggleState));
                // First, set the initial state, in a synchronous way.
                toggleState(onlineState.check()
                    .timeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .blockingGet());
                // Watch for state updates
                onlineState.startDetecting();
            }));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("CheckResult")
    private void toggleState(boolean isOnline) {
        LOG.info("Setting the online state to = " + isOnline);
        if (!isOnline) {
            subscriptionProcessor.stopAllActivity();
            mutationProcessor.stopDrainingMutationOutbox();
            return;
        }
        subscriptionProcessor.startSubscriptions();
        syncProcessor.hydrate()
            .andThen(mutationProcessor.drainMutationOutbox())
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .andThen(Single.just(mutationProcessor.startDrainingMutationOutbox()))
            .timeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .blockingGet();
        subscriptionProcessor.startDrainingMutationBuffer();
        LOG.info("Cloud synchronization is now fully active.");
    }

    /**
     * Stops the orchestrator.
     */
    public void stop() {
        onlineDisposable.clear();
    }
}
