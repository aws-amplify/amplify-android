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
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.logging.Logger;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/*
 * The {@link MutationOutbox} is a persistently-backed in-order staging ground
 * for changes that have already occurred in the storage adapter, and need
 * to be synchronized with a remote GraphQL API, via (a) GraphQL mutation(s).
 *
 * This component is an "offline mutation queue,"; items in the mutation outbox are observed,
 * and written out over the network. When an item is written out over the network successfully,
 * it is safe to remove it from this outbox.
 */
final class PersistentMutationOutbox implements MutationOutbox {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final LocalStorageAdapter storage;
    private final ConcurrentHashMap<String, PendingMutation<? extends Model>> mutationsByModelId;
    private final Queue<PendingMutation<? extends Model>> mutationQueue;
    private final PendingMutation.Converter converter;
    private final Subject<EnqueueEvent> events;
    private final Semaphore semaphore;

    PersistentMutationOutbox(@NonNull final LocalStorageAdapter localStorageAdapter) {
        this.storage = Objects.requireNonNull(localStorageAdapter);
        this.mutationsByModelId = new ConcurrentHashMap<>();
        this.mutationQueue = new LinkedList<>();
        this.converter = new GsonPendingMutationConverter();
        this.events = PublishSubject.<EnqueueEvent>create().toSerialized();
        this.semaphore = new Semaphore(1);
    }

    @Override
    public boolean hasPendingMutation(@NonNull String modelId) {
        Objects.requireNonNull(modelId);
        return mutationsByModelId.containsKey(modelId);
    }

    @NonNull
    @Override
    public <T extends Model> Completable enqueue(@NonNull PendingMutation<T> incomingMutation) {
        Objects.requireNonNull(incomingMutation);
        // If there is no existing mutation for the model, then just apply the incoming
        // mutation, and be done with this.
        String modelId = incomingMutation.getMutatedItem().getId();
        @SuppressWarnings("unchecked") // Model id is the same, so class type should be, too.
        PendingMutation<T> existingMutation = (PendingMutation<T>) mutationsByModelId.get(modelId);
        if (existingMutation == null) {
            return save(incomingMutation)
                .andThen(emit(EnqueueEvent.ITEM_ADDED));
        }
        switch (incomingMutation.getMutationType()) {
            case CREATE:
                return handleCreation();
            case UPDATE:
                return handleUpdate(existingMutation, incomingMutation);
            case DELETE:
                return handleDeletion(existingMutation, incomingMutation);
            default:
                return unknownMutationType(incomingMutation.getMutationType());
        }
    }

    private Completable handleCreation() {
        return Completable.error(new DataStoreException(
            "Attempted to enqueue a model creation, but there is already a pending mutation for that model ID.",
            "Please report at https://github.com/aws-amplify/amplify-android/issues."
        ));
    }

    private <T extends Model> Completable handleUpdate(
            PendingMutation<T> existingMutation, PendingMutation<T> incomingMutation) {
        switch (existingMutation.getMutationType()) {
            case CREATE:
            case UPDATE:
                return overwriteExisting(existingMutation, incomingMutation, existingMutation.getMutationType());
            case DELETE:
                return Completable.error(new DataStoreException(
                    "Attempted to enqueue a model mutation, but that model already had a delete mutation pending.",
                    "This should not be possible. Please report on GitHub issues."
                ));
            default:
                return unknownMutationType(existingMutation.getMutationType());
        }
    }

    private <T extends Model> Completable handleDeletion(
            PendingMutation<T> existingMutation, PendingMutation<T> incomingMutation) {
        switch (existingMutation.getMutationType()) {
            case CREATE:
                return remove(existingMutation);
            case UPDATE:
            case DELETE:
                return overwriteExisting(existingMutation, incomingMutation, PendingMutation.Type.DELETE);
            default:
                return Completable.complete();
        }
    }

    private Completable unknownMutationType(PendingMutation.Type unknownType) {
        return Completable.error(new DataStoreException(
            "Existing mutation of unknown type = " + unknownType,
            "Please report at https://github.com/aws-amplify/amplify-android/issues."
        ));
    }

