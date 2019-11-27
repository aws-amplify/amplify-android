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
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.StreamListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.hub.HubCategory;

/**
 * API category behaviors include REST and GraphQL operations. These
 * include the family of HTTP verbs (GET, POST, etc.), and the GraphQL
 * query/subscribe/mutate operations.
 */
public interface ApiCategoryBehavior {

    /**
     * This is a special helper method for easily calling a list query for
     * all items of the specified model type with no filtering condition.
     *
     * Perform a GraphQL query against a configured GraphQL endpoint.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided
     * to the response listener, and via Hub.  If there is data present
     * in the response, it will be cast as the requested class type.
     * @param apiName The name of a configured API
     * @param modelClass The class of the Model we are querying on
     * @param responseListener
     *        Invoked when response data/errors are available.  If null,
     *        response can still be obtained via Hub.
     * @param <T> The type of data in the response, if available. Must extend Model.
     * @return An {@link ApiOperation} to track progress and provide
     *         a means to cancel the asynchronous operation
     */
    <T extends Model> GraphQLOperation<T> query(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @Nullable ResultListener<GraphQLResponse<Iterable<T>>> responseListener);

    /**
     * This is a special helper method for easily performing GET Queries
     * on Model objects which are autogenerated from your schema.
     *
     * Perform a GraphQL query against a configured GraphQL endpoint.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided to
     * the response listener, and via {@link HubCategory}.  If there is data
     * present in the response, it will be cast as the requested class type.
     * @param apiName The name of a configured API
     * @param modelClass The class of the Model we are querying on
     * @param objectId The unique ID of the object you want to get
     * @param responseListener
     *        Invoked when response data/errors are available.  If null,
     *        response can still be obtained via Hub.
     * @param <T> The type of data in the response, if available. Must extend Model.
     * @return An {@link ApiOperation} to track progress and provide
     *         a means to cancel the asynchronous operation
     */
    <T extends Model> GraphQLOperation<T> query(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @NonNull String objectId,
            @Nullable ResultListener<GraphQLResponse<T>> responseListener);

