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
import com.amplifyframework.api.aws.auth.ApiRequestDecoratorFactory;
import com.amplifyframework.api.aws.auth.RequestDecorator;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.auth.AuthorizationTypeIterator;
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
public final class MultiAuthAppSyncGraphQLOperation<R> extends GraphQLOperation<R> {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-api");
    private static final String CONTENT_TYPE = "application/json";

    private final String endpoint;
    private final OkHttpClient client;
    private final Consumer<GraphQLResponse<R>> onResponse;
    private final Consumer<ApiException> onFailure;
    private final ApiRequestDecoratorFactory apiRequestDecoratorFactory;
    private final ExecutorService executorService;

    private AuthorizationTypeIterator authTypes;
    private Call ongoingCall;

    /**
     * Constructs a new AppSyncGraphQLOperation.
     * @param builder An instance of the {@link Builder} object.
     */
    private MultiAuthAppSyncGraphQLOperation(Builder<R> builder) {
        super(builder.request, builder.responseFactory);
        this.apiRequestDecoratorFactory = builder.apiRequestDecoratorFactory;
        this.endpoint = builder.endpoint;
        this.client = builder.client;
        this.onResponse = builder.onResponse;
        this.onFailure = builder.onFailure;
        this.executorService = builder.executorService;

        if (!(getRequest() instanceof AppSyncGraphQLRequest)) {
            onFailure.accept(new ApiException(
                "Multiauth only supported with AppSyncGraphQLRequest<T>.",
                AmplifyException.TODO_RECOVERY_SUGGESTION
            ));
            return;
        }
        AppSyncGraphQLRequest<R> appSyncRequest = (AppSyncGraphQLRequest<R>) getRequest();
        this.authTypes = MultiAuthModeStrategy.getInstance()
                                              .authTypesFor(appSyncRequest.getModelSchema(),
                                                            appSyncRequest.getAuthRuleOperation());
    }

    @Override
    public void start() {
        // No-op if start() is called post-execution
        if (ongoingCall != null && ongoingCall.isExecuted()) {
            return;
        }
        executorService.submit(this::dispatchRequest);
        /*try {
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
        }*/
    }

    private void dispatchRequest() {
        if (authTypes.hasNext()) {
            AuthorizationType authType = authTypes.next();
            try {
                LOG.debug("Request: " + getRequest().getContent());
                RequestDecorator requestDecorator = apiRequestDecoratorFactory.forAuthType(authType);
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
                LOG.warn("Failed to make a successful request with " + authType, error);
                executorService.submit(this::dispatchRequest);
            }
        } else {
            onFailure.accept(new ApiException(
                "Unable to successfully complete request with any of the compatible auth types.",
                "Check your application logs for detail."
            ));
        }
    }

    @Override
    public void cancel() {
        ongoingCall.cancel();
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
                    onFailure.accept(new ApiException(
                        "Could not retrieve the response body from the returned JSON",
                        exception, AmplifyException.TODO_RECOVERY_SUGGESTION
                    ));
                    return;
                }
            }

            try {
                GraphQLResponse<R> graphQLResponse = wrapResponse(jsonResponse);
                if (graphQLResponse.hasErrors()) {
                    for (GraphQLResponse.Error error : graphQLResponse.getErrors()) {
                        if (error.getExtensions() != null &&
                            "UnauthorizedException".equals(error.getExtensions().get("errorType"))) {
                            executorService.submit(MultiAuthAppSyncGraphQLOperation.this::dispatchRequest);
                            return;
                        }
                    }
                    onFailure.accept(new ApiException("The server response contains errors:" +
                                                                    graphQLResponse.getErrors().toString(),
                                                                AmplifyException.TODO_RECOVERY_SUGGESTION));
                } else {
                    onResponse.accept(graphQLResponse);
                }
                //TODO: Dispatch to hub
            } catch (ApiException exception) {
                onFailure.accept(exception);
            }
        }

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException exception) {
            onFailure.accept(new ApiException(
                "OkHttp client request failed.", exception, "See attached exception for more details."
            ));
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

        Builder<R> apiRequestDecoratorFactory(ApiRequestDecoratorFactory apiRequestDecoratorFactory) {
            this.apiRequestDecoratorFactory = apiRequestDecoratorFactory;
            return this;
        }

        Builder<R> executorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        @SuppressLint("SyntheticAccessor")
        MultiAuthAppSyncGraphQLOperation<R> build() {
            return new MultiAuthAppSyncGraphQLOperation<>(this);
        }

    }
}
