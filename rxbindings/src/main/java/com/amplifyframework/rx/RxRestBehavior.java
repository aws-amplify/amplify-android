/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.rx;

import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.RestBehavior;
import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.api.rest.RestResponse;

import io.reactivex.Single;

/**
 * An Rx-idiomatic expression of Amplify's {@link RestBehavior}.
 */
@SuppressWarnings("unused")
public interface RxRestBehavior {

    /**
     * This is a helper method for easily invoking GET HTTP request.
     * Requires that only one API is configured in your
     * `amplifyconfiguration.json`. Otherwise, emits an ApiException
     * via the Single's error callback.
     * @param request GET request object.
     * @return A cold single which emits an {@link RestResponse} on success,
     *         or an {@link ApiException} on failure to obtain a response.
     *         The network operation does not begin until subscription. The operation
     *         may be canceled by disposing the single subscription.
     */
    @NonNull
    Single<RestResponse> get(@NonNull RestOptions request);

    /**
     * This is a helper method for easily invoking GET HTTP request.
     * @param apiName The name of a configured API.
     * @param request GET request object.
     * @return A cold single which emits an {@link RestResponse} on success,
     *         or an {@link ApiException} on failure to obtain a response.
     *         The network operation does not begin until subscription. The operation
     *         may be canceled by disposing the single subscription.
     */
    @NonNull
    Single<RestResponse> get(@NonNull String apiName, @NonNull RestOptions request);

    /**
     * This is a helper method for easily invoking PUT HTTP request.
     * Requires that only one API is configured in your
     * `amplifyconfiguration.json`. Otherwise, emits an ApiException
     * via the returned Single's error callback.
     * @param request PUT request object.
     * @return A cold single which emits an {@link RestResponse} on success,
     *         or an {@link ApiException} on failure to obtain a response.
     *         The network operation does not begin until subscription. The operation
     *         may be canceled by disposing the single subscription.
     */
    @NonNull
    Single<RestResponse> put(@NonNull RestOptions request);

    /**
     * This is a helper method for easily invoking PUT HTTP request.
     * @param apiName The name of a configured API.
     * @param request PUT request object.
     * @return A cold single which emits an {@link RestResponse} on success,
     *         or an {@link ApiException} on failure to obtain a response.
     *         The network operation does not begin until subscription. The operation
     *         may be canceled by disposing the single subscription.
     */
    @NonNull
    Single<RestResponse> put(@NonNull String apiName, @NonNull RestOptions request);

    /**
     * This is a helper method for easily invoking POST HTTP request.
     * Requires that only one API is configured in your
     * `amplifyconfiguration.json`. Otherwise, emits an ApiException
     * via the returned Single's error callback.
     * @param request POST request object.
     * @return A cold single which emits an {@link RestResponse} on success,
     *         or an {@link ApiException} on failure to obtain a response.
     *         The network operation does not begin until subscription. The operation
     *         may be canceled by disposing the single subscription.
     */
    @NonNull
    Single<RestResponse> post(@NonNull RestOptions request);

    /**
     * This is a helper method for easily invoking POST HTTP request.
     * @param apiName The name of a configured API.
     * @param request POST request object.
     * @return A cold single which emits an {@link RestResponse} on success,
     *         or an {@link ApiException} on failure to obtain a response.
     *         The network operation does not begin until subscription. The operation
     *         may be canceled by disposing the single subscription.
     */
    @NonNull
    Single<RestResponse> post(@NonNull String apiName, @NonNull RestOptions request);

    /**
     * This is a helper method for easily invoking DELETE HTTP request.
     * Requires that only one API is configured in your
     * `amplifyconfiguration.json`. Otherwise, emits an ApiException
     * via the returned Single's error callback.
     * @param request DELETE request object.
     * @return A cold single which emits an {@link RestResponse} on success,
     *         or an {@link ApiException} on failure to obtain a response.
     *         The network operation does not begin until subscription. The operation
     *         may be canceled by disposing the single subscription.
     */
    @NonNull
    Single<RestResponse> delete(@NonNull RestOptions request);

    /**
     * This is a helper method for easily invoking DELETE HTTP request.
     * @param apiName The name of a configured API.
     * @param request DELETE request object.
     * @return A cold single which emits an {@link RestResponse} on success,
     *         or an {@link ApiException} on failure to obtain a response.
     *         The network operation does not begin until subscription. The operation
     *         may be canceled by disposing the single subscription.
     */
    @NonNull
    Single<RestResponse> delete(@NonNull String apiName, @NonNull RestOptions request);

    /**
     * This is a helper method for easily invoking HEAD HTTP request.
     * Requires that only one API is configured in your
     * `amplifyconfiguration.json`. Otherwise, emits an ApiException
     * via the returned Single's error callback.
     * @param request HEAD request object.
     * @return A cold single which emits an {@link RestResponse} on success,
     *         or an {@link ApiException} on failure to obtain a response.
     *         The network operation does not begin until subscription. The operation
     *         may be canceled by disposing the single subscription.
     */
    @NonNull
    Single<RestResponse> head(@NonNull RestOptions request);

    /**
     * This is a helper method for easily invoking HEAD HTTP request.
     * @param apiName The name of a configured API.
     * @param request HEAD request object.
     * @return A cold single which emits an {@link RestResponse} on success,
     *         or an {@link ApiException} on failure to obtain a response.
     *         The network operation does not begin until subscription. The operation
     *         may be canceled by disposing the single subscription.
     */
    @NonNull
    Single<RestResponse> head(@NonNull String apiName, @NonNull RestOptions request);

    /**
     * This is a helper method for easily invoking PATCH HTTP request.
     * Requires that only one API is configured in your
     * `amplifyconfiguration.json`. Otherwise, emits an ApiException
     * via the returned Single's error callback.
     * @param request PATCH request object.
     * @return A cold single which emits an {@link RestResponse} on success,
     *         or an {@link ApiException} on failure to obtain a response.
     *         The network operation does not begin until subscription. The operation
     *         may be canceled by disposing the single subscription.
     */
    @NonNull
    Single<RestResponse> patch(@NonNull RestOptions request);

    /**
     * This is a helper method for easily invoking PATCH HTTP request.
     * @param apiName The name of a configured API
     * @param request PATCH request object
     * @return A cold single which emits an {@link RestResponse} on success,
     *         or an {@link ApiException} on failure to obtain a response.
     *         The network operation does not begin until subscription. The operation
     *         may be canceled by disposing the single subscription.
     */
    @NonNull
    Single<RestResponse> patch(@NonNull String apiName, @NonNull RestOptions request);
}
