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

import com.amplifyframework.core.model.Model;

/**
 * Container class to hold an instance of an object with it's metadata.
 * @param <M> The model represented by this container
 */
public class ModelWithMetadata<M extends Model> {
    private M model;
    private ModelMetadata syncMetadata;

    /**
     * Holds an instance of a model in one object and its sync metadata in another.
     * @param model An instance of a model
     * @param syncMetadata The metadata for this model about it's synchronization history.
     */
    public ModelWithMetadata(M model, ModelMetadata syncMetadata) {
        this.model = model;
        this.syncMetadata = syncMetadata;
    }

    /**
     * Get the model instance.
     * @return the model instance
     */
    public M getModel() {
        return model;
    }

    /**
     * Get the sync/version metadata for the model instance.
     * @return the sync/version metadata
     */
    public ModelMetadata getSyncMetadata() {
        return syncMetadata;
    }
}
