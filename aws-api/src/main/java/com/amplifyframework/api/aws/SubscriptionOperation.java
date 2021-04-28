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
import com.amplifyframework.logging.Logger;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

final class SubscriptionOperation<T> extends GraphQLOperation<T> {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-api");

    private final SubscriptionEndpoint subscriptionEndpoint;
    private final ExecutorService executorService;
    private final Consumer<String> onSubscriptionStart;
    private final Consumer<GraphQLResponse<T>> onNextItem;
    private final Consumer<ApiException> onSubscriptionError;
    private final Action onSubscriptionComplete;
    private final AtomicBoolean canceled;
    private final Iterator<AuthorizationType> authTypes;

    private String subscriptionId;
    private Future<?> subscriptionFuture;

    @SuppressWarnings("ParameterNumber")
    private SubscriptionOperation(
            GraphQLRequest<T> graphQlRequest,
            GraphQLResponse.Factory responseFactory,
            SubscriptionEndpoint subscriptionEndpoint,
            Iterator<AuthorizationType> authTypes,
            Consumer<String> onSubscriptionStart,
            Consumer<GraphQLResponse<T>> onNextItem,
            Consumer<ApiException> onSubscriptionError,
            Action onSubscriptionComplete,
            ExecutorService executorService) {
        super(graphQlRequest, responseFactory);
        this.subscriptionEndpoint = subscriptionEndpoint;
        this.authTypes = authTypes;
        this.onSubscriptionStart = onSubscriptionStart;
        this.onNextItem = onNextItem;
        this.onSubscriptionError = onSubscriptionError;
        this.onSubscriptionComplete = onSubscriptionComplete;
        this.executorService = executorService;
        this.canceled = new AtomicBoolean(false);
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
        final AtomicBoolean isStarted = new AtomicBoolean(false);
        subscriptionFuture = executorService.submit(() -> {
            LOG.debug("Requesting subscription: " + getRequest().getContent());
            while (authTypes.hasNext() && !isStarted.get()) {
                AuthorizationType authType = authTypes.next();
                LOG.debug("Attempting to setup subscription with authType = " + authType);
                subscriptionEndpoint.requestSubscription(
                    getRequest(),
                    authType,
                    subscriptionId -> {
                        isStarted.set(true);
                        SubscriptionOperation.this.subscriptionId = subscriptionId;
                        onSubscriptionStart.accept(subscriptionId);
                    },
                    onNextItem,
                    apiException -> {
                        if (!authTypes.hasNext()){
                            cancel();
                            onSubscriptionError.accept(apiException);
                        }
                    },
                    onSubscriptionComplete
                );
            }
        });
    }

    @Override
    public synchronized void cancel() {
        if (subscriptionId != null && !canceled.get()) {
            canceled.set(true);
            try {
                LOG.debug("Cancelling subscription: " + subscriptionId);
                subscriptionEndpoint.releaseSubscription(subscriptionId);
            } catch (ApiException exception) {
                onSubscriptionError.accept(exception);
            }
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
        private Iterator<AuthorizationType> authTypes;

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
        public Builder<T> authTypes(Iterator<AuthorizationType> authTypes) {
            this.authTypes = authTypes;
            return this;
        }

        @NonNull
        public SubscriptionOperation<T> build() {
            return new SubscriptionOperation<>(
                Objects.requireNonNull(Builder.this.graphQlRequest),
                Objects.requireNonNull(Builder.this.responseFactory),
                Objects.requireNonNull(Builder.this.subscriptionEndpoint),
                Objects.requireNonNull(Builder.this.authTypes),
                Objects.requireNonNull(Builder.this.onSubscriptionStart),
                Objects.requireNonNull(Builder.this.onNextItem),
                Objects.requireNonNull(Builder.this.onSubscriptionError),
                Objects.requireNonNull(Builder.this.onSubscriptionComplete),
                Objects.requireNonNull(Builder.this.executorService)
            );
        }
    }
}
