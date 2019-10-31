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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
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
@SuppressWarnings("ParameterName") //To keep the parameter names consistent with official Chain API
final class MockChain implements Interceptor.Chain {
    private static final int MOCK_STATUS_CODE = 200;

    @NotNull
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

    @NotNull
    @Override
    public Response proceed(@NotNull Request request) throws IOException {
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

    @NotNull
    @Override
    public Request request() {
        return new Request.Builder()
                .url("http://localhost/")
                .post(RequestBody.create("mock request body",
                        MediaType.parse("application/json")))
                .build();
    }

    @NotNull
    @Override
    public Interceptor.Chain withConnectTimeout(int i, @NotNull TimeUnit timeUnit) {
        return null;
    }

    @NotNull
    @Override
    public Interceptor.Chain withReadTimeout(int i, @NotNull TimeUnit timeUnit) {
        return null;
    }

    @NotNull
    @Override
    public Interceptor.Chain withWriteTimeout(int i, @NotNull TimeUnit timeUnit) {
        return null;
    }

    @Override
    public int writeTimeoutMillis() {
        return 0;
    }
}
