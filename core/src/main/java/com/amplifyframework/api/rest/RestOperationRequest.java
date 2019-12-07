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

import com.amplifyframework.util.Immutable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * Request object used by the RestOperation.
 */
public final class RestOperationRequest {

    private final HttpMethod httpMethod;
    private final String path;
    private final byte[] data;
    private final Map<String, String> headers;
    private final Map<String, String> queryParameters;

    /**
     * Constructs a request object for RestOperation.
     * @param httpMethod The rest operation type
     * @param path Path against which the request is made.
     * @param data Data for the rest option
     * @param headers Header map for the request
     * @param queryParameters Query parameters for the request.
     */
    public RestOperationRequest(HttpMethod httpMethod,
                                String path,
                                byte[] data,
                                Map<String, String> headers,
                                Map<String, String> queryParameters) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.headers = headers == null ? Collections.emptyMap() : Immutable.of(headers);
        this.data = data == null ? null : Arrays.copyOf(data, data.length);
        this.queryParameters = queryParameters == null ? Collections.emptyMap() : Immutable.of(queryParameters);
    }

    /**
     * Constructs a request object for RestOperation.
     * @param httpMethod The rest operation type
     * @param path Path against which the request is made.
     * @param headers Header map for the request.
     * @param queryParameters Query parameters for the request.
     */
    public RestOperationRequest(HttpMethod httpMethod,
                                String path,
                                Map<String, String> headers,
                                Map<String, String> queryParameters) {
        this(httpMethod, path, null, headers, queryParameters);
    }

    /**
     * Returns the operation type of the request.
     * @return Operation type of the request.
     */
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    /**
     * Path to be added to the endpoint.
     * @return URL path
     */
    public String getPath() {
        return path;
    }

    /**
     * Query parameters for the request.
     * @return Query parameters
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
     * Returns the headers if present.
     * @return Header map, null if not present
     */
    public Map<String, String> getHeaders() {
        return headers;
    }
}
