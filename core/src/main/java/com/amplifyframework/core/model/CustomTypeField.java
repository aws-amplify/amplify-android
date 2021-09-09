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

/**
 * Represents a field of the {@link CustomTypeSchema} class.
 * Encapsulates all the information of a field.
 */
public final class CustomTypeField {
    // Name of the field is the name of the instance variable
    // of the CustomTypeSchema class.
    private final String name;

    // The Java class for the field value.
    private final Class<?> javaClassForValue;

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

    // True if the field is a CustomType
    private final boolean isCustomType;

    /**
     * Construct the CustomTypeField object from the builder.
     */
    private CustomTypeField(@NonNull CustomTypeFieldBuilder builder) {
        this.name = builder.name;
        this.javaClassForValue = builder.javaClassForValue;
        this.targetType = builder.targetType;
        this.isRequired = builder.isRequired;
        this.isArray = builder.isArray;
        this.isEnum = builder.isEnum;
        this.isCustomType = builder.isCustomType;
    }

    /**
     * Return the builder object.
     * @return the builder object.
     */
    public static CustomTypeFieldBuilder builder() {
        return new CustomTypeFieldBuilder();
    }

    /**
     * Returns the name of the instance variable of the CustomTypeSchema class.
     * @return Name of the instance variable of the CustomTypeSchema class.
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
     * Returns true if the field's target type is CustomType.
     *
     * @return True if the field's target type is CustomType.
     */
    public boolean isCustomType() {
        return isCustomType;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        CustomTypeField that = (CustomTypeField) thatObject;

        if (isRequired != that.isRequired) {
            return false;
        }
        if (isArray != that.isArray) {
            return false;
        }
        if (isEnum != that.isEnum) {
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
        result = 31 * result + (isRequired ? 1 : 0);
        result = 31 * result + (isArray ? 1 : 0);
        result = 31 * result + (isEnum ? 1 : 0);
        result = 31 * result + (isCustomType ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CustomTypeField{" +
                "name='" + name + '\'' +
                ", javaClassForValue='" + javaClassForValue + '\'' +
                ", targetType='" + targetType + '\'' +
                ", isRequired=" + isRequired +
                ", isArray=" + isArray +
                ", isEnum=" + isEnum +
                ", isCustomType=" + isCustomType +
                '}';
    }

    /**
     * Builder class for {@link CustomTypeField}.
     */
    public static class CustomTypeFieldBuilder {
        // Name of the field is the name of the instance variable
        // of the Model class.
        private String name;

        // Java class for the field value, e.g. "java.lang.Integer".
        private Class<?> javaClassForValue;

        // The data targetType of the field.
        private String targetType;

        // If the field is a required or an optional field
        private boolean isRequired = false;

        // If the field is an array targetType. False if it is a primitive
        // targetType and True if it is an array targetType.
        private boolean isArray = false;

        // True if the field's target type is Enum.
        private boolean isEnum = false;

        // True if the field's target type is CustomType.
        private boolean isCustomType = false;

        /**
         * Set the name of the field.
         * @param name Name of the field is the name of the instance variable of the CustomTypeSchema class.
         * @return the builder object
         */
        public CustomTypeFieldBuilder name(String name) {
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
        public CustomTypeFieldBuilder javaClassForValue(@Nullable Class<?> javaClassForValue) {
            this.javaClassForValue = javaClassForValue;
            return this;
        }

        /**
         * Set the data targetType of the field.
         * @param targetType The data targetType of the field.
         * @return the builder object
         */
        public CustomTypeFieldBuilder targetType(String targetType) {
            this.targetType = targetType;
            return this;
        }

        /**
         * Set the flag indicating if the field is a required field or not.
         * @param isRequired ff the field is a required or an optional field
         * @return the builder object
         */
        public CustomTypeFieldBuilder isRequired(boolean isRequired) {
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
        public CustomTypeFieldBuilder isArray(boolean isArray) {
            this.isArray = isArray;
            return this;
        }

        /**
         * Sets a flag indicating whether or not the field's target type is an Enum.
         * @param isEnum flag indicating if the field is an enum targetType
         * @return the builder object
         */
        public CustomTypeFieldBuilder isEnum(boolean isEnum) {
            this.isEnum = isEnum;
            return this;
        }

        /**
         * Sets a flag indicating whether or not the field's target type is an Enum.
         * @param isCustomType flag indicating if the field is an enum targetType
         * @return the builder object
         */
        public CustomTypeFieldBuilder isCustomType(boolean isCustomType) {
            this.isCustomType = isCustomType;
            return this;
        }

        /**
         * Build the CustomTypeField object and return.
         * @return the {@link CustomTypeField} object.
         */
        public CustomTypeField build() {
            return new CustomTypeField(this);
        }
    }
}
