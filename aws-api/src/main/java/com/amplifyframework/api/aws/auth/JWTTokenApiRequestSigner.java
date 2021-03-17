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

import com.amazonaws.Request;

import java.util.function.Supplier;

/**
 * Request signer that adds a JWT token to the provided request.
 */
public class JWTTokenApiRequestSigner extends ApiRequestSigner {
    private static final String AUTHORIZATION = "authorization";
    private final Supplier<String> tokenSupplier;

    /**
     * Constructor that accepts a supplier function which will be used to retrieve the JWT token at runtime.
     * @param tokenSupplier Supplier function that returns the JWT token.
     */
    public JWTTokenApiRequestSigner(Supplier<String> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void addAuthHeader(Request<?> request) {
        request.addHeader(AUTHORIZATION, tokenSupplier.get());
    }
}
