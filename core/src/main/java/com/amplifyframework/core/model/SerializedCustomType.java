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

import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A container for custom type data, when passed from hybrid platforms.
 * Needs to be paired with an {@link CustomTypeSchema} to understand the structure.
 */
public final class SerializedCustomType {
    private final Map<String, Object> serializedData;
    private Map<String, Object> flatSerializedData;
    private final CustomTypeSchema customTypeSchema;

    private SerializedCustomType(Map<String, Object> serializedData, CustomTypeSchema customTypeSchema) {
        this.serializedData = serializedData;
        this.customTypeSchema = customTypeSchema;
    }

    /**
     * Return a builder of {@link SerializedCustomType}.
     *
     * @return A serialized model builder
     */
    @NonNull
    public static SerializedCustomType.BuilderSteps.SerializedDataStep builder() {
        return new SerializedCustomType.Builder();
    }

    /**
     * Return a Map to use as the serializedData field of {@link SerializedCustomType}.
     *
     * @param serializedData flat Map presents serializedData's values
     * @param customTypeName CustomTypeSchema name
     * @param schemaRegistry SchemaRegistry instance
     * @return A a Map to use as the serializedData
     */
    public static Map<String, Object> parseSerializedData(Map<String, Object> serializedData,
                                                          String customTypeName, SchemaRegistry schemaRegistry) {
        Map<String, Object> result = new HashMap<>();
        CustomTypeSchema customTypeSchema = schemaRegistry.getCustomTypeSchemaForCustomTypeClass(customTypeName);

        for (Map.Entry<String, CustomTypeField> entry : customTypeSchema.getFields().entrySet()) {
            String key = entry.getKey();
            CustomTypeField field = entry.getValue();

            if (!serializedData.containsKey(key)) {
                continue;
            }

            Object fieldValue = serializedData.get(key);

            if (fieldValue == null) {
                result.put(key, null);
                continue;
            }

            if (field.isCustomType()) {
                CustomTypeSchema fieldCustomTypeSchema =
                        schemaRegistry.getCustomTypeSchemaForCustomTypeClass(field.getTargetType());
                if (field.isArray()) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> fieldListData = (List<Map<String, Object>>) fieldValue;
                    List<SerializedCustomType> fieldList = new ArrayList<>();
                    if (!fieldListData.isEmpty()) {
                        for (Map<String, Object> item : fieldListData) {
                            Map<String, Object> customTypeSerializedData =
                                    parseSerializedData(item, field.getTargetType(), schemaRegistry);
                            fieldList.add(SerializedCustomType.builder()
                                    .serializedData(customTypeSerializedData)
                                    .customTypeSchema(
                                            schemaRegistry.getCustomTypeSchemaForCustomTypeClass(field.getTargetType()))
                                    .build());
                        }
                    }
                    result.put(key, fieldList);
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fieldData = (Map<String, Object>) fieldValue;
                    Map<String, Object> customTypeSerializedData =
                            parseSerializedData(fieldData, field.getTargetType(), schemaRegistry);
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
     * Gets the serialized data.
     *
     * @return Serialized data
     */
    @NonNull
    public Map<String, Object> getSerializedData() {
        return serializedData;
    }

    /**
     * Gets the serialized data that doesn't contain SerializedCustomType structure.
     *
     * @return Serialized data
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public Map<String, Object> getFlatSerializedData() {
        if (flatSerializedData != null) {
            return flatSerializedData;
        }

        flatSerializedData = new HashMap<>();

        for (Map.Entry<String, Object> entry : serializedData.entrySet()) {
            CustomTypeField field = customTypeSchema.getFields().get(entry.getKey());

            if (field == null) {
                continue;
            }

            Object fieldValue = entry.getValue();

            if (field.isCustomType() && fieldValue != null) {
                if (field.isArray()) {
                    ArrayList<SerializedCustomType> items = (ArrayList<SerializedCustomType>) fieldValue;
                    ArrayList<Map<String, Object>> flattenItems = new ArrayList<>();
                    for (SerializedCustomType item : items) {
                        flattenItems.add(item.getFlatSerializedData());
                    }
                    flatSerializedData.put(entry.getKey(), flattenItems);
                } else {
                    flatSerializedData.put(entry.getKey(), ((SerializedCustomType) fieldValue).getFlatSerializedData());
                }
            } else {
                flatSerializedData.put(entry.getKey(), fieldValue);
            }
        }

        return flatSerializedData;
    }

    /**
     * Gets the CustomType schema.
     *
     * @return CustomType schema
     */
    @Nullable
    public CustomTypeSchema getCustomTypeSchema() {
        return customTypeSchema;
    }

    /**
     * Gets the serialized value of a given field in the CustomType.
     *
     * @param modelField Schema definition of a field in the CustomType
     * @return The value of the field in the serialized data
     */
    @Nullable
    public Object getValue(ModelField modelField) {
        return serializedData.get(modelField.getName());
    }

    /**
     * Gets the name of the CustomType as known in the hybrid platform, e.g. "S3Object".
     *
     * @return Name of the CustomType in the hybrid platform.
     */
    @Nullable
    public String getCustomTypeName() {
        return customTypeSchema == null ? null : customTypeSchema.getName();
    }

    @NonNull
    @Override
    public String toString() {
        return "SerializedCustomType{" +
                "serializedData=" + serializedData +
                ", customTypeName=" + getCustomTypeSchema() +
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

        SerializedCustomType that = (SerializedCustomType) thatObject;
        return ObjectsCompat.equals(this.serializedData, that.serializedData) &&
                ObjectsCompat.equals(this.customTypeSchema, that.customTypeSchema);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(serializedData, customTypeSchema);
    }

    /**
     * Steps to build a {@link SerializedCustomType}.
     */
    public interface BuilderSteps {
        /**
         * Step to configure the serialized data.
         */
        interface SerializedDataStep {
            /**
             * Configures the serialized data.
             *
             * @param serializedData Serialized form of a CustomType
             * @return The next builder step
             */
            @NonNull
            SerializedCustomType.BuilderSteps.CustomTypeSchemaStep serializedData(
                    @NonNull Map<String, Object> serializedData);
        }

        /**
         * Step to configure the CustomType schema.
         */
        interface CustomTypeSchemaStep {
            /**
             * Configures the CustomType schema.
             *
             * @param customTypeSchema CustomType schema describing layout of the serialized data
             * @return The next builder step
             */
            @NonNull
            SerializedCustomType.BuilderSteps.BuildStep customTypeSchema(@Nullable CustomTypeSchema customTypeSchema);
        }

        /**
         * Step to build the serialized CustomType.
         */
        interface BuildStep {
            /**
             * Builds a SerializedCustomType.
             *
             * @return A SerializedCustomType
             */
            @NonNull
            SerializedCustomType build();
        }
    }

    private static final class Builder implements
            SerializedCustomType.BuilderSteps.SerializedDataStep,
            SerializedCustomType.BuilderSteps.CustomTypeSchemaStep,
            SerializedCustomType.BuilderSteps.BuildStep {
        private final Map<String, Object> serializedData;
        private CustomTypeSchema customTypeSchema;

        private Builder() {
            this.serializedData = new HashMap<>();
        }

        @NonNull
        @Override
        public SerializedCustomType.BuilderSteps.CustomTypeSchemaStep serializedData(
                @NonNull Map<String, Object> serializedData) {
            this.serializedData.putAll(Objects.requireNonNull(serializedData));
            return SerializedCustomType.Builder.this;
        }

        @NonNull
        @Override
        public SerializedCustomType.BuilderSteps.BuildStep customTypeSchema(
                @Nullable CustomTypeSchema customTypeSchema) {
            this.customTypeSchema = customTypeSchema;
            return SerializedCustomType.Builder.this;
        }

        @NonNull
        @Override
        public SerializedCustomType build() {
            return new SerializedCustomType(Immutable.of(serializedData), customTypeSchema);
        }
    }
}
