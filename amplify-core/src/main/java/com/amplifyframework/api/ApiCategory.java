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

import com.amplifyframework.api.operation.ApiOperation;
import com.amplifyframework.core.async.Listener;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;

import org.json.JSONObject;

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
    public ApiOperation get(@NonNull String apiName,
                            @NonNull String path,
                            @Nullable JSONObject init) throws ApiException {
        return getSelectedPlugin().get(apiName, path, init);
    }

    @Override
    public <T> ApiOperation get(@NonNull String apiName,
                                @NonNull String path,
                                @Nullable JSONObject init,
                                Listener<ApiResult<T>> listener) throws ApiException {
        return getSelectedPlugin().get(apiName, path, init, listener);
    }

    @Override
    public ApiOperation post(@NonNull String apiName,
                             @NonNull String path,
                             @Nullable JSONObject init) throws ApiException {
        return getSelectedPlugin().post(apiName, path, init);
    }

    @Override
    public <T> ApiOperation post(@NonNull String apiName,
                                 @NonNull String path,
                                 @Nullable JSONObject init,
                                 Listener<ApiResult<T>> listener) throws ApiException {
        return getSelectedPlugin().post(apiName, path, init, listener);
    }

    @Override
    public ApiOperation put(@NonNull String apiName,
                            @NonNull String path,
                            @Nullable JSONObject init) throws ApiException {
        return getSelectedPlugin().put(apiName, path, init);
    }

    @Override
    public <T> ApiOperation put(@NonNull String apiName,
                                @NonNull String path,
                                @Nullable JSONObject init,
                                Listener<ApiResult<T>> listener) throws ApiException {
        return getSelectedPlugin().put(apiName, path, init, listener);
    }

    @Override
    public ApiOperation patch(@NonNull String apiName,
                              @NonNull String path,
                              @Nullable JSONObject init) throws ApiException {
        return getSelectedPlugin().patch(apiName, path, init);
    }

    @Override
    public <T> ApiOperation patch(@NonNull String apiName,
                                  @NonNull String path,
                                  @Nullable JSONObject init,
                                  Listener<ApiResult<T>> listener) throws ApiException {
        return getSelectedPlugin().patch(apiName, path, init, listener);
    }

    @Override
    public ApiOperation delete(@NonNull String apiName,
                               @NonNull String path,
                               @Nullable JSONObject init) throws ApiException {
        return getSelectedPlugin().delete(apiName, path, init);
    }

    @Override
    public <T> ApiOperation delete(@NonNull String apiName,
                                   @NonNull String path,
                                   @Nullable JSONObject init,
                                   Listener<ApiResult<T>> listener) throws ApiException {
        return getSelectedPlugin().delete(apiName, path, init, listener);
    }

    @Override
    public ApiOperation head(@NonNull String apiName,
                             @NonNull String path,
                             @Nullable JSONObject init) throws ApiException {
        return getSelectedPlugin().head(apiName, path, init);
    }

    @Override
    public <T> ApiOperation head(@NonNull String apiName,
                                 @NonNull String path,
                                 @Nullable JSONObject init,
                                 Listener<ApiResult<T>> listener) throws ApiException {
        return getSelectedPlugin().head(apiName, path, init, listener);
    }

    @Override
    public String endpoint(@NonNull String apiName) throws ApiException {
        return getSelectedPlugin().endpoint(apiName);
    }
}

