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
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;

import java.util.Map;

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
    public <T> GraphQLOperation<T> query(
            @NonNull String apiName,
            @NonNull String gqlDocument,
            @Nullable Map<String, Object> variables,
            @NonNull Class<T> classToCast,
            @Nullable ResultListener<GraphQLResponse<T>> responseListener) {
        return getSelectedPlugin().query(apiName, gqlDocument, variables, classToCast, responseListener);
    }

    @Override
    public <T> GraphQLOperation<T> query(@NonNull String apiName,
                                         @NonNull GraphQLRequest<T> graphQlRequest,
                                         @Nullable ResultListener<GraphQLResponse<T>> responseListener) {
        return getSelectedPlugin().query(apiName, graphQlRequest, responseListener);
    }

    @Override
    public <T> GraphQLOperation<T> mutate(
            @NonNull String apiName,
            @NonNull String gqlDocument,
            @Nullable Map<String, Object> variables,
            @NonNull Class<T> classToCast,
            @Nullable ResultListener<GraphQLResponse<T>> responseListener) {
        return getSelectedPlugin().mutate(apiName, gqlDocument, variables, classToCast, responseListener);
    }

    @Override
    public <T> GraphQLOperation<T> mutate(@NonNull String apiName,
                                          @NonNull GraphQLRequest<T> graphQlRequest,
                                          @Nullable ResultListener<GraphQLResponse<T>> responseListener) {
        return getSelectedPlugin().mutate(apiName, graphQlRequest, responseListener);
    }


    @Override
    public <T> GraphQLOperation<T> subscribe(
            @NonNull String apiName,
            @NonNull String gqlDocument,
            @Nullable Map<String, String> variables,
            @NonNull Class<T> classToCast,
            @Nullable StreamListener<GraphQLResponse<T>> subscriptionListener) {
        return getSelectedPlugin().subscribe(apiName, gqlDocument, variables, classToCast, subscriptionListener);
    }
}

