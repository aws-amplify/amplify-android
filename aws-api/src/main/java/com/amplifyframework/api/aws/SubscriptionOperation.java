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

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.logging.Logger;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

final class SubscriptionOperation<T> extends GraphQLOperation<T> {
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.API, "amplify:aws-api");

    private final SubscriptionEndpoint subscriptionEndpoint;
    private final ExecutorService executorService;
    private final Consumer<String> onSubscriptionStart;
    private final Consumer<GraphQLResponse<T>> onNextItem;
    private final Consumer<ApiException> onSubscriptionError;
    private final Action onSubscriptionComplete;
    private final AtomicBoolean canceled;
    private final AuthorizationType authorizationType;

    private String subscriptionId;
    private Future<?> subscriptionFuture;

    private SubscriptionOperation(Builder<T> builder) {
        super(builder.graphQlRequest, builder.responseFactory);
        this.subscriptionEndpoint = builder.subscriptionEndpoint;
        this.onSubscriptionStart = builder.onSubscriptionStart;
        this.onNextItem = builder.onNextItem;
        this.onSubscriptionError = builder.onSubscriptionError;
        this.onSubscriptionComplete = builder.onSubscriptionComplete;
        this.executorService = builder.executorService;
        this.canceled = new AtomicBoolean(false);
        this.authorizationType = builder.authorizationType;
    }

    @NonNull
    static <T> Builder<T> builder() {
        return new Builder<>();
    }

    @Override
    public synchronized void start() {
        if (canceled.get()) {
            onSubscriptionError.accept(new ApiException(
                "Operation already canceled.", "Don't cancel the subscription before starting it!"
            ));
            return;
        }

        subscriptionFuture = executorService.submit(() -> {
            LOG.debug("Requesting subscription: " + getRequest().getContent());
            subscriptionEndpoint.requestSubscription(
                getRequest(),
                authorizationType,
                subscriptionId -> {
                    SubscriptionOperation.this.subscriptionId = subscriptionId;
                    onSubscriptionStart.accept(subscriptionId);
                },
                onNextItem,
                apiException -> {
                    cancel();
                    onSubscriptionError.accept(apiException);
                },
                onSubscriptionComplete
            );
        });
    }

    @Override
    public synchronized void cancel() {
        if (subscriptionId != null && !canceled.get()) {
            canceled.set(true);
            executorService.execute(() -> {
                try {
                    LOG.debug("Cancelling subscription: " + subscriptionId);
                    subscriptionEndpoint.releaseSubscription(subscriptionId);
                } catch (ApiException exception) {
                    onSubscriptionError.accept(exception);
                }
            });
        } else if (subscriptionFuture != null && subscriptionFuture.cancel(true)) {
            LOG.debug("Subscription attempt was canceled.");
        } else {
            LOG.debug("Nothing to cancel. Subscription not yet created, or already cancelled.");
        }
    }

    static final class Builder<T> {
        private SubscriptionEndpoint subscriptionEndpoint;
        private GraphQLRequest<T> graphQlRequest;
        private GraphQLResponse.Factory responseFactory;
        private ExecutorService executorService;
        private Consumer<String> onSubscriptionStart;
        private Consumer<GraphQLResponse<T>> onNextItem;
        private Consumer<ApiException> onSubscriptionError;
        private Action onSubscriptionComplete;
        private AuthorizationType authorizationType;

        @NonNull
        public Builder<T> subscriptionEndpoint(@NonNull SubscriptionEndpoint subscriptionEndpoint) {
            this.subscriptionEndpoint = Objects.requireNonNull(subscriptionEndpoint);
            return this;
        }

        @NonNull
        public Builder<T> graphQlRequest(@NonNull GraphQLRequest<T> graphQlRequest) {
            this.graphQlRequest = Objects.requireNonNull(graphQlRequest);
            return this;
        }

        @NonNull
        public Builder<T> responseFactory(@NonNull GraphQLResponse.Factory responseFactory) {
            this.responseFactory = Objects.requireNonNull(responseFactory);
            return this;
        }

        @NonNull
        public Builder<T> executorService(@NonNull ExecutorService executorService) {
            this.executorService = Objects.requireNonNull(executorService);
            return this;
        }

        @NonNull
        public Builder<T> onSubscriptionStart(@NonNull Consumer<String> onSubscriptionStart) {
            this.onSubscriptionStart = Objects.requireNonNull(onSubscriptionStart);
            return this;
        }

        @NonNull
        public Builder<T> onNextItem(@NonNull Consumer<GraphQLResponse<T>> onNextItem) {
            this.onNextItem = Objects.requireNonNull(onNextItem);
            return this;
        }

        @NonNull
        public Builder<T> onSubscriptionError(@NonNull Consumer<ApiException> onSubscriptionError) {
            this.onSubscriptionError = Objects.requireNonNull(onSubscriptionError);
            return this;
        }

        @NonNull
        public Builder<T> onSubscriptionComplete(@NonNull Action onSubscriptionComplete) {
            this.onSubscriptionComplete = Objects.requireNonNull(onSubscriptionComplete);
            return this;
        }

        @NonNull
        public Builder<T> authorizationType(AuthorizationType authorizationType) {
            this.authorizationType = authorizationType;
            return this;
        }

        @NonNull
        public SubscriptionOperation<T> build() {
            return new SubscriptionOperation<>(this);
        }
    }
}
