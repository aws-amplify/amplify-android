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

import com.amplifyframework.api.rest.RestOperation;
import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.core.ResultListener;

/**
 * REST behaviors which include the family of HTTP verbs (GET, POST, etc.).
 */
public interface RestBehavior {

    /**
     * This is a helper method for easily invoking GET HTTP request.
     * Requires that only one API is configured in your
     * `amplifyconfiguration.json`. Otherwise, emits an ApiException
     * to the provided `ResultListener`.
     *
     * @param request GET request object.
     * @param responseListener
     *      Invoked when response data/errors are available. If null,
     *      response can still be obtained via Hub.
     * @return An {@link ApiOperation} to track progress and provide
     * a means to cancel the asynchronous operation
     */
    @Nullable
    RestOperation get(
            @NonNull RestOptions request,
            @NonNull ResultListener<RestResponse, ApiException> responseListener
    );

    /**
     * This is a helper method for easily invoking GET HTTP request.
     *
     * @param apiName The name of a configured API.
     * @param request GET request object.
     * @param responseListener
     *      Invoked when response data/errors are available. If null,
     *      response can still be obtained via Hub.
     * @return An {@link ApiOperation} to track progress and provide
     * a means to cancel the asynchronous operation
     */
    @Nullable
    RestOperation get(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @NonNull ResultListener<RestResponse, ApiException> responseListener
    );

    /**
     * This is a helper method for easily invoking PUT HTTP request.
     * Requires that only one API is configured in your
     * `amplifyconfiguration.json`. Otherwise, emits an ApiException
     * to the provided `ResultListener`.
     *
     * @param request PUT request object.
     * @param responseListener
     *      Invoked when response data/errors are available. If null,
     *      response can still be obtained via Hub.
     * @return An {@link ApiOperation} to track progress and provide
     * a means to cancel the asynchronous operation
     */
    @Nullable
    RestOperation put(
            @NonNull RestOptions request,
            @NonNull ResultListener<RestResponse, ApiException> responseListener
    );

    /**
     * This is a helper method for easily invoking PUT HTTP request.
     *
     * @param apiName The name of a configured API.
     * @param request PUT request object.
     * @param responseListener
     *      Invoked when response data/errors are available. If null,
     *      response can still be obtained via Hub.
     * @return An {@link ApiOperation} to track progress and provide
     * a means to cancel the asynchronous operation
     */
    @Nullable
    RestOperation put(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @NonNull ResultListener<RestResponse, ApiException> responseListener
    );

    /**
     * This is a helper method for easily invoking POST HTTP request.
     * Requires that only one API is configured in your
     * `amplifyconfiguration.json`. Otherwise, emits an ApiException
     * to the provided `ResultListener`.
     *
     * @param request POST request object.
     * @param responseListener
     *      Invoked when response data/errors are available. If null,
     *      response can still be obtained via Hub.
     * @return An {@link ApiOperation} to track progress and provide
     * a means to cancel the asynchronous operation
     */
    @Nullable
    RestOperation post(
            @NonNull RestOptions request,
            @NonNull ResultListener<RestResponse, ApiException> responseListener
    );

    /**
     * This is a helper method for easily invoking POST HTTP request.
     *
     * @param apiName The name of a configured API.
     * @param request POST request object.
     * @param responseListener
     *      Invoked when response data/errors are available. If null,
     *      response can still be obtained via Hub.
     * @return An {@link ApiOperation} to track progress and provide
     * a means to cancel the asynchronous operation
     */
    @Nullable
    RestOperation post(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @NonNull ResultListener<RestResponse, ApiException> responseListener
    );

    /**
     * This is a helper method for easily invoking DELETE HTTP request.
     * Requires that only one API is configured in your
     * `amplifyconfiguration.json`. Otherwise, emits an ApiException
     * to the provided `ResultListener`.
     *
     * @param request DELETE request object.
     * @param responseListener
     *      Invoked when response data/errors are available. If null,
     *      response can still be obtained via Hub.
     * @return An {@link ApiOperation} to track progress and provide
     * a means to cancel the asynchronous operation
     */
    @Nullable
    RestOperation delete(
            @NonNull RestOptions request,
            @NonNull ResultListener<RestResponse, ApiException> responseListener
    );

    /**
     * This is a helper method for easily invoking DELETE HTTP request.
     *
     * @param apiName The name of a configured API.
     * @param request DELETE request object.
     * @param responseListener
     *      Invoked when response data/errors are available. If null,
     *      response can still be obtained via Hub.
     * @return An {@link ApiOperation} to track progress and provide
     * a means to cancel the asynchronous operation
     */
    @Nullable
    RestOperation delete(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @NonNull ResultListener<RestResponse, ApiException> responseListener
    );

    /**
     * This is a helper method for easily invoking HEAD HTTP request.
     * Requires that only one API is configured in your
     * `amplifyconfiguration.json`. Otherwise, emits an ApiException
     * to the provided `ResultListener`.
     *
     * @param request HEAD request object.
     * @param responseListener
     *      Invoked when response data/errors are available. If null,
     *      response can still be obtained via Hub.
     * @return An {@link ApiOperation} to track progress and provide
     * a means to cancel the asynchronous operation
     */
    @Nullable
    RestOperation head(
            @NonNull RestOptions request,
            @NonNull ResultListener<RestResponse, ApiException> responseListener
    );

    /**
     * This is a helper method for easily invoking HEAD HTTP request.
     *
     * @param apiName The name of a configured API.
     * @param request HEAD request object.
     * @param responseListener
     *      Invoked when response data/errors are available. If null,
     *      response can still be obtained via Hub.
     * @return An {@link ApiOperation} to track progress and provide
     * a means to cancel the asynchronous operation
     */
    @Nullable
    RestOperation head(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @NonNull ResultListener<RestResponse, ApiException> responseListener
    );

    /**
     * This is a helper method for easily invoking PATCH HTTP request.
     * Requires that only one API is configured in your
     * `amplifyconfiguration.json`. Otherwise, emits an ApiException
     * to the provided `ResultListener`.
     *
     * @param request PATCH request object.
     * @param responseListener
     *      Invoked when response data/errors are available. If null,
     *      response can still be obtained via Hub.
     * @return An {@link ApiOperation} to track progress and provide
     * a means to cancel the asynchronous operation
     */
    @Nullable
    RestOperation patch(
            @NonNull RestOptions request,
            @NonNull ResultListener<RestResponse, ApiException> responseListener
    );

    /**
     * This is a helper method for easily invoking PATCH HTTP request.
     *
     * @param apiName The name of a configured API.
     * @param request PATCH request object.
     * @param responseListener
     *      Invoked when response data/errors are available. If null,
     *      response can still be obtained via Hub.
     * @return An {@link ApiOperation} to track progress and provide
     * a means to cancel the asynchronous operation
     */
    @Nullable
    RestOperation patch(
            @NonNull String apiName,
            @NonNull RestOptions request,
            @NonNull ResultListener<RestResponse, ApiException> responseListener
    );
}
