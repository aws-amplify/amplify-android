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

package com.amplifyframework.api.rest;

import com.amplifyframework.core.Immutable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * Request against REST endpoint.
 */
public final class RestOptions {

    private final String path;
    private final byte[] data;
    private final Map<String, String> queryParameters;

    /**
     * Construct a REST request.
     * @param path Path for the endpoint to make the request
     * @param data Data for the rest option
     * @param queryParameters Query parameters for the request. This value is nullable
     */
    public RestOptions(String path,
                       byte[] data,
                       Map<String, String> queryParameters) {
        this.path = path;
        this.data = data == null ? null : Arrays.copyOf(data, data.length);
        this.queryParameters = queryParameters == null ? Collections.emptyMap() : Immutable.of(queryParameters);
    }

    /**
     * Construct a REST request.
     * @param path Path for the endpoint to make the request
     * @param queryParameters Query parameters for the request. This value is nullable
     */
    public RestOptions(String path,
                       Map<String, String> queryParameters) {
        this(path, null, queryParameters);
    }

    /**
     * Construct a REST request.
     * @param path Path for the endpoint to make the request
     */
    public RestOptions(String path) {
        this(path, null, Collections.emptyMap());
    }

    /**
     * Returns the path for the request.
     * @return Path for the HTTP REST request
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the query parameters if present.
     * @return Map of query parameters
     */
    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

    /**
     * Returns the data if present.
     * @return Data for the request.
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Checks if the options contains data.
     * @return True if data is not null.
     */
    public boolean hasData() {
        return data != null;
    }
}
