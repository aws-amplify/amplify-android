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

import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.StreamListener;

import java.util.Map;

/**
 * API category behaviors include REST and GraphQL operations. These
 * include the family of HTTP verbs (GET, POST, etc.), and the GraphQL
 * query/subscribe/mutate operations.
 */
public interface ApiCategoryBehavior {

    /**
     * Perform a GraphQL query against a configured GraphQL endpoint.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided
     * to the response listener, and via Hub.  If there is data present
     * in the response, it will be cast as the requested class type.
     * @param apiName The name of a configured API
     * @param gqlDocument A GraphQL operation document, as a String
     * @param classToCast The type to which response data will be cast
     * @param variables GraphQL query variables if needed
     * @param responseListener
     *        Invoked when response data/errors are available.  If null,
     *        response can still be obtained via Hub.
     * @param <T> The type of data in the response, if available
     * @return An {@link ApiOperation} to track progress and provide
     *         a means to cancel the asynchronous operation
     */
    <T> GraphQLOperation<T> query(
            @NonNull String apiName,
            @NonNull String gqlDocument,
            @Nullable Map<String, Object> variables,
            @NonNull Class<T> classToCast,
            @Nullable ResultListener<GraphQLResponse<T>> responseListener);

    /**
     * Perform a GraphQL query against a configured GraphQL endpoint.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided
     * to the response listener, and via Hub.  If there is data present
     * in the response, it will be cast as the requested class type.
     * @param apiName The name of a configured API
     * @param graphQlRequest Wrapper for request details
     * @param responseListener
     *        Invoked when response data/errors are available.  If null,
     *        response can still be obtained via Hub.
     * @param <T> The type of data in the response, if available
     * @return An {@link ApiOperation} to track progress and provide
     *         a means to cancel the asynchronous operation
     */
    <T> GraphQLOperation<T> query(
            @NonNull String apiName,
            @NonNull GraphQLRequest<T> graphQlRequest,
            @Nullable ResultListener<GraphQLResponse<T>> responseListener);

    /**
     * Perform a GraphQL mutation against a configured GraphQL endpoint.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided
     * to the response listener, and via Hub.  If there is data
     * present in the response, it will be cast as the requested class
     * type.
     * @param apiName The name of a configured API
     * @param gqlDocument A GraphQL operation document, as a String
     * @param classToCast The type to which response data will be cast
     * @param variables GraphQL query variables if needed
     * @param responseListener
     *        Invoked when response data/errors are available.  If null,
     *        response can still be obtained via Hub.
     * @param <T> The type of data in the response, if available
     * @return An {@link ApiOperation} to track progress and provide
     *         a means to cancel the asynchronous operation
     */
    <T> GraphQLOperation<T> mutate(
            @NonNull String apiName,
            @NonNull String gqlDocument,
            @Nullable Map<String, Object> variables,
            @NonNull Class<T> classToCast,
            @Nullable ResultListener<GraphQLResponse<T>> responseListener);

    /**
     * Perform a GraphQL mutation against a configured GraphQL endpoint.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided
     * to the response listener, and via Hub.  If there is data
     * present in the response, it will be cast as the requested class
     * type.
     * @param apiName The name of a configured API
     * @param graphQlRequest Wrapper for request details
     * @param responseListener
     *        Invoked when response data/errors are available.  If null,
     *        response can still be obtained via Hub.
     * @param <T> The type of data in the response, if available
     * @return An {@link ApiOperation} to track progress and provide
     *         a means to cancel the asynchronous operation
     */
    <T> GraphQLOperation<T> mutate(
            @NonNull String apiName,
            @NonNull GraphQLRequest<T> graphQlRequest,
            @Nullable ResultListener<GraphQLResponse<T>> responseListener);

    /**
     * Initiates a GraphQL subscription against a configured GraphQL
     * endpoint. The operation is on-going and emits a stream of
     * {@link GraphQLResponse}s to the provided stream listener.
     * The subscription may be canceled by calling
     * {@link GraphQLOperation#cancel()}.
     * @param apiName The name of a previously configured GraphQL API
     * @param gqlDocument A subscription operation document
     * @param variables Resolution variables for the provided document
     * @param classToCast Expected data type for subscription responses
     * @param subscriptionListener
     *        A listener to receive notifications when new items are
     *        available via the subscription stream
     * @param <T> The type of data expected in the subscription stream
     * @return A GraphQLOperation representing this ongoing subscription
     */
    <T> GraphQLOperation<T> subscribe(
            @NonNull String apiName,
            @NonNull String gqlDocument,
            @Nullable Map<String, String> variables,
            @NonNull Class<T> classToCast,
            @Nullable StreamListener<GraphQLResponse<T>> subscriptionListener);
}

