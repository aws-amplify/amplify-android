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

import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.StreamListener;

import java.util.Objects;

import okhttp3.OkHttpClient;

final class SubscriptionOperation<T> extends GraphQLOperation<T> {
    private final String endpoint;
    private final OkHttpClient client;
    private final SubscriptionEndpoint subscriptionEndpoint;
    private final StreamListener<GraphQLResponse<T>> subscriptionListener;

    private String subscriptionId;

    private SubscriptionOperation(
            final SubscriptionEndpoint subscriptionEndpoint,
            final String endpoint,
            final OkHttpClient client,
            final GraphQLRequest graphQLRequest,
            final GraphQLResponse.Factory responseFactory,
            final Class<T> classToCast,
            final StreamListener<GraphQLResponse<T>> subscriptionListener) {
        super(graphQLRequest, responseFactory, classToCast);
        this.endpoint = endpoint;
        this.client = client;
        this.subscriptionEndpoint = subscriptionEndpoint;
        this.subscriptionListener = subscriptionListener;
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

    @Nullable
    StreamListener<GraphQLResponse<T>> subscriptionListener() {
        return subscriptionListener;
    }

    boolean hasSubscriptionListener() {
        return subscriptionListener != null;
    }

    @NonNull
    static <T> SubscriptionManagerStep<T> builder() {
        return new Builder<>();
    }

    @Override
    public void start() {
        subscriptionId = subscriptionEndpoint.requestSubscription(
            getRequest(), subscriptionListener, getClassToCast());
    }

    @Override
    public void cancel() {
        subscriptionEndpoint.releaseSubscription(subscriptionId);
    }

    static final class Builder<T> implements
            SubscriptionManagerStep<T>,
            EndpointStep<T>,
            OkHttpClientStep<T>,
            GraphQlRequestStep<T>,
            ResponseFactoryStep<T>,
            ClassToCastStep<T>,
            StreamListenerStep<T>,
            BuilderStep<T> {
        private SubscriptionEndpoint subscriptionEndpoint;
        private String endpoint;
        private OkHttpClient client;
        private GraphQLRequest graphQLRequest;
        private GraphQLResponse.Factory responseFactory;
        private Class<T> classToCast;
        private StreamListener<GraphQLResponse<T>> streamListener;

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
        public ResponseFactoryStep<T> graphQLRequest(@NonNull GraphQLRequest graphQLRequest) {
            this.graphQLRequest = Objects.requireNonNull(graphQLRequest);
            return this;
        }

        @NonNull
        @Override
        public ClassToCastStep<T> responseFactory(@NonNull GraphQLResponse.Factory responseFactory) {
            this.responseFactory = Objects.requireNonNull(responseFactory);
            return this;
        }

        @NonNull
        @Override
        public StreamListenerStep<T> classToCast(@NonNull Class<T> classToCast) {
            this.classToCast = Objects.requireNonNull(classToCast);
            return this;
        }

        @NonNull
        @Override
        public BuilderStep<T> streamListener(@Nullable final StreamListener<GraphQLResponse<T>> streamListener) {
            this.streamListener = streamListener;
            return this;
        }

        @NonNull
        @Override
        public SubscriptionOperation<T> build() {
            return new SubscriptionOperation<>(
                Objects.requireNonNull(Builder.this.subscriptionEndpoint),
                Objects.requireNonNull(Builder.this.endpoint),
                Objects.requireNonNull(Builder.this.client),
                Objects.requireNonNull(Builder.this.graphQLRequest),
                Objects.requireNonNull(Builder.this.responseFactory),
                Objects.requireNonNull(Builder.this.classToCast),
                Builder.this.streamListener // It's a @Nullable field
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
        ResponseFactoryStep<T> graphQLRequest(@NonNull GraphQLRequest graphQlRequest);
    }

    interface ResponseFactoryStep<T> {
        @NonNull
        ClassToCastStep<T> responseFactory(@NonNull GraphQLResponse.Factory responseFactory);
    }

    interface ClassToCastStep<T> {
        @NonNull
        StreamListenerStep<T> classToCast(@NonNull Class<T> classToCast);
    }

    interface StreamListenerStep<T> {
        @NonNull
        BuilderStep<T> streamListener(@Nullable StreamListener<GraphQLResponse<T>> streamListener);
    }

    interface BuilderStep<T> {
        @NonNull
        SubscriptionOperation<T> build();
    }
}
