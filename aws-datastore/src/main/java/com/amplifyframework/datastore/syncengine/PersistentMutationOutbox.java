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
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
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
    private final Subject<OutboxEvent> events;
    private final Semaphore semaphore;

    PersistentMutationOutbox(@NonNull final LocalStorageAdapter localStorageAdapter) {
        this.storage = Objects.requireNonNull(localStorageAdapter);
        this.mutationsByModelId = new ConcurrentHashMap<>();
        this.mutationQueue = new LinkedList<>();
        this.converter = new GsonPendingMutationConverter();
        this.events = PublishSubject.<OutboxEvent>create().toSerialized();
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
                .andThen(notifyContentAvailable());
        } else {
            // Figure out what needs to happen when there's a mutation for that model already present
            return resolveConflict(incomingMutation, existingMutation);
        }
    }

    private <T extends Model> Completable resolveConflict(@NonNull PendingMutation<T> existingMutation,
                                                          @NonNull PendingMutation<T> incomingMutation) {
        IncomingMutationConflictHandler<T> mutationConflictHandler =
            new IncomingMutationConflictHandler<>(existingMutation, incomingMutation);
        return mutationConflictHandler.resolve();
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
                    if (!mutationQueue.isEmpty()) {
                        notifyContentAvailable();
                    }
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
    public Observable<OutboxEvent> events() {
        return events;
    }

    private Completable notifyContentAvailable() {
        return Completable.fromAction(() -> events.onNext(OutboxEvent.CONTENT_AVAILABLE));
    }

    @Nullable
    @Override
    public PendingMutation<? extends Model> peek() {
        return mutationQueue.peek();
    }

    /**
     * Encapsulate the logic to determine which actions to take based on incoming and existing
     * mutations. Non-static so we can access instance methods of the outer class. Private because
     * we don't want this logic called from anywhere else.
     * @param <T> the model type
     */
    private final class IncomingMutationConflictHandler<T extends Model> {
        private final PendingMutation<T> existing;
        private final PendingMutation<T> incoming;

        /**
         * Constructor for a Pair.
         *
         * @param existing the first object in the Pair
         * @param incoming the second object in the pair
         */
        private IncomingMutationConflictHandler(@NonNull PendingMutation<T> existing,
                                                @NonNull PendingMutation<T> incoming) {
            this.existing = existing;
            this.incoming = incoming;
        }

        /**
         * Handle the conflict based on the incoming mutation type.
         * @return A completable with the actions to resolve the conflict.
         */
        Completable resolve() {
            switch (incoming.getMutationType()) {
                case CREATE:
                    return handleIncomingCreate();
                case UPDATE:
                    return handleIncomingUpdate();
                case DELETE:
                    return handleIncomingDelete();
                default:
                    return unknownMutationType(existing.getMutationType());
            }
        }

        /**
         * Determine which action to take when the incoming mutation type is {@linkplain PendingMutation.Type#CREATE}.
         * @return A completable with the actions needed to resolve the conflict
         */
        private Completable handleIncomingCreate() {
            switch (existing.getMutationType()) {
                case CREATE:
                    // Double create, return an different error than in the default block of the switch
                    // statement. This way, we can differentiate between an incoming create being processed
                    // multiple times (this case), versus outgoing mutations being processed out of order.
                    return conflictingCreationError();
                default:
                    // A create mutation should never show up after an update or delete for the same modelId.
                    return unexpectedMutationScenario();
            }
        }

        /**
         * Determine which action to take when the incoming mutation type is {@linkplain PendingMutation.Type#UPDATE}.
         * @return A completable with the actions needed to resolve the conflict
         */
        private Completable handleIncomingUpdate() {
            switch (existing.getMutationType()) {
                case CREATE:
                    // Update after the create -> replace item of the create mutation (and keep it as a create).
                    // No condition needs to be provided because as far as the remote store is concerned,
                    // we're simply performing the create (with the updated item item contents)
                    return overwriteExistingAndNotify(PendingMutation.Type.CREATE, null);
                case UPDATE:
                    if (incoming.getPredicate() == null) {
                        // If the incoming update does not have a condition, we want to delete any
                        // existing mutations for the modelId before saving the incoming one.
                        return remove(existing).andThen(saveIncomingAndNotify());
                    } else {
                        // If it has a condition, we want to just add it to the queue
                        return saveIncomingAndNotify();
                    }
                case DELETE:
                    // Incoming update after a delete -> throw exception
                    return modelAlreadyScheduledForDeletion();
                default:
                    return unexpectedMutationScenario();
            }
        }

        /**
         * Determine which action to take when the incoming mutation type is {@linkplain PendingMutation.Type#DELETE}.
         * @return A completable with the actions needed to resolve the conflict
         */
        private Completable handleIncomingDelete() {
            switch (existing.getMutationType()) {
                case CREATE:
                    // The existing create mutation hasn't made it to the remote store, so we ignore incoming
                    // and remove the existing create mutation from outbox.
                    return remove(existing);
                case UPDATE:
                case DELETE:
                    // If there's a pending update OR delete, we want to replace it with the incoming delete.
                    return overwriteExistingAndNotify(PendingMutation.Type.DELETE, incoming.getPredicate());
                default:
                    return unexpectedMutationScenario();
            }
        }

        private Completable overwriteExistingAndNotify(@NonNull PendingMutation.Type type,
                                                       @Nullable QueryPredicate predicate) {
            // Keep the old mutation ID, but update the contents of that mutation.
            // Now, it will have the contents of the incoming update mutation.
            TimeBasedUuid id = existing.getMutationId();
            T item = incoming.getMutatedItem();
            Class<T> clazz = incoming.getClassOfMutatedItem();
            return save(PendingMutation.instance(id, item, clazz, type, predicate))
                .andThen(notifyContentAvailable());
        }

        private Completable saveIncomingAndNotify() {
            return save(incoming)
                .andThen(notifyContentAvailable());
        }

        private Completable conflictingCreationError() {
            return Completable.error(new DataStoreException(
                "Attempted to enqueue a model creation, but there is already a pending creation for that model ID.",
                "Please report at https://github.com/aws-amplify/amplify-android/issues."
            ));
        }

        private Completable modelAlreadyScheduledForDeletion() {
            return Completable.error(new DataStoreException(
                "Attempted to enqueue a model mutation, but that model already had a delete mutation pending.",
                "This should not be possible. Please report on GitHub issues."
            ));
        }

        private Completable unknownMutationType(PendingMutation.Type unknownType) {
            return Completable.error(new DataStoreException(
                "Existing mutation of unknown type = " + unknownType,
                "Please report at https://github.com/aws-amplify/amplify-android/issues."
            ));
        }

        private Completable unexpectedMutationScenario() {
            return Completable.error(new DataStoreException(
                "Unable to handle existing mutation of type = " + existing.getMutationType() +
                " and incoming mutation of type = " + incoming.getMutationType(),
                "Please report at https://github.com/aws-amplify/amplify-android/issues."
            ));
        }
    }
}
