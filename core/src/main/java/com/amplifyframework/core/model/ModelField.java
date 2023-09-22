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
import androidx.annotation.Nullable;
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

    // The Java class for the field value.
    private final Class<?> javaClassForValue;

    // The type of the field in the target. For example: type of the
    // field in the GraphQL target.
    private final String targetType;

    // If the field can be modified
    private final boolean isReadOnly;

    // If the field is a required or an optional field
    private final boolean isRequired;

    // If the field is an array targetType. False if it is a primitive
    // targetType and True if it is an array targetType.
    private final boolean isArray;

    // True if the field is an enumeration type.
    private final boolean isEnum;

    // True if the field is an instance of model.
    private final boolean isModel;

    // True if the field is an instance of ModelReference.
    private final boolean isModelReference;

    // True if the field is an instance of ModelList.
    private final boolean isModelList;

    // True if the field is an instance of CustomType
    private final boolean isCustomType;

    // An array of rules for owner based authorization
    private final List<AuthRule> authRules;

    /**
     * Construct the ModelField object from the builder.
     */
    private ModelField(@NonNull ModelFieldBuilder builder) {
        this.name = builder.name;
        this.javaClassForValue = builder.javaClassForValue;
        this.targetType = builder.targetType;
        this.isReadOnly = builder.isReadOnly;
        this.isRequired = builder.isRequired;
        this.isArray = builder.isArray;
        this.isEnum = builder.isEnum;
        this.isModel = builder.isModel;
        this.isModelReference = builder.isModelReference;
        this.isModelList = builder.isModelList;
        this.isCustomType = builder.isCustomType;
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
     * Returns the Java class of the value of this field, if available.
     * @return The Java class of the field, if available.
     */
    @Nullable
    public Class<?> getJavaClassForValue() {
        return javaClassForValue;
    }

    /**
     * Returns the data targetType of the field.
     * @return The data targetType of the field.
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * Returns true if the field is read only.
     * @return true if the field is read only.
     */
    public boolean isReadOnly() {
        return isReadOnly;
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
     * Returns true if the field's target type is ModelReference.
     *
     * @return True if the field's target type is ModelReference.
     */
    public boolean isModelReference() {
        return isModelReference;
    }

    /**
     * Returns true if the field's target type is ModelList.
     *
     * @return True if the field's target type is ModelList.
     */
    public boolean isModelList() {
        return isModelList;
    }

    /**
     * Returns true if the field's target type is CustomType.
     *
     * @return True if the field's target type is CustomType.
     */
    public boolean isCustomType() {
        return isCustomType;
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

        if (isReadOnly != that.isReadOnly) {
            return false;
        }
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
        if (isModelReference != that.isModelReference) {
            return false;
        }
        if (isModelList != that.isModelList) {
            return false;
        }
        if (isCustomType != that.isCustomType) {
            return false;
        }
        if (!ObjectsCompat.equals(name, that.name)) {
            return false;
        }
        if (!ObjectsCompat.equals(javaClassForValue, that.javaClassForValue)) {
            return false;
        }
        return ObjectsCompat.equals(targetType, that.targetType);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (javaClassForValue != null ? javaClassForValue.hashCode() : 0);
        result = 31 * result + (targetType != null ? targetType.hashCode() : 0);
        result = 31 * result + (isReadOnly ? 1 : 0);
        result = 31 * result + (isRequired ? 1 : 0);
        result = 31 * result + (isArray ? 1 : 0);
        result = 31 * result + (isEnum ? 1 : 0);
        result = 31 * result + (isModel ? 1 : 0);
        result = 31 * result + (isModelReference ? 1 : 0);
        result = 31 * result + (isModelList ? 1 : 0);
        result = 31 * result + (isCustomType ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ModelField{" +
            "name='" + name + '\'' +
            ", javaClassForValue='" + javaClassForValue + '\'' +
            ", targetType='" + targetType + '\'' +
            ", isReadOnly=" + isReadOnly +
            ", isRequired=" + isRequired +
            ", isArray=" + isArray +
            ", isEnum=" + isEnum +
            ", isModel=" + isModel +
            ", isModelReference=" + isModelReference +
            ", isModelList=" + isModelList +
            ", isCustomType=" + isCustomType +
            '}';
    }

    /**
     * Builder class for {@link ModelField}.
     */
    public static class ModelFieldBuilder {
        // Name of the field is the name of the instance variable
        // of the Model class.
        private String name;

        // Java class for the field value, e.g. "java.lang.Integer".
        private Class<?> javaClassForValue;

        // The data targetType of the field.
        private String targetType;

        // If the field can be modified.
        private boolean isReadOnly = false;

        // If the field is a required or an optional field
        private boolean isRequired = false;

        // If the field is an array targetType. False if it is a primitive
        // targetType and True if it is an array targetType.
        private boolean isArray = false;

        // True if the field's target type is Enum.
        private boolean isEnum = false;

        // True if the field's target type is Model.
        private boolean isModel = false;

        // True if the field's target type is a ModelReference type.
        private boolean isModelReference = false;

        // True if the field's target type is a ModelList type.
        private boolean isModelList = false;

        // True if the field's target type is CustomType.
        private boolean isCustomType = false;

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
         * Sets the Java class for the field value, e.g. "java.lang.Integer".
         *
         * If the field value is a custom Java model, or is an enumeration type,
         * this must the class name, e.g.,
         * "com.amplifyframework.datastore.generated.model.Weekday".
         *
         * If the field value is a model, but there is no Java type available (such as when
         * the schema is generated from Flutter or React Native), you may pass "null".
         *
         * @param javaClassForValue The java class of the value
         * @return the builder object
         */
        public ModelFieldBuilder javaClassForValue(@Nullable Class<?> javaClassForValue) {
            this.javaClassForValue = javaClassForValue;
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
         * Set the flag indicating if the field can be modified.
         * @param isReadOnly if the field can be modified.
         * @return the builder object
         */
        public ModelFieldBuilder isReadOnly(boolean isReadOnly) {
            this.isReadOnly = isReadOnly;
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
         * Sets a flag indicating whether or not the field's target type is a ModelReference.
         * @param isModelReference flag indicating if the field is a ModelReference type
         * @return the builder object
         */
        public ModelFieldBuilder isModelReference(boolean isModelReference) {
            this.isModelReference = isModelReference;
            return this;
        }

        /**
         * Sets a flag indicating whether or not the field's type is a ModelList type.
         * @param isModelList flag indicating if the field is a ModelList type
         * @return the builder object
         */
        public ModelFieldBuilder isModelList(boolean isModelList) {
            this.isModelList = isModelList;
            return this;
        }

        /**
         * Sets a flag indicating whether or not the field's target type is a Model.
         * @param isCustomType flag indicating if the field is a model
         * @return the builder object
         */
        public ModelFieldBuilder isCustomType(boolean isCustomType) {
            this.isCustomType = isCustomType;
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
