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

import java.util.List;

/**
 * Attributes of a {@link Model}.
 */
public final class ModelAttribute {
    // name of the Model in the target. For example: the name of the
    // model in the GraphQL Schema.
    private final String targetModelName;

    // name of the Index that will be used to create indexes
    // in the persistence layer. For example: the Android SQLite
    // tables when created will have an index identified by this
    // name.
    private final String indexName;

    // names of the different fields on which the index will be
    // constructed.
    private final List<String> indexFieldNames;

    private ModelAttribute(String targetModelName,
                           String indexName,
                           List<String> indexFieldNames) {
        this.targetModelName = targetModelName;
        this.indexName = indexName;
        this.indexFieldNames = indexFieldNames;
    }

    /**
     * Returns the builder object of ModelAttributeBuilder to
     * build an object of {@link ModelAttribute}.
     * @return the builder object of ModelAttributeBuilder to
     *         build an object of {@link ModelAttribute}.
     */
    public static ModelAttributeBuilder builder() {
        return new ModelAttributeBuilder();
    }

    /**
     * Returns the name of the Model in the target. For example: the name of the
     * model in the GraphQL Schema.
     * @return the name of the Model in the target. For example: the name of the
     *         model in the GraphQL Schema.
     */
    public String getTargetModelName() {
        return targetModelName;
    }

    /**
     * Returns the name of the Index that will be used to create indexes
     * in the persistence layer. For example: the Android SQLite
     * tables when created will have an index identified by this
     * name.
     * @return name of the Index that will be used to create indexes
     *         in the persistence layer. For example: the Android SQLite
     *         tables when created will have an index identified by this
     *         name.
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Returns the names of the different fields on which the index will be
     * constructed.
     * @return names of the different fields on which the index will be
     *         constructed.
     */
    public List<String> getIndexFieldNames() {
        return indexFieldNames;
    }

    /**
     * The builder class for {@link ModelAttributeBuilder}.
     */
    public static final class ModelAttributeBuilder {
        private String targetModelName;
        private String indexName;
        private List<String> indexFieldNames;

        /**
         * Returns the name of the Model in the target. For example: the name of the
         * model in the GraphQL Schema.
         * @param targetModelName the name of the Model in the target.
         *                        For example: the name of the model in the GraphQL Schema.
         * @return the builder object.
         */
        public ModelAttributeBuilder targetModelName(String targetModelName) {
            this.targetModelName = targetModelName;
            return this;
        }

        /**
         * Returns the name of the Index that will be used to create indexes
         * in the persistence layer. For example: the Android SQLite
         * tables when created will have an index identified by this
         * name.
         * @param indexName the name of the Index that will be used to create indexes
         *                  in the persistence layer. For example: the Android SQLite
         *                  tables when created will have an index identified by this
         *                  name.
         * @return the builder object.
         */
        public ModelAttributeBuilder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        /**
         * Returns the names of the different fields on which the index will be
         * constructed.
         * @param indexFieldNames names of the different fields on which the index
         *                        will be constructed.
         * @return the builder object.
         */
        public ModelAttributeBuilder indexFieldNames(List<String> indexFieldNames) {
            this.indexFieldNames = indexFieldNames;
            return this;
        }

        /**
         * Returns the model attribute object.
         * @return the model attribute object.
         */
        public ModelAttribute build() {
            return new ModelAttribute(targetModelName, indexName, indexFieldNames);
        }
    }
}
