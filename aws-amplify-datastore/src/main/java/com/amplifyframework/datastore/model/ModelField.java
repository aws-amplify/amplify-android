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

package com.amplifyframework.datastore.model;

import androidx.annotation.NonNull;

/**
 * Represents a field of the {@link Model} class.
 * Encapsulates all the information of a field.
 */
public final class ModelField {
    // Name of the field is the name of the instance variable
    // of the Model class.
    private final String name;

    // Name of the field in the target. For example: name of the
    // field in the GraphQL type.
    private final String targetName;

    // The data type of the field.
    private final String type;

    // If the field is a required or an optional field
    private final boolean isRequired;

    // If the field is an array type. False if it is a primitive
    // type and True if it is an array type.
    private final boolean isArray;

    // True if the field is a primary key in the Model.
    private final boolean isPrimaryKey;

    // Name of the Model that this field is connecting to.
    private final String connectionTarget;

    /**
     * Construct the ModelField object from the builder.
     */
    private ModelField(@NonNull ModelFieldBuilder builder) {
        this.name = builder.name;
        this.targetName = builder.targetName;
        this.type = builder.type;
        this.isRequired = builder.isRequired;
        this.isArray = builder.isArray;
        this.isPrimaryKey = builder.isPrimaryKey;
        this.connectionTarget = builder.connectionTarget;
    }

    /**
     * @return the builder object.
     */
    public static ModelFieldBuilder builder() {
        return new ModelFieldBuilder();
    }

    /**
     * @return Name of the field is the name of the instance variable of the Model class.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Name of the field in the target. For example: name of the field in the GraphQL type.
     */
    public String getTargetName() {
        return targetName;
    }

    /**
     * @return The data type of the field.
     */
    public String getType() {
        return type;
    }

    /**
     * @return If the field is a required or an optional field.
     */
    public boolean isRequired() {
        return isRequired;
    }

    /**
     * @return If the field is an array type. False if it is a primitive type and True if it
     *         is an array type.
     */
    public boolean isArray() {
        return isArray;
    }

    /**
     * @return True if the field is a primary key in the Model.
     */
    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    /**
     * @return  Name of the Model that this field is connecting to.
     */
    public String getConnectionTarget() {
        return connectionTarget;
    }

    /**
     * @return True if this ModelField is connected to an other Model.
     */
    public boolean isConnected() {
        return connectionTarget != null;
    }

    /**
     * Builder class for {@link ModelField}.
     */
    public static class ModelFieldBuilder {
        // Name of the field is the name of the instance variable
        // of the Model class.
        private String name;

        // Name of the field in the target. For example: name of the
        // field in the GraphQL type.
        private String targetName;

        // The data type of the field.
        private String type;

        // If the field is a required or an optional field
        private boolean isRequired;

        // If the field is an array type. False if it is a primitive
        // type and True if it is an array type.
        private boolean isArray;

        // True if the field is a primary key in the Model.
        private boolean isPrimaryKey;

        // Name of the Model that this field is connecting to.
        private String connectionTarget;

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
         * Set the name of the field in the target. For example: name of the
         * field in the GraphQL type.
         * @param targetName the name of the field in the target
         * @return the builder object
         */
        public ModelFieldBuilder targetName(String targetName) {
            this.targetName = targetName;
            return this;
        }

        /**
         * Set the data type of the field.
         * @param type The data type of the field.
         * @return the builder object
         */
        public ModelFieldBuilder type(String type) {
            this.type = type;
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
         * Set the flag indicating if the field is an array type.
         * False if it is a primitive type and True if it
         * is an array type.
         * @param isArray flag indicating if the field is an array type
         * @return the builder object
         */
        public ModelFieldBuilder isArray(boolean isArray) {
            this.isArray = isArray;
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
         * Set the name of the Model that this field is connected to.
         * @param connectionTarget Name of the Model that this field is connected to
         * @return the builder object
         */
        public ModelFieldBuilder connectionTarget(String connectionTarget) {
            this.connectionTarget = connectionTarget;
            return this;
        }

        /**
         * Build the ModelField object and return
         * @return the {@link ModelField} object.
         */
        public ModelField build() {
            return new ModelField(this);
        }
    }
}
