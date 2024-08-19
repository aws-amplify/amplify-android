/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.core.model.Model
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

/*
 * The {@link MutationOutbox} is a persistently-backed in-order staging ground
 * for changes that have already occurred in the storage adapter, and need
 * to be synchronized with a remote GraphQL API, via (a) GraphQL mutation(s).
 *
 * This component is an "offline mutation queue,"; items in the mutation outbox are observed,
 * and written out over the network. When an item is written out over the network successfully,
 * it is safe to remove it from this outbox.
 */
internal interface MutationOutbox {
    /**
     * Loads any/all previously unhandled mutations, from disk, into memory.
     * The [PendingMutation]s are taken out of durable storage.
     * That storage may still contain mutations, perhaps remaining from a previously-terminated session.
     * These mutations should be processed whenever the [Orchestrator] comes online.
     * @return A Completable which succeeds when all mutations have been read from disk.
     */
    fun load(): Completable

    /**
     * Returns a set of ids for the provided models that have pending mutations
     *
     * @param T Model Type
     * @param models list of Models to search for p ending mutations
     * @param modelClass The fully qualified class name of the models for which you want to check
     * @param excludeInFlight Set true to exclude in flight mutations from the returned Set
     * pending mutations. This should match the name returned by the model's
     * getClass().getName() method.
     * @return set of model ids that contained pending mutations
     */
    fun <T : Model> fetchPendingMutations(models: List<T>, modelClass: String, excludeInFlight: Boolean): Set<String>

    /**
     * Write a new [PendingMutation] into the outbox.
     *
     *
     * This involves:
     *
     *
     * 1. Writing a [PendingMutation] into a persistent store, by first converting it
     * to and [PendingMutation.PersistentRecord].
     *
     *
     * 2. Notifying the observers of the outbox that there is a new [PendingMutation]
     * that needs to be processed.
     *
     * @param incomingMutation A mutation to be enqueued into the outbox
     * @param <T>              The type of model to which the mutation refers; e.g., if the PendingMutation
     * is intending to create Person object, this type could be Person.
     * @return A Completable that emits success upon successful enqueue, or failure if it is not
     * possible to enqueue the mutation
     </T> */
    fun <T : Model> enqueue(incomingMutation: PendingMutation<T>): Completable

    /**
     * Remove an item from the outbox. The [SyncProcessor] calls this after it successfully
     * publishes an update over the network.
     * @param pendingMutationId ID of a mutation that has been processed, and can be removed from the outbox
     * @return A Completable which completes when the requested PendingMutation is deleted,
     * -- or, emits an error, if unable to delete it
     */
    fun remove(pendingMutationId: TimeBasedUuid): Completable

    /**
     * Take a peek at the next item in the outbox.
     * @return The next pending mutation, if there is one. Null otherwise.
     */
    fun peek(): PendingMutation<out Model>?

    /**
     * Marks a pending mutation as "in-flight." An in-flight mutation becomes
     * frozen to any further modifications, until it can be removed from the outbox, entirely.
     * Mutations enter this state while they are being processed, and published the cloud.
     * Mutations leave this state when the are removed from the outbox.
     * The "in-flight" status is NOT persisted, by design. When the system restarts,
     * no mutation is regarded as "in-flight."
     * @param pendingMutationId The ID of a mutation that is in the outbox, and will be marked as in-flight
     * @return A completable which completes with success if the existing mutation is successfully marked
     * as in-flight. If it's already in-flight, still completes successfully. If there is no such
     * mutation in the outbox, the completable will emit an [DataStoreException].
     */
    fun markInFlight(pendingMutationId: TimeBasedUuid): Completable

    /**
     * Observe the enqueue events that occur in the mutation outbox.
     * When one is received, a consumer should inspect [.peek],
     * to consume the pending mutations in the outbox. Note that it is possible
     * that the contents of the outbox has changed since the event was dispatched.
     * @return A stream of [OutboxEvent]
     */
    fun events(): Observable<OutboxEvent>

    /**
     * Different types of events that may occur in the [MutationOutbox].
     */
    enum class OutboxEvent {
        /**
         * There is content available in the Mutation Outbox.
         */
        CONTENT_AVAILABLE
    }
}
