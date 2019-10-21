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

package com.amplifyframework.api.graphql;

/**
 * Enum of GraphQL operation types.
 */
public enum OperationType {
    /** GraphQL query. */
    QUERY("query"),

    /** GraphQL mutation. */
    MUTATION("mutation"),

    /** GraphQL subscription. */
    SUBSCRIPTION("subscription");

    private final String queryPrefix;

    /**
     * Construct the enum with the query type.
     * @param queryPrefix the query prefix used in GraphQL operation
     */
    OperationType(final String queryPrefix) {
        this.queryPrefix = queryPrefix;
    }

    /**
     * Returns the operation type as a string to be
     * used in constructing a GraphQL query.
     * @return The operation type as a string
     */
    public String getQueryPrefix() {
        return queryPrefix;
    }
}
