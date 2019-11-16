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

import java.util.List;

/**
 * Represents a connection of the {@link Model} class.
 * Encapsulates all the information of a connection.
 */
public final class ModelConnection {
    // Name of the connection to identify the relationship.
    private final String name;

    // Relationship that model has with the field.
    private final Relationship relationship;

    // The name of the foreign key.
    private final String keyField;

    // The sort key equivalent in DynamoDB.
    private final String sortField;

    // The pagination limit.
    private final int limit;

    // The key name used for indexing in DynamoDB.
    private final String keyName;

    // Sorted collection of keyField, sortField, and
    // additional fields.
    private final List<String> fields;

    // Type of target field.
    private final String connectionTarget;

    /**
     * Construct the ModelConnection object from the builder.
     */
    private ModelConnection(@NonNull ModelConnectionBuilder builder) {
        this.name = builder.name;
        this.relationship = builder.relationship;
        this.keyField = builder.keyField;
        this.sortField = builder.sortField;
        this.limit = builder.limit;
        this.keyName = builder.keyName;
        this.fields = builder.fields;
        this.connectionTarget = builder.connectionTarget;
    }

    /**
     * Return the builder object.
     * @return the builder object.
     */
    public static ModelConnectionBuilder builder() {
        return new ModelConnectionBuilder();
    }

    /**
     * Gets the name of the connection to identify the relationship.
     * @return The name of the connection to identify the relationship.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the relationship type that this model has with field.
     * @return The type of relationship
     */
    public Relationship getRelationship() {
        return relationship;
    }

    /**
     * Gets the name of the foreign key.
     * @return The name of the foreign key.
     */
    public String getKeyField() {
        return keyField;
    }

    /**
     * Gets the sort key equivalent in DynamoDB.
     * @return The sort key equivalent in DynamoDB.
     */
    public String getSortField() {
        return sortField;
    }

    /**
     * Gets the pagination limit.
     * @return The pagination limit.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Gets the key name used for indexing in DynamoDB.
     * @return The key name used for indexing in DynamoDB.
     */
    public String getKeyName() {
        return keyName;
    }

    /**
     * Gets the sorted collection of keyField, sortField, and
     * additional fields.
     * @return The collection of fields
     */
    public List<String> getFields() {
        return fields;
    }

    /**
     * Gets the connection target field's type.
     * @return The field type of connection target
     */
    public String getConnectionTarget() {
        return connectionTarget;
    }

    /**
     * Builder class for {@link ModelConnection}.
     */
    public static final class ModelConnectionBuilder {
        // Name of the connection to identify the relationship.
        private String name;

        // Type of relationship that this model has with field.
        private Relationship relationship;

        // The name of the foreign key.
        private String keyField;

        // The sort key equivalent in DynamoDB.
        private String sortField;

        // Pagination limit.
        private int limit;

        // The key name used for indexing in DynamoDB.
        private String keyName;

        // Sorted collection of keyField, sortField, and
        // additional fields.
        private List<String> fields;

        // Type of connection target field.
        private String connectionTarget;

        /**
         * Sets the name of connection model.
         * @param name name of the connection model
         * @return the connection model with given name
         */
        public ModelConnectionBuilder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the relationship of connection.
         * @param relationship relationship that model has with field
         * @return the relationship that model has with field
         */
        public ModelConnectionBuilder relationship(Relationship relationship) {
            this.relationship = relationship;
            return this;
        }

        /**
         * Sets the name of foreign key.
         * @param fieldName name of the key field
         * @return the connection model with given key field
         */
        public ModelConnectionBuilder keyField(String fieldName) {
            this.keyField = fieldName;
            return this;
        }

        /**
         * Sets the name of sort key.
         * @param fieldName name of the sort field
         * @return the connection model with given sort field
         */
        public ModelConnectionBuilder sortField(String fieldName) {
            this.sortField = fieldName;
            return this;
        }

        /**
         * Sets the pagination limit.
         * @param limit the pagination limit
         * @return the connection model with given limit
         */
        public ModelConnectionBuilder limit(int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Sets the new index name for connection.
         * @param keyName name of the key
         * @return the connection model with given key name
         */
        public ModelConnectionBuilder keyName(String keyName) {
            this.keyName = keyName;
            return this;
        }

        /**
         * Sets the list of fields.
         * @param fields list of fields
         * @return the connection model with given list of fields
         */
        public ModelConnectionBuilder fields(List<String> fields) {
            this.fields = fields;
            return this;
        }

        /**
         * Sets the connection target field's type.
         * @param connectionTarget The field type of connection target
         * @return the connection model with given connection target
         */
        public ModelConnectionBuilder connectionTarget(String connectionTarget) {
            this.connectionTarget = connectionTarget;
            return this;
        }

        /**
         * Builds an immutable ModelConnection instance using
         * builder object.
         * @return ModelConnection instance
         */
        public ModelConnection build() {
            return new ModelConnection(this);
        }
    }
}
