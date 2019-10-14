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

import com.amplifyframework.api.operation.ApiOperation;
import com.amplifyframework.core.async.Listener;

/**
 * API category behaviors include REST and GraphQL operations. These
 * include the family of HTTP verbs (GET, POST, etc.), and the GraphQL
 * query/subscribe/mutate operations.
 */
public interface ApiCategoryBehavior {

    /**
     * Make a GET request
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param json request extra parameters
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to obtain the requested resource from API.
     */
    ApiOperation get(@NonNull String apiName,
                     @NonNull String path, String json) throws ApiException;

    /**
     * Make a GET request with a callback
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param json request extra parameters
     * @param listener API callback listener
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to obtain the requested resource from API.
     */
    ApiOperation get(@NonNull String apiName,
                     @NonNull String path,
                     String json, Listener<ApiResult> listener) throws ApiException;

    /**
     * Make a POST request
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param json request extra parameters
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to post the requested resource to API.
     */
    ApiOperation post(@NonNull String apiName,
                      @NonNull String path, String json) throws ApiException;

    /**
     * Make a POST request with a callback
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param json request extra parameters
     * @param listener API callback listener
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to post the requested resource to API.
     */
    ApiOperation post(@NonNull String apiName,
                      @NonNull String path,
                      String json, Listener<ApiResult> listener) throws ApiException;

    /**
     * Make a PUT request
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param json request extra parameters
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to put the requested resource to API.
     */
    ApiOperation put(@NonNull String apiName,
                      @NonNull String path, String json) throws ApiException;

    /**
     * Make a PUT request with a callback
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param json request extra parameters
     * @param listener API callback listener
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to put the requested resource to API.
     */
    ApiOperation put(@NonNull String apiName,
                     @NonNull String path,
                     String json, Listener<ApiResult> listener) throws ApiException;

    /**
     * Make a PATCH request
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param json request extra parameters
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to patch the requested resource in API.
     */
    ApiOperation patch(@NonNull String apiName,
                      @NonNull String path, String json) throws ApiException;

    /**
     * Make a PATCH request with a callback
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param json request extra parameters
     * @param listener API callback listener
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to patch the requested resource in API.
     */
    ApiOperation patch(@NonNull String apiName,
                       @NonNull String path,
                       String json, Listener<ApiResult> listener) throws ApiException;

    /**
     * Make a DELETE request
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param json request extra parameters
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to delete the requested resource in API.
     */
    ApiOperation delete(@NonNull String apiName,
                      @NonNull String path, String json) throws ApiException;

    /**
     * Make a DELETE request with a callback
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param json request extra parameters
     * @param listener API callback listener
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to patch the requested resource in API.
     */
    ApiOperation delete(@NonNull String apiName,
                       @NonNull String path,
                       String json, Listener<ApiResult> listener) throws ApiException;

    /**
     * Make a HEAD request
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param json request extra parameters
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to obtain the requested header from API.
     */
    ApiOperation head(@NonNull String apiName,
                      @NonNull String path, String json) throws ApiException;

    /**
     * Make a HEAD request wit ha callback
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param json request extra parameters
     * @param listener API callback listener
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to obtain the requested header from API.
     */
    ApiOperation head(@NonNull String apiName,
                      @NonNull String path,
                      String json, Listener<ApiResult> listener) throws ApiException;

    /**
     * Get endpoint for API
     * @param apiName the name of the API
     * @return the endpoint of the API
     * @throws ApiException when there is no API associated with the name.
     */
    String endpoint(@NonNull String apiName) throws ApiException;
}

