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

package com.amplifyframework.datastore.syncengine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.model.Model;

import io.reactivex.Completable;
import io.reactivex.Observable;

/*
 * The {@link MutationOutbox} is a persistently-backed in-order staging ground
 * for changes that have already occurred in the storage adapter, and need
 * to be synchronized with a remote GraphQL API, via (a) GraphQL mutation(s).
 *
 * This component is an "offline mutation queue,"; items in the mutation outbox are observed,
 * and written out over the network. When an item is written out over the network successfully,
 * it is safe to remove it from this outbox.
 */
interface MutationOutbox {
    /**
     * Loads any/all previously unhandled mutations, from disk, into memory.
     * The {@link PendingMutation}s are taken out of durable storage.
     * That storage may still contain mutations, perhaps remaining from a previously-terminated session.
     * These mutations should be processed whenever the {@link Orchestrator} comes online.
     * @return A Completable which succeeds when all mutations have been read from disk.
     */
    @NonNull
    Completable load();

    /**
     * Checks to see if there is a pending mutation for a model with the given ID.
     *
     * @param modelId ID of any model in the system
     * @return true if there is a pending mutation for the model id, false if not.
     */
    boolean hasPendingMutation(@NonNull String modelId);

    /**
     * Write a new {@link PendingMutation} into the outbox.
     * <p>
     * This involves:
     * <p>
     * 1. Writing a {@link PendingMutation} into a persistent store, by first converting it
     * to and {@link PendingMutation.PersistentRecord}.
     * <p>
     * 2. Notifying the observers of the outbox that there is a new {@link PendingMutation}
     * that needs to be processed.
     *
     * @param incomingMutation A mutation to be enqueued into the outbox
     * @param <T>              The type of model to which the mutation refers; e.g., if the PendingMutation
     *                         is intending to create Person object, this type could be Person.
     * @return A Completable that emits success upon successful enqueue, or failure if it is not
     * possible to enqueue the mutation
     */
    @NonNull
    <T extends Model> Completable enqueue(@NonNull PendingMutation<T> incomingMutation);

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
    <T extends Model> Completable remove(@NonNull PendingMutation<T> pendingMutation);

    /**
     * Take a peek at the next item in the outbox.
     * @return The next pending mutation, if there is one. Null otherwise.
     */
    @Nullable
    PendingMutation<? extends Model> peek();

    /**
     * Observe the enqueue events that occur in the mutation outbox.
     * When one is received, a consumer should inspect {@link #peek()},
     * to consume the pending mutations in the outbox. Note that it is possible
     * that the contents of the outbox has changed since the event was dispatched.
     * @return A stream of {@link OutboxEvent}
     */
    @NonNull
    Observable<OutboxEvent> events();

    /**
     * Different types of events that may occur in the {@link MutationOutbox}.
     */
    enum OutboxEvent {
        /**
         * There is content available in the Mutation Outbox.
         */
        CONTENT_AVAILABLE
    }
}

