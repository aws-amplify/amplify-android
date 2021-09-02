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

package com.amplifyframework.core.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
     * Creates a SerializedModel from a generated Java Model object.
     *
     * @param model       Model object
     * @param modelSchema schema for the Model object
     * @param <T>         type of the Model object.
     * @return SerializedModel equivalent of the Model object.
     * @throws AmplifyException ModelConverter.toMap
     */
    public static <T extends Model> SerializedModel create(T model, ModelSchema modelSchema) throws AmplifyException {
        return SerializedModel.builder()
                .serializedData(ModelConverter.toMap(model, modelSchema))
                .modelSchema(modelSchema)
                .build();
    }

    /**
     * Computes the difference between two Models, comparing equality of each field value for each Model, and returns
     * the difference as a SerializedModel.
     *
     * @param updated     the updated Model, whose values will be used to build the resulting SerializedModel.
     * @param original    the original Model to compare against.
     * @param modelSchema ModelSchema for the Models between compared.
     * @param <T>         type of the Models being compared.
     * @return a SerializedModel, containing only the values from the updated Model that are different from the
     * corresponding values in original.
     * @throws AmplifyException ModelConverter.toMap
     */
    public static <T extends Model> SerializedModel difference(T updated, T original, ModelSchema modelSchema)
            throws AmplifyException {
        if (original == null) {
            return SerializedModel.create(updated, modelSchema);
        }
        Map<String, Object> updatedMap = ModelConverter.toMap(updated, modelSchema);
        Map<String, Object> originalMap = ModelConverter.toMap(original, modelSchema);
        Map<String, Object> patchMap = new HashMap<>();
        for (String key : updatedMap.keySet()) {
            Set<String> primaryIndexFields = new HashSet<>();

            // This can be removed once we fully support custom primary keys.  For now, it is required though, since
            // SerializedModel requires the `id` field.
            primaryIndexFields.add(PrimaryKey.fieldName());

            primaryIndexFields.addAll(modelSchema.getPrimaryIndexFields());
            if (primaryIndexFields.contains(key) || !ObjectsCompat.equals(originalMap.get(key), updatedMap.get(key))) {
                patchMap.put(key, updatedMap.get(key));
            }
        }
        return SerializedModel.builder()
                .serializedData(patchMap)
                .modelSchema(modelSchema)
                .build();
    }

    /**
     * Merge the serialized data from existing to incoming model.
     *
     * @param incoming    the incoming Model to which serialized data fields will be added.
     * @param existing    the original Model to compare against.
     * @param modelSchema ModelSchema for the Models between compared.
     * @return a SerializedModel, containing the values from the incoming Model and existing Model.
     */
    public static SerializedModel merge(SerializedModel incoming, SerializedModel existing, ModelSchema modelSchema) {
        Map<String, Object> mergedSerializedData = new HashMap<>(incoming.serializedData);
        for (String key : existing.getSerializedData().keySet()) {
            if (!mergedSerializedData.containsKey(key)) {
                mergedSerializedData.put(key, existing.getSerializedData().get(key));
            }
        }
        return SerializedModel.builder()
                .serializedData(mergedSerializedData)
                .modelSchema(modelSchema)
                .build();
    }

    /**
     * Return a Map to use as the serializedData field of {@link SerializedModel}.
     *
     * @param serializedData flat Map presents serializedData's values
     * @param modelName ModelSchema name
     * @param schemaRegistry SchemaRegistry instance
     * @return A a Map to use as the serializedData
     */
    public static Map<String, Object> parseSerializedData(Map<String, Object> serializedData, String modelName,
                                                   SchemaRegistry schemaRegistry) {
        Map<String, Object> result = new HashMap<>();
        ModelSchema modelSchema = schemaRegistry.getModelSchemaForModelClass(modelName);

        for (Map.Entry<String, ModelField> entry : modelSchema.getFields().entrySet()) {
            String key = entry.getKey();
            ModelField field = entry.getValue();
            if (!serializedData.containsKey(key)) {
                continue;
            }

            Object fieldValue = serializedData.get(key);

            if (fieldValue == null) {
                result.put(key, null);
                continue;
            }

            if (field.isModel()) {
                ModelSchema fieldModelSchema = schemaRegistry.getModelSchemaForModelClass(field.getTargetType());
                @SuppressWarnings("unchecked")
                Map<String, Object> fieldData = (Map<String, Object>) serializedData.get(key);
                if (fieldData != null) {
                    result.put(key, SerializedModel.builder()
                            .serializedData(fieldData)
                            .modelSchema(fieldModelSchema)
                            .build());
                }
            } else if (field.isCustomType()) {
                if (field.isArray()) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> fieldListData = (List<Map<String, Object>>) fieldValue;
                    if (!fieldListData.isEmpty()) {
                        List<SerializedCustomType> fieldList = new ArrayList<>();
                        for (Map<String, Object> item : fieldListData) {
                            Map<String, Object> customTypeSerializedData =
                                    SerializedCustomType.parseSerializedData(
                                            item, field.getTargetType(), schemaRegistry);
                            fieldList.add(SerializedCustomType.builder()
                                    .serializedData(customTypeSerializedData)
                                    .customTypeSchema(
                                            schemaRegistry.getCustomTypeSchemaForCustomTypeClass(field.getTargetType()))
                                    .build());
                        }
                        result.put(key, fieldList);
                    }
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fieldData = (Map<String, Object>) fieldValue;
                    Map<String, Object> customTypeSerializedData =
                            SerializedCustomType.parseSerializedData(fieldData, field.getTargetType(), schemaRegistry);
                    result.put(key, SerializedCustomType.builder()
                            .serializedData(customTypeSerializedData)
                            .customTypeSchema(
                                    schemaRegistry.getCustomTypeSchemaForCustomTypeClass(field.getTargetType()))
                            .build());
                }
            } else {
                result.put(key, fieldValue);
            }
        }

        return result;
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
    @Override
    public String getModelName() {
        return modelSchema == null ? null : modelSchema.getName();
    }

    @NonNull
    @Override
    public String toString() {
        return "SerializedModel{" +
            "id='" + modelId + '\'' +
            ", serializedData=" + serializedData +
            ", modelName=" + getModelName() +
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
