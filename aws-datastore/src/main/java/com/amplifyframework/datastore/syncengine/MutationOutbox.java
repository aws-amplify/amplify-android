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
import androidx.annotation.WorkerThread;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.logging.Logger;

import java.util.Objects;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

/*
 * The {@link MutationOutbox} is a persistently-backed in-order staging ground
 * for changes that have already occurred in the storage adapter, and need
 * to be synchronized with a remote GraphQL API, via (a) GraphQL mutation(s).
 *
 * This component is an "offline mutation queue,"; items in the mutation outbox are observed,
 * and written out over the network. When an item is written out over the network successfully,
 * it is safe to remove it from this outbox.
 */
final class MutationOutbox {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final LocalStorageAdapter storage;
    private final PublishSubject<PendingMutation<? extends Model>> pendingMutations;
    private final PendingMutation.Converter converter;

    MutationOutbox(@NonNull final LocalStorageAdapter localStorageAdapter) {
        this.storage = Objects.requireNonNull(localStorageAdapter);
        this.pendingMutations = PublishSubject.create();
        this.converter = new GsonPendingMutationConverter();
    }

    /**
     * Checks to see if there is a pending mutation for a model with the given ID.
     * @param modelId ID of any model in the system
     * @return An {@link Single} which emits true if there is a pending mutation for
     *         the model id, emits false if not, and emits error if not determinable
     */
    @NonNull
    Single<Boolean> hasPendingMutation(@NonNull String modelId) {
        Objects.requireNonNull(modelId);
        return Single.create(emitter ->
            storage.query(PendingMutation.PersistentRecord.class, Where.id(modelId),
                results -> emitter.onSuccess(results.hasNext()),
                emitter::onError
            )
        );
    }

    /**
     * Write a new {@link PendingMutation} into the outbox.
     *
     * This involves:
     *
     *   1. Writing a {@link PendingMutation} into a persistent store, by first converting it
     *      to and {@link PendingMutation.PersistentRecord}.
     *
     *   2. Notifying the observers of the outbox that there is a new {@link PendingMutation}
     *      that needs to be processed.
     *
     * @param pendingMutation A mutation to be enqueued into the outbox
     * @param <T> The type of model to which the mutation refers; e.g., if the PendingMutation
     *            is intending to create Person object, this type could be Person.
     * @return A Completable that emits success upon successful enqueue, or failure if it is not
     *         possible to enqueue the mutation
     */
    @NonNull
    <T extends Model> Completable enqueue(@NonNull PendingMutation<T> pendingMutation) {
        Objects.requireNonNull(pendingMutation);

        // defer() the creation of a Completable, until someone subscribes enqueue().
        // When they do, create() a Completable that wraps a save() call to LocalStorageAdapter.
        return Completable.defer(() -> Completable.create(subscriber -> {
            // Convert the PendingMutation (that we want to store) into a Record
            PendingMutation.PersistentRecord record = converter.toRecord(pendingMutation);
            // Save it.
            storage.save(record, StorageItemChange.Initiator.SYNC_ENGINE,
                saved -> {
                    // The return value is a record that we saved a record.
                    // So, we would have to "unwrap" it, to get the item we saved, out.
                    // Forget that. We know the save succeeded, so just emit the
                    // original thing enqueue() got as a param.
                    LOG.info("Successfully enqueued " + pendingMutation);
                    pendingMutations.onNext(pendingMutation);
                    subscriber.onComplete();
                },
                error -> {
                    pendingMutations.onError(error);
                    subscriber.onError(error);
                }
            );
        }));
    }

    /**
     * Observe the {@link MutationOutbox}, for newly enqueued {@link PendingMutation}s.
     * The {@link SyncProcessor} may invoke this method to consume items out of the outbox. After
     * processing an item on this observable, that item should be removed from the
     * MutationOutbox via {@link #remove(PendingMutation)}.
     * @return An observable stream of items that have yet to be published via the network
     */
    @WorkerThread
    @NonNull
    Observable<PendingMutation<? extends Model>> observe() {
        return pendingMutations
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .startWith(previouslyUnprocessedMutations());
    }

    /**
     * Remove an item from the outbox. The {@link SyncProcessor} calls this after it successfully
     * publishes an update over the network.
     * @param pendingMutation A mutation that has been processed, and can be removed from the outbox
     * @param <T> Type of model to which the mutation makes reference; for example, if the mutation is
     *            to create a Person, T is Person.
     * @return A Completable which completes when the requests PendingMutation is deleted,
     *         -- or, emits an error, if unable to delete it
     */
    @NonNull
    <T extends Model> Completable remove(PendingMutation<T> pendingMutation) {
        // defer() creation of the Completable until someone subscribe()s via remove() method.
        // At that time, create() a Completable that wraps a call to LocalStorageAdapter#delete(...).
        return Completable.defer(() -> Completable.create(subscriber -> {
            // Convert the PendingMutation into a Record
            PendingMutation.PersistentRecord record = converter.toRecord(pendingMutation);
            // Delete it.
            storage.delete(
                record,
                StorageItemChange.Initiator.SYNC_ENGINE,
                ignored -> subscriber.onComplete(),
                subscriber::onError
            );
        }));
    }

    /**
     * Builds an Observable stream onto which any/all previously unhandled mutations are emitted.
     * The {@link PendingMutation}s on this stream are taken out of durable storage.
     * That storage may still contain mutations, perhaps remaining from a previously-terminated session.
     * This contents of this stream should be processed whenever the {@link Orchestrator} comes online.
     * @return An observable stream of mutations that weren't handled in the last session
     */
    private Observable<PendingMutation<? extends Model>> previouslyUnprocessedMutations() {
        // defer() creation of this Observable until someone subscribe()s to previouslyUnprocessedMutations()
        // when they do, respond by create()ing an Observable which emits the results of a
        // query to LocalStorageAdapter, for any existing instances of PendingMutation.Record.
        return Observable.defer(() -> Observable.create(emitter ->
            storage.query(PendingMutation.PersistentRecord.class,
                results -> {
                    while (results.hasNext()) {
                        try {
                            emitter.onNext(converter.fromRecord(results.next()));
                        } catch (DataStoreException conversionFailure) {
                            emitter.onError(conversionFailure);
                        }
                    }
                    emitter.onComplete();
                },
                emitter::onError
            )
        ));
    }
}
