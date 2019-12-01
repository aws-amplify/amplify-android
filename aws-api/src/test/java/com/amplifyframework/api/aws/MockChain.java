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

package com.amplifyframework.api.aws;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * A mock chain to be used for testing interceptors.
 * Only the methods utilized in
 * {@link com.amplifyframework.api.aws.sigv4.AppSyncSigV4SignerInterceptor#intercept(Interceptor.Chain)}
 * were implemented in this mock class.
 */
//To keep the parameter names consistent with official Chain API
final class MockChain implements Interceptor.Chain {
    private static final int MOCK_STATUS_CODE = 200;

    @SuppressWarnings("NullableProblems") // It's supposed to be @NonNull, but it isn't. So.
    @Override
    public Call call() {
        return null;
    }

    @Override
    public int connectTimeoutMillis() {
        return 0;
    }

    @Nullable
    @Override
    public Connection connection() {
        return null;
    }

    @NonNull
    @Override
    public Response proceed(@NonNull Request request) {
        return new Response.Builder()
                .code(MOCK_STATUS_CODE)
                .message("response message")
                .protocol(Protocol.HTTP_1_1)
                .request(request)
                .build();
    }

    @Override
    public int readTimeoutMillis() {
        return 0;
    }

    @NonNull
    @Override
    public Request request() {
        return new Request.Builder()
                .url("http://localhost/")
                .post(RequestBody.create("mock request body",
                        MediaType.parse("application/json")))
                .build();
    }

    @NonNull
    @Override
    public Interceptor.Chain withConnectTimeout(int timeout, @NonNull TimeUnit timeUnit) {
        return this;
    }

    @NonNull
    @Override
    public Interceptor.Chain withReadTimeout(int timeout, @NonNull TimeUnit timeUnit) {
        return this;
    }

    @NonNull
    @Override
    public Interceptor.Chain withWriteTimeout(int timeout, @NonNull TimeUnit timeUnit) {
        return this;
    }

    @Override
    public int writeTimeoutMillis() {
        return 0;
    }
}
