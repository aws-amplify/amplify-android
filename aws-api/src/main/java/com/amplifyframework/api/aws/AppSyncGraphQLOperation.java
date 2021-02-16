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
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.sigv4.AWSRequestSigner;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.logging.Logger;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Route;

/**
 * An operation to enqueue a GraphQL request to OkHttp client,
 * with the goal of obtaining a list of responses. For example,
 * this is used for a LIST query vs. a GET query or most mutations.
 * @param <R> Casted type of GraphQL result data
 */
public final class AppSyncGraphQLOperation<R> extends GraphQLOperation<R> {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-api");
    private static final String CONTENT_TYPE = "application/json";
    private static final String TAG = "AppSyncGraphQLOperation";

    private final String endpoint;
    private final OkHttpClient client;
    private final Consumer<GraphQLResponse<R>> onResponse;
    private final Consumer<ApiException> onFailure;
    private final AuthProviderChainRepository.AuthProviderChain authProviderChain;
    private final ApiAuthProviders authProviders;
    private final AWSRequestSigner requestSigner;

    private Call ongoingCall;

    /**
     * Constructs a new AppSyncGraphQLOperation.
     * @param endpoint API endpoint being hit
     * @param client OkHttp client being used to hit the endpoint
     * @param request GraphQL request being enacted
     * @param responseFactory an implementation of GsonGraphQLResponseFactory
     * @param onResponse Invoked when response is attained from endpoint
     * @param onFailure Invoked upon failure to obtain response from endpoint
     */
    @SuppressWarnings("ParameterNumber") //TODO: This is ugly. Need something better.
    private AppSyncGraphQLOperation(
            @NonNull String endpoint,
            @NonNull OkHttpClient client,
            @NonNull GraphQLRequest<R> request,
            @NonNull GraphQLResponse.Factory responseFactory,
            @Nullable AuthProviderChainRepository.AuthProviderChain authProviderChain,
            @NonNull ApiAuthProviders authProviders,
            @NonNull AWSRequestSigner requestSigner,
            @NonNull Consumer<GraphQLResponse<R>> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        super(request, responseFactory);
        this.endpoint = endpoint;
        this.client = client;
        this.authProviderChain = authProviderChain;
        this.authProviders = authProviders;
        this.requestSigner = requestSigner;
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
            Request httpRequest = new Request.Builder()
                .url(endpoint)
                .addHeader("accept", CONTENT_TYPE)
                .addHeader("content-type", CONTENT_TYPE)
                .post(RequestBody.create(getRequest().getContent(), MediaType.parse(CONTENT_TYPE)))
                .build();
            Log.d(TAG, "First attempt => Auth chain state: " + authProviderChain.toString());
            ongoingCall = client.newBuilder().authenticator(new Authenticator() {
                @org.jetbrains.annotations.Nullable
                @Override
                public Request authenticate(Route route,
                                            Response response) throws IOException {
                    if (authProviderChain.nextProvider()) {
                        Log.d(TAG, "Retry attempt => Auth chain state: " + authProviderChain.toString());
                        Request clonedHttpRequest = response.request()
                                                            .newBuilder()
                                                            .removeHeader("Authorization")
                                                            .removeHeader("x-api-key")
                                                            .build();
                        try {
                            return requestSigner.sign(clonedHttpRequest,
                                                      AuthorizationType.from(authProviderChain.getCurrent().name()));
                        } catch (ApiException apiException) {
                            Log.w(TAG,
                                  "Failed to retry the HTTP request with provider " + authProviderChain,
                                  apiException);
                            return null;
                        }
                    } else {
                        return null;
                    }

                }
            }).build().newCall(
                requestSigner.sign(httpRequest,
                                   AuthorizationType.from(authProviderChain.getCurrent().name())));
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
                onResponse.accept(wrapResponse(jsonResponse));
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
        private Consumer<GraphQLResponse<R>> onResponse;
        private Consumer<ApiException> onFailure;
        private AuthProviderChainRepository.AuthProviderChain authProviderChain;
        private ApiAuthProviders apiAuthProviders;
        private AWSRequestSigner requestSigner;

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

        Builder<R> apiAuthProviders(ApiAuthProviders apiAuthProviders) {
            this.apiAuthProviders = apiAuthProviders;
            return this;
        }

        Builder<R> authProviderChain(AuthProviderChainRepository.AuthProviderChain authProviderChain) {
            this.authProviderChain = authProviderChain;
            return this;
        }

        Builder<R> requestSigner(AWSRequestSigner requestSigner) {
            this.requestSigner = requestSigner;
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

        @SuppressLint("SyntheticAccessor")
        AppSyncGraphQLOperation<R> build() {
            return new AppSyncGraphQLOperation<>(
                Objects.requireNonNull(endpoint),
                Objects.requireNonNull(client),
                Objects.requireNonNull(request),
                Objects.requireNonNull(responseFactory),
                authProviderChain,
                Objects.requireNonNull(apiAuthProviders),
                Objects.requireNonNull(requestSigner),
                Objects.requireNonNull(onResponse),
                Objects.requireNonNull(onFailure)
            );
        }
    }
}
