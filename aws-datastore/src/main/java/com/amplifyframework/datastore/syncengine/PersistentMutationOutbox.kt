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
package com.amplifyframework.datastore.syncengine

import androidx.annotation.VisibleForTesting
import com.amplifyframework.AmplifyException
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.core.model.SerializedModel
import com.amplifyframework.core.model.query.Page
import com.amplifyframework.core.model.query.Where
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.core.model.query.predicate.QueryPredicateGroup
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.events.OutboxStatusEvent
import com.amplifyframework.datastore.storage.LocalStorageAdapter
import com.amplifyframework.datastore.storage.StorageItemChange
import com.amplifyframework.datastore.syncengine.MutationOutbox.OutboxEvent
import com.amplifyframework.datastore.syncengine.PendingMutation.PersistentRecord
import com.amplifyframework.hub.HubChannel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.MaybeEmitter
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import java.util.Objects
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.runBlocking

/**
 * The [MutationOutbox] is a persistently-backed in-order staging ground
 * for changes that have already occurred in the storage adapter, and need
 * to be synchronized with a remote GraphQL API, via (a) GraphQL mutation(s).
 *
 * This component is an "offline mutation queue,"; items in the mutation outbox are observed,
 * and written out over the network. When an item is written out over the network successfully,
 * it is safe to remove it from this outbox.
 */
internal class PersistentMutationOutbox(private val storage: LocalStorageAdapter) : MutationOutbox {

    private val inFlightMutations: MutableSet<TimeBasedUuid> = HashSet()
    private val converter: PendingMutation.Converter = GsonPendingMutationConverter()
    private val events: Subject<OutboxEvent> = PublishSubject.create<OutboxEvent>().toSerialized()
    private val semaphore = Semaphore(1)
    private var countMutations = true
    private var loadedMutation: PendingMutation<out Model>? = null
    private var numMutationsInOutbox = 0

    @VisibleForTesting
    fun getMutationForModelId(modelId: String, modelClass: String): PendingMutation<out Model>? {
        val mutationResult = AtomicReference<PendingMutation<out Model>>()
        Completable.create { emitter: CompletableEmitter ->
            storage.query(
                PersistentRecord::class.java,
                Where.matches(PersistentRecord.CONTAINED_MODEL_ID.eq(modelId)),
                { results: Iterator<PersistentRecord> ->
                    if (results.hasNext()) {
                        try {
                            val persistentRecord = results.next()
                            val pendingMutation: PendingMutation<*> =
                                converter.fromRecord<Model>(persistentRecord)
                            if (pendingMutation.modelSchema.name == modelClass) {
                                mutationResult.set(pendingMutation)
                            }
                        } catch (throwable: Throwable) {
                            emitter.onError(throwable)
                        }
                    }
                    emitter.onComplete()
                },
                { t: DataStoreException ->
                    emitter.onError(t)
                }
            )
        }
            .doOnSubscribe { semaphore.acquire() }
            .doOnTerminate { semaphore.release() }
            .blockingAwait()
        return mutationResult.get()
    }

    override fun <T : Model> fetchPendingMutations(
        models: List<T>,
        modelClass: String,
        excludeInFlight: Boolean
    ): Set<String> {
        // We chunk sql query to 950 items to prevent hitting 1k sqlite predicate limit
        // Improvement would be to use IN, but not currently supported in our query builders
        semaphore.acquire()
        val pendingMutations: Set<String> = models.chunked(950).fold(mutableSetOf()) { acc, chunk ->
            val queryOptions = Where.matches(
                QueryPredicateGroup(
                    QueryPredicateGroup.Type.OR,
                    chunk.map {
                        PersistentRecord.CONTAINED_MODEL_ID.eq(it.primaryKeyString)
                    }
                )
            )

            // Fetch chunk list of sqlite results
            val chunkResult = runBlocking {
                suspendCoroutine { continuation ->
                    storage.query(
                        PersistentRecord::class.java,
                        queryOptions,
                        {
                            continuation.resume(it)
                        },
                        {
                            LOG.debug("Failed to query PersistentRecord outbox.")
                            continuation.resume(emptyList<PersistentRecord>().iterator())
                        }
                    )
                }
            }

            // Add id to the accumulator set
            chunkResult.forEach {
                val pendingMutation: PendingMutation<*> = converter.fromRecord<Model>(it)
                /*
                We need to make sure the model type matches since multiple models could share identical primary keys
                Additionally, if excludeInFlight is set, we remove in flight mutations at this time.
                 */
                if (pendingMutation.modelSchema.modelClass.name == modelClass &&
                    (excludeInFlight && !inFlightMutations.contains(pendingMutation.mutationId))
                ) {
                    acc.add(it.containedModelId)
                }
            }
            acc
        }
        semaphore.release()
        return pendingMutations
    }