    // Type is sometimes the existing type, sometimes the incoming type.
    private <T extends Model> Completable overwriteExisting(
            PendingMutation<T> existingMutation, PendingMutation<T> incomingMutation, PendingMutation.Type type) {
        // Keep the old mutation ID, but update the contents of that mutation.
        // Now, it will have the contents of the incoming update mutation.
        TimeBasedUuid id = existingMutation.getMutationId();
        T item = incomingMutation.getMutatedItem();
        Class<T> clazz = incomingMutation.getClassOfMutatedItem();
        return save(PendingMutation.instance(id, item, clazz, type))
            .andThen(emit(EnqueueEvent.ITEM_UPDATED));
    }

    private <T extends Model> Completable save(PendingMutation<T> pendingMutation) {
        return Completable.defer(() -> Completable.create(subscriber -> {
            semaphore.acquire();
            storage.save(
                converter.toRecord(pendingMutation),
                StorageItemChange.Initiator.SYNC_ENGINE,
                saved -> {
                    // The return value is StorageItemChange, referring to a PersistentRecord
                    // that was saved. We could "unwrap" a PendingMutation from that PersistentRecord,
                    // to get identically the thing that was saved. But we know the save succeeded.
                    // So, let's skip the unwrapping, and use the thing that was enqueued,
                    // the pendingMutation, directly.
                    String modelId = pendingMutation.getMutatedItem().getId();
                    mutationsByModelId.put(modelId, pendingMutation);
                    removeFromQueue(modelId);
                    mutationQueue.offer(pendingMutation);
                    LOG.info("Successfully enqueued " + pendingMutation);
                    semaphore.release();
                    subscriber.onComplete();
                },
                failure -> {
                    semaphore.release();
                    subscriber.onError(failure);
                }
            );
        }));
    }

    @NonNull
    @Override
    public <T extends Model> Completable remove(@NonNull PendingMutation<T> pendingMutation) {
        Objects.requireNonNull(pendingMutation);
        return Completable.defer(() -> Completable.create(subscriber -> {
            semaphore.acquire();
            storage.delete(
                converter.toRecord(pendingMutation),
                StorageItemChange.Initiator.SYNC_ENGINE,
                ignored -> {
                    String modelId = pendingMutation.getMutatedItem().getId();
                    mutationsByModelId.remove(modelId);
                    removeFromQueue(modelId);
                    LOG.info("Successfully removed from mutations outbox" + pendingMutation);
                    semaphore.release();
                    subscriber.onComplete();
                },
                failure -> {
                    semaphore.release();
                    subscriber.onError(failure);
                }
            );
        }));
    }

    private void removeFromQueue(String modelId) {
        Iterator<PendingMutation<? extends Model>> iterator = mutationQueue.iterator();
        while (iterator.hasNext()) {
            PendingMutation<? extends Model> next = iterator.next();
            if (ObjectsCompat.equals(modelId, next.getMutatedItem().getId())) {
                iterator.remove();
            }
        }
    }

    @NonNull
    @Override
    public Completable load() {
        return Completable.defer(() -> Completable.create(emitter -> {
            semaphore.acquire();
            mutationQueue.clear();
            mutationsByModelId.clear();
            storage.query(PendingMutation.PersistentRecord.class,
                results -> {
                    while (results.hasNext()) {
                        try {
                            PendingMutation<? extends Model> mutation = converter.fromRecord(results.next());
                            String modelId = mutation.getMutatedItem().getId();
                            mutationQueue.offer(mutation);
                            mutationsByModelId.put(modelId, mutation);
                        } catch (DataStoreException conversionFailure) {
                            semaphore.release();
                            emitter.onError(conversionFailure);
                            return;
                        }
                    }
                    semaphore.release();
                    emitter.onComplete();
                },
                failure -> {
                    semaphore.release();
                    emitter.onError(failure);
                }
            );
        }));
    }

    @NonNull
    @Override
    public Observable<EnqueueEvent> events() {
        return events;
    }

    private Completable emit(EnqueueEvent event) {
        return Completable.fromAction(() -> events.onNext(event));
    }

    @Nullable
    @Override
    public PendingMutation<? extends Model> peek() {
        return mutationQueue.peek();
    }
}
