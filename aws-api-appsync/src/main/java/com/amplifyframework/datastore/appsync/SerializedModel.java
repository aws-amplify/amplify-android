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

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.datastore.DataStoreException;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings("all")
@ModelConfig(pluralName = "SerializedModels")
public final class SerializedModel implements Model {
    private final @ModelField(targetType="ID", isRequired = true) String id;
    private final @ModelField(targetType="String", isRequired = true) Map<String, Object> serializedData;
    private final @ModelField(targetType="String", isRequired = true) String modelName;

    public String getId() {
        return id;
    }

    public Map<String, Object> getSerializedData() {
        return serializedData;
    }

    public String getModelName() {
        return modelName;
    }

    private SerializedModel(String id, Map<String, Object> serializedData, String modelName) {
        this.id = id;
        this.serializedData = serializedData;
        this.modelName = modelName;
    }

    /**
     * Uses seriazlied data to return the field value.
     *
     * @param field the model field for which to retrieve the value from this model instance
     * @return the field value or <code>null</code>
     * @throws DataStoreException in case of a error happens during the dynamic reflection calls
     */
    public Object getValue(com.amplifyframework.core.model.ModelField field) throws DataStoreException {
        return getSerializedData().get(field.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if(obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            SerializedModel SerializedModel = (SerializedModel) obj;
            return ObjectsCompat.equals(getId(), SerializedModel.getId()) &&
                    ObjectsCompat.equals(getSerializedData(), SerializedModel.getSerializedData()) &&
                    ObjectsCompat.equals(getModelName(), SerializedModel.getModelName());
        }
    }

    @Override
    public int hashCode() {
        return new StringBuilder()
                .append(getId())
                .append(getSerializedData())
                .append(getModelName())
                .toString()
                .hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("SerializedData {")
                .append("id=" + String.valueOf(getId()) + ", ")
                .append("serializedData=" + String.valueOf(getSerializedData()) + ", ")
                .append("modelName=" + String.valueOf(getModelName()))
                .append("}")
                .toString();
    }

    public static NameStep builder() {
        return new Builder();
    }

    /**
     * WARNING: This method should not be used to build an instance of this object for a CREATE mutation.
     * This is a convenience method to return an instance of the object with only its ID populated
     * to be used in the context of a parameter in a delete mutation or referencing a foreign key
     * in a relationship.
     * @param id the id of the existing item this instance will represent
     * @return an instance of this model with only ID populated
     * @throws IllegalArgumentException Checks that ID is in the proper format
     */
    public static SerializedModel justId(String id) {
        try {
            UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Model IDs must be unique in the format of UUID. This method is for creating instances " +
                            "of an existing object with only its ID field for sending as a mutation parameter. When " +
                            "creating a new object, use the standard builder method and leave the ID field blank."
            );
        }
        return new SerializedModel(
                id,
                null,
                null);
    }

    public CopyOfBuilder copyOfBuilder() {
        return new CopyOfBuilder(id,
                serializedData,
                modelName);
    }

    public interface NameStep {
        BuildStep serializedData(Map<String, Object> serializedData);
    }


    public interface BuildStep {
        SerializedModel build();
        BuildStep id(String id) throws IllegalArgumentException;
        BuildStep modelName(String modelName);
    }


    public static class Builder implements NameStep, BuildStep {
        private String id;
        private Map<String, Object> serializedData;
        private String modelName;

        @Override
        public SerializedModel build() {
            String id = this.id != null ? this.id : UUID.randomUUID().toString();

            return new SerializedModel(
                    id,
                    serializedData,
                    modelName);
        }

        @Override
        public BuildStep serializedData(Map<String, Object> serializedData) {
            Objects.requireNonNull(serializedData);
            this.serializedData = serializedData;
            return this;
        }

        @Override
        public BuildStep modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        /**
         * WARNING: Do not set ID when creating a new object. Leave this blank and one will be auto generated for you.
         * This should only be set when referring to an already existing object.
         * @param id id
         * @return Current Builder instance, for fluent method chaining
         * @throws IllegalArgumentException Checks that ID is in the proper format
         */
        public BuildStep id(String id) throws IllegalArgumentException {
            this.id = id;

            try {
                UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
            } catch (Exception exception) {
                throw new IllegalArgumentException("Model IDs must be unique in the format of UUID.",
                        exception);
            }

            return this;
        }
    }


    public final class CopyOfBuilder extends Builder {
        private CopyOfBuilder(String id, Map<String, Object> serializedData, String modelName) {
            super.id(id);
            super.serializedData(serializedData)
                    .modelName(modelName);
        }

        @Override
        public CopyOfBuilder serializedData(Map<String, Object> serializedData) {
            return (CopyOfBuilder) super.serializedData(serializedData);
        }

        @Override
        public CopyOfBuilder modelName(String modelName) {
            return (CopyOfBuilder) super.modelName(modelName);
        }
    }

}
