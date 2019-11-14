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
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.StreamListener;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.FilteringPredicate;

/**
 * The API category provides methods for interacting with remote systems
 * using REST and GraphQL constructs. The category is implemented by
 * zero or more {@link ApiPlugin}. The operations made available by the
 * category are defined in the {@link ApiCategoryBehavior}.
 */
public final class ApiCategory extends Category<ApiPlugin<?>> implements ApiCategoryBehavior {
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.API;
    }

    /**
     * This is a special helper method for easily performing Queries
     * on Model objects which are autogenerated from your schema.
     * <p>
     * Perform a GraphQL query against a configured GraphQL endpoint.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided
     * to the response listener, and via Hub.  If there is data present
     * in the response, it will be cast as the requested class type.
     *
     * @param apiName          The name of a configured API
     * @param modelClass       The class of the Model we are querying on
     * @param predicate        Filtering conditions for the query
     * @param queryType        What type of query to perform (e.g. Get, List)
     * @param responseListener Invoked when response data/errors are available.  If null,
     *                         response can still be obtained via Hub.
     * @return An {@link ApiOperation} to track progress and provide
     * a means to cancel the asynchronous operation
     */
    @Override
    public <T extends Model> GraphQLOperation<T> query(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @NonNull FilteringPredicate<T> predicate,
            @NonNull QueryType queryType,
            @Nullable ResultListener<GraphQLResponse<T>> responseListener
    ) {
        return getSelectedPlugin().query(apiName, modelClass, predicate, queryType, responseListener);
    }

    /**
     * Perform a GraphQL query against a configured GraphQL endpoint.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided
     * to the response listener, and via Hub.  If there is data present
     * in the response, it will be cast as the requested class type.
     *
     * @param apiName          The name of a configured API
     * @param graphQlRequest   Wrapper for request details
     * @param responseListener Invoked when response data/errors are available.  If null,
     *                         response can still be obtained via Hub.
     * @return An {@link ApiOperation} to track progress and provide
     * a means to cancel the asynchronous operation
     */
    @Override
    public <T> GraphQLOperation<T> query(
            @NonNull String apiName,
            @NonNull GraphQLRequest<T> graphQlRequest,
            @Nullable ResultListener<GraphQLResponse<T>> responseListener
    ) {
        return getSelectedPlugin().query(apiName, graphQlRequest, responseListener);
    }

    /**
     * This is a special helper method for easily performing Mutations
     * on Model objects which are autogenerated from your schema.
     * <p>
     * Perform a GraphQL mutation against a configured GraphQL endpoint.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided
     * to the response listener, and via Hub.  If there is data
     * present in the response, it will be cast as the requested class
     * type.
     *
     * @param apiName          The name of a configured API
     * @param model            An instance of the Model with the values to mutate
     * @param predicate        Filtering conditions for the query
     * @param mutationType     What type of mutation to perform (e.g. Create, Update, Delete)
     * @param responseListener Invoked when response data/errors are available.  If null,
     *                         response can still be obtained via Hub.
     * @return An {@link ApiOperation} to track progress and provide
     * a means to cancel the asynchronous operation
     */
    @Override
    public <T extends Model> GraphQLOperation<T> mutate(
            @NonNull String apiName,
            @NonNull T model,
            @NonNull FilteringPredicate<T> predicate,
            @NonNull MutationType mutationType,
            @Nullable ResultListener<GraphQLResponse<T>> responseListener
    ) {
        return getSelectedPlugin().mutate(apiName, model, predicate, mutationType, responseListener);
    }

    /**
     * Perform a GraphQL mutation against a configured GraphQL endpoint.
     * This operation is asynchronous and may be canceled by calling
     * cancel on the returned operation. The response will be provided
     * to the response listener, and via Hub.  If there is data
     * present in the response, it will be cast as the requested class
     * type.
     *
     * @param apiName          The name of a configured API
     * @param graphQlRequest   Wrapper for request details
     * @param responseListener Invoked when response data/errors are available.  If null,
     *                         response can still be obtained via Hub.
     * @return An {@link ApiOperation} to track progress and provide
     * a means to cancel the asynchronous operation
     */
    @Override
    public <T> GraphQLOperation<T> mutate(
            @NonNull String apiName,
            @NonNull GraphQLRequest<T> graphQlRequest,
            @Nullable ResultListener<GraphQLResponse<T>> responseListener
    ) {
        return getSelectedPlugin().mutate(apiName, graphQlRequest, responseListener);
    }

    /**
     * This is a special helper method for easily subscribing to events
     * on Model objects which are autogenerated from your schema.
     * <p>
     * Initiates a GraphQL subscription against a configured GraphQL
     * endpoint. The operation is on-going and emits a stream of
     * {@link GraphQLResponse}s to the provided stream listener.
     * The subscription may be canceled by calling
     * {@link GraphQLOperation#cancel()}.
     *
     * @param apiName              The name of a previously configured GraphQL API
     * @param modelClass           The class of the Model we are subscribing to
     * @param predicate            Filtering conditions for the query
     * @param subscriptionType     What type of subscription to listen for (e.g. OnCreate, OnUpdate, OnDelete)
     * @param subscriptionListener A listener to receive notifications when new items are
     *                             available via the subscription stream
     * @return A GraphQLOperation representing this ongoing subscription
     */
    @Override
    public <T extends Model> GraphQLOperation<T> subscribe(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @NonNull FilteringPredicate<T> predicate,
            @NonNull SubscriptionType subscriptionType,
            @Nullable StreamListener<GraphQLResponse<T>> subscriptionListener
    ) {
        return getSelectedPlugin().subscribe(apiName, modelClass, predicate, subscriptionType, subscriptionListener);
    }

    /**
     * Initiates a GraphQL subscription against a configured GraphQL
     * endpoint. The operation is on-going and emits a stream of
     * {@link GraphQLResponse}s to the provided stream listener.
     * The subscription may be canceled by calling
     * {@link GraphQLOperation#cancel()}.
     *
     * @param apiName          The name of a configured API
     * @param graphQlRequest   Wrapper for request details
     * @param responseListener Invoked when response data/errors are available.  If null,
     *                         response can still be obtained via Hub.
     * @return A GraphQLOperation representing this ongoing subscription
     */
    @Override
    public <T> GraphQLOperation<T> subscribe(
            @NonNull String apiName,
            @NonNull GraphQLRequest<T> graphQlRequest,
            @Nullable ResultListener<GraphQLResponse<T>> responseListener
    ) {
        return getSelectedPlugin().subscribe(apiName, graphQlRequest, responseListener);
    }
}