    private fun getMutationById(mutationId: String): PendingMutation<out Model>? {
        val mutationResult = AtomicReference<PendingMutation<out Model>>()
        Completable.create { emitter: CompletableEmitter ->
            storage.query(
                PersistentRecord::class.java,
                Where.matches(PersistentRecord.ID.eq(mutationId)),
                { results: Iterator<PersistentRecord> ->
                    if (results.hasNext()) {
                        try {
                            val persistentRecord = results.next()
                            mutationResult.set(converter.fromRecord(persistentRecord))
                        } catch (throwable: Throwable) {
                            emitter.onError(throwable)
                        }
                    }
                    emitter.onComplete()
                },
                { t: DataStoreException ->
                    emitter.onError(t)
                }
            )
        }
            .doOnSubscribe { semaphore.acquire() }
            .doOnTerminate { semaphore.release() }
            .blockingAwait()
        return mutationResult.get()
    }

    override fun <T : Model> enqueue(incomingMutation: PendingMutation<T>): Completable {
        return Completable.defer {
            // If there is no existing mutation for the model, then just apply the incoming
            // mutation, and be done with this.
            val modelId = incomingMutation.mutatedItem.primaryKeyString
            val modelClass = incomingMutation.mutatedItem.modelName
            val existingMutation = getMutationForModelId(modelId, modelClass) as PendingMutation<T>?
            if (existingMutation == null || inFlightMutations.contains(existingMutation.mutationId)) {
                return@defer save<T>(incomingMutation, true)
                    .andThen(notifyContentAvailable())
            } else {
                return@defer resolveConflict<T>(existingMutation, incomingMutation)
            }
        }
            .doOnSubscribe { semaphore.acquire() }
            .doOnTerminate { semaphore.release() }
    }

    private fun <T : Model> resolveConflict(
        existingMutation: PendingMutation<T>,
        incomingMutation: PendingMutation<T>
    ): Completable {
        val mutationConflictHandler =
            IncomingMutationConflictHandler(existingMutation, incomingMutation)
        return mutationConflictHandler.resolve()
    }

    private fun <T : Model> save(
        pendingMutation: PendingMutation<T>,
        addingNewMutation: Boolean
    ): Completable {
        val item = converter.toRecord(pendingMutation)
        return Completable.create { emitter: CompletableEmitter ->
            storage.save(
                item,
                StorageItemChange.Initiator.SYNC_ENGINE,
                QueryPredicates.all(),
                {
                    // The return value is StorageItemChange, referring to a PersistentRecord
                    // that was saved. We could "unwrap" a PendingMutation from that PersistentRecord,
                    // to get identically the thing that was saved. But we know the save succeeded.
                    // So, let's skip the unwrapping, and use the thing that was enqueued,
                    // the pendingMutation, directly.
                    LOG.info("Successfully enqueued $pendingMutation")
                    if (addingNewMutation) {
                        numMutationsInOutbox += 1
                    }
                    announceEventEnqueued(pendingMutation)
                    publishCurrentOutboxStatus()
                    emitter.onComplete()
                },
                { t: DataStoreException ->
                    emitter.onError(t)
                }
            )
        }
    }

