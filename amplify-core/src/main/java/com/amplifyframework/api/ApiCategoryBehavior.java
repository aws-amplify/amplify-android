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

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.async.Listener;

/**
 * API category behaviors include REST and GraphQL operations. These
 * include the family of HTTP verbs (GET, POST, etc.), and the GraphQL
 * query/subscribe/mutate operations.
 */
public interface ApiCategoryBehavior {

    /**
     * Perform an asynchronous GraphQL query operation
     * against a previously configured API.
     * It casts the queried result to json string.
     * Events will only be dispatched to Amplify Hub.
     *
     * @param apiName name of API being invoked
     * @param document valid GraphQL string
     * @return GraphQLQuery query object being enqueued
     */
    ApiOperation<String, GraphQLResponse<String>> query(@NonNull String apiName,
                                                        @NonNull String document);

    /**
     * Perform an asynchronous GraphQL query operation
     * against a previously configured API.
     * It casts the queried result to json string.
     * Events will be dispatched both locally and
     * to Amplify Hub.
     *
     * @param apiName name of API being invoked
     * @param document valid GraphQL string
     * @param callback callback to attach
     * @return GraphQLQuery query object being enqueued
     */
    ApiOperation<String, GraphQLResponse<String>> query(@NonNull String apiName,
                                                        @NonNull String document,
                                                        Listener<GraphQLResponse<String>> callback);

    /**
     * Perform an asynchronous GraphQL query operation
     * against a previously configured API.
     * It casts the queried result to specified data type.
     * Events will only be dispatched to Amplify Hub.
     *
     * @param <T> type of object being queried for
     * @param apiName name of API being invoked
     * @param document valid GraphQL string
     * @param classToCast class to be cast to
     * @return GraphQLQuery query object being enqueued
     */
    <T> ApiOperation<T, GraphQLResponse<T>> query(@NonNull String apiName,
                                                  @NonNull String document,
                                                  Class<T> classToCast);

    /**
     * Perform an asynchronous GraphQL query operation
     * against a previously configured API.
     * It casts the queried result to specified data type.
     * Events will be dispatched both locally and
     * to Amplify Hub.
     *
     * @param <T> type of object being queried for
     * @param apiName name of API being invoked
     * @param document valid GraphQL string
     * @param classToCast class to be cast to
     * @param callback callback to attach
     * @return GraphQLQuery query object being enqueued
     */
    <T> ApiOperation<T, GraphQLResponse<T>> query(@NonNull String apiName,
                                                  @NonNull String document,
                                                  Class<T> classToCast,
                                                  Listener<GraphQLResponse<T>> callback);

    /**
     * Perform an asynchronous GraphQL mutation operation
     * against a previously configured API.
     * It casts the queried result to json string.
     * Events will only be dispatched to Amplify Hub.
     *
     * @param apiName name of API being invoked
     * @param document valid GraphQL string
     * @return GraphQLQuery query object being enqueued
     */
    ApiOperation<String, GraphQLResponse<String>> mutate(@NonNull String apiName,
                                                         @NonNull String document);

    /**
     * Perform an asynchronous GraphQL mutation operation
     * against a previously configured API.
     * It casts the queried result to json string.
     * Events will be dispatched both locally and
     * to Amplify Hub.
     *
     * @param apiName name of API being invoked
     * @param document valid GraphQL string
     * @param callback callback to attach
     * @return GraphQLQuery query object being enqueued
     */
    ApiOperation<String, GraphQLResponse<String>> mutate(@NonNull String apiName,
                                                         @NonNull String document,
                                                         Listener<GraphQLResponse<String>> callback);

    /**
     * Perform an asynchronous GraphQL mutation operation
     * against a previously configured API.
     * It casts the queried result to specified data type.
     * Events will only be dispatched to Amplify Hub.
     *
     * @param <T> type of object being queried for
     * @param apiName name of API being invoked
     * @param document valid GraphQL string
     * @param classToCast class to be cast to
     * @return GraphQLQuery query object being enqueued
     */
    <T> ApiOperation<T, GraphQLResponse<T>> mutate(@NonNull String apiName,
                                                   @NonNull String document,
                                                   Class<T> classToCast);

    /**
     * Perform an asynchronous GraphQL mutation operation
     * against a previously configured API.
     * It casts the queried result to specified data type.
     * Events will be dispatched both locally and
     * to Amplify Hub.
     *
     * @param <T> type of object being queried for
     * @param apiName name of API being invoked
     * @param document valid GraphQL string
     * @param classToCast class to be cast to
     * @param callback callback to attach
     * @return GraphQLQuery query object being enqueued
     */
    <T> ApiOperation<T, GraphQLResponse<T>> mutate(@NonNull String apiName,
                                                   @NonNull String document,
                                                   Class<T> classToCast,
                                                   Listener<GraphQLResponse<T>> callback);
}

