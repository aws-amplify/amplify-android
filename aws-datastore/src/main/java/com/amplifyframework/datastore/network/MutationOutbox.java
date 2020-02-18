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

import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.GsonStorageItemChangeConverter;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;

import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

/*
 * An {@link MutationOutbox} is a persistently-backed in-order staging ground
 * for changes that have already occurred in the storage adapter, and need
 * to be synchronized with a remote GraphQL API, via (a) GraphQL mutation(s).
 *
 * This component may also be thought of as an "offline mutation queue," except for
 * that the implementation doesn't store GraphQL primitives, it stores storage change
 * primitives. These are consumed and converted to GraphQL mutations, though.
 *
 * Items in the mutation outbox are observed, and written out over the network.
 * When a write completes successfully, it is safe to remove the corresponding item
 * from the outbox.
 */
// In this class, some lambdas look more readable w/ blocks
// The generics get crazy, so we break convention and use labels MODEL and SIC, not just M, S.
@SuppressWarnings({"CodeBlock2Expr", "checkstyle:MethodTypeParameterName"})
final class MutationOutbox {
    private final LocalStorageAdapter localStorageAdapter;
    private final PublishSubject<StorageItemChange<? extends Model>> pendingStorageItemChanges;
    private final GsonStorageItemChangeConverter storageItemChangeConverter;

    MutationOutbox(@NonNull final LocalStorageAdapter localStorageAdapter) {
        this.localStorageAdapter = Objects.requireNonNull(localStorageAdapter);
        this.pendingStorageItemChanges = PublishSubject.create();
        this.storageItemChangeConverter = new GsonStorageItemChangeConverter();
    }

    /**
     * Write a new {@link StorageItemChange} into the outbox.
     * This involves:
     *   1. Writing the {@link StorageItemChange.Record} into a persistent store
     *      (we use the storage adapter, again, for this). To make our lives easier,
     *      we first convert the {@link StorageItemChange} to a {@link StorageItemChange.Record},
     *      which is something the storage adapter can handle.
     *   2. Notifying the observers of the outbox that there is a new
     *      {@link StorageItemChange} that needs to be processed.
     * @param storageItemChange Storage item change to be placed into the outbox
     * @param <MODEL> Any Java type that extends {@link Model}
     * @param <SIC> Any Java type that extends {@link StorageItemChange} with template param of MODEL
     * @return A Single that emits the StorageItemChange that was put into the outbox, if successful,
     *         or emits error, if not.
     */
    @NonNull
    <MODEL extends Model, SIC extends StorageItemChange<MODEL>> Single<SIC> enqueue(
            @NonNull SIC storageItemChange) {
        Objects.requireNonNull(storageItemChange);

        // defer() the creation of a Single, until someone subscribes enqueue().
        // When they do, create() a single that wraps a save() call to LocalStorageAdapter.
        return Single.defer(() -> Single.create(subscriber -> {
            // Convert the storageItemChange (that we want to store) into a record
            StorageItemChange.Record record = storageItemChange.toRecord(storageItemChangeConverter);
            // Save it.
            localStorageAdapter.save(record, StorageItemChange.Initiator.SYNC_ENGINE,
                recordOfRecord -> {
                    // The return value is a record that we saved a record.
                    // So, we would have to "unwrap" it, to get the item we saved, out.
                    // Forget that. We know the save succeeded, so just emit the
                    // original thing enqueue() got as a param.
                    pendingStorageItemChanges.onNext(storageItemChange);
                    subscriber.onSuccess(storageItemChange);
                },
                error -> {
                    pendingStorageItemChanges.onError(error);
                    subscriber.onError(error);
                }
            );
        }));
    }

    /**
     * Observe the {@link MutationOutbox}, for new {@link StorageItemChange}s.
     * The Orchestrator may invoke this method to consume items out of the outbox. After
     * processing an item on this observable, that item should be removed from the
     * MutationOutbox.
     * @return An observable stream of items that have yet to be published via the network
     */
    @WorkerThread
    @NonNull
    Observable<StorageItemChange<? extends Model>> observe() {
        return pendingStorageItemChanges
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .startWith(previouslyUnprocessedChanges())
            .filter(storageItemChange -> {
                return !StorageItemChange.Initiator.SYNC_ENGINE.equals(storageItemChange.initiator());
            });
    }

    /**
     * Remove an item from the outbox. The sync engine calls this after it successfully
     * publishes an update over the network.
     * @param storageItemChange The item to remove from the outbox
     * @param <MODEL> Java type that extends {@link Model} type.
     * @param <SIC> Java type that extends a {@link StorageItemChange} with parameter type of MODEL.
     * @return A Single which will complete with the successfully deleted storageItemChange,
     *         of will error with a throwable cause, if the deletion fails
     */
    @NonNull
    <MODEL extends Model, SIC extends StorageItemChange<MODEL>> Single<SIC> remove(
            final SIC storageItemChange) {
        // defer() creation of the Single until someone subscribe()s via remove() method.
        // At that time, create() a Single that wraps a call to delete an item from the LocalStorageAdapter
        return Single.defer(() -> Single.create(subscriber -> {
            // Convert the storageItemChange into a record
            StorageItemChange.Record record = storageItemChange.toRecord(storageItemChangeConverter);
            // Delete it.
            localStorageAdapter.delete(
                record,
                StorageItemChange.Initiator.SYNC_ENGINE,
                recordOfRecord -> {
                    // The response is a record that we deleted a record.
                    // We would have to unpack the contained item (the record we deleted)
                    // So, forget that. Just return the copy we received via remove() method call.
                    subscriber.onSuccess(storageItemChange);
                },
                subscriber::onError
            );
        }));
    }

    /**
     * Builds an Observable stream onto which any/all unhandled storage changes are emitted.
     * This is effectively an Rx wrapper around a collection of query results. This should
     * be used when the Sync Engine is coming online for the first time.
     * @return An observable stream of unhandled changes
     */
    private Observable<StorageItemChange<? extends Model>> previouslyUnprocessedChanges() {
        // defer() creation of this Observable until someone subscribe()s to previouslyUnprocessedChanges()
        // when they do, respond by create()ing an Observable which emits the results of a
        // query to LocalStorageAdapter, for any existing StorageItemChange.Record.
        return Observable.defer(() -> Observable.create(emitter -> {
            localStorageAdapter.query(StorageItemChange.Record.class,
                queryResultsIterator -> {
                    while (queryResultsIterator.hasNext()) {
                        try {
                            final StorageItemChange<? extends Model> storageItemChange =
                                queryResultsIterator.next().toStorageItemChange(storageItemChangeConverter);
                            emitter.onNext(storageItemChange);
                        } catch (DataStoreException exception) {
                            emitter.onError(exception);
                        }
                    }
                    emitter.onComplete();
                },
                emitter::onError
            );
        }));
    }
}
