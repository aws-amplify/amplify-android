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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.ApiOperation;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;

/**
 * GraphQL behaviors include varying approaches to perform the query,
 * subscribe and mutate GraphQL operations.
 */
public interface GraphQLBehavior {

    /**
     * Perform a GraphQL query against a configured GraphQL endpoint.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided
     * to the `onResponse` callback.  If there is data present
     * in the response, it will be cast as the requested class type.
     * Requires that only one API is configured in your
     * `amplifyconfiguration.json`. Otherwise, emits an ApiException to
     * the provided `onFailure` callback.
     * @param graphQlRequest Wrapper for request details
     * @param onResponse Invoked when a response is available; may contain errors from endpoint
     * @param onFailure Invoked when a response is not available due to operational failures
     * @param <R> The type of data in the response, if available
     * @return An {@link ApiOperation} to track progress and provide
     *         a means to cancel the asynchronous operation
     */
    @Nullable
    <R> GraphQLOperation<R> query(
            @NonNull GraphQLRequest<R> graphQlRequest,
            @NonNull Consumer<GraphQLResponse<R>> onResponse,
            @NonNull Consumer<ApiException> onFailure
    );

    /**
     * Perform a GraphQL query against a configured GraphQL endpoint.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided
     * to the `onResponse` callback.  If there is data present
     * in the response, it will be cast as the requested class type.
     * @param apiName The name of a configured API
     * @param graphQlRequest Wrapper for request details
     * @param onResponse Invoked when a response is available; may contain errors from endpoint
     * @param onFailure Invoked when a response is not available due to operational failures
     * @param <R> The type of data in the response, if available
     * @return An {@link ApiOperation} to track progress and provide
     *         a means to cancel the asynchronous operation
     */
    @Nullable
    <R> GraphQLOperation<R> query(
            @NonNull String apiName,
            @NonNull GraphQLRequest<R> graphQlRequest,
            @NonNull Consumer<GraphQLResponse<R>> onResponse,
            @NonNull Consumer<ApiException> onFailure
    );

    /**
     * Perform a GraphQL mutation against a configured GraphQL endpoint.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided
     * to the `onResponse` callback.  If there is data
     * present in the response, it will be cast as the requested class
     * type. Requires that only one API is configured in your
     * `amplifyconfiguration.json`. Otherwise, emits an ApiException to
     * the provided `onFailure` callback.
     *
     * @param graphQlRequest Wrapper for request details
     * @param onResponse Invoked when a response is available; may contain errors from endpoint
     * @param onFailure Invoked when a response is not available due to operational failures
     * @param <T> The type of data in the response, if available
     * @return An {@link ApiOperation} to track progress and provide
     *         a means to cancel the asynchronous operation
     */
    @Nullable
    <T> GraphQLOperation<T> mutate(
            @NonNull GraphQLRequest<T> graphQlRequest,
            @NonNull Consumer<GraphQLResponse<T>> onResponse,
            @NonNull Consumer<ApiException> onFailure
    );

    /**
     * Perform a GraphQL mutation against a configured GraphQL endpoint.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided
     * to the `onResponse` callback. If there is data
     * present in the response, it will be cast as the requested class type.
     *
     * @param apiName The name of a configured API
     * @param graphQlRequest Wrapper for request details
     * @param onResponse Invoked when a response is available; may contain errors from endpoint
     * @param onFailure Invoked when a response is not available due to operational failures
     * @param <R> The type of data in the response, if available
     * @return An {@link ApiOperation} to track progress and provide
     *         a means to cancel the asynchronous operation
     */
    @Nullable
    <R> GraphQLOperation<R> mutate(
            @NonNull String apiName,
            @NonNull GraphQLRequest<R> graphQlRequest,
            @NonNull Consumer<GraphQLResponse<R>> onResponse,
            @NonNull Consumer<ApiException> onFailure
    );

    /**
     * Initiates a GraphQL subscription against a configured GraphQL
     * endpoint. The operation is on-going and emits a stream of
     * {@link GraphQLResponse}s to the provided `onNextResponse` callback.
     * The subscription may be canceled by calling {@link GraphQLOperation#cancel()} on
     * the returned object.
     *
     * Requires that only one API is configured in your
     * `amplifyconfiguration.json`. Otherwise, emits an ApiException to
     * the provided `onSubscriptionFailure` callback.
     *
     * @param graphQlRequest Wrapper for request details
     * @param onSubscriptionEstablished
     *        Called when a subscription has been established over the network
     * @param onNextResponse
     *        Consumes a stream of responses on the subscription. This may be
     *        called 0..n times per subscription.
     * @param onSubscriptionFailure
     *        Called when the subscription stream terminates with a failure.
     *        Note that items passed via onNextResponse may themselves contain
     *        errors in the response from the endpoint, but the subscription
     *        may continue to be active even after these are received.
     *        This is a terminal event following 0..n many calls to onNextResponse.
     * @param onSubscriptionComplete
     *        Called when a subscription has ended gracefully (without failure).
     *        This is a terminal event following 0..n many calls to onNextResponse.
     * @param <R> The type of data expected in the subscription stream
     * @return An {@link GraphQLOperation} representing this ongoing subscription
     */
    @Nullable
    <R> GraphQLOperation<R> subscribe(
            @NonNull GraphQLRequest<R> graphQlRequest,
            @NonNull Consumer<String> onSubscriptionEstablished,
            @NonNull Consumer<GraphQLResponse<R>> onNextResponse,
            @NonNull Consumer<ApiException> onSubscriptionFailure,
            @NonNull Action onSubscriptionComplete
    );

    /**
     * Initiates a GraphQL subscription against a configured GraphQL
     * endpoint. The operation is on-going and emits a stream of
     * {@link GraphQLResponse}s to the provided `onNextResponse` callback.
     * The subscription may be canceled by calling {@link GraphQLOperation#cancel()}
     * on the returned object.
     *
     * @param apiName The name of a configured API
     * @param graphQlRequest Wrapper for request details
     * @param onSubscriptionEstablished
     *        Called when a subscription has been established over the network
     * @param onNextResponse
     *        Consumes a stream of responses on the subscription. This may be
     *        called 0..n times per subscription.
     * @param onSubscriptionFailure
     *        Called when the subscription stream terminates with a failure.
     *        Note that items passed via onNextResponse may themselves contain
     *        errors in the response from the endpoint, but the subscription
     *        may continue to be active even after these are received.
     *        This is a terminal event following 0..n many calls to onNextResponse.
     * @param onSubscriptionComplete
     *        Called when a subscription has ended gracefully (without failure).
     *        This is a terminal event following 0..n many calls to onNextResponse.
     * @param <R> The type of data expected in the subscription stream
     * @return An {@link GraphQLOperation} representing this ongoing subscription
     */
    @Nullable
    <R> GraphQLOperation<R> subscribe(
            @NonNull String apiName,
            @NonNull GraphQLRequest<R> graphQlRequest,
            @NonNull Consumer<String> onSubscriptionEstablished,
            @NonNull Consumer<GraphQLResponse<R>> onNextResponse,
            @NonNull Consumer<ApiException> onSubscriptionFailure,
            @NonNull Action onSubscriptionComplete
    );
}
