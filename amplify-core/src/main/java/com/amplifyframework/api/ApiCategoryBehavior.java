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

package com.amplifyframework.api;

import androidx.annotation.NonNull;

import com.amplifyframework.api.graphql.GraphQLCallback;
import com.amplifyframework.api.graphql.GraphQLQuery;
import com.amplifyframework.api.graphql.OperationType;

/**
 * API category behaviors include REST and GraphQL operations. These
 * include the family of HTTP verbs (GET, POST, etc.), and the GraphQL
 * query/subscribe/mutate operations.
 */
public interface ApiCategoryBehavior {

    /**
     * Send a GraphQL document to endpoint as POST.
     *
     * @param <T> type of object being queried for
     * @param apiName name of API being invoked
     * @param operationType graphQL operation type
     * @param document valid GraphQL string
     * @param classToCast class to be cast to
     * @return GraphQLQuery query object being enqueued
     */
    <T> GraphQLQuery<T> graphql(@NonNull String apiName,
                                @NonNull OperationType operationType,
                                @NonNull String document,
                                @NonNull Class<T> classToCast);

    /**
     * Send a GraphQL document to endpoint as POST.
     *
     * @param <T> type of object being queried for
     * @param apiName name of API being invoked
     * @param operationType graphQL operation type
     * @param document valid GraphQL string
     * @param classToCast class to be cast to
     * @param callback callback to attach
     * @return GraphQLQuery query object being enqueued
     */
    <T> GraphQLQuery<T> graphql(@NonNull String apiName,
                                @NonNull OperationType operationType,
                                @NonNull String document,
                                @NonNull Class<T> classToCast,
                                GraphQLCallback<T> callback);
}

