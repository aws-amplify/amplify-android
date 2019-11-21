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
    private final String type;

    // Name of the field in the target. For example: name of the
    // field in the GraphQL target.
    private final String targetName;

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

    // True if the field is a primary key in the Model.
    private final boolean isPrimaryKey;

    // Type of foreign key model that this field identifies.
    private final String belongsTo;

    // Name of the Model that this field is connecting to.
    private final ModelConnection connection;

    /**
     * Construct the ModelField object from the builder.
     */
    private ModelField(@NonNull ModelFieldBuilder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.targetName = builder.targetName;
        this.targetType = builder.targetType;
        this.isRequired = builder.isRequired;
        this.isArray = builder.isArray;
        this.isEnum = builder.isEnum;
        this.isPrimaryKey = builder.isPrimaryKey;
        this.belongsTo = builder.belongsTo;
        this.connection = builder.connection;
    }

    /**
     * Return the builder object.
     * @return the builder object.
     */
    public static ModelFieldBuilder builder() {
        return new ModelFieldBuilder();
    }

    /**
     * Returns the name of the field is the name of the instance variable of the Model class.
     * @return Name of the field is the name of the instance variable of the Model class.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the name of the field in the target. For example: name of the field in the GraphQL targetType.
     * @return Name of the field in the target. For example: name of the field in the GraphQL targetType.
     */
    public String getTargetName() {
        return targetName;
    }

    /**
     * Returns the data targetType of the field.
     * @return The data targetType of the field.
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * Returns if the field is a required or an optional field.
     * @return If the field is a required or an optional field.
     */
    public boolean isRequired() {
        return isRequired;
    }

    /**
     * Returns if the field is an array targetType. False if it is a primitive targetType and True if it
     * is an array targetType.
     *
     * @return If the field is an array targetType. False if it is a primitive targetType and True if it
     *         is an array targetType.
     */
    public boolean isArray() {
        return isArray;
    }

    /**
     * Returns true if the field's target type is Enum.
     *
     * @return true if the field's target type is Enum.
     */
    public boolean isEnum() {
        return isEnum;
    }

    /**
     * Returns true if the field is a primary key in the Model.
     * @return True if the field is a primary key in the Model.
     */
    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    /**
     * Returns true if the field is a foreign key in the Model.
     * @return True if the field is a foreign key in the Model.
     */
    public boolean isForeignKey() {
        return belongsTo != null;
    }

    /**
     * Returns the name of model that this field belongs to.
     * @return the name of model that this field belongs to.
     */
    public String belongsTo() {
        return belongsTo;
    }

    /**
     * Returns the Model Connection metadata of this field.
     * @return The Model Connection metadata of this field.
     */
    public ModelConnection getConnection() {
        return connection;
    }

    /**
     * Returns true if this ModelField is connected to an other Model.
     * @return True if this ModelField is connected to an other Model.
     */
    public boolean isConnected() {
        return connection != null;
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
        private String type;

        // Name of the field in the target. For example: name of the
        // field in the GraphQL targetType.
        private String targetName;

        // The data targetType of the field.
        private String targetType;

        // If the field is a required or an optional field
        private boolean isRequired = false;

        // If the field is an array targetType. False if it is a primitive
        // targetType and True if it is an array targetType.
        private boolean isArray = false;

        // True if the field's target type is Enum.
        private boolean isEnum = false;

        // True if the field is a primary key in the Model.
        private boolean isPrimaryKey = false;

        // Name of the model that this field identifies.
        private String belongsTo;

        // The Model Connection metadata of this field.
        private ModelConnection connection;

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
        public ModelFieldBuilder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Set the name of the field in the target. For example: name of the
         * field in the GraphQL targetType.
         * @param targetName the name of the field in the target
         * @return the builder object
         */
        public ModelFieldBuilder targetName(String targetName) {
            this.targetName = targetName;
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
         * Set the flag indicating if the field is a primary key.
         * @param isPrimaryKey  True if the field is a primary key in the Model
         * @return the builder object
         */
        public ModelFieldBuilder isPrimaryKey(boolean isPrimaryKey) {
            this.isPrimaryKey = isPrimaryKey;
            return this;
        }

        /**
         * Set the name of model that this field identifies as foreign key.
         * @param belongsTo  name of model that acts as foreign key
         * @return the builder object
         */
        public ModelFieldBuilder belongsTo(String belongsTo) {
            this.belongsTo = belongsTo;
            return this;
        }

        /**
         * Set the Model Connection metadata of this field.
         * @param connection The Model Connection metadata of this field.
         * @return the builder object
         */
        public ModelFieldBuilder connection(ModelConnection connection) {
            this.connection = connection;
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
