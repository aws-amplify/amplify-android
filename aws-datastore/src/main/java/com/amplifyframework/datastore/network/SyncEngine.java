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

package com.amplifyframework.datastore.network;

import android.util.Log;
import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.StreamListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.GsonStorageItemChangeConverter;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;

import java.util.Objects;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Synchronizes changed data between the {@link LocalStorageAdapter}
 * and a GraphQL API.
 *
 * The SyncEngine will monitor changes in the {@link LocalStorageAdapter}, and record
 * them into a {@link StorageItemChangeJournal}. The journal is persistent. Its purpose is
 * to be durable to application and network failures, and to allow the SyncEngine to retry
 * publication to a remote system, upon restart.
 *
 * At the same time, the SyncEngine will drain this journal, and try to publish each
 * change out over the network via the
 * {@link ApiCategoryBehavior#mutate(String, Model, QueryPredicate, MutationType, ResultListener)} .
 *
 * Meanwhile, the SyncEngine also subscribes to remote changes via the
 * {@link ApiCategoryBehavior#subscribe(String, GraphQLRequest, StreamListener)} operations.
 * Remote changes are written into the local storage without going into the journal.
 */
// The generics get intense, so we use MODEL and SIC instead of just M and S.
@SuppressWarnings("checkstyle:MethodTypeParameterName")
public final class SyncEngine {
    private static final String TAG = SyncEngine.class.getSimpleName();

    private final ApiCategoryBehavior api;
    private final ConfiguredApiProvider apiNameProvider;
    private final RemoteModelMutations remoteModelMutations;
    private final LocalStorageAdapter storageAdapter;
    private final StorageItemChangeJournal storageItemChangeJournal;
    private final GsonStorageItemChangeConverter storageItemChangeConverter;
    private final CompositeDisposable observationsToDispose;

    /**
     * Constructs a new SyncEngine. This sync engine will
     * synchronize data between the provided API and the provided
     * {@link LocalStorageAdapter}.
     * @param api Interface to a remote GraphQL endpoint
     * @param apiNameProvider Provides the name of the configured GraphQL endpoint API
     * @param modelProvider A provider of the models to be synchronized
     * @param storageAdapter Interface to local storage, used to
     *                       durably store offline changes until
     *                       then can be written to the network
     */
    public SyncEngine(
            @NonNull final ApiCategoryBehavior api,
            @NonNull final ConfiguredApiProvider apiNameProvider,
            @NonNull final ModelProvider modelProvider,
            @NonNull final LocalStorageAdapter storageAdapter) {
        this.api = Objects.requireNonNull((api));
        this.apiNameProvider = Objects.requireNonNull(apiNameProvider);
        this.remoteModelMutations = new RemoteModelMutations(api, apiNameProvider, modelProvider);
        this.storageAdapter = Objects.requireNonNull(storageAdapter);
        this.storageItemChangeJournal = new StorageItemChangeJournal(storageAdapter);
        this.storageItemChangeConverter = new GsonStorageItemChangeConverter();
        this.observationsToDispose = new CompositeDisposable();
    }

    /**
     * Start performing sync operations between the local storage adapter
     * and the remote GraphQL endpoint.
     */
    public void start() {
        startModelSubscriptions();
        startDrainingChangeJournal();
        startObservingStorageChanges();
    }

    private void startModelSubscriptions() {
        observationsToDispose.add(
            remoteModelMutations.observe()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMapSingle(this::applyMutationToLocalStorage)
                .subscribe(
                    savedMutation -> Log.i(TAG, "Successfully applied remote mutation, locally:"),
                    error -> Log.w(TAG, "Error applying mutation to local storage.", error),
                    () -> Log.i(TAG, "Subscription to remote model mutations is completed.")
                )
        );
    }

    private Single<Mutation<? extends Model>> applyMutationToLocalStorage(Mutation<? extends Model> mutation) {
        final StorageItemChange.Initiator initiator = StorageItemChange.Initiator.SYNC_ENGINE;
        return Single.defer(() -> Single.create(emitter -> {
            final ResultListener<StorageItemChange.Record> storageResultListener =
                new ResultListener<StorageItemChange.Record>() {
                    @Override
                    public void onResult(StorageItemChange.Record result) {
                        emitter.onSuccess(mutation);
                    }

                    @Override
                    public void onError(Throwable error) {
                        emitter.onError(error);
                    }
                };

            switch (mutation.type()) {
                case UPDATE:
                case CREATE:
                    storageAdapter.save(mutation.model(), initiator, storageResultListener);
                    break;
                case DELETE:
                    storageAdapter.delete(mutation.model(), initiator, storageResultListener);
                    break;
                default:
                    throw new DataStoreException(
                        "Unknown mutation type = " + mutation.type(),
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                    );
            }
        }));
    }

    /**
     * Start observing the change journal for locally-initiated changes.
     */
    private void startDrainingChangeJournal() {
        observationsToDispose.add(
            storageItemChangeJournal.observe()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMapSingle(this::publishToNetwork)
                .flatMapSingle(storageItemChangeJournal::remove)
                .subscribe(
                    processedChange -> Log.i(TAG, "Change processed successfully! " + processedChange),
                    error -> Log.w(TAG, "Error ended journal subscription: ", error),
                    () -> Log.w(TAG, "Change journal subscription was completed.")
                )
        );
    }

    /**
     * When a change is observed on the storage adapter, and that change wasn't caused
     * by the sync engine, then place that change into the persistently-backed change journal.
     */
    private void startObservingStorageChanges() {
        observationsToDispose.add(
            storageAdapter.observe()
                .map(record -> record.toStorageItemChange(storageItemChangeConverter))
                .filter(possiblyCyclicChange -> {
                    // Don't continue if the storage change was caused by the sync engine itself
                    return !StorageItemChange.Initiator.SYNC_ENGINE.equals(possiblyCyclicChange.initiator());
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMapSingle(storageItemChangeJournal::enqueue)
                .subscribe(
                    pendingChange -> Log.i(TAG, "Successfully enqueued " + pendingChange),
                    error -> Log.w(TAG, "Storage adapter subscription ended in error", error),
                    () -> Log.w(TAG, "Storage adapter subscription terminated with completion.")
                )
        );
    }

    /**
     * To process a StorageItemChange, we try to publish it to the remote GraphQL
     * API. If that succeeds, then we can remove it from the journal. Otherwise,
     * we have to keep the journal in the journal, so that we can try to publish
     * it again later, when network conditions become favorable again.
     * @param storageItemChange A storage item change to be published to remote API
     * @return A single which completes with the successfully published item, or errors
     *         if the publication fails
     */
    private <MODEL extends Model, SIC extends StorageItemChange<MODEL>> Single<SIC> publishToNetwork(
            final SIC storageItemChange) {
        //noinspection CodeBlock2Expr More readable as a block statement
        return Single.defer(() -> Single.create(subscriber -> {
            api.mutate(
                apiNameProvider.getDataStoreApiName(),
                storageItemChange.item(),
                selectMutationType(storageItemChange),
                new ResultListener<GraphQLResponse<MODEL>>() {
                    @Override
                    public void onResult(final GraphQLResponse<MODEL> result) {
                        if (result.hasErrors() || !result.hasData()) {
                            subscriber.onError(new RuntimeException("Failed to publish item to network."));
                        }
                        subscriber.onSuccess(storageItemChange);
                    }

                    @Override
                    public void onError(final Throwable error) {
                        subscriber.onError(error);
                    }
                }
            );
        }));
    }

    /**
     * Determines the appropriate mutation type for a StorageItemChange.
     * @param storageItemChange A {@link StorageItemChange} to be published to network
     * @return An appropriate {@link MutationType} for this storage item change
     */
    private static <T extends Model> MutationType selectMutationType(
            @SuppressWarnings("unused" /* one sec ... */) StorageItemChange<T> storageItemChange) {
        // TODO: add business logic here, this should be update/delete sometimes.
        return MutationType.CREATE;
    }

    /**
     * Stop synchronizing state between the local storage adapter
     * and a remote GraphQL endpoint.
     */
    public void stop() {
        observationsToDispose.clear();
    }
}
