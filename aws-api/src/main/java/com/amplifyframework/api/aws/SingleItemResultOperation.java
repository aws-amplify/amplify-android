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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.logging.Logger;

import java.io.IOException;
import java.util.Objects;

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
 * with the goal of obtaining a single response. Games aside,
 * this means a query or a mutation, and *NOT* a subscription.
 * @param <T> Casted type of GraphQL result data
 */
public final class SingleItemResultOperation<T> extends GraphQLOperation<T> {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-api");
    private static final String CONTENT_TYPE = "application/json";

    private final String endpoint;
    private final OkHttpClient client;
    private final Consumer<GraphQLResponse<T>> onResponse;
    private final Consumer<ApiException> onFailure;

    private Call ongoingCall;

    /**
     * Constructs a new SingleResultOperation.
     * @param endpoint API endpoint being hit
     * @param client OkHttp client being used to hit the endpoint
     * @param request GraphQL request being enacted
     * @param responseFactory an implementation of GsonGraphQLResponseFactory
     * @param onResponse Called when a response is available from the endpoint
     * @param onFailure Called upon failure to obtain a response from endpoint
     */
    private SingleItemResultOperation(
            @NonNull String endpoint,
            @NonNull OkHttpClient client,
            @NonNull GraphQLRequest<T> request,
            @NonNull GraphQLResponse.Factory responseFactory,
            @NonNull Consumer<GraphQLResponse<T>> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        super(request, responseFactory);
        this.endpoint = endpoint;
        this.client = client;
        this.onResponse = onResponse;
        this.onFailure = onFailure;
    }

    @Override
    public void start() {
        // No-op if start() is called post-execution
        if (ongoingCall != null && ongoingCall.isExecuted()) {
            return;
        }

        try {
            LOG.debug("Request: " + getRequest().getContent());
            ongoingCall = client.newCall(new Request.Builder()
                    .url(endpoint)
                    .addHeader("accept", CONTENT_TYPE)
                    .addHeader("content-type", CONTENT_TYPE)
                    .post(RequestBody.create(getRequest().getContent(), MediaType.parse(CONTENT_TYPE)))
                    .build());
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
    public void cancel() {
        ongoingCall.cancel();
    }

    static <T> Builder<T> builder() {
        return new Builder<>();
    }

    class OkHttpCallback implements Callback {
        @SuppressLint("SyntheticAccessor")
        @Override
        public void onResponse(@NonNull Call call,
                               @NonNull Response response) {
            final ResponseBody responseBody = response.body();
            String jsonResponse = null;
            if (responseBody != null) {
                try {
                    jsonResponse = responseBody.string();
                } catch (IOException exception) {
                    onFailure.accept(new ApiException(
                        "Could not retrieve the response body from the returned JSON",
                        exception, AmplifyException.TODO_RECOVERY_SUGGESTION
                    ));
                    return;
                }
            }

            try {
                onResponse.accept(wrapSingleResultResponse(jsonResponse));
                //TODO: Dispatch to hub
            } catch (ApiException exception) {
                onFailure.accept(exception);
            }
        }

        @SuppressLint("SyntheticAccessor")
        @Override
        public void onFailure(@NonNull Call call,
                              @NonNull IOException exception) {
            onFailure.accept(new ApiException(
                "Could not retrieve the response body from the returned JSON",
                exception, AmplifyException.TODO_RECOVERY_SUGGESTION
            ));
        }
    }

    static final class Builder<T> {
        private String endpoint;
        private OkHttpClient client;
        private GraphQLRequest<T> request;
        private GraphQLResponse.Factory responseFactory;
        private Consumer<GraphQLResponse<T>> onResponse;
        private Consumer<ApiException> onFailure;

        Builder<T> endpoint(@NonNull String endpoint) {
            this.endpoint = Objects.requireNonNull(endpoint);
            return this;
        }

        Builder<T> client(@NonNull OkHttpClient client) {
            this.client = Objects.requireNonNull(client);
            return this;
        }

        Builder<T> request(@NonNull GraphQLRequest<T> request) {
            this.request = Objects.requireNonNull(request);
            return this;
        }

        Builder<T> responseFactory(@NonNull GraphQLResponse.Factory responseFactory) {
            this.responseFactory = Objects.requireNonNull(responseFactory);
            return this;
        }

        Builder<T> onResponse(@NonNull Consumer<GraphQLResponse<T>> onResponse) {
            this.onResponse = Objects.requireNonNull(onResponse);
            return this;
        }

        Builder<T> onFailure(@NonNull Consumer<ApiException> onFailure) {
            this.onFailure = Objects.requireNonNull(onFailure);
            return this;
        }

        @SuppressLint("SyntheticAccessor")
        SingleItemResultOperation<T> build() {
            return new SingleItemResultOperation<>(
                Objects.requireNonNull(endpoint),
                Objects.requireNonNull(client),
                Objects.requireNonNull(request),
                Objects.requireNonNull(responseFactory),
                Objects.requireNonNull(onResponse),
                Objects.requireNonNull(onFailure)
            );
        }
    }
}
