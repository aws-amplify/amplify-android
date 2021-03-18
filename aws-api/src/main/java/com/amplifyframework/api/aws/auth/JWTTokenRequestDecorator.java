/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws.auth;

import java.io.IOException;

import okhttp3.MediaType;

/**
 * Request signer that adds a JWT token to the provided request.
 */
public class JWTTokenRequestDecorator implements RequestDecorator {
    private static final String APP_SYNC_SERVICE_NAME = "appsync";
    private static final String AUTHORIZATION = "authorization";
    private static final String CONTENT_TYPE = "application/json";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse(CONTENT_TYPE);
    private final TokenSupplier tokenSupplier;

    /**
     * Constructor that accepts a supplier function which will be used to retrieve the JWT token at runtime.
     * @param tokenSupplier Supplier function that returns the JWT token.
     */
    public JWTTokenRequestDecorator(TokenSupplier tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    /**
     * Adds the appropriate header to the provided HTTP request.
     * @param req The request to be signed.
     * @return A new instance of the request containing the signature headers.
     * @throws IOException If the signing process fails.
     */
    public final okhttp3.Request decorate(okhttp3.Request req) throws IOException {
        return req.newBuilder().addHeader(AUTHORIZATION, tokenSupplier.get()).build();
    }

    /**
     * Defines a simple interface through which this decorator can retrieve the
     * JWT token to be added to the request.
     */
    public interface TokenSupplier {
        /**
         * Implementations of this method should return a JWT token ready to be added as an
         * HTTP request header.
         * @return The JWT token.
         */
        String get();
    }
}
