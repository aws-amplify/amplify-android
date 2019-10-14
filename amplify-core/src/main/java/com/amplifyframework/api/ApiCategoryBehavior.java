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

import org.json.JSONObject;

/**
 * API category behaviors include REST and GraphQL operations. These
 * include the family of HTTP verbs (GET, POST, etc.), and the GraphQL
 * query/subscribe/mutate operations.
 */
public interface ApiCategoryBehavior {

    /**
     * Make an asynchronous GET request.
     * Messages will be delivered via Hub.
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param init request extra parameters
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to obtain the requested resource from API.
     */
    <T> ApiOperation get(@NonNull String apiName,
                         @NonNull String path,
                         @Nullable JSONObject init) throws ApiException;

    /**
     * Make an asynchronous GET request with
     * local callback.
     * Messages will still be delivered via Hub also.
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param init request extra parameters
     * @param listener API callback listener
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to obtain the requested resource from API.
     */
    <T> ApiOperation get(@NonNull String apiName,
                         @NonNull String path,
                         @Nullable JSONObject init, Listener<ApiResult<T>> listener) throws ApiException;

    /**
     * Make an asynchronous POST request.
     * Messages will be delivered via Hub.
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param init request extra parameters
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to post the requested resource to API.
     */
    <T> ApiOperation post(@NonNull String apiName,
                          @NonNull String path,
                          @Nullable JSONObject init) throws ApiException;

    /**
     * Make an asynchronous POST request with
     * local callback.
     * Messages will still be delivered via Hub also.
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param init request extra parameters
     * @param listener API callback listener
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to post the requested resource to API.
     */
    <T> ApiOperation post(@NonNull String apiName,
                          @NonNull String path,
                          @Nullable JSONObject init, Listener<ApiResult<T>> listener) throws ApiException;

    /**
     * Make an asynchronous PUT request.
     * Messages will be delivered via Hub.
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param init request extra parameters
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to put the requested resource to API.
     */
    <T> ApiOperation put(@NonNull String apiName,
                         @NonNull String path,
                         @Nullable JSONObject init) throws ApiException;

    /**
     * Make an asynchronous PUT request with
     * local callback.
     * Messages will still be delivered via Hub also.
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param init request extra parameters
     * @param listener API callback listener
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to put the requested resource to API.
     */
    <T> ApiOperation put(@NonNull String apiName,
                         @NonNull String path,
                         @Nullable JSONObject init, Listener<ApiResult<T>> listener) throws ApiException;

    /**
     * Make an asynchronous PATCH request.
     * Messages will be delivered via Hub.
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param init request extra parameters
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to patch the requested resource in API.
     */
    <T> ApiOperation patch(@NonNull String apiName,
                           @NonNull String path,
                           @Nullable JSONObject init) throws ApiException;

    /**
     * Make an asynchronous PATCH request with
     * local callback.
     * Messages will still be delivered via Hub also.
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param init request extra parameters
     * @param listener API callback listener
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to patch the requested resource in API.
     */
    <T> ApiOperation patch(@NonNull String apiName,
                           @NonNull String path,
                           @Nullable JSONObject init, Listener<ApiResult<T>> listener) throws ApiException;

    /**
     * Make an asynchronous DELETE request.
     * Messages will be delivered via Hub.
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param init request extra parameters
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to delete the requested resource in API.
     */
    <T> ApiOperation delete(@NonNull String apiName,
                            @NonNull String path,
                            @Nullable JSONObject init) throws ApiException;

    /**
     * Make an asynchronous DELETE request with
     * local callback.
     * Messages will still be delivered via Hub also.
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param init request extra parameters
     * @param listener API callback listener
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to patch the requested resource in API.
     */
    <T> ApiOperation delete(@NonNull String apiName,
                            @NonNull String path,
                            @Nullable JSONObject init, Listener<ApiResult<T>> listener) throws ApiException;

    /**
     * Make an asynchronous HEAD request.
     * Messages will be delivered via Hub.
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param init request extra parameters
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to obtain the requested header from API.
     */
    <T> ApiOperation head(@NonNull String apiName,
                          @NonNull String path,
                          @Nullable JSONObject init) throws ApiException;

    /**
     * Make an asynchronous HEAD request with
     * local callback.
     * Messages will still be delivered via Hub also.
     * @param apiName the API name of the request
     * @param path the path of the request
     * @param init request extra parameters
     * @param listener API callback listener
     * @return an operation object that provides notifications and
     *         actions related to API invocation
     * @throws ApiException
     *         On failure to obtain the requested header from API.
     */
    <T> ApiOperation head(@NonNull String apiName,
                          @NonNull String path,
                          @Nullable JSONObject init, Listener<ApiResult<T>> listener) throws ApiException;

    /**
     * Get endpoint for API.
     * @param apiName the name of the API
     * @return the endpoint of the API
     * @throws ApiException when there is no API associated with the name.
     */
    String endpoint(@NonNull String apiName) throws ApiException;
}

