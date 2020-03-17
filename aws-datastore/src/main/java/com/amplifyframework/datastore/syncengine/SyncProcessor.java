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

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.logging.Logger;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

/**
 * "Hydrates" the local DataStore, using model metadata receive from the
 * {@link RemoteModelState}. Hydration refers to populating the local storage
 * with values from a remote system.
 *
 * TODO: the sync processor should save items via the Merger, not directly
 * into the {@link LocalStorageAdapter} as it is currently.
 */
final class SyncProcessor {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private final RemoteModelState remoteModelState;
    private final LocalStorageAdapter localStorageAdapter;
    private final ModelProvider modelProvider;
    private final ModelSchemaRegistry modelSchemaRegistry;

    SyncProcessor(
            RemoteModelState remoteModelState,
            LocalStorageAdapter localStorageAdapter,
            ModelProvider modelProvider,
            ModelSchemaRegistry modelSchemaRegistry) {
        this.remoteModelState = remoteModelState;
        this.localStorageAdapter = localStorageAdapter;
        this.modelProvider = modelProvider;
        this.modelSchemaRegistry = modelSchemaRegistry;
    }

    /**
     * The task of hydrating the DataStore either succeeds (with no return value),
     * or it fails, with an explanation.
     * @return An Rx {@link Completable} which can be used to perform the operation.
     */
    Completable hydrate() {
        ModelWithMetadataComparator modelWithMetadataComparator =
            new ModelWithMetadataComparator(modelProvider, modelSchemaRegistry);

        // Observe the remote model states,
        return remoteModelState.observe()
            .sorted(modelWithMetadataComparator::compare)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            // For each model state, provide it as an input to a completable operation
            .flatMapCompletable(modelWithMetadata -> {
                // Save the metadata if the data save succeeds
                LOG.info("Base sync, saving " + modelWithMetadata);
                return saveItemToStorage(modelWithMetadata)
                    .andThen(saveMetadataToStorage(modelWithMetadata));
            });
    }

    private <T extends Model> Completable saveItemToStorage(ModelWithMetadata<T> modelWithMetadata) {
        return Completable.create(emitter -> {
            // Save the model portion
            final ModelMetadata metadata = modelWithMetadata.getSyncMetadata();
            final T item = modelWithMetadata.getModel();
            if (Boolean.TRUE.equals(metadata.isDeleted())) {
                ifPresent(item.getClass(), item.getId(), () ->
                    localStorageAdapter.delete(item, StorageItemChange.Initiator.SYNC_ENGINE,
                        ignoredRecord -> emitter.onComplete(), emitter::onError),
                    emitter::onComplete
                );
            } else {
                localStorageAdapter.save(item, StorageItemChange.Initiator.SYNC_ENGINE,
                    ignoredRecord -> emitter.onComplete(), emitter::onError);
            }
        });
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
        localStorageAdapter.query(clazz, QueryField.field("id").eq(modelId), iterator -> {
            if (iterator.hasNext()) {
                onPresent.call();
            } else {
                onNotPresent.call();
            }
        }, failure -> onNotPresent.call());
    }

    private <T extends Model> Completable saveMetadataToStorage(ModelWithMetadata<T> modelWithMetadata) {
        return Completable.create(emitter -> {
            // Save the metadata portion
            // This is separate from the model save since they have two distinct completions.
            final ModelMetadata metadata = modelWithMetadata.getSyncMetadata();
            localStorageAdapter.save(metadata, StorageItemChange.Initiator.SYNC_ENGINE,
                ignoredRecord -> emitter.onComplete(), emitter::onError);
        });
    }

    @SuppressWarnings("MethodTypeParameterName") // <MWM> is used; <M> could be mistaken for just Model.
    private static final class ModelWithMetadataComparator {
        private final ModelSchemaRegistry modelSchemaRegistry;
        private final TopologicalOrdering topologicalOrdering;

        ModelWithMetadataComparator(ModelProvider modelProvider, ModelSchemaRegistry modelSchemaRegistry) {
            this.modelSchemaRegistry = modelSchemaRegistry;
            this.topologicalOrdering =
                TopologicalOrdering.forRegisteredModels(modelSchemaRegistry, modelProvider);
        }

        private <MWM extends ModelWithMetadata<? extends Model>> int compare(MWM left, MWM right) {
            return topologicalOrdering.compare(schemaFor(left), schemaFor(right));
        }

        /**
         * Gets the model schema for a model.
         * @param modelWithMetadata A model with metadata about it
         * @param <MWM> Type of model
         * @return Model Schema for model
         */
        @NonNull
        private <MWM extends ModelWithMetadata<? extends Model>> ModelSchema schemaFor(MWM modelWithMetadata) {
            return modelSchemaRegistry.getModelSchemaForModelInstance(modelWithMetadata.getModel());
        }
    }
}