    override fun remove(pendingMutationId: TimeBasedUuid): Completable {
        return removeNotLocking(pendingMutationId)
            .doOnSubscribe { semaphore.acquire() }
            .doOnTerminate { semaphore.release() }
    }

    private fun removeNotLocking(pendingMutationId: TimeBasedUuid): Completable {
        Objects.requireNonNull(pendingMutationId)
        return Completable.defer {
            val pendingMutation = getMutationById(pendingMutationId.toString())
                ?: throw DataStoreException(
                    "Outbox was asked to remove a mutation with ID = " + pendingMutationId + ". " +
                        "However, there was no mutation with that ID in the outbox, to begin with.",
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
                )
            Maybe.create { subscriber: MaybeEmitter<OutboxEvent> ->
                storage.delete(
                    converter.toRecord(pendingMutation),
                    StorageItemChange.Initiator.SYNC_ENGINE,
                    QueryPredicates.all(),
                    {
                        inFlightMutations.remove(pendingMutationId)
                        LOG.info("Successfully removed from mutations outbox$pendingMutation")
                        numMutationsInOutbox -= 1
                        val contentAvailable = numMutationsInOutbox > 0
                        if (contentAvailable) {
                            subscriber.onSuccess(OutboxEvent.CONTENT_AVAILABLE)
                        } else {
                            subscriber.onComplete()
                        }
                    },
                    { t: DataStoreException ->
                        subscriber.onError(t)
                    }
                )
            }
                .flatMapCompletable { notifyContentAvailable() }
        }
    }

