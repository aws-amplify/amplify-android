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

package com.amplifyframework.datastore.appsync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.util.Immutable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A container for model data, when passed from hybrid platforms.
 * Needs to be paired with an {@link ModelSchema} to understand the structure.
 */
public final class SerializedModel implements Model {
    private final String modelId;
    private final Map<String, Object> serializedData;
    private final ModelSchema modelSchema;

    private SerializedModel(
            String modelId, Map<String, Object> serializedData, ModelSchema modelSchema) {
        this.modelId = modelId;
        this.serializedData = serializedData;
        this.modelSchema = modelSchema;
    }

    /**
     * Return a builder of {@link SerializedModel}.
     * @return A serialized model builder
     */
    @NonNull
    public static SerializedModel.BuilderSteps.SerializedDataStep builder() {
        return new SerializedModel.Builder();
    }

    @NonNull
    @Override
    public String getId() {
        return modelId;
    }

    /**
     * Gets the serialized data.
     * @return Serialized data
     */
    @NonNull
    public Map<String, Object> getSerializedData() {
        return serializedData;
    }

    /**
     * Gets the model schema.
     * @return Model schema
     */
    @Nullable
    public ModelSchema getModelSchema() {
        return modelSchema;
    }

    /**
     * Gets the serialized value of a given field in the model.
     * @param modelField Schema definition of a field in the model
     * @return The value of the field in the serialized data
     */
    @Nullable
    public Object getValue(ModelField modelField) {
        return serializedData.get(modelField.getName());
    }

    /**
     * Gets the name of the model as known in the hybrid platform, e.g. "Post".
     * @return Name of the model in the hybrid platform.
     */
    @Nullable
    public String getModelName() {
        return modelSchema == null ? null : modelSchema.getName();
    }

    @NonNull
    @Override
    public String toString() {
        return "SerializedModel{" +
            "id='" + modelId + '\'' +
            ", serializedData=" + serializedData +
            ", modelSchema=" + modelSchema +
            '}';
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        SerializedModel that = (SerializedModel) thatObject;
        return ObjectsCompat.equals(this.modelId, that.modelId) &&
            ObjectsCompat.equals(this.serializedData, that.serializedData) &&
            ObjectsCompat.equals(this.modelSchema, that.modelSchema);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(modelId, serializedData, modelSchema);
    }

    /**
     * Steps to build a {@link SerializedModel}.
     */
    public interface BuilderSteps {
        /**
         * Step to configure the serialized data.
         */
        interface SerializedDataStep {
            /**
             * Configures the serialized data.
             * @param serializedData Serialized form of a model
             * @return The next builder step
             */
            @NonNull
            ModelSchemaStep serializedData(@NonNull Map<String, Object> serializedData);
        }

        /**
         * Step to configure the model schema.
         */
        interface ModelSchemaStep {
            /**
             * Configures the model schema.
             * @param modelSchema Model schema describing layout of the serialized data
             * @return The next builder step
             */
            @NonNull
            BuildStep modelSchema(@Nullable ModelSchema modelSchema);
        }

        /**
         * Step to build the serialized model.
         */
        interface BuildStep {
            /**
             * Builds a SerializedModel.
             * @return A SerializedModel
             */
            @NonNull
            SerializedModel build();
        }
    }

    private static final class Builder implements
            BuilderSteps.SerializedDataStep, BuilderSteps.ModelSchemaStep, BuilderSteps.BuildStep {
        private final Map<String, Object> serializedData;
        private ModelSchema modelSchema;
        private String id;

        private Builder() {
            this.serializedData = new HashMap<>();
        }

        @NonNull
        @Override
        public BuilderSteps.ModelSchemaStep serializedData(@NonNull Map<String, Object> serializedData) {
            this.serializedData.putAll(Objects.requireNonNull(serializedData));
            this.id = Objects.requireNonNull((String) serializedData.get("id"));
            return Builder.this;
        }

        @NonNull
        @Override
        public BuilderSteps.BuildStep modelSchema(@Nullable ModelSchema modelSchema) {
            this.modelSchema = modelSchema;
            return Builder.this;
        }

        @NonNull
        @Override
        public SerializedModel build() {
            return new SerializedModel(id, Immutable.of(serializedData), modelSchema);
        }
    }
}
