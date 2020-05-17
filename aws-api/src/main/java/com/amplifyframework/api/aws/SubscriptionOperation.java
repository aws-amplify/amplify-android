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

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.logging.Logger;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

import okhttp3.OkHttpClient;

@SuppressWarnings("unused")
final class SubscriptionOperation<T> extends GraphQLOperation<T> {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-api");

    private final String endpoint;
    private final OkHttpClient client;
    private final SubscriptionEndpoint subscriptionEndpoint;
    private final ExecutorService executorService;
    private final Consumer<String> onSubscriptionStarted;
    private final Consumer<GraphQLResponse<T>> onNextItem;
    private final Consumer<ApiException> onSubscriptionError;
    private final Action onSubscriptionComplete;

    private String subscriptionId;

    @SuppressLint("SyntheticAccessor")
    private SubscriptionOperation(@NonNull SubscriptionOperation.Builder<T> builder) {
        super(builder.graphQLRequest, builder.responseFactory);
        this.endpoint = builder.endpoint;
        this.client = builder.client;
        this.subscriptionEndpoint = builder.subscriptionEndpoint;
        this.onNextItem = builder.onNextItem;
        this.onSubscriptionStarted = builder.onSubscriptionStarted;
        this.onSubscriptionError = builder.onSubscriptionError;
        this.onSubscriptionComplete = builder.onSubscriptionComplete;
        this.executorService = builder.executorService;
    }

    @NonNull
    SubscriptionEndpoint subscriptionManager() {
        return subscriptionEndpoint;
    }

    @NonNull
    String endpoint() {
        return endpoint;
    }

    @NonNull
    OkHttpClient client() {
        return client;
    }

    @NonNull
    Consumer<String> onSubscriptionStarted() {
        return onSubscriptionStarted;
    }

    @NonNull
    Consumer<GraphQLResponse<T>> onNextItem() {
        return onNextItem;
    }

    @NonNull
    Consumer<ApiException> onSubscriptionError() {
        return onSubscriptionError;
    }

    @NonNull
    Action onSubscriptionComplete() {
        return onSubscriptionComplete;
    }

    @NonNull
    ExecutorService executorService() {
        return executorService;
    }

    @NonNull
    static <T> SubscriptionManagerStep<T> builder() {
        return new Builder<>();
    }

    @Override
    public void start() {
        executorService.submit(() -> {
            LOG.debug("Requesting subscription: " + getRequest().getContent());
            subscriptionId = subscriptionEndpoint.requestSubscription(
                getRequest(),
                onSubscriptionStarted,
                onNextItem,
                onSubscriptionError,
                onSubscriptionComplete
            );
        });
    }

    @Override
    public void cancel() {
        if (subscriptionId != null) {
            try {
                subscriptionEndpoint.releaseSubscription(subscriptionId);
            } catch (ApiException exception) {
                onSubscriptionError.accept(exception);
            }
        }
    }

