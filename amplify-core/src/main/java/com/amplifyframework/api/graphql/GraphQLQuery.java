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
 * An extension of general Query class customized for GraphQL functionality.
 * @param <T> type of object being queried
 */
public class GraphQLQuery<T> extends Query<T> {
    /**
     * Default constructor for GraphQLQuery.
     * Type of API call defaults to "query".
     * @param document query document to process
     * @param classToCast class to be cast to
     */
    public GraphQLQuery(String document, Class<T> classToCast) {
        this(OperationType.QUERY, document, classToCast);
    }

    /**
     * Constructor for GraphQLQuery with
     * specification for type of API call.
     * @param operationType type of API operation being made
     * @param document query document to process
     * @param classToCast class to be cast to
     */
    public GraphQLQuery(OperationType operationType, String document, Class<T> classToCast) {
        super(operationType.value(), document, classToCast);
    }
}
