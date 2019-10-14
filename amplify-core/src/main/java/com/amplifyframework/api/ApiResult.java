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
public final class ApiResult implements Result {
    private int statusCode;
    private String jsonPayload;

    /**
     * Constructs an API result object containing server response.
     * @param statusCode HTTP status code
     * @param jsonPayload response JSON data
     */
    public ApiResult(@NonNull int statusCode, String jsonPayload) {
        this.statusCode = statusCode;
        this.jsonPayload = jsonPayload;
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
     * @return response JSON data
     */
    public String getJsonPayload() {
        return jsonPayload;
    }
}
