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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.logging.Logger;

import java.io.IOException;

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
 * @param <T> Casted type of GraphQL result data
 */
public final class SingleArrayResultOperation<T> extends GraphQLOperation<T> {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-api");
    private static final String CONTENT_TYPE = "application/json";

    private final String endpoint;
    private final OkHttpClient client;
    private final ResultListener<GraphQLResponse<Iterable<T>>> responseListener;

    private Call ongoingCall;

    /**
     * Constructs a new SingleResultOperation.
     * @param endpoint API endpoint being hit
     * @param client OkHttp client being used to hit the endpoint
     * @param request GraphQL request being enacted
     * @param responseFactory an implementation of GsonGraphQLResponseFactory
     * @param responseListener
     *        listener to be invoked when response is available, or if
     */
    private SingleArrayResultOperation(
            @NonNull String endpoint,
            @NonNull OkHttpClient client,
            @NonNull GraphQLRequest<T> request,
            @NonNull GraphQLResponse.Factory responseFactory,
            @NonNull ResultListener<GraphQLResponse<Iterable<T>>> responseListener) {
        super(request, responseFactory);
        this.endpoint = endpoint;
        this.client = client;
        this.responseListener = responseListener;
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

            // If a response listener was provided, then dispatch the
            // errors to it. Otherwise, throw the error synchronously to
            // the caller.
            ApiException wrappedError =
                    new ApiException(
                        "OkHttp client failed to make a successful request.",
                        error,
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                    );

            responseListener.onError(wrappedError);
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
        @Override
        public void onResponse(@NonNull Call call,
                               @NonNull Response response) {
            final ResponseBody responseBody = response.body();
            String jsonResponse = null;
            if (responseBody != null) {
                try {
                    jsonResponse = responseBody.string();
                } catch (IOException exception) {
                    responseListener.onError(new ApiException(
                            "Could not retrieve the response body from the returned JSON",
                            exception,
                            AmplifyException.TODO_RECOVERY_SUGGESTION
                    ));
                }
            }

            try {
                GraphQLResponse<Iterable<T>> wrappedResponse = wrapMultiResultResponse(jsonResponse);

                responseListener.onResult(wrappedResponse);

                //TODO: Dispatch to hub
            } catch (ApiException exception) {
                responseListener.onError(exception);
            }
        }

        @Override
        public void onFailure(@NonNull Call call,
                              @NonNull IOException exception) {
            responseListener.onError(new ApiException(
                    "Could not retrieve the response body from the returned JSON",
                    exception,
                    AmplifyException.TODO_RECOVERY_SUGGESTION
            ));
        }
    }

    static final class Builder<T> {
        private String endpoint;
        private OkHttpClient client;
        private GraphQLRequest<T> request;
        private GraphQLResponse.Factory responseFactory;
        private ResultListener<GraphQLResponse<Iterable<T>>> responseListener;

        Builder<T> endpoint(final String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        Builder<T> client(final OkHttpClient client) {
            this.client = client;
            return this;
        }

        Builder<T> request(final GraphQLRequest<T> request) {
            this.request = request;
            return this;
        }

        Builder<T> responseFactory(final GraphQLResponse.Factory responseFactory) {
            this.responseFactory = responseFactory;
            return this;
        }

        Builder<T> responseListener(final ResultListener<GraphQLResponse<Iterable<T>>> responseListener) {
            this.responseListener = responseListener;
            return this;
        }

        SingleArrayResultOperation<T> build() {
            return new SingleArrayResultOperation<>(
                    endpoint,
                    client,
                    request,
                    responseFactory,
                    responseListener);
        }
    }
}
