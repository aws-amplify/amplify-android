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

package com.amplifyframework.core.model;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Schema of a Model that implements the {@link Model} interface.
 * The schema encapsulates the metadata information of a Model.
 */
public final class CustomTypeSchema {
    // Name of the CustomType.
    private final String name;

    // The plural version of the name of the CustomType.
    // Useful for generating GraphQL list query names.
    private final String pluralName;

    // A map that contains the fields of a CustomType.
    // The key is the name of the instance variable in the Java class that represents the CustomType
    // The value is the CustomTypeField object that encapsulates all the information about the instance variable.
    private final Map<String, CustomTypeField> fields;

    private CustomTypeSchema(Builder builder) {
        this.name = builder.name;
        this.pluralName = builder.pluralName;
        this.fields = builder.fields;
    }

    /**
     * Return the builder object.
     * @return the builder object.
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the name of the CustomType.
     *
     * @return the name of the CustomType.
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Returns the plural name of the CustomType in the target.
     * Null if not explicitly annotated in ModelConfig.
     *
     * @return the plural name of the CustomType in the target
     *         if explicitly provided.
     */
    @Nullable
    public String getPluralName() {
        return pluralName;
    }

    /**
     * Returns the map of fieldName and the fieldObject
     * of all the fields of the CustomType.
     *
     * @return map of fieldName and the fieldObject
     *         of all the fields of the CustomType.
     */
    @NonNull
    public Map<String, CustomTypeField> getFields() {
        return fields;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            CustomTypeSchema that = (CustomTypeSchema) obj;
            return ObjectsCompat.equals(getName(), that.getName()) &&
                    ObjectsCompat.equals(getPluralName(), that.getPluralName()) &&
                    ObjectsCompat.equals(getFields(), that.getFields());
        }
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getName(),
                getPluralName(),
                getFields()
        );
    }

    @Override
    public String toString() {
        return "CustomTypeSchema{" +
                "name='" + name + '\'' +
                ", pluralName='" + pluralName + '\'' +
                ", fields=" + fields +
                '}';
    }

    /**
     * The Builder to build the {@link CustomTypeSchema} object.
     */
    @SuppressWarnings("WeakerAccess")
    public static final class Builder {
        private final Map<String, CustomTypeField> fields;
        private String name;
        private String pluralName;

        Builder() {
            this.fields = new TreeMap<>();
        }

        /**
         * Set the name of the CustomType.
         * @param name the name of the CustomType.
         * @return the builder object
         */
        @NonNull
        public Builder name(@NonNull String name) {
            this.name = Objects.requireNonNull(name);
            return this;
        }

        /**
         * The plural version of the name of the CustomType.
         * If null, a default plural version name will be generated.
         * @param pluralName the plural version of CustomType name.
         * @return the builder object
         */
        @NonNull
        public Builder pluralName(@Nullable String pluralName) {
            this.pluralName = pluralName;
            return this;
        }

        /**
         * Set the map of fieldName and the fieldObject of all the fields of the CustomType.
         * @param fields the map of fieldName and the fieldObject of all the fields of the CustomType.
         * @return the builder object.
         */
        @NonNull
        public Builder fields(@NonNull Map<String, CustomTypeField> fields) {
            Objects.requireNonNull(fields);
            this.fields.clear();
            this.fields.putAll(fields);
            return this;
        }

        /**
         * Return the CustomTypeSchema object.
         * @return the CustomTypeSchema object.
         */
        @SuppressLint("SyntheticAccessor")
        @NonNull
        public CustomTypeSchema build() {
            Objects.requireNonNull(name);
            return new CustomTypeSchema(Builder.this);
        }
    }
}
