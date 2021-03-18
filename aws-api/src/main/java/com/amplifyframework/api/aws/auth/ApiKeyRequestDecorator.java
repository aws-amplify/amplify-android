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

import androidx.annotation.NonNull;

import com.amplifyframework.api.aws.sigv4.ApiKeyAuthProvider;

/**
 * Request signer that adds a header with the API key.
 */
public final class ApiKeyRequestDecorator implements RequestDecorator {
    private static final String X_API_KEY = "x-api-key";
    private final ApiKeyAuthProvider apiKeyProvider;

    /**
     * Constructor that takes in the API key provider to be used when signing the request.
     * @param apiKeyProvider An implementation of the {@link ApiKeyAuthProvider} interface.
     */
    public ApiKeyRequestDecorator(@NonNull ApiKeyAuthProvider apiKeyProvider) {
        this.apiKeyProvider = apiKeyProvider;
    }

    @Override
    public okhttp3.Request decorate(okhttp3.Request request) {
        return request.newBuilder()
                      .addHeader(X_API_KEY, apiKeyProvider.getAPIKey())
                      .build();
    }
}
