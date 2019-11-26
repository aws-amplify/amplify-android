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

package com.amplifyframework.core.model.query.predicate;

/**
 * The base class for all comparison objects.
 * Contains the Enum list of valid types and stores the given operator's type.
 */
public abstract class QueryOperator {
    private Type type;

    /**
     * Constructs a new QueryOperator of the specified type.
     * @param type the type of this comparison operator
     */
    public QueryOperator(Type type) {
        this.type = type;
    }

    /**
     * Returns this operator's type.
     * @return the type of this comparison operator
     */
    public Type type() {
        return type;
    }

    /**
     * List of possible comparison types.
     */
    public enum Type {
        /**
         * Not equal to some value comparison.
         */
        NOT_EQUAL,
        /**
         * Equals some value comparison.
         */
        EQUAL,
        /**
         * Less than or equal to some value comparison.
         */
        LESS_OR_EQUAL,
        /**
         * Less than some value comparison.
         */
        LESS_THAN,
        /**
         * Greater than or equal to some value comparison.
         */
        GREATER_OR_EQUAL,
        /**
         * Greater than some value comparison.
         */
        GREATER_THAN,
        /**
         * Contains some value comparison.
         */
        CONTAINS,
        /**
         * Between two values comparison.
         */
        BETWEEN,
        /**
         * Begins with some value comparison.
         */
        BEGINS_WITH
    }
}
