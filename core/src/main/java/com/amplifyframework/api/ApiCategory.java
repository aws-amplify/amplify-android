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
import com.amplifyframework.api.rest.RestOperation;
import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;

/**
 * The API category provides methods for interacting with remote systems
 * using REST and GraphQL constructs. The category is implemented by
 * zero or more {@link ApiPlugin}. The operations made available by the
 * category are defined in the {@link ApiCategoryBehavior}.
 */
public final class ApiCategory extends Category<ApiPlugin<?>> implements ApiCategoryBehavior {
    @NonNull
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.API;
    }

    @Nullable
    @Override
    public <R> GraphQLOperation<R> query(
            @NonNull GraphQLRequest<R> graphQlRequest,
            @NonNull Consumer<GraphQLResponse<R>> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        return getSelectedPlugin().query(graphQlRequest, onResponse, onFailure);
    }

    @Nullable
    @Override
    public <R> GraphQLOperation<R> query(
            @NonNull String apiName,
            @NonNull GraphQLRequest<R> graphQlRequest,
            @NonNull Consumer<GraphQLResponse<R>> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        return getSelectedPlugin().query(apiName, graphQlRequest, onResponse, onFailure);
    }

    @Nullable
    @Override
    public <R> GraphQLOperation<R> mutate(
            @NonNull GraphQLRequest<R> graphQlRequest,
            @NonNull Consumer<GraphQLResponse<R>> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        return getSelectedPlugin().mutate(graphQlRequest, onResponse, onFailure);
    }

    @Nullable
    @Override
    public <T> GraphQLOperation<T> mutate(
            @NonNull String apiName,
            @NonNull GraphQLRequest<T> graphQlRequest,
            @NonNull Consumer<GraphQLResponse<T>> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        return getSelectedPlugin().mutate(apiName, graphQlRequest, onResponse, onFailure);
    }

    @Nullable
    @Override
    public <T> GraphQLOperation<T> subscribe(
            @NonNull GraphQLRequest<T> graphQlRequest,
            @NonNull Consumer<String> onSubscriptionEstablished,
            @NonNull Consumer<GraphQLResponse<T>> onNextResponse,
            @NonNull Consumer<ApiException> onSubscriptionFailure,
            @NonNull Action onSubscriptionCompleted) {
        return getSelectedPlugin().subscribe(graphQlRequest,
                onSubscriptionEstablished, onNextResponse, onSubscriptionFailure, onSubscriptionCompleted);
    }

    @Nullable
    @Override
    public <T> GraphQLOperation<T> subscribe(
            @NonNull String apiName,
            @NonNull GraphQLRequest<T> graphQlRequest,
            @NonNull Consumer<String> onSubscriptionEstablished,
            @NonNull Consumer<GraphQLResponse<T>> onNextResponse,
            @NonNull Consumer<ApiException> onSubscriptionFailure,
            @NonNull Action onSubscriptionCompleted) {
        return getSelectedPlugin().subscribe(apiName, graphQlRequest,
                onSubscriptionEstablished, onNextResponse, onSubscriptionFailure, onSubscriptionCompleted);
    }

    @Nullable
    @Override
    public RestOperation get(
            @NonNull RestOptions request,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        return getSelectedPlugin().get(request, onResponse, onFailure);
    }

    @Nullable
    @Override
    public RestOperation get(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        return getSelectedPlugin().get(apiName, request, onResponse, onFailure);
    }

    @Nullable
    @Override
    public RestOperation put(
            @NonNull RestOptions request,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        return getSelectedPlugin().put(request, onResponse, onFailure);
    }

    @Nullable
    @Override
    public RestOperation put(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        return getSelectedPlugin().put(apiName, request, onResponse, onFailure);
    }

    @Nullable
    @Override
    public RestOperation post(
            @NonNull RestOptions request,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        return getSelectedPlugin().post(request, onResponse, onFailure);
    }

    @Nullable
    @Override
    public RestOperation post(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        return getSelectedPlugin().post(apiName, request, onResponse, onFailure);
    }

    @Nullable
    @Override
    public RestOperation delete(
            @NonNull RestOptions request,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        return getSelectedPlugin().delete(request, onResponse, onFailure);
    }

    @Nullable
    @Override
    public RestOperation delete(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        return getSelectedPlugin().delete(apiName, request, onResponse, onFailure);
    }

    @Nullable
    @Override
    public RestOperation head(
            @NonNull RestOptions request,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        return getSelectedPlugin().head(request, onResponse, onFailure);
    }

    @Nullable
    @Override
    public RestOperation head(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        return getSelectedPlugin().head(apiName, request, onResponse, onFailure);
    }

    @Nullable
    @Override
    public RestOperation patch(
            @NonNull RestOptions request,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        return getSelectedPlugin().patch(request, onResponse, onFailure);
    }

    @Nullable
    @Override
    public RestOperation patch(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        return getSelectedPlugin().patch(apiName, request, onResponse, onFailure);
    }
}
