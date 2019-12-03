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
import com.amplifyframework.api.rest.RestOperation;
import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.StreamListener;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

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

    @Override
    public <T extends Model> GraphQLOperation<T> query(
            @NonNull Class<T> modelClass,
            @NonNull ResultListener<GraphQLResponse<Iterable<T>>> responseListener
    ) {
        return getSelectedPlugin().query(modelClass, responseListener);
    }

    @Override
    public <T extends Model> GraphQLOperation<T> query(
            @NonNull Class<T> modelClass,
            @NonNull String objectId,
            @NonNull ResultListener<GraphQLResponse<T>> responseListener
    ) {
        return getSelectedPlugin().query(modelClass, objectId, responseListener);
    }

    @Override
    public <T extends Model> GraphQLOperation<T> query(
            @NonNull Class<T> modelClass,
            QueryPredicate predicate,
            @NonNull ResultListener<GraphQLResponse<Iterable<T>>> responseListener
    ) {
        return getSelectedPlugin().query(modelClass, predicate, responseListener);
    }

    @Override
    public <T> GraphQLOperation<T> query(
            @NonNull GraphQLRequest<T> graphQlRequest,
            @NonNull ResultListener<GraphQLResponse<Iterable<T>>> responseListener
    ) {
        return getSelectedPlugin().query(graphQlRequest, responseListener);
    }

    @Override
    public <T extends Model> GraphQLOperation<T> query(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @NonNull ResultListener<GraphQLResponse<Iterable<T>>> responseListener
    ) {
        return getSelectedPlugin().query(apiName, modelClass, responseListener);
    }

    @Override
    public <T extends Model> GraphQLOperation<T> query(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @NonNull String objectId,
            @NonNull ResultListener<GraphQLResponse<T>> responseListener
    ) {
        return getSelectedPlugin().query(apiName, modelClass, objectId, responseListener);
    }

    @Override
    public <T extends Model> GraphQLOperation<T> query(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            QueryPredicate predicate,
            @NonNull ResultListener<GraphQLResponse<Iterable<T>>> responseListener
    ) {
        return getSelectedPlugin().query(apiName, modelClass, predicate, responseListener);
    }

    @Override
    public <T> GraphQLOperation<T> query(
            @NonNull String apiName,
            @NonNull GraphQLRequest<T> graphQlRequest,
            @NonNull ResultListener<GraphQLResponse<Iterable<T>>> responseListener
    ) {
        return getSelectedPlugin().query(apiName, graphQlRequest, responseListener);
    }

    @Override
    public <T extends Model> GraphQLOperation<T> mutate(
            @NonNull T model,
            @NonNull MutationType mutationType,
            @NonNull ResultListener<GraphQLResponse<T>> responseListener
    ) {
        return getSelectedPlugin().mutate(model, mutationType, responseListener);
    }

    @Override
    public <T extends Model> GraphQLOperation<T> mutate(
            @NonNull T model,
            QueryPredicate predicate,
            @NonNull MutationType mutationType,
            @NonNull ResultListener<GraphQLResponse<T>> responseListener
    ) {
        return getSelectedPlugin().mutate(model, predicate, mutationType, responseListener);
    }

    @Override
    public <T> GraphQLOperation<T> mutate(
            @NonNull GraphQLRequest<T> graphQlRequest,
            @NonNull ResultListener<GraphQLResponse<T>> responseListener
    ) {
        return getSelectedPlugin().mutate(graphQlRequest, responseListener);
    }

    @Override
    public <T extends Model> GraphQLOperation<T> mutate(
            @NonNull String apiName,
            @NonNull T model,
            @NonNull MutationType mutationType,
            @NonNull ResultListener<GraphQLResponse<T>> responseListener
    ) {
        return getSelectedPlugin().mutate(apiName, model, mutationType, responseListener);
    }

    @Override
    public <T extends Model> GraphQLOperation<T> mutate(
            @NonNull String apiName,
            @NonNull T model,
            QueryPredicate predicate,
            @NonNull MutationType mutationType,
            @NonNull ResultListener<GraphQLResponse<T>> responseListener
    ) {
        return getSelectedPlugin().mutate(apiName, model, predicate, mutationType, responseListener);
    }

    @Override
    public <T> GraphQLOperation<T> mutate(
            @NonNull String apiName,
            @NonNull GraphQLRequest<T> graphQlRequest,
            @NonNull ResultListener<GraphQLResponse<T>> responseListener
    ) {
        return getSelectedPlugin().mutate(apiName, graphQlRequest, responseListener);
    }

    @Override
    public <T extends Model> GraphQLOperation<T> subscribe(
            @NonNull Class<T> modelClass,
            @NonNull SubscriptionType subscriptionType,
            @NonNull StreamListener<GraphQLResponse<T>> subscriptionListener) {
        return getSelectedPlugin().subscribe(modelClass, subscriptionType, subscriptionListener);
    }

    @Override
    public <T> GraphQLOperation<T> subscribe(
            @NonNull GraphQLRequest<T> graphQlRequest,
            @NonNull StreamListener<GraphQLResponse<T>> subscriptionListener
    ) {
        return getSelectedPlugin().subscribe(graphQlRequest, subscriptionListener);
    }

    @Override
    public <T extends Model> GraphQLOperation<T> subscribe(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @NonNull SubscriptionType subscriptionType,
            @NonNull StreamListener<GraphQLResponse<T>> subscriptionListener) {
        return getSelectedPlugin().subscribe(apiName, modelClass, subscriptionType, subscriptionListener);
    }

    @Override
    public <T> GraphQLOperation<T> subscribe(
            @NonNull String apiName,
            @NonNull GraphQLRequest<T> graphQlRequest,
            @NonNull StreamListener<GraphQLResponse<T>> subscriptionListener
    ) {
        return getSelectedPlugin().subscribe(apiName, graphQlRequest, subscriptionListener);
    }

    @Override
    public RestOperation get(
            @NonNull RestOptions request,
            @Nullable ResultListener<RestResponse> responseListener
    ) {
        return getSelectedPlugin().get(request, responseListener);
    }

    @Override
    public RestOperation get(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @Nullable ResultListener<RestResponse> responseListener
    ) {
        return getSelectedPlugin().get(apiName, request, responseListener);
    }

    @Override
    public RestOperation put(
            @NonNull RestOptions request,
            @Nullable ResultListener<RestResponse> responseListener
    ) {
        return getSelectedPlugin().put(request, responseListener);
    }

    @Override
    public RestOperation put(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @Nullable ResultListener<RestResponse> responseListener
    ) {
        return getSelectedPlugin().put(apiName, request, responseListener);
    }

    @Override
    public RestOperation post(
            @NonNull RestOptions request,
            @Nullable ResultListener<RestResponse> responseListener
    ) {
        return getSelectedPlugin().post(request, responseListener);
    }

    @Override
    public RestOperation post(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @Nullable ResultListener<RestResponse> responseListener
    ) {
        return getSelectedPlugin().post(apiName, request, responseListener);
    }

    @Override
    public RestOperation delete(
            @NonNull RestOptions request,
            @Nullable ResultListener<RestResponse> responseListener
    ) {
        return getSelectedPlugin().delete(request, responseListener);
    }

    @Override
    public RestOperation delete(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @Nullable ResultListener<RestResponse> responseListener
    ) {
        return getSelectedPlugin().delete(apiName, request, responseListener);
    }

    @Override
    public RestOperation head(
            @NonNull RestOptions request,
            @Nullable ResultListener<RestResponse> responseListener
    ) {
        return getSelectedPlugin().head(request, responseListener);
    }

    @Override
    public RestOperation head(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @Nullable ResultListener<RestResponse> responseListener
    ) {
        return getSelectedPlugin().head(apiName, request, responseListener);
    }

    @Override
    public RestOperation patch(
            @NonNull RestOptions request,
            @Nullable ResultListener<RestResponse> responseListener
    ) {
        return getSelectedPlugin().patch(request, responseListener);
    }

    @Override
    public RestOperation patch(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @Nullable ResultListener<RestResponse> responseListener
    ) {
        return getSelectedPlugin().patch(apiName, request, responseListener);
    }
}