    override fun load(): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            inFlightMutations.clear()
            var queryOptions = Where.matchesAll()
            if (!countMutations) {
                queryOptions = queryOptions.paginated(Page.firstResult())
            }
            storage.query(
                PersistentRecord::class.java,
                queryOptions,
                { results: Iterator<PersistentRecord> ->
                    if (!results.hasNext()) {
                        loadedMutation = null
                        numMutationsInOutbox = 0
                    }
                    var firstResult = true
                    while (results.hasNext()) {
                        val persistentRecord = results.next()
                        if (firstResult) {
                            firstResult = false
                            loadedMutation = try {
                                converter.fromRecord<Model>(persistentRecord)
                            } catch (throwable: Throwable) {
                                emitter.onError(throwable)
                                return@query
                            }
                            numMutationsInOutbox = if (countMutations) {
                                0
                            } else {
                                break
                            }
                        }
                        if (countMutations) {
                            numMutationsInOutbox += 1
                        }
                    }
                    countMutations = false
                    // Publish outbox status upon loading
                    publishCurrentOutboxStatus()
                    emitter.onComplete()
                },
                { t: DataStoreException ->
                    emitter.onError(t)
                }
            )
        }
            .doOnSubscribe { semaphore.acquire() }
            .doOnTerminate { semaphore.release() }
    }

    override fun events(): Observable<OutboxEvent> {
        return events
    }

    private fun notifyContentAvailable(): Completable {
        return Completable.fromAction { events.onNext(OutboxEvent.CONTENT_AVAILABLE) }
    }

    override fun peek(): PendingMutation<out Model>? {
        load().blockingAwait()
        return loadedMutation
    }

    override fun markInFlight(pendingMutationId: TimeBasedUuid): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            val mutation = getMutationById(pendingMutationId.toString())
            if (mutation != null) {
                inFlightMutations.add(mutation.mutationId)
                emitter.onComplete()
                return@create
            }
            emitter.onError(
                DataStoreException(
                    "Outbox was asked to mark a mutation with ID = " + pendingMutationId + " as in-flight. " +
                        "However, there was no mutation with that ID in the outbox, to begin with.",
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
                )
            )
        }
    }

    /**
     * Announce over hub that a mutation has been enqueued to the outbox.
     * @param pendingMutation A mutation that has been successfully enqueued to outbox
     * @param <T> Type of model
     </T> */
    private fun <T : Model> announceEventEnqueued(pendingMutation: PendingMutation<T>) {
        val mutationEvent = OutboxMutationEvent.fromPendingMutation(pendingMutation)
        Amplify.Hub.publish(HubChannel.DATASTORE, mutationEvent.toHubEvent())
    }

    /**
     * Publish current outbox status to hub.
     */
    private fun publishCurrentOutboxStatus() {
        Amplify.Hub.publish(
            HubChannel.DATASTORE,
            OutboxStatusEvent(numMutationsInOutbox == 0).toHubEvent()
        )
    }

    /**
     * Encapsulate the logic to determine which actions to take based on incoming and existing
     * mutations. Non-static so we can access instance methods of the outer class. Private because
     * we don't want this logic called from anywhere else.
     * @param <T> the model type
     * @param existing The existing mutation.
     * @param incoming The incoming mutation.
     </T> */
    private inner class IncomingMutationConflictHandler<T : Model>(
        private val existing: PendingMutation<T>,
        private val incoming: PendingMutation<T>
    ) {
        /**
         * Handle the conflict based on the incoming mutation type.
         * @return A completable with the actions to resolve the conflict.
         */
        fun resolve(): Completable {
            LOG.debug(
                "IncomingMutationConflict - " +
                    " existing " + existing.mutationType +
                    " incoming " + incoming.mutationType
            )
            return when (incoming.mutationType) {
                PendingMutation.Type.CREATE -> handleIncomingCreate()
                PendingMutation.Type.UPDATE -> handleIncomingUpdate()
                PendingMutation.Type.DELETE -> handleIncomingDelete()
            }
        }

        /**
         * Determine which action to take when the incoming mutation type is [PendingMutation.Type.CREATE].
         * @return A completable with the actions needed to resolve the conflict
         */
        private fun handleIncomingCreate(): Completable {
            return when (existing.mutationType) {
                PendingMutation.Type.CREATE ->
                    // Double create, return an different error than in the default block of the switch
                    // statement. This way, we can differentiate between an incoming create being processed
                    // multiple times (this case), versus outgoing mutations being processed out of order.
                    conflictingCreationError()

                PendingMutation.Type.DELETE, PendingMutation.Type.UPDATE ->
                    // A create mutation should never show up after an update or delete for the same modelId.
                    unexpectedMutationScenario()
            }
        }

        /**
         * Determine which action to take when the incoming mutation type is [PendingMutation.Type.UPDATE].
         * @return A completable with the actions needed to resolve the conflict
         */
        private fun handleIncomingUpdate(): Completable {
            return when (existing.mutationType) {
                PendingMutation.Type.CREATE ->
                    // Update after the create -> if the incoming & existing is of type SerializedModel
                    // then merge the existing model.
                    // If not, then replace the item of the create mutation (and keep it as a create).
                    // No condition needs to be provided, because as far as the remote store is concerned,
                    // we're simply performing the create (with the updated item item contents)
                    if (incoming.mutatedItem is SerializedModel &&
                        existing.mutatedItem is SerializedModel
                    ) {
                        val mergedPendingMutation = mergeAndCreatePendingMutation(
                            incoming.mutatedItem as SerializedModel,
                            existing.mutatedItem as SerializedModel,
                            incoming.modelSchema,
                            PendingMutation.Type.CREATE
                        )
                        removeNotLocking(existing.mutationId)
                            .andThen(saveAndNotify(mergedPendingMutation, true))
                    } else {
                        overwriteExistingAndNotify(
                            PendingMutation.Type.CREATE,
                            QueryPredicates.all()
                        )
                    }

                PendingMutation.Type.UPDATE ->
                    // If the incoming update does not have a condition, we want to delete any
                    // existing mutations for the modelId before saving the incoming one.
                    if (QueryPredicates.all() == incoming.predicate) {
                        // If the incoming & existing update is of type serializedModel
                        // then merge the existing model data into incoming.
                        if (incoming.mutatedItem is SerializedModel &&
                            existing.mutatedItem is SerializedModel
                        ) {
                            val mergedPendingMutation = mergeAndCreatePendingMutation(
                                incoming.mutatedItem as SerializedModel,
                                existing.mutatedItem as SerializedModel,
                                incoming.modelSchema,
                                PendingMutation.Type.UPDATE
                            )
                            removeNotLocking(existing.mutationId)
                                .andThen(saveAndNotify(mergedPendingMutation, true))
                        } else {
                            removeNotLocking(existing.mutationId).andThen(
                                saveAndNotify(
                                    incoming,
                                    true
                                )
                            )
                        }
                    } else {
                        // If it has a condition, we want to just add it to the queue
                        saveAndNotify(incoming, true)
                    }

                PendingMutation.Type.DELETE ->
                    // Incoming update after a delete -> throw exception
                    modelAlreadyScheduledForDeletion()
            }
        }

        /**
         * Determine which action to take when the incoming mutation type is [PendingMutation.Type.DELETE].
         * @return A completable with the actions needed to resolve the conflict
         */
        private fun handleIncomingDelete(): Completable {
            return when (existing.mutationType) {
                PendingMutation.Type.CREATE ->
                    if (inFlightMutations.contains(existing.mutationId)) {
                        // Existing create is already in flight, then save the delete
                        save(incoming, true)
                    } else {
                        // The existing create mutation hasn't made it to the remote store, so we
                        // ignore the incoming and remove the existing create mutation from outbox.
                        removeNotLocking(existing.mutationId)
                    }

                PendingMutation.Type.UPDATE, PendingMutation.Type.DELETE ->
                    // If there's a pending update OR delete, we want to replace it with the incoming delete.
                    overwriteExistingAndNotify(PendingMutation.Type.DELETE, incoming.predicate)
            }
        }

        private fun overwriteExistingAndNotify(
            type: PendingMutation.Type,
            predicate: QueryPredicate
        ): Completable {
            // Keep the old mutation ID, but update the contents of that mutation.
            // Now, it will have the contents of the incoming update mutation.
            val id = existing.mutationId
            val item: T = incoming.mutatedItem
            val schema = incoming.modelSchema
            return save(PendingMutation.instance(id, item, schema, type, predicate), false)
                .andThen(notifyContentAvailable())
        }

        private fun saveAndNotify(
            incoming: PendingMutation<T>,
            addedNewMutation: Boolean
        ): Completable {
            return save(incoming, addedNewMutation)
                .andThen(notifyContentAvailable())
        }

        private fun conflictingCreationError(): Completable {
            return Completable.error(
                DataStoreException(
                    "Attempted to enqueue a model creation, but there is already a pending creation for that model ID.",
                    "Please report at https://github.com/aws-amplify/amplify-android/issues."
                )
            )
        }

        private fun modelAlreadyScheduledForDeletion(): Completable {
            return Completable.error(
                DataStoreException(
                    "Attempted to enqueue a model mutation, but that model already had a delete mutation pending.",
                    "This should not be possible. Please report on GitHub issues."
                )
            )
        }

        private fun unknownMutationType(unknownType: PendingMutation.Type): Completable {
            return Completable.error(
                DataStoreException(
                    "Existing mutation of unknown type = $unknownType",
                    "Please report at https://github.com/aws-amplify/amplify-android/issues."
                )
            )
        }

        private fun unexpectedMutationScenario(): Completable {
            return Completable.error(
                DataStoreException(
                    "Unable to handle existing mutation of type = " + existing.mutationType +
                        " and incoming mutation of type = " + incoming.mutationType,
                    "Please report at https://github.com/aws-amplify/amplify-android/issues."
                )
            )
        }

        private fun mergeAndCreatePendingMutation(
            incomingItem: SerializedModel,
            existingItem: SerializedModel,
            modelSchema: ModelSchema,
            type: PendingMutation.Type
        ): PendingMutation<T> {
            val mergedSerializedModel = SerializedModel.merge(
                incomingItem,
                existingItem,
                modelSchema
            )
            return PendingMutation.instance(
                mergedSerializedModel,
                modelSchema,
                type,
                QueryPredicates.all()
            ) as PendingMutation<T>
        }
    }

    companion object {
        private val LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore")
    }
}
