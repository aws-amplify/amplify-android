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

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.MutationEvent;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;

import java.util.Iterator;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;

/*
 * An outgoing mutation queue is a persistently-backed in-order staging ground
 * for mutations that have already occurred in the storage adapter, and need
 * to be synchronized with a remove GraphQL API.
 *
 * Items in the mutation queue are observed, and written out over the network.
 * When a write completes successfully, it is safe to remove the corresponding item
 * from the mutation queue.
 */
@SuppressWarnings("CodeBlock2Expr") // In this class, some lambdas look more readable w/ blocks
final class OutgoingMutationsQueue {
    private final LocalStorageAdapter localStorageAdapter;
    private final PublishSubject<MutationEvent<?>> pendingMutations;

    OutgoingMutationsQueue(@NonNull final LocalStorageAdapter localStorageAdapter) {
        this.localStorageAdapter = Objects.requireNonNull(localStorageAdapter);
        this.pendingMutations = PublishSubject.create();
    }

    /*
     * Enqueue a mutation into the mutation queue.
     * This involves:
     *   1. Writing the mutation into a persistent store
     *      (we use the storage adapter, again, for this)
     *   2. Notifying the queue observers that there is a new
     *      mutation that needs to be processed.
     * @param mutationEvent Event to be placed into queue
     * @return A Single that emits the value that was enqueued, if successful,
     *         of the enqueue error, if not
     */
    @NonNull
    <T extends Model, M extends MutationEvent<T>> Single<M> enqueue(@NonNull M mutationEvent) {
        Objects.requireNonNull(mutationEvent);
        return Single.defer(() -> Single.create(subscriber -> {
            localStorageAdapter.save(mutationEvent, new ResultListener<MutationEvent<M>>() {
                @Override
                public void onResult(final MutationEvent<M> mutationOfMutation) {
                    pendingMutations.onNext(mutationOfMutation.data());
                    subscriber.onSuccess(mutationOfMutation.data());
                }

                @Override
                public void onError(final Throwable error) {
                    pendingMutations.onError(error);
                    subscriber.onError(error);
                }
            });
        }));
    }

    @WorkerThread
    @NonNull
    Observable<MutationEvent<?>> observe() {
        return pendingMutations
            .startWith(previouslyUnhandledMutations())
            .filter(mutationEvent -> {
                return !MutationEvent.Source.SYNC_ENGINE.equals(mutationEvent.source());
            });
    }

    @NonNull
    <T extends Model, M extends MutationEvent<T>> Single<M> remove(final M mutationEvent) {
        return Single.defer(() -> Single.create(subscriber -> {
            localStorageAdapter.delete(mutationEvent, new ResultListener<MutationEvent<M>>() {
                @Override
                public void onResult(final MutationEvent<M> mutationOfMutation) {
                    subscriber.onSuccess(mutationOfMutation.data());
                }

                @Override
                public void onError(final Throwable error) {
                    subscriber.onError(error);
                }
            });
        }));
    }

    @SuppressWarnings("rawtypes") // TODO: Use Class<MutationEvent<?>>, not Class<MutationEvent>.
    private Observable<MutationEvent<?>> previouslyUnhandledMutations() {
        return Observable.defer(() -> Observable.create(emitter -> {
            localStorageAdapter.query(MutationEvent.class, new ResultListener<Iterator<MutationEvent>>() {
                @Override
                public void onResult(final Iterator<MutationEvent> result) {
                    while (result.hasNext()) {
                        emitter.onNext(result.next());
                    }
                    emitter.onComplete();
                }

                @Override
                public void onError(final Throwable error) {
                    emitter.onError(error);
                }
            });
        }));
    }
}
