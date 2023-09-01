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

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.auth.ApiRequestDecoratorFactory;
import com.amplifyframework.api.aws.auth.RequestDecorator;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.logging.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * An operation to enqueue a GraphQL request to OkHttp client,
 * with the goal of obtaining a list of responses. For example,
 * this is used for a LIST query vs. a GET query or most mutations.
 * @param <R> Casted type of GraphQL result data
 */
public final class AppSyncGraphQLOperation<R> extends AWSGraphQLOperation<R> {
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.API, "amplify:aws-api");
    private static final String CONTENT_TYPE = "application/json";
    private static final int START_OF_CLIENT_ERROR_CODE = 400;
    private static final int END_OF_CLIENT_ERROR_CODE = 499;
    private final String endpoint;
    private final OkHttpClient client;
    private final Consumer<GraphQLResponse<R>> onResponse;
    private final Consumer<ApiException> onFailure;
    private final ExecutorService executorService;
    private final ApiRequestDecoratorFactory apiRequestDecoratorFactory;

    @Nullable
    private Call ongoingCall;

    /**
     * Constructs a new AppSyncGraphQLOperation.
     * @param builder operation builder instance
     */
    private AppSyncGraphQLOperation(@NonNull Builder<R> builder) {
        super(builder.request, builder.responseFactory, builder.apiName);
        this.endpoint = Objects.requireNonNull(builder.endpoint);
        this.client = Objects.requireNonNull(builder.client);
        this.apiRequestDecoratorFactory = Objects.requireNonNull(builder.apiRequestDecoratorFactory);
        this.executorService = Objects.requireNonNull(builder.executorService);
        this.onResponse = Objects.requireNonNull(builder.onResponse);
        this.onFailure = Objects.requireNonNull(builder.onFailure);
    }

    @Override
    public void start() {
        // No-op if start() is called post-execution or canceled
        if (ongoingCall != null && (ongoingCall.isExecuted() || ongoingCall.isCanceled())) {
            return;
        }
        executorService.submit(this::dispatchRequest);
    }

    private void dispatchRequest() {
        try {
            LOG.debug("Request: " + getRequest().getContent());
            RequestDecorator requestDecorator = apiRequestDecoratorFactory.fromGraphQLRequest(getRequest());
            Request okHttpRequest = new Request.Builder()
                .url(endpoint)
                .addHeader("accept", CONTENT_TYPE)
                .addHeader("content-type", CONTENT_TYPE)
                .post(RequestBody.create(getRequest().getContent(), MediaType.parse(CONTENT_TYPE)))
                .build();

            ongoingCall = client.newCall(requestDecorator.decorate(okHttpRequest));
            ongoingCall.enqueue(new OkHttpCallback());
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

    static <R> Builder<R> builder() {
        return new Builder<>();
    }

    @SuppressLint("SyntheticAccessor")
    class OkHttpCallback implements Callback {
        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) {
            final ResponseBody responseBody = response.body();
            String jsonResponse = null;
            if (responseBody != null) {
                try {
                    jsonResponse = responseBody.string();
                } catch (IOException exception) {
                    LOG.warn("Error retrieving JSON from response.", exception);
                    onFailure.accept(new ApiException(
                        "Could not retrieve the response body from the returned JSON",
                        exception, AmplifyException.TODO_RECOVERY_SUGGESTION
                    ));
                    return;
                }
            }
            if (response.code() >= START_OF_CLIENT_ERROR_CODE && response.code() <= END_OF_CLIENT_ERROR_CODE) {
                onFailure.accept(new ApiException
                        .NonRetryableException("OkHttp client request failed.", "Irrecoverable error")
                );
                return;
            }

            try {
                onResponse.accept(wrapResponse(jsonResponse));
                //TODO: Dispatch to hub
            } catch (ApiException exception) {
                onFailure.accept(exception);
            }
        }

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException exception) {
            if (!call.isCanceled()) {
                onFailure.accept(new ApiException(
                        "OkHttp client request failed.", exception, "See attached exception for more details."
                ));
            }
        }
    }

    static final class Builder<R> {
        private String endpoint;
        private OkHttpClient client;
        private GraphQLRequest<R> request;
        private GraphQLResponse.Factory responseFactory;
        private ApiRequestDecoratorFactory apiRequestDecoratorFactory;
        private Consumer<GraphQLResponse<R>> onResponse;
        private Consumer<ApiException> onFailure;
        private ExecutorService executorService;
        private String apiName;

        Builder<R> endpoint(@NonNull String endpoint) {
            this.endpoint = Objects.requireNonNull(endpoint);
            return this;
        }

        Builder<R> client(@NonNull OkHttpClient client) {
            this.client = Objects.requireNonNull(client);
            return this;
        }

        Builder<R> request(@NonNull GraphQLRequest<R> request) {
            this.request = Objects.requireNonNull(request);
            return this;
        }

        Builder<R> responseFactory(@NonNull GraphQLResponse.Factory responseFactory) {
            this.responseFactory = Objects.requireNonNull(responseFactory);
            return this;
        }

        Builder<R> onResponse(@NonNull Consumer<GraphQLResponse<R>> onResponse) {
            this.onResponse = Objects.requireNonNull(onResponse);
            return this;
        }

        Builder<R> onFailure(@NonNull Consumer<ApiException> onFailure) {
            this.onFailure = Objects.requireNonNull(onFailure);
            return this;
        }

        Builder<R> apiRequestDecoratorFactory(@NonNull ApiRequestDecoratorFactory apiRequestDecoratorFactory) {
            this.apiRequestDecoratorFactory = Objects.requireNonNull(apiRequestDecoratorFactory);
            return this;
        }

        Builder<R> executorService(@NonNull ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        Builder<R> apiName(String apiName) {
            this.apiName = apiName;
            return this;
        }

        @SuppressLint("SyntheticAccessor")
        AppSyncGraphQLOperation<R> build() {
            return new AppSyncGraphQLOperation<>(this);
        }
    }
}
