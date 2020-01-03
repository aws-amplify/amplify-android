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
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.StreamListener;
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
    private final StreamListener<GraphQLResponse<T>, ApiException> subscriptionListener;
    private final ExecutorService executorService;

    private String subscriptionId;

    private SubscriptionOperation(
            @NonNull final SubscriptionEndpoint subscriptionEndpoint,
            @NonNull final String endpoint,
            @NonNull final OkHttpClient client,
            @NonNull final GraphQLRequest<T> graphQLRequest,
            @NonNull final GraphQLResponse.Factory responseFactory,
            @NonNull final StreamListener<GraphQLResponse<T>, ApiException> subscriptionListener,
            @NonNull final ExecutorService executorService) {
        super(graphQLRequest, responseFactory);
        this.endpoint = endpoint;
        this.client = client;
        this.subscriptionEndpoint = subscriptionEndpoint;
        this.subscriptionListener = subscriptionListener;
        this.executorService = executorService;
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
    StreamListener<GraphQLResponse<T>, ApiException> subscriptionListener() {
        return subscriptionListener;
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
            LOG.debug("Request " + getRequest().getContent());
            subscriptionId = subscriptionEndpoint.requestSubscription(
                    getRequest(), subscriptionListener);
        });
    }

    @Override
    public void cancel() {
        if (subscriptionId != null) {
            try {
                subscriptionEndpoint.releaseSubscription(subscriptionId);
            } catch (ApiException exception) {
                subscriptionListener.onError(exception);
            }
        }
    }

    static final class Builder<T> implements
            SubscriptionManagerStep<T>,
            EndpointStep<T>,
            OkHttpClientStep<T>,
            GraphQlRequestStep<T>,
            ResponseFactoryStep<T>,
            StreamListenerStep<T>,
            BuilderStep<T>,
            ExecutorServiceStep<T>
        {
        private SubscriptionEndpoint subscriptionEndpoint;
        private String endpoint;
        private OkHttpClient client;
        private GraphQLRequest<T> graphQLRequest;
        private GraphQLResponse.Factory responseFactory;
        private StreamListener<GraphQLResponse<T>, ApiException> streamListener;
        private ExecutorService executorService;

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
        public StreamListenerStep<T> responseFactory(@NonNull GraphQLResponse.Factory responseFactory) {
            this.responseFactory = Objects.requireNonNull(responseFactory);
            return this;
        }

        @NonNull
        @Override
        public ExecutorServiceStep<T> streamListener(
                @NonNull final StreamListener<GraphQLResponse<T>, ApiException> streamListener) {
            this.streamListener = Objects.requireNonNull(streamListener);
            return this;
        }

        @NonNull
        @Override
        public BuilderStep<T> executorService(
                ExecutorService executorService) {
            this.executorService = Objects.requireNonNull(executorService);
            return this;
        }

        @SuppressLint("SyntheticAccessor")
        @NonNull
        @Override
        public SubscriptionOperation<T> build() {
            return new SubscriptionOperation<>(
                Objects.requireNonNull(Builder.this.subscriptionEndpoint),
                Objects.requireNonNull(Builder.this.endpoint),
                Objects.requireNonNull(Builder.this.client),
                Objects.requireNonNull(Builder.this.graphQLRequest),
                Objects.requireNonNull(Builder.this.responseFactory),
                Objects.requireNonNull(Builder.this.streamListener),
                Objects.requireNonNull(Builder.this.executorService)
            );
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
        StreamListenerStep<T> responseFactory(@NonNull GraphQLResponse.Factory responseFactory);
    }

    interface StreamListenerStep<T> {
        @NonNull
        ExecutorServiceStep<T> streamListener(@NonNull StreamListener<GraphQLResponse<T>, ApiException> streamListener);
    }

    interface ExecutorServiceStep<T> {
        @NonNull
        BuilderStep<T> executorService(@NonNull ExecutorService executorService);
    }

    interface BuilderStep<T> {
        @NonNull
        SubscriptionOperation<T> build();
    }
}
