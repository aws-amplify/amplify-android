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

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

final class SubscriptionOperation<T> extends GraphQLOperation<T> {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-api");

    private final SubscriptionEndpoint subscriptionEndpoint;
    private final ExecutorService executorService;
    private final Consumer<String> onSubscriptionStarted;
    private final Consumer<GraphQLResponse<T>> onNextItem;
    private final Consumer<ApiException> onSubscriptionError;
    private final Action onSubscriptionComplete;
    private final AtomicReference<String> subscriptionId;

    private SubscriptionOperation(
            GraphQLRequest<T> graphQLRequest,
            GraphQLResponse.Factory responseFactory,
            SubscriptionEndpoint subscriptionEndpoint,
            ExecutorService executorService,
            Consumer<String> onSubscriptionStarted,
            Consumer<GraphQLResponse<T>> onNextItem,
            Consumer<ApiException> onSubscriptionError,
            Action onSubscriptionComplete) {
        super(graphQLRequest, responseFactory);
        this.subscriptionEndpoint = subscriptionEndpoint;
        this.executorService = executorService;
        this.onNextItem = onNextItem;
        this.onSubscriptionStarted = onSubscriptionStarted;
        this.onSubscriptionError = onSubscriptionError;
        this.onSubscriptionComplete = onSubscriptionComplete;
        this.subscriptionId = new AtomicReference<>(null);
    }

    @NonNull
    static <T> BuilderSteps.SubscriptionEndpointStep<T> builder() {
        return new Builder<>();
    }

    @Override
    public void start() {
        executorService.submit(() -> {
            LOG.debug("Request " + getRequest().getContent());
            subscriptionEndpoint.requestSubscription(
                getRequest(),
                justBeganSubscriptionId -> {
                    subscriptionId.set(justBeganSubscriptionId);
                    onSubscriptionStarted.accept(justBeganSubscriptionId);
                },
                onNextItem,
                onSubscriptionError,
                onSubscriptionComplete
            );
        });
    }

    @Override
    public void cancel() {
        String currentSubscriptionId = subscriptionId.get();
        if (currentSubscriptionId == null) {
            return;
        }
        try {
            subscriptionEndpoint.releaseSubscription(currentSubscriptionId);
        } catch (ApiException exception) {
            onSubscriptionError.accept(exception);
        }
    }

    static final class Builder<T> implements
            BuilderSteps.SubscriptionEndpointStep<T>,
            BuilderSteps.GraphQlRequestStep<T>,
            BuilderSteps.ResponseFactoryStep<T>,
            BuilderSteps.ExecutorServiceStep<T>,
            BuilderSteps.OnSubscriptionStartedStep<T>,
            BuilderSteps.OnNextItemStep<T>,
            BuilderSteps.OnSubscriptionErrorStep<T>,
            BuilderSteps.OnSubscriptionCompleteStep<T>,
        BuilderSteps.BuildStep<T> {
        private SubscriptionEndpoint subscriptionEndpoint;
        private GraphQLRequest<T> graphQLRequest;
        private GraphQLResponse.Factory responseFactory;
        private ExecutorService executorService;
        private Consumer<String> onSubscriptionStarted;
        private Consumer<GraphQLResponse<T>> onNextItem;
        private Consumer<ApiException> onSubscriptionError;
        private Action onSubscriptionComplete;

        @NonNull
        @Override
        public BuilderSteps.GraphQlRequestStep<T> subscriptionEndpoint(@NonNull SubscriptionEndpoint subscriptionEndpoint) {
            this.subscriptionEndpoint = Objects.requireNonNull(subscriptionEndpoint);
            return this;
        }

        @NonNull
        @Override
        public BuilderSteps.ResponseFactoryStep<T> graphQLRequest(@NonNull GraphQLRequest<T> graphQLRequest) {
            this.graphQLRequest = Objects.requireNonNull(graphQLRequest);
            return this;
        }

        @NonNull
        @Override
        public BuilderSteps.ExecutorServiceStep<T> responseFactory(@NonNull GraphQLResponse.Factory responseFactory) {
            this.responseFactory = Objects.requireNonNull(responseFactory);
            return this;
        }

        @NonNull
        @Override
        public BuilderSteps.OnSubscriptionStartedStep<T> executorService(@NonNull ExecutorService executorService) {
            this.executorService = Objects.requireNonNull(executorService);
            return this;
        }

        @NonNull
        @Override
        public BuilderSteps.OnNextItemStep<T> onSubscriptionStarted(@NonNull Consumer<String> onSubscriptionStarted) {
            this.onSubscriptionStarted = Objects.requireNonNull(onSubscriptionStarted);
            return this;
        }

        @NonNull
        @Override
        public BuilderSteps.OnSubscriptionErrorStep<T> onNextItem(@NonNull Consumer<GraphQLResponse<T>> onNextItem) {
            this.onNextItem = Objects.requireNonNull(onNextItem);
            return this;
        }

        @NonNull
        @Override
        public BuilderSteps.OnSubscriptionCompleteStep<T> onSubscriptionError(
                @NonNull Consumer<ApiException> onSubscriptionError) {
            this.onSubscriptionError = Objects.requireNonNull(onSubscriptionError);
            return this;
        }

        @NonNull
        @Override
        public BuilderSteps.BuildStep<T> onSubscriptionComplete(@NonNull Action onSubscriptionComplete) {
            this.onSubscriptionComplete = Objects.requireNonNull(onSubscriptionComplete);
            return this;
        }

        @NonNull
        @Override
        public SubscriptionOperation<T> build() {
            return new SubscriptionOperation<>(
                graphQLRequest,
                responseFactory,
                subscriptionEndpoint,
                executorService,
                onSubscriptionStarted,
                onNextItem,
                onSubscriptionError,
                onSubscriptionComplete
            );
        }
    }

    interface BuilderSteps {
        interface SubscriptionEndpointStep<T> {
            @NonNull
            GraphQlRequestStep<T> subscriptionEndpoint(@NonNull SubscriptionEndpoint subscriptionEndpoint);
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
            BuildStep<T> onSubscriptionComplete(@NonNull Action onSubscriptionComplete);
        }

        interface BuildStep<T> {
            @NonNull
            SubscriptionOperation<T> build();
        }
    }
}

