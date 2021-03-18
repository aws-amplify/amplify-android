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

package com.amplifyframework.api.aws.operation;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.auth.ApiRequestDecoratorFactory;
import com.amplifyframework.api.aws.auth.RequestDecorator;
import com.amplifyframework.api.aws.utils.RestRequestFactory;
import com.amplifyframework.api.rest.RestOperation;
import com.amplifyframework.api.rest.RestOperationRequest;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.core.Consumer;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * An operation to enqueue a REST HTTP request to OkHttp client.
 */
@SuppressLint("SyntheticAccessor")
public final class AWSRestOperation extends RestOperation {

    private final String endpoint;
    private final OkHttpClient client;
    private final Consumer<RestResponse> onResponse;
    private final Consumer<ApiException> onFailure;
    private final ApiRequestDecoratorFactory apiRequestDecoratorFactory;

    private Call ongoingCall;

    /**
     * Constructs a REST operation.
     * @param request REST request that contains the query and data.
     * @param endpoint Endpoint against which the request to be made.
     * @param client OKHTTPClient to be used for the request.
     * @param apiRequestDecoratorFactory The request decorator factory for the API.
     * @param onResponse Callback to be invoked when a response is available from endpoint
     * @param onFailure Callback to be invoked when there is a failure to obtain any response
     */
    public AWSRestOperation(
            @NonNull RestOperationRequest request,
            @NonNull String endpoint,
            @NonNull OkHttpClient client,
            @NonNull ApiRequestDecoratorFactory apiRequestDecoratorFactory,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        super(Objects.requireNonNull(request));
        this.endpoint = Objects.requireNonNull(endpoint);
        this.client = Objects.requireNonNull(client);
        this.apiRequestDecoratorFactory = apiRequestDecoratorFactory;
        this.onResponse = Objects.requireNonNull(onResponse);
        this.onFailure = Objects.requireNonNull(onFailure);
    }

    @Override
    public void start() {
        // No-op if start() is called post-execution
        if (ongoingCall != null && ongoingCall.isExecuted()) {
            return;
        }
        try {
            RequestDecorator requestDecorator = apiRequestDecoratorFactory.fromRestRequest(getRequest());
            URL url = RestRequestFactory.createURL(endpoint,
                    getRequest().getPath(),
                    getRequest().getQueryParameters());
            Request request = RestRequestFactory.createRequest(url,
                    getRequest().getData(),
                    getRequest().getHeaders(),
                    getRequest().getHttpMethod());
            ongoingCall = client.newBuilder()
                                .addInterceptor(new Interceptor() {
                                    @NotNull
                                    @Override
                                    public Response intercept(@NotNull Chain chain) throws IOException {
                                        Request decoratedRequest = requestDecorator.decorate(chain.request());
                                        return chain.proceed(decoratedRequest);
                                    }
                                })
                                .build()
                                .newCall(request);
            ongoingCall.enqueue(new AWSRestOperation.OkHttpCallback());
        } catch (Exception error) {
            // Cancel if possible
            if (ongoingCall != null) {
                ongoingCall.cancel();
            }

            onFailure.accept(new ApiException(
                "OkHttp client failed to make a successful request.",
                error, AmplifyException.TODO_RECOVERY_SUGGESTION
            ));
        }
    }

    @Override
    public synchronized void cancel() {
        if (ongoingCall != null) {
            ongoingCall.cancel();
        }
    }

    private final class OkHttpCallback implements Callback {
        @Override
        public void onResponse(@NonNull Call call,
                               @NonNull Response response) throws IOException {
            final ResponseBody responseBody = response.body();
            final int statusCode = response.code();
            final Map<String, String> headersMap = new HashMap<>();
            final Map<String, List<String>> headersMultiMap = response.headers().toMultimap();
            for (String key : headersMultiMap.keySet()) {
                List<String> value = headersMultiMap.get(key);
                if (value.size() > 0) {
                    headersMap.put(key, TextUtils.join(",", value));
                }
            }
            RestResponse restResponse;
            if (responseBody != null) {
                final byte[] data = responseBody.bytes();
                restResponse = new RestResponse(statusCode, headersMap, data);
            } else {
                restResponse = new RestResponse(statusCode, headersMap);
            }

            onResponse.accept(restResponse);
        }

        @Override
        public void onFailure(@NonNull Call call,
                              @NonNull IOException ioe) {
            // Don't emit a failure if the user has canceled the operation.
            // This behavior is consistent with how iOS' REST API category works
            // Rx streams similarly do not report cancellation via
            // onError or onComplete.
            if (ongoingCall != null && ongoingCall.isCanceled()) {
                return;
            }

            onFailure.accept(new ApiException(
                "Received an IO exception while making the request.",
                ioe, "Retry the request."
            ));
        }
    }
}
