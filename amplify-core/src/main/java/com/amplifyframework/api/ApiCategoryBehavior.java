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
import androidx.annotation.Nullable;

import com.amplifyframework.api.graphql.GraphQLCallback;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.OperationType;

/**
 * API category behaviors include REST and GraphQL operations. These
 * include the family of HTTP verbs (GET, POST, etc.), and the GraphQL
 * query/subscribe/mutate operations.
 */
public interface ApiCategoryBehavior {

    /**
     * In the case that there is only one configured GraphQL endpoint,
     * perform a GraphQL operation against it. This operation is
     * asynchronous and may be canceled by calling cancel on the
     * returned operation.  The response will be rendered as a String
     * inside of a Hub payload.
     * @param operationType Type of GraphQL operation (Query, etc.)
     * @param operationGql A GraphQL operation, as a String
     * @return A GraphQLOperation to track progress and provide
     *         a means to cancel the asynchronous operation
     * @throws ApiException If more than one API is configured and it is
     *                      ambiguous which API is meant for this call
     */
    GraphQLOperation graphql(
            @NonNull OperationType operationType,
            @NonNull String operationGql) throws ApiException;

    /**
     * Perform a GraphQL operation against a configured GraphQL API.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be rendered
     * as a String and will be published inside a Hub payload.
     * @param operationType Type of GraphQL operation (Query, etc.)
     * @param operationGql A GraphQL operation, as a String
     * @param apiName The name of a configured API
     * @return A GraphQLOperation to track progress and provide
     *         a means to cancel the asynchronous operation
     */
    GraphQLOperation graphql(
            @NonNull OperationType operationType,
            @NonNull String operationGql,
            @NonNull String apiName);

    /**
     * In the case that there is only a single GraphQL API, perform an
     * operation against it. This operation is asynchronous and may be
     * canceled by calling cancel on the returned operation. The
     * response of the call will be provided as a payload on the Hub. If
     * response data is present, it will be cast as the provided class
     * type.
     * @param operationType Type of GraphQL operation (Query, etc.)
     * @param operationGql A GraphQL operation, as a String
     * @param classToCast The type to which response data will be cast
     * @param <T> The type of data in the response, if available
     * @return A GraphQLOperation to track progress and provide
     *         a means to cancel the asynchronous operation
     * @throws ApiException If more than one API is configured and it is
     *                      ambiguous which API should be invoked
     */
    <T> GraphQLOperation graphql(
            @NonNull OperationType operationType,
            @NonNull String operationGql,
            @NonNull Class<T> classToCast) throws ApiException;

    /**
     * Perform a GraphQL operation against a configured GraphQL API.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided
     * in a payload over Hub. If response data is present, it will be
     * cast to an object of the requested class type.
     * @param operationType Type of GraphQL operation (Query, etc.)
     * @param operationGql A GraphQL operation, as a String
     * @param classToCast The type to which response data will be cast
     * @param apiName The name of a configured API
     * @param <T> The type of data in the response, if available
     * @return A GraphQLOperation to track progress and provide
     *         a means to cancel the asynchronous operation
     */
    <T> GraphQLOperation graphql(
            @NonNull OperationType operationType,
            @NonNull String operationGql,
            @NonNull Class<T> classToCast,
            @NonNull String apiName);

    /**
     * In the case that there is only one GraphQL API, perform an
     * operation against it.  This operation is asynchronous and may be
     * canceled by calling cancel on the returned operation. The
     * response will be provided to the callback, and via Hub. If the
     * response contains data, it will be cast as the provided class
     * type.
     * @param operationType Type of GraphQL operation (Query, etc.)
     * @param operationGql A GraphQL operation, as a String
     * @param classToCast The type to which response data will be cast
     * @param callback Invoked when response data/errors are available.
     *                 If null, the response is still available via Hub.
     * @param <T> The type of data in the response, if available
     * @return A GraphQLOperation to track progress and provide
     *         a means to cancel the asynchronous operation
     * @throws ApiException If more than one API is configured and it is
     *                      ambiguous which API should be invoked
     */
    <T> GraphQLOperation graphql(
            @NonNull OperationType operationType,
            @NonNull String operationGql,
            @NonNull Class<T> classToCast,
            @Nullable GraphQLCallback<T> callback) throws ApiException;

    /**
     * Perform a GraphQL operation against a configured GraphQL
     * endpoint.  This operation is asynchronous and may be canceled by
     * calling cancel on the returned operation. The response will be
     * provided to the callback, and via Hub.  If there is data present
     * in the response, it will be cast as the requested class type.
     * @param operationType Type of GraphQL operation (Query, etc.)
     * @param operationGql A GraphQL operation, as a String
     * @param classToCast The type to which response data will be cast
     * @param callback Invoked when response data/errors are available.
     *                 If null, response can still be obtained via Hub.
     * @param apiName The name of a configured API
     * @param <T> The type of data in the response, if available
     * @return A GraphQLOperation to track progress and provide
     *         a means to cancel the asynchronous operation
     */
    <T> GraphQLOperation graphql(
            @NonNull OperationType operationType,
            @NonNull String operationGql,
            @NonNull Class<T> classToCast,
            @Nullable GraphQLCallback<T> callback,
            @NonNull String apiName);
}

