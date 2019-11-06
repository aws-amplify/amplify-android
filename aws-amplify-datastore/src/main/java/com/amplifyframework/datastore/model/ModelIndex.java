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
public final class ModelIndex {
    // name of the Index that will be used to create indexes
    // in the persistence layer. For example: the Android SQLite
    // tables when created will have an index identified by this
    // name.
    private final String indexName;

    // names of the different fields on which the index will be
    // constructed.
    private final List<String> indexFieldNames;

    private ModelIndex(String indexName,
                       List<String> indexFieldNames) {
        this.indexName = indexName;
        this.indexFieldNames = indexFieldNames;
    }

    /**
     * Returns the builder object of Builder to
     * build an object of {@link ModelIndex}.
     * @return the builder object of Builder to
     *         build an object of {@link ModelIndex}.
     */
    public static Builder builder() {
        return new Builder();
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
     * The builder class for {@link Builder}.
     */
    public static final class Builder {
        private String indexName;
        private List<String> indexFieldNames;

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
        public Builder indexName(String indexName) {
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
        public Builder indexFieldNames(List<String> indexFieldNames) {
            this.indexFieldNames = indexFieldNames;
            return this;
        }

        /**
         * Returns the model attribute object.
         * @return the model attribute object.
         */
        public ModelIndex build() {
            return new ModelIndex(indexName, indexFieldNames);
        }
    }
}