    /**
     * This is a special helper method for easily performing LIST Queries
     * on Model objects which are autogenerated from your schema.
     *
     * Perform a GraphQL query against a configured GraphQL endpoint.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided
     * to the response listener, and via Hub.  If there is data present
     * in the response, it will be cast as the requested class type.
     * @param apiName The name of a configured API
     * @param modelClass The class of the Model we are querying on
     * @param predicate Filtering conditions for the query
     * @param responseListener
     *        Invoked when response data/errors are available.  If null,
     *        response can still be obtained via Hub.
     * @param <T> The type of data in the response, if available. Must extend Model.
     * @return An {@link ApiOperation} to track progress and provide
     *         a means to cancel the asynchronous operation
     */
    <T extends Model> GraphQLOperation<T> query(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            QueryPredicate predicate,
            @Nullable ResultListener<GraphQLResponse<Iterable<T>>> responseListener);

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
            @Nullable ResultListener<GraphQLResponse<Iterable<T>>> responseListener);

    /**
     * This is a special helper method for easily performing Mutations
     * on Model objects which are autogenerated from your schema.
     *
     * Perform a GraphQL mutation against a configured GraphQL endpoint.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided
     * to the response listener, and via Hub.  If there is data
     * present in the response, it will be cast as the requested class
     * type.
     * @param apiName The name of a configured API
     * @param model An instance of the Model with the values to mutate
     * @param mutationType  What type of mutation to perform (e.g. Create, Update, Delete)
     * @param responseListener
     *        Invoked when response data/errors are available.  If null,
     *        response can still be obtained via Hub.
     * @param <T> The type of data in the response, if available. Must extend Model.
     * @return An {@link ApiOperation} to track progress and provide
     *         a means to cancel the asynchronous operation
     */
    <T extends Model> GraphQLOperation<T> mutate(
            @NonNull String apiName,
            @NonNull T model,
            @NonNull MutationType mutationType,
            @Nullable ResultListener<GraphQLResponse<T>> responseListener);

    /**
     * This is a special helper method for easily performing Mutations
     * on Model objects which are autogenerated from your schema.
     *
     * Perform a GraphQL mutation against a configured GraphQL endpoint.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided
     * to the response listener, and via Hub.  If there is data
     * present in the response, it will be cast as the requested class
     * type.
     * @param apiName The name of a configured API
     * @param model An instance of the Model with the values to mutate
     * @param predicate Conditions on the current data to determine whether to go through
     *                  with an UPDATE or DELETE operation
     * @param mutationType  What type of mutation to perform (e.g. Create, Update, Delete)
     * @param responseListener
     *        Invoked when response data/errors are available.  If null,
     *        response can still be obtained via Hub.
     * @param <T> The type of data in the response, if available. Must extend Model.
     * @return An {@link ApiOperation} to track progress and provide
     *         a means to cancel the asynchronous operation
     */
    <T extends Model> GraphQLOperation<T> mutate(
            @NonNull String apiName,
            @NonNull T model,
            QueryPredicate predicate,
            @NonNull MutationType mutationType,
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
     * This is a special helper method for easily subscribing to events
     * on Model objects which are autogenerated from your schema.
     *
     * Initiates a GraphQL subscription against a configured GraphQL
     * endpoint. The operation is on-going and emits a stream of
     * {@link GraphQLResponse}s to the provided stream listener.
     * The subscription may be canceled by calling
     * {@link GraphQLOperation#cancel()}.
     * @param apiName The name of a previously configured GraphQL API
     * @param modelClass The class of the Model we are subscribing to
     * @param predicate Filtering conditions for the query
     * @param subscriptionType What type of subscription to listen for (e.g. OnCreate, OnUpdate, OnDelete)
     * @param subscriptionListener
     *        A listener to receive notifications when new items are
     *        available via the subscription stream
     * @param <T> The type of data expected in the subscription stream. Must extend Model.
     * @return A GraphQLOperation representing this ongoing subscription
     */
    <T extends Model> GraphQLOperation<T> subscribe(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @NonNull QueryPredicate predicate,
            @NonNull SubscriptionType subscriptionType,
            @Nullable StreamListener<GraphQLResponse<T>> subscriptionListener);

    /**
     * This is a special helper method for easily subscribing to events
     * on Model objects which are autogenerated from your schema.
     *
     * Initiates a GraphQL subscription against a configured GraphQL
     * endpoint. The operation is on-going and emits a stream of
     * {@link GraphQLResponse}s to the provided stream listener.
     * The subscription may be canceled by calling
     * {@link GraphQLOperation#cancel()}.
     * @param apiName The name of a previously configured GraphQL API
     * @param modelClass The class of the Model we are subscribing to
     * @param subscriptionType What type of subscription to listen for (e.g. OnCreate, OnUpdate, OnDelete)
     * @param subscriptionListener
     *        A listener to receive notifications when new items are
     *        available via the subscription stream
     * @param <T> The type of data expected in the subscription stream. Must extend Model.
     * @return A GraphQLOperation representing this ongoing subscription
     */
    <T extends Model> GraphQLOperation<T> subscribe(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @NonNull SubscriptionType subscriptionType,
            @Nullable StreamListener<GraphQLResponse<T>> subscriptionListener);

    /**
     * Initiates a GraphQL subscription against a configured GraphQL
     * endpoint. The operation is on-going and emits a stream of
     * {@link GraphQLResponse}s to the provided stream listener.
     * The subscription may be canceled by calling
     * {@link GraphQLOperation#cancel()}.
     * @param apiName The name of a configured API
     * @param graphQlRequest Wrapper for request details
     * @param subscriptionListener
     *        A listener to receive notifications when new items are
     *        available via the subscription stream
     * @param <T> The type of data expected in the subscription stream
     * @return A GraphQLOperation representing this ongoing subscription
     */
    <T> GraphQLOperation<T> subscribe(
            @NonNull String apiName,
            @NonNull GraphQLRequest<T> graphQlRequest,
            @Nullable StreamListener<GraphQLResponse<T>> subscriptionListener);
}
