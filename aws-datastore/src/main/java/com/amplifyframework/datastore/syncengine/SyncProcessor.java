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

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;

import java.util.Objects;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

/**
 * "Hydrates" the local DataStore, using model metadata receive from the
 * {@link RemoteModelState}. Hydration refers to populating the local storage
 * with values from a remote system.
 *
 * For all items returned by the sync, merge them back into local storage through
 * the {@link Merger}.
 */
final class SyncProcessor {
    private final RemoteModelState remoteModelState;
    private final Merger merger;
    private final ModelProvider modelProvider;
    private final ModelSchemaRegistry modelSchemaRegistry;

    /**
     * Constructs a new SyncProcessor.
     * @param remoteModelState Provides an observable stream of the state of all remote models
     * @param merger Allows network data to be ingested back into the local repository
     * @param modelProvider Provides the set of model classes managed by this system
     * @param modelSchemaRegistry A registry of schema for all models managed by this system
     */
    SyncProcessor(
            @NonNull RemoteModelState remoteModelState,
            @NonNull Merger merger,
            @NonNull ModelProvider modelProvider,
            @NonNull ModelSchemaRegistry modelSchemaRegistry) {
        this.remoteModelState = Objects.requireNonNull(remoteModelState);
        this.merger = Objects.requireNonNull(merger);
        this.modelProvider = Objects.requireNonNull(modelProvider);
        this.modelSchemaRegistry = Objects.requireNonNull(modelSchemaRegistry);
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
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .sorted(modelWithMetadataComparator::compare)
            // For each ModelWithMetadata, merge it into the local store.
            .flatMapCompletable(merger::merge);
    }

    /**
     * Compares to {@link ModelWithMetadata}, according to the topological order
     * of the {@link Model} within each. Topological order is determined by the
     * {@link TopologicalOrdering} utility.
     */
    private static final class ModelWithMetadataComparator {
        private final ModelSchemaRegistry modelSchemaRegistry;
        private final TopologicalOrdering topologicalOrdering;

        ModelWithMetadataComparator(ModelProvider modelProvider, ModelSchemaRegistry modelSchemaRegistry) {
            this.modelSchemaRegistry = modelSchemaRegistry;
            this.topologicalOrdering =
                TopologicalOrdering.forRegisteredModels(modelSchemaRegistry, modelProvider);
        }

        private <M extends ModelWithMetadata<? extends Model>> int compare(M left, M right) {
            return topologicalOrdering.compare(schemaFor(left), schemaFor(right));
        }

        /**
         * Gets the model schema for a model.
         * @param modelWithMetadata A model with metadata about it
         * @param <M> Type for ModelWithMetadata containing arbitrary model instances
         * @return Model Schema for model
         */
        @NonNull
        private <M extends ModelWithMetadata<? extends Model>> ModelSchema schemaFor(M modelWithMetadata) {
            return modelSchemaRegistry.getModelSchemaForModelInstance(modelWithMetadata.getModel());
        }
    }
}
