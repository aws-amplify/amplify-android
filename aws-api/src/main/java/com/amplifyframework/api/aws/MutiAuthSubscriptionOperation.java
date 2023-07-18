/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.api.ApiException.ApiAuthException;
import com.amplifyframework.api.aws.auth.AuthRuleRequestDecorator;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.auth.AuthorizationTypeIterator;
import com.amplifyframework.datastore.appsync.AppSyncExtensions;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.Empty;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

final class MutiAuthSubscriptionOperation<T> extends GraphQLOperation<T> {
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.API, "amplify:aws-api");

    private final SubscriptionEndpoint subscriptionEndpoint;
    private final ExecutorService executorService;
    private final Consumer<String> onSubscriptionStart;
    private final Consumer<GraphQLResponse<T>> onNextItem;
    private final Consumer<ApiException> onSubscriptionError;
    private final Action onSubscriptionComplete;
    private final AtomicBoolean canceled;
    private final AuthRuleRequestDecorator requestDecorator;

    private AuthorizationTypeIterator authTypes;
    private String subscriptionId;
    private Future<?> subscriptionFuture;

    private MutiAuthSubscriptionOperation(Builder<T> builder) {
        super(builder.graphQlRequest, builder.responseFactory);
        this.subscriptionEndpoint = builder.subscriptionEndpoint;
        this.onSubscriptionStart = builder.onSubscriptionStart;
        this.onNextItem = builder.onNextItem;
        this.onSubscriptionError = builder.onSubscriptionError;
        this.onSubscriptionComplete = builder.onSubscriptionComplete;
        this.executorService = builder.executorService;
        this.canceled = new AtomicBoolean(false);
        this.requestDecorator = builder.requestDecorator;
        this.authTypes = MultiAuthModeStrategy.getInstance()
                                              .authTypesFor(builder.graphQlRequest.getModelSchema(),
                                                            builder.graphQlRequest.getAuthRuleOperation());
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
        subscriptionFuture = executorService.submit(this::dispatchRequest);
    }

    private void dispatchRequest() {
        LOG.debug("Processing subscription request: " + getRequest().getContent());
        // If the auth types iterator still has items to return;
        if (authTypes.hasNext()) {
            // Advance the iterator, and get the next auth type to try.
            AuthorizationType authorizationType = authTypes.next();
            LOG.debug("Attempting to subscribe with " + authorizationType.name());
            GraphQLRequest<T> request = getRequest();
            // if the rule we're currently processing is an owner-based rule,
            // then call the AuthRuleRequestDecorator to see if the owner needs to be
            // added to the request.
            if (authTypes.isOwnerBasedRule()) {
                try {
                    request = requestDecorator.decorate(request, authorizationType);
                } catch (ApiAuthException apiAuthException) {
                    // For ApiAuthExceptions, just queue up a dispatchRequest call. If there are no
                    // other auth types left, it will emit the error to the client's callback
                    // because authTypes.hasNext() will be false.
                    subscriptionFuture = executorService.submit(this::dispatchRequest);
                    return;
                } catch (ApiException apiException) {
                    LOG.warn("Unable to automatically add an owner to the request.", apiException);
                    emitErrorAndCancelSubscription(apiException);
                    return;
                }
            }
            subscriptionEndpoint.requestSubscription(
                request,
                authorizationType,
                subscriptionId -> {
                    MutiAuthSubscriptionOperation.this.subscriptionId = subscriptionId;
                    onSubscriptionStart.accept(subscriptionId);
                },
                response -> {
                    if (response.hasErrors() && hasAuthRelatedErrors(response) && authTypes.hasNext()) {
                        // If there are auth-related errors, dispatch an ApiAuthException
                        executorService.submit(this::dispatchRequest);
                    } else {
                        // Otherwise, we just want to dispatch it as a next item and
                        // let callers deal with the errors.
                        onNextItem.accept(response);
                    }
                },
                apiException -> {
                    LOG.warn("A subscription error occurred.", apiException);
                    if (apiException instanceof ApiAuthException && authTypes.hasNext()) {
                        executorService.submit(this::dispatchRequest);
                    } else {
                        emitErrorAndCancelSubscription(apiException);
                    }
                },
                onSubscriptionComplete
            );
        } else {
            emitErrorAndCancelSubscription(new ApiException("Unable to establish subscription connection.",
                                                        AmplifyException.TODO_RECOVERY_SUGGESTION));
        }

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

    private boolean hasAuthRelatedErrors(GraphQLResponse<T> response) {
        for (GraphQLResponse.Error error : response.getErrors()) {
            if (!Empty.check(error.getExtensions())) {
                AppSyncExtensions extensions = new AppSyncExtensions(error.getExtensions());
                return extensions.isUnauthorizedErrorType();
            }
        }
        return false;
    }

    private void emitErrorAndCancelSubscription(ApiException apiException) {
        cancel();
        onSubscriptionError.accept(apiException);
    }

    static final class Builder<T> {
        private SubscriptionEndpoint subscriptionEndpoint;
        private AppSyncGraphQLRequest<T> graphQlRequest;
        private GraphQLResponse.Factory responseFactory;
        private ExecutorService executorService;
        private Consumer<String> onSubscriptionStart;
        private Consumer<GraphQLResponse<T>> onNextItem;
        private Consumer<ApiException> onSubscriptionError;
        private Action onSubscriptionComplete;
        private AuthRuleRequestDecorator requestDecorator;

        @NonNull
        public Builder<T> subscriptionEndpoint(@NonNull SubscriptionEndpoint subscriptionEndpoint) {
            this.subscriptionEndpoint = Objects.requireNonNull(subscriptionEndpoint);
            return this;
        }

        @NonNull
        public Builder<T> graphQlRequest(@NonNull AppSyncGraphQLRequest<T> graphQlRequest) {
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

        public Builder<T> requestDecorator(AuthRuleRequestDecorator requestDecorator) {
            this.requestDecorator = requestDecorator;
            return this;
        }

        @NonNull
        public MutiAuthSubscriptionOperation<T> build() {
            return new MutiAuthSubscriptionOperation<>(this);
        }
    }
}