    static final class Builder<T> implements
            SubscriptionManagerStep<T>,
            EndpointStep<T>,
            OkHttpClientStep<T>,
            GraphQlRequestStep<T>,
            ResponseFactoryStep<T>,
            ExecutorServiceStep<T>,
            OnSubscriptionStartedStep<T>,
            OnNextItemStep<T>,
            OnSubscriptionErrorStep<T>,
            OnSubscriptionCompleteStep<T>,
            BuilderStep<T> {
        private SubscriptionEndpoint subscriptionEndpoint;
        private String endpoint;
        private OkHttpClient client;
        private GraphQLRequest<T> graphQLRequest;
        private GraphQLResponse.Factory responseFactory;
        private ExecutorService executorService;
        private Consumer<String> onSubscriptionStarted;
        private Consumer<GraphQLResponse<T>> onNextItem;
        private Consumer<ApiException> onSubscriptionError;
        private Action onSubscriptionComplete;

        @NonNull
        @Override
        public EndpointStep<T> subscriptionManager(@NonNull SubscriptionEndpoint subscriptionEndpoint) {
            this.subscriptionEndpoint = Objects.requireNonNull(subscriptionEndpoint);
            return this;
        }

        @NonNull
        @Override
        public OkHttpClientStep<T> endpoint(@NonNull final String endpoint) {
            this.endpoint = Objects.requireNonNull(endpoint);
            return this;
        }

        @NonNull
        @Override
        public GraphQlRequestStep<T> client(@NonNull final OkHttpClient client) {
            this.client = Objects.requireNonNull(client);
            return this;
        }

        @NonNull
        @Override
        public ResponseFactoryStep<T> graphQLRequest(@NonNull GraphQLRequest<T> graphQLRequest) {
            this.graphQLRequest = Objects.requireNonNull(graphQLRequest);
            return this;
        }

        @NonNull
        @Override
        public ExecutorServiceStep<T> responseFactory(@NonNull GraphQLResponse.Factory responseFactory) {
            this.responseFactory = Objects.requireNonNull(responseFactory);
            return this;
        }

        @NonNull
        @Override
        public OnSubscriptionStartedStep<T> executorService(@NonNull ExecutorService executorService) {
            this.executorService = Objects.requireNonNull(executorService);
            return this;
        }

        @NonNull
        @Override
        public OnNextItemStep<T> onSubscriptionStarted(@NonNull Consumer<String> onSubscriptionStarted) {
            this.onSubscriptionStarted = Objects.requireNonNull(onSubscriptionStarted);
            return this;
        }

        @NonNull
        @Override
        public OnSubscriptionErrorStep<T> onNextItem(@NonNull Consumer<GraphQLResponse<T>> onNextItem) {
            this.onNextItem = Objects.requireNonNull(onNextItem);
            return this;
        }

        @NonNull
        @Override
        public OnSubscriptionCompleteStep<T> onSubscriptionError(@NonNull Consumer<ApiException> onSubscriptionError) {
            this.onSubscriptionError = Objects.requireNonNull(onSubscriptionError);
            return this;
        }

        @NonNull
        @Override
        public BuilderStep<T> onSubscriptionComplete(@NonNull Action onSubscriptionComplete) {
            this.onSubscriptionComplete = Objects.requireNonNull(onSubscriptionComplete);
            return this;
        }

        @SuppressLint("SyntheticAccessor")
        @NonNull
        @Override
        public SubscriptionOperation<T> build() {
            Objects.requireNonNull(Builder.this.subscriptionEndpoint);
            Objects.requireNonNull(Builder.this.endpoint);
            Objects.requireNonNull(Builder.this.client);
            Objects.requireNonNull(Builder.this.graphQLRequest);
            Objects.requireNonNull(Builder.this.responseFactory);
            Objects.requireNonNull(Builder.this.onSubscriptionStarted);
            Objects.requireNonNull(Builder.this.onNextItem);
            Objects.requireNonNull(Builder.this.onSubscriptionError);
            Objects.requireNonNull(Builder.this.onSubscriptionComplete);
            Objects.requireNonNull(Builder.this.executorService);
            return new SubscriptionOperation<>(Builder.this);
        }
    }

    interface SubscriptionManagerStep<T> {
        @NonNull
        EndpointStep<T> subscriptionManager(@NonNull SubscriptionEndpoint subscriptionEndpoint);
    }

    interface EndpointStep<T> {
        @NonNull
        OkHttpClientStep<T> endpoint(@NonNull String apiName);
    }

    interface OkHttpClientStep<T> {
        @NonNull
        GraphQlRequestStep<T> client(@NonNull OkHttpClient okHttpClient);
    }

    interface GraphQlRequestStep<T> {
        @NonNull
        ResponseFactoryStep<T> graphQLRequest(@NonNull GraphQLRequest<T> graphQlRequest);
    }

    interface ResponseFactoryStep<T> {
        @NonNull
        ExecutorServiceStep<T> responseFactory(@NonNull GraphQLResponse.Factory responseFactory);
    }

    interface ExecutorServiceStep<T> {
        @NonNull
        OnSubscriptionStartedStep<T> executorService(@NonNull ExecutorService executorService);
    }

    interface OnSubscriptionStartedStep<T> {
        @NonNull
        OnNextItemStep<T> onSubscriptionStarted(@NonNull Consumer<String> onSubscriptionStarted);
    }

    interface OnNextItemStep<T> {
        @NonNull
        OnSubscriptionErrorStep<T> onNextItem(@NonNull Consumer<GraphQLResponse<T>> onNextItem);
    }

    interface OnSubscriptionErrorStep<T> {
        @NonNull
        OnSubscriptionCompleteStep<T> onSubscriptionError(@NonNull Consumer<ApiException> onSubscriptionError);
    }

    interface OnSubscriptionCompleteStep<T> {
        @NonNull
        BuilderStep<T> onSubscriptionComplete(@NonNull Action onSubscriptionComplete);
    }

    interface BuilderStep<T> {
        @NonNull
        SubscriptionOperation<T> build();
    }
}

