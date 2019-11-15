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

import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.MutationEvent;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;

import java.util.Collections;
import java.util.Objects;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Synchronizes changed data between the {@link LocalStorageAdapter}
 * and a GraphQL API.
 */
public final class SyncEngine {
    private static final String TAG = SyncEngine.class.getName();

    private final ApiCategoryBehavior api;
    private final String apiName;
    private final LocalStorageAdapter storageAdapter;
    private final OutgoingMutationsQueue outgoingMutationsQueue;
    private final CompositeDisposable observationsToDispose;

    /**
     * Constructs a new SyncEngine. This sync engine will
     * synchronized data between the provided API and the provided
     * {@link LocalStorageAdapter}.
     * @param api Interface to a remote GraphQL endpoint
     * @param apiName The name of the configured GraphQL endpoint API
     * @param storageAdapter Interface to a local data cache
     * @param outgoingMutationsQueue A queue into which mutation events are
     *                      stored durably until being writ to network
     */
    SyncEngine(
            @NonNull ApiCategoryBehavior api,
            @NonNull String apiName,
            @NonNull final LocalStorageAdapter storageAdapter,
            @NonNull final OutgoingMutationsQueue outgoingMutationsQueue) {
        this.api = Objects.requireNonNull((api));
        this.apiName = Objects.requireNonNull(apiName);
        this.storageAdapter = Objects.requireNonNull(storageAdapter);
        this.outgoingMutationsQueue = Objects.requireNonNull(outgoingMutationsQueue);
        this.observationsToDispose = new CompositeDisposable();
    }

    /**
     * Start performing sync operations between the local storage adapter
     * and the remote GraphQL endpoint.
     */
    public void start() {
        // Start by observing the mutation queue for locally-initiated changes
        observationsToDispose.add(
            outgoingMutationsQueue.observe()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMapSingle(this::publishToNetwork)
                .flatMapSingle(outgoingMutationsQueue::remove)
                .subscribe(
                    publishedMutationEvent -> Log.i(TAG, "Mutation successfully published! " + publishedMutationEvent),
                    errorInQueue -> Log.w(TAG, "Error ended mutation queue subscription: ", errorInQueue),
                    () -> Log.w(TAG, "Mutation queue subscription was completed.")
                )
        );

        /*
         * When a change is observed on the storage adapter,
         * and that change wasn't caused by the sync engine,
         * then place that change into a persistently-backed
         * mutation queue.
         */
        observationsToDispose.add(
            storageAdapter.observe()
                .filter(possibleCycleEvent -> {
                    // Don't continue if the event was caused by the sync engine itself
                    return !MutationEvent.Source.SYNC_ENGINE.equals(possibleCycleEvent.source());
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMapSingle(outgoingMutationsQueue::enqueue)
                .subscribe(
                    externalMutationEvent -> Log.i(TAG, "Successfully enqueued " + externalMutationEvent),
                    errorInAdapter -> Log.w(TAG, "Storage adapter subscription ended in error", errorInAdapter),
                    () -> Log.w(TAG, "Storage adapter subscription terminated with completion.")
                )
        );
    }

    /*
     * To process a mutation event, we try to publish it to the remote GraphQL
     * API. If that succeeds, then we can remove it from the queue. Otherwise,
     * we have to keep the event in the queue, so that we can try to publish
     * it again later, when network conditions become favorable again.
     */
    @SuppressWarnings("unchecked") // mutationEvent.getClass() yields Class<?>, not Class<M>.
    private <T extends Model, M extends MutationEvent<T>> Single<M> publishToNetwork(final M mutationEvent) {
        //noinspection CodeBlock2Expr More readable as a block statement
        return Single.defer(() -> Single.create(subscriber -> {
            api.mutate(
                apiName,
                MutationDocument.from(mutationEvent),
                Collections.emptyMap(),
                (Class<M>) mutationEvent.getClass(),
                new ResultListener<GraphQLResponse<M>>() {
                    @Override
                    public void onResult(final GraphQLResponse<M> result) {
                        if (result.hasErrors() || !result.hasData()) {
                            subscriber.onError(new RuntimeException("Failed to publish data to network."));
                        }
                        subscriber.onSuccess(mutationEvent);
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
     * Stop synchronizing state between the local storage adapter
     * and a remote GraphQL endpoint.
     */
    public void stop() {
        observationsToDispose.clear();
    }
}

