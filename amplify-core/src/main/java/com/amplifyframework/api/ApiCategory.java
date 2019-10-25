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

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.async.Listener;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;

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
    public <T> ApiOperation<T, GraphQLResponse<T>> query(@NonNull String apiName,
                                                         @NonNull String operationGql,
                                                         @NonNull Class<T> classToCast,
                                                         @Nullable Listener<GraphQLResponse<T>> callback) {
        return getSelectedPlugin().query(apiName, operationGql, classToCast, callback);
    }

    @Override
    public <T> ApiOperation<T, GraphQLResponse<T>> mutate(@NonNull String apiName,
                                                          @NonNull String operationGql,
                                                          @NonNull Class<T> classToCast,
                                                          @Nullable Listener<GraphQLResponse<T>> callback) {
        return getSelectedPlugin().mutate(apiName, operationGql, classToCast, callback);
    }
}

