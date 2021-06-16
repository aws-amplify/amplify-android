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
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.events.OutboxStatusEvent;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.logging.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Semaphore;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

/**
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
    private final MutationQueue mutationQueue;
    private final Set<TimeBasedUuid> inFlightMutations;
    private final PendingMutation.Converter converter;
    private final Subject<OutboxEvent> events;
    private final Semaphore semaphore;

    PersistentMutationOutbox(@NonNull final LocalStorageAdapter localStorageAdapter) {
        this(localStorageAdapter, new MutationQueue());
    }

    @VisibleForTesting
    PersistentMutationOutbox(@NonNull final LocalStorageAdapter localStorageAdapter,
                             @NonNull MutationQueue mutationQueue) {
        this.storage = Objects.requireNonNull(localStorageAdapter);
        this.mutationQueue = mutationQueue;
        this.inFlightMutations = new HashSet<>();
        this.converter = new GsonPendingMutationConverter();
        this.events = PublishSubject.<OutboxEvent>create().toSerialized();
        this.semaphore = new Semaphore(1);
    }

    @Override
    public boolean hasPendingMutation(@NonNull String modelId) {
        Objects.requireNonNull(modelId);
        return mutationQueue.nextMutationForModelId(modelId) != null;
    }

    @NonNull
    @Override
    public <T extends Model> Completable enqueue(@NonNull PendingMutation<T> incomingMutation) {
        Objects.requireNonNull(incomingMutation);
        return Completable.defer(() -> {
            // If there is no existing mutation for the model, then just apply the incoming
            // mutation, and be done with this.
            String modelId = incomingMutation.getMutatedItem().getId();
            @SuppressWarnings("unchecked")
            PendingMutation<T> existingMutation = (PendingMutation<T>) mutationQueue.nextMutationForModelId(modelId);
            if (existingMutation == null || inFlightMutations.contains(existingMutation.getMutationId())) {
                return save(incomingMutation)
                    .andThen(notifyContentAvailable());
            } else {
                return resolveConflict(existingMutation, incomingMutation);
            }
        })
        .doOnSubscribe(disposable -> semaphore.acquire())
        .doOnTerminate(semaphore::release);
    }

    private <T extends Model> Completable resolveConflict(@NonNull PendingMutation<T> existingMutation,
                                                          @NonNull PendingMutation<T> incomingMutation) {
        IncomingMutationConflictHandler<T> mutationConflictHandler =
            new IncomingMutationConflictHandler<>(existingMutation, incomingMutation);
        return mutationConflictHandler.resolve();
    }

    private <T extends Model> Completable save(PendingMutation<T> pendingMutation) {
        return Completable.create(emitter -> storage.save(
            converter.toRecord(pendingMutation),
            StorageItemChange.Initiator.SYNC_ENGINE,
            QueryPredicates.all(),
            saved -> {
                // The return value is StorageItemChange, referring to a PersistentRecord
                // that was saved. We could "unwrap" a PendingMutation from that PersistentRecord,
                // to get identically the thing that was saved. But we know the save succeeded.
                // So, let's skip the unwrapping, and use the thing that was enqueued,
                // the pendingMutation, directly.
                mutationQueue.updateExistingQueueItemOrAppendNew(pendingMutation.getMutationId(), pendingMutation);
                LOG.info("Successfully enqueued " + pendingMutation);
                announceEventEnqueued(pendingMutation);
                publishCurrentOutboxStatus();
                emitter.onComplete();
            },
            emitter::onError
        ));
    }

    @NonNull
    @Override
    public Completable remove(@NonNull TimeBasedUuid pendingMutationId) {
        return removeNotLocking(pendingMutationId)
            .doOnSubscribe(disposable -> semaphore.acquire())
            .doOnTerminate(semaphore::release);
    }

    @NonNull
    private Completable removeNotLocking(@NonNull TimeBasedUuid pendingMutationId) {
        Objects.requireNonNull(pendingMutationId);
        return Completable.defer(() -> {
            PendingMutation<? extends Model> pendingMutation = mutationQueue.getMutationById(pendingMutationId);
            if (pendingMutation == null) {
                throw new DataStoreException(
                    "Outbox was asked to remove a mutation with ID = " + pendingMutationId + ". " +
                        "However, there was no mutation with that ID in the outbox, to begin with.",
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
                );
            }
            return Maybe.<OutboxEvent>create(subscriber -> {
                storage.delete(
                    converter.toRecord(pendingMutation),
                    StorageItemChange.Initiator.SYNC_ENGINE,
                    QueryPredicates.all(),
                    ignored -> {
                        mutationQueue.removeById(pendingMutation.getMutationId());
                        inFlightMutations.remove(pendingMutationId);
                        LOG.info("Successfully removed from mutations outbox" + pendingMutation);
                        final boolean contentAvailable = !mutationQueue.isEmpty();
                        if (contentAvailable) {
                            subscriber.onSuccess(OutboxEvent.CONTENT_AVAILABLE);
                        } else {
                            subscriber.onComplete();
                        }
                    },
                    subscriber::onError
                );
            })
            .flatMapCompletable(contentAvailable -> notifyContentAvailable());
        });
    }

    @NonNull
    @Override
    public Completable load() {
        return Completable.create(emitter -> {
            inFlightMutations.clear();
            mutationQueue.clear();
            storage.query(PendingMutation.PersistentRecord.class, Where.matchesAll(),
                results -> {
                    while (results.hasNext()) {
                        try {
                            mutationQueue.add(converter.fromRecord(results.next()));
                        } catch (DataStoreException conversionFailure) {
                            emitter.onError(conversionFailure);
                            return;
                        }
                    }
                    // Publish outbox status upon loading
                    publishCurrentOutboxStatus();
                    emitter.onComplete();
                },
                emitter::onError
            );
        })
        .doOnSubscribe(disposable -> semaphore.acquire())
        .doOnTerminate(semaphore::release);
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

    @NonNull
    @Override
    public Completable markInFlight(@NonNull TimeBasedUuid pendingMutationId) {
        return Completable.create(emitter -> {
            PendingMutation<? extends Model> mutation = mutationQueue.getMutationById(pendingMutationId);
            if (mutation != null) {
                inFlightMutations.add(mutation.getMutationId());
                emitter.onComplete();
                return;
            }
            emitter.onError(new DataStoreException(
                "Outbox was asked to mark a mutation with ID = " + pendingMutationId + " as in-flight. " +
                    "However, there was no mutation with that ID in the outbox, to begin with.",
                AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            ));
        });
    }

    /**
     * Announce over hub that a mutation has been enqueued to the outbox.
     * @param pendingMutation A mutation that has been successfully enqueued to outbox
     * @param <T> Type of model
     */
    private <T extends Model> void announceEventEnqueued(PendingMutation<T> pendingMutation) {
        OutboxMutationEvent<T> mutationEvent = OutboxMutationEvent.fromPendingMutation(pendingMutation);
        Amplify.Hub.publish(HubChannel.DATASTORE, mutationEvent.toHubEvent());
    }

    /**
     * Publish current outbox status to hub.
     */
    private void publishCurrentOutboxStatus() {
        Amplify.Hub.publish(
            HubChannel.DATASTORE,
            new OutboxStatusEvent(mutationQueue.isEmpty()).toHubEvent()
        );
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
         * Constructor for a IncomingMutationConflictHandler.
         * @param existing The existing mutation.
         * @param incoming The incoming mutation.
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
            LOG.debug("IncomingMutationConflict - "
                + " existing " + existing.getMutationType()
                + " incoming " + incoming.getMutationType());
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
                case DELETE:
                case UPDATE:
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
                    // No condition needs to be provided, because as far as the remote store is concerned,
                    // we're simply performing the create (with the updated item item contents)
                    return overwriteExistingAndNotify(PendingMutation.Type.CREATE, QueryPredicates.all());
                case UPDATE:
                    if (QueryPredicates.all().equals(incoming.getPredicate())) {
                        // If the incoming update does not have a condition, we want to delete any
                        // existing mutations for the modelId before saving the incoming one.
                        try {
                            @SuppressWarnings("unchecked")
                            PendingMutation<T> mergedPendingMutation = (PendingMutation<T>) merge(incoming, existing);
                            return removeNotLocking(existing.getMutationId())
                                    .andThen(saveIncomingAndNotify(mergedPendingMutation));
                        } catch (Exception exception) {
                            LOG.error("Failed to merge, skip merging. exceptions: " + exception);
                            return removeNotLocking(existing.getMutationId()).andThen(saveIncomingAndNotify(incoming));
                        }
                    } else {
                        // If it has a condition, we want to just add it to the queue
                        return saveIncomingAndNotify(incoming);
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
                    //
                    if (inFlightMutations.contains(existing.getMutationId())) {
                        // Existing create is already in flight, then save the delete
                        return save(incoming);
                    } else {
                        // The existing create mutation hasn't made it to the remote store, so we
                        // ignore the incoming and remove the existing create mutation from outbox.
                        return removeNotLocking(existing.getMutationId());
                    }
                case UPDATE:
                case DELETE:
                    // If there's a pending update OR delete, we want to replace it with the incoming delete.
                    return overwriteExistingAndNotify(PendingMutation.Type.DELETE, incoming.getPredicate());
                default:
                    return unexpectedMutationScenario();
            }
        }

        private Completable overwriteExistingAndNotify(@NonNull PendingMutation.Type type,
                                                       @NonNull QueryPredicate predicate) {
            // Keep the old mutation ID, but update the contents of that mutation.
            // Now, it will have the contents of the incoming update mutation.
            TimeBasedUuid id = existing.getMutationId();
            T item = incoming.getMutatedItem();
            ModelSchema schema = incoming.getModelSchema();
            return save(PendingMutation.instance(id, item, schema, type, predicate))
                .andThen(notifyContentAvailable());
        }

        private Completable saveIncomingAndNotify(PendingMutation<T> incoming) {
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

        private PendingMutation<Model> merge(PendingMutation<T> incoming, PendingMutation<T> existing)
                throws AmplifyException {
            SerializedModel incomingSerializedModel = SerializedModel.create(incoming.getMutatedItem(),
                                                                            incoming.getModelSchema());
            SerializedModel existingSerializedModel = SerializedModel.create(existing.getMutatedItem(),
                                                                            existing.getModelSchema());
            Map<String, Object> mergedSerializedData = new HashMap<>();
            for (String key : incomingSerializedModel.getSerializedData().keySet()) {
                Object value = incomingSerializedModel.getSerializedData().get(key) != null ?
                        incomingSerializedModel.getSerializedData().get(key)
                        : existingSerializedModel.getSerializedData().get(key);
                mergedSerializedData.put(key, value);
            }
            SerializedModel mergedSerializedModel = SerializedModel.builder()
                    .serializedData(mergedSerializedData)
                    .modelSchema(incoming.getModelSchema())
                    .build();
            return PendingMutation.update(mergedSerializedModel, incoming.getModelSchema());
        }
    }
}
