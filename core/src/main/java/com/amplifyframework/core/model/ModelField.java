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

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a field of the {@link Model} class.
 * Encapsulates all the information of a field.
 */
public final class ModelField {
    // Name of the field is the name of the instance variable
    // of the Model class.
    private final String name;

    // Type of the field is the data type of the instance variables
    // of the Model class.
    private final Class<?> type;

    // The type of the field in the target. For example: type of the
    // field in the GraphQL target.
    private final String targetType;

    // If the field is a required or an optional field
    private final boolean isRequired;

    // If the field is an array targetType. False if it is a primitive
    // targetType and True if it is an array targetType.
    private final boolean isArray;

    // True if the field is an enumeration type.
    private final boolean isEnum;

    // True if the field is an instance of model.
    private final boolean isModel;

    // An array of rules for owner based authorization
    private final List<AuthRule> authRules;

    /**
     * Construct the ModelField object from the builder.
     */
    private ModelField(@NonNull ModelFieldBuilder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.targetType = builder.targetType;
        this.isRequired = builder.isRequired;
        this.isArray = builder.isArray;
        this.isEnum = builder.isEnum;
        this.isModel = builder.isModel;
        this.authRules = builder.authRules;
    }

    /**
     * Return the builder object.
     * @return the builder object.
     */
    public static ModelFieldBuilder builder() {
        return new ModelFieldBuilder();
    }

    /**
     * Returns the name of the instance variable of the Model class.
     * @return Name of the instance variable of the Model class.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the data type of the instance variable of the Model class.
     * @return Data type of the instance variable of the Model class.
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Returns the data targetType of the field.
     * @return The data targetType of the field.
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * Returns true if the field represents a unique ID.
     * @return True if the field represents a unique ID.
     */
    public boolean isId() {
        return PrimaryKey.matches(name);
    }

    /**
     * Returns true if the field is a required field.
     * @return True if the field is a required field.
     */
    public boolean isRequired() {
        return isRequired;
    }

    /**
     * Returns whether the field is an array targetType. False if it is a primitive targetType and True if it
     * is an array targetType.
     *
     * @return Whether the field is an array targetType. False if it is a primitive targetType and True if it
     *         is an array targetType.
     */
    public boolean isArray() {
        return isArray;
    }

    /**
     * Returns true if the field's target type is Enum.
     *
     * @return True if the field's target type is Enum.
     */
    public boolean isEnum() {
        return isEnum;
    }

    /**
     * Returns true if the field's target type is Model.
     *
     * @return True if the field's target type is Model.
     */
    public boolean isModel() {
        return isModel;
    }

    /**
     * Specifies an array of rules for owner based authorization.
     *
     * @return list of {@link AuthRule}s
     */
    public List<AuthRule> getAuthRules() {
        return authRules;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        ModelField that = (ModelField) thatObject;

        if (isRequired != that.isRequired) {
            return false;
        }
        if (isArray != that.isArray) {
            return false;
        }
        if (isEnum != that.isEnum) {
            return false;
        }
        if (isModel != that.isModel) {
            return false;
        }
        if (!ObjectsCompat.equals(name, that.name)) {
            return false;
        }
        if (!ObjectsCompat.equals(type, that.type)) {
            return false;
        }
        return ObjectsCompat.equals(targetType, that.targetType);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (targetType != null ? targetType.hashCode() : 0);
        result = 31 * result + (isRequired ? 1 : 0);
        result = 31 * result + (isArray ? 1 : 0);
        result = 31 * result + (isEnum ? 1 : 0);
        result = 31 * result + (isModel ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ModelField{" +
            "name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", targetType='" + targetType + '\'' +
            ", isRequired=" + isRequired +
            ", isArray=" + isArray +
            ", isEnum=" + isEnum +
            ", isModel=" + isModel +
            '}';
    }

    /**
     * Builder class for {@link ModelField}.
     */
    public static class ModelFieldBuilder {
        // Name of the field is the name of the instance variable
        // of the Model class.
        private String name;

        // Type of the field is the data type of the instance variables
        // of the Model class.
        private Class<?> type;

        // The data targetType of the field.
        private String targetType;

        // If the field is a required or an optional field
        private boolean isRequired = false;

        // If the field is an array targetType. False if it is a primitive
        // targetType and True if it is an array targetType.
        private boolean isArray = false;

        // True if the field's target type is Enum.
        private boolean isEnum = false;

        // True if the field's target type is Model.
        private boolean isModel = false;

        // A list of rules for owner based authorization
        private List<AuthRule> authRules = new ArrayList<>();

        /**
         * Set the name of the field.
         * @param name Name of the field is the name of the instance variable of the Model class.
         * @return the builder object
         */
        public ModelFieldBuilder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the type of the field.
         * @param type Type of the field is the type of the instance variable of the Model class.
         * @return the builder object
         */
        public ModelFieldBuilder type(Class<?> type) {
            this.type = type;
            return this;
        }

        /**
         * Set the data targetType of the field.
         * @param targetType The data targetType of the field.
         * @return the builder object
         */
        public ModelFieldBuilder targetType(String targetType) {
            this.targetType = targetType;
            return this;
        }

        /**
         * Set the flag indicating if the field is a required field or not.
         * @param isRequired ff the field is a required or an optional field
         * @return the builder object
         */
        public ModelFieldBuilder isRequired(boolean isRequired) {
            this.isRequired = isRequired;
            return this;
        }

        /**
         * Set the flag indicating if the field is an array targetType.
         * False if it is a primitive targetType and True if it
         * is an array targetType.
         * @param isArray flag indicating if the field is an array targetType
         * @return the builder object
         */
        public ModelFieldBuilder isArray(boolean isArray) {
            this.isArray = isArray;
            return this;
        }

        /**
         * Sets a flag indicating whether or not the field's target type is an Enum.
         * @param isEnum flag indicating if the field is an enum targetType
         * @return the builder object
         */
        public ModelFieldBuilder isEnum(boolean isEnum) {
            this.isEnum = isEnum;
            return this;
        }

        /**
         * Sets a flag indicating whether or not the field's target type is a Model.
         * @param isModel flag indicating if the field is a model
         * @return the builder object
         */
        public ModelFieldBuilder isModel(boolean isModel) {
            this.isModel = isModel;
            return this;
        }

        /**
         * Set the authRules of the {@link ModelField}.
         * @param authRules list of authorization rules
         * @return the builder object
         */
        public ModelFieldBuilder authRules(List<AuthRule> authRules) {
            this.authRules = authRules;
            return this;
        }

        /**
         * Build the ModelField object and return.
         * @return the {@link ModelField} object.
         */
        public ModelField build() {
            return new ModelField(this);
        }
    }
}
