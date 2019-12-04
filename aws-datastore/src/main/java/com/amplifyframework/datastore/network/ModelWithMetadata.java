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

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;

import java.util.Objects;

/**
 * Container class to hold an instance of an object with it's metadata.
 * @param <M> The model represented by this container
 */
public final class ModelWithMetadata<M extends Model> {
    private M model;
    private ModelMetadata syncMetadata;

    /**
     * Holds an instance of a model in one object and its sync metadata in another.
     * @param model An instance of a model
     * @param syncMetadata The metadata for this model about it's synchronization history.
     */
    public ModelWithMetadata(@NonNull M model, @NonNull ModelMetadata syncMetadata) {
        this.model = Objects.requireNonNull(model);
        this.syncMetadata = Objects.requireNonNull(syncMetadata);
    }

    /**
     * Get the model instance.
     * @return the model instance
     */
    @NonNull
    public M getModel() {
        return model;
    }

    /**
     * Get the sync/version metadata for the model instance.
     * @return the sync/version metadata
     */
    @NonNull
    public ModelMetadata getSyncMetadata() {
        return syncMetadata;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        ModelWithMetadata<?> that = (ModelWithMetadata<?>) thatObject;

        if (!ObjectsCompat.equals(model, that.model)) {
            return false;
        }
        return ObjectsCompat.equals(syncMetadata, that.syncMetadata);
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Override
    public int hashCode() {
        int result = model != null ? model.hashCode() : 0;
        result = 31 * result + (syncMetadata != null ? syncMetadata.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ModelWithMetadata{" +
            "model=" + model +
            ", syncMetadata=" + syncMetadata +
            '}';
    }
}
