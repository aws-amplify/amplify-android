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
import java.util.HashMap;
import java.util.Map;

/**
 * Request against REST endpoint.
 */
public final class RestOptions {

    private final String path;
    private final byte[] data;
    private final Map<String, String> headers;
    private final Map<String, String> queryParameters;

    /**
     * Construct a REST request.
     * @param path Path for the endpoint to make the request
     * @param data Data for the rest option
     * @param headers Headers for the request.
     * @param queryParameters Query parameters for the request. This value is nullable
     */
    public RestOptions(String path,
                       byte[] data,
                       Map<String, String> headers,
                       Map<String, String> queryParameters) {
        this.path = path;
        this.data = data == null ? null : Arrays.copyOf(data, data.length);
        this.headers = headers == null ? Collections.emptyMap() : Immutable.of(headers);
        this.queryParameters = queryParameters == null ? Collections.emptyMap() : Immutable.of(queryParameters);
    }

    /**
     * Construct a REST request.
     * @param path Path for the endpoint to make the request
     * @param data Data for the rest option
     */
    public RestOptions(String path,
                       byte[] data) {
        this(path, data, null, null);
    }

    /**
     * Construct a REST request.
     * @param path Path for the endpoint to make the request
     * @param queryParameters Query parameters for the request. This value is nullable
     */
    public RestOptions(String path,
                       Map<String, String> queryParameters) {
        this(path, null, null, queryParameters);
    }

    /**
     * Construct a REST request.
     * @param path Path for the endpoint to make the request
     */
    public RestOptions(String path) {
        this(path, null, null, null);
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
     * Returns the header map if present.
     * @return Map of header key values
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Checks if the options contains data.
     * @return True if data is not null.
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * Gets a builder instance.
     * @return A builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A utility that can be used to configure and construct immutable
     * instances of the {@link Builder}, by chaining
     * fluent configuration method calls.
     */
    public static final class Builder {

        private String path;
        private byte[] data;
        private Map<String, String> queryParameters;
        private Map<String, String> headers;

        Builder() { }

        /**
         * Configures the path for the request.
         * @param path Path for request.
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder addPath(final String path) {
            this.path = path;
            return this;
        }

        /**
         * Configures the body of the request.
         * @param data Body of the request in byte array.
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder addBody(final byte[] data) {
            this.data = data;
            return this;
        }

        /**
         * Configures the query parameters for the request.
         * @param queryParameters Query parameters for the request.
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder addQueryParameters(final Map<String, String> queryParameters) {
            if (this.queryParameters == null) {
                this.queryParameters = new HashMap<String, String>();
            }
            this.queryParameters.putAll(queryParameters);
            return this;
        }

        /**
         * Configures the headers for the request.
         * @param headers Header map for the request.
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder addHeaders(final Map<String, String> headers) {
            if (this.headers == null) {
                this.headers = new HashMap<String, String>();
            }
            this.headers.putAll(headers);
            return this;
        }

        /**
         * Configure header for the request.
         * @param key Header key
         * @param value Header value
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder addHeader(final String key, final String value) {
            if (this.headers == null) {
                this.headers = new HashMap<String, String>();
            }
            this.headers.put(key, value);
            return this;
        }

        /**
         * Builds the RestOptions.
         * @return RestOptions with all the property set.
         */
        public RestOptions build() {
            return new RestOptions(
                    this.path,
                    this.data,
                    this.headers,
                    this.queryParameters);
        }
    }
}
