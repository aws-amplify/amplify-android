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

import com.amplifyframework.core.async.Result;

/**
 * Result of REST-verb operation on the API category.
 */
public final class ApiResult<T> implements Result {
    private final int statusCode;
    private final T data;

    /**
     * Constructs an API result object containing server response.
     * @param statusCode HTTP status code
     * @param data response data
     */
    public ApiResult(@NonNull int statusCode, T data) {
        this.statusCode = statusCode;
        this.data = data;
    }

    /**
     * Gets the HTTP status code.
     * @return HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Gets the response data.
     * @return response data
     */
    public T getData() {
        return data;
    }
}
