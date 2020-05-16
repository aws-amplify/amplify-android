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

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;

import java.util.Objects;

import io.reactivex.Completable;

/**
 * The merger is responsible for merging cloud data back into the local store.
 * Subscriptions, Mutations, and Syncs, all generate cloud-side data models.
 * All of these sources must be rectified with the current state of the local storage.
 * This is the purpose of the merger.
 */
final class Merger {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private final MutationOutbox mutationOutbox;
    private final LocalStorageAdapter localStorageAdapter;

    /**
     * Constructs a Merger.
     * @param localStorageAdapter A local storage adapter
     */
    Merger(@NonNull MutationOutbox mutationOutbox, @NonNull LocalStorageAdapter localStorageAdapter) {
        this.mutationOutbox = Objects.requireNonNull(mutationOutbox);
        this.localStorageAdapter = Objects.requireNonNull(localStorageAdapter);
    }

    /**
     * Merge an item back into the local store.
     * TODO: check the state of the mutation outbox, before accessing the local storage.
     * @param modelWithMetadata A model, combined with metadata about it
     * @param <T> Type of model
     * @return A completable operation to merge the item
     */
    <T extends Model> Completable merge(ModelWithMetadata<T> modelWithMetadata) {
        ModelMetadata metadata = modelWithMetadata.getSyncMetadata();
        boolean isDelete = Boolean.TRUE.equals(metadata.isDeleted());
        T model = modelWithMetadata.getModel();

        // Check if there is a pending mutation for this model, in the outbox.
        return mutationOutbox.hasPendingMutation(model.getId())
            // If there is *not* a pending mutation, we will proceed. Otherwise, not.
            .filter(value -> !Boolean.TRUE.equals(value))
            .ignoreElement() // ignore the always-`false` element
            // Apply the model change (delete/save)
            .andThen(isDelete ? delete(model) : save(model))
            // Update the metadata for it
            .andThen(save(metadata))
            // Let the world know that we've done a good thing.
            .doOnComplete(() -> {
                announceSuccessfulMerge(modelWithMetadata);
                LOG.verbose("Remote model update was sync'd down into local storage: " + modelWithMetadata);
            })
            .doOnError(failure ->
                LOG.warn("Failed to sync remote model into local storage: " + modelWithMetadata)
            );
    }

    /**
     * Announce a successful merge over Hub.
     * @param modelWithMetadata Model with metadata that was successfully merged
     * @param <T> Type of model
     */
    private <T extends Model> void announceSuccessfulMerge(ModelWithMetadata<T> modelWithMetadata) {
        Amplify.Hub.publish(HubChannel.DATASTORE,
            HubEvent.create(DataStoreChannelEventName.RECEIVED_FROM_CLOUD, modelWithMetadata)
        );
    }

    // Delete a model.
    private <T extends Model> Completable delete(T model) {
        return Completable.defer(() -> Completable.create(emitter -> {
            // First, check if the thing exists.
            // If we don't, we'll get an exception saying basically,
            // "failed to delete a non-existing thing."
            ifPresent(model.getClass(), model.getId(),
                () -> localStorageAdapter.delete(
                    model,
                    StorageItemChange.Initiator.SYNC_ENGINE,
                    ignored -> emitter.onComplete(),
                    emitter::onError
                ),
                emitter::onComplete
            );
        }));
    }

    // Create or update a model.
    private <T extends Model> Completable save(T model) {
        return Completable.defer(() -> Completable.create(emitter ->
            localStorageAdapter.save(
                model,
                StorageItemChange.Initiator.SYNC_ENGINE,
                ignored -> emitter.onComplete(),
                emitter::onError
            )
        ));
    }

    /**
     * If the DataStore contains an item of the given class and with the given ID,
     * then perform an action. Otherwise, perform some other action.
     * @param clazz Search for this class in the DataStore
     * @param modelId Search for an item with this ID in the DataStore
     * @param onPresent If there is a match, perform this action
     * @param onNotPresent If there is NOT a match, perform this action as a fallback
     * @param <T> The type of item being searched
     */
    private <T extends Model> void ifPresent(
            Class<T> clazz, String modelId, Action onPresent, Action onNotPresent) {
        localStorageAdapter.query(clazz, Where.id(modelId), iterator -> {
            if (iterator.hasNext()) {
                onPresent.call();
            } else {
                onNotPresent.call();
            }
        }, failure -> onNotPresent.call());
    }
}
