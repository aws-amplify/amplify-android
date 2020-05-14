/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.graphql;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;

import java.util.Objects;

/**
 * A {@link ApiSubscriptionListener} which works by delegating its lifecycle callbacks
 * to one of four provided functional interfaces. For example:
 * <pre>
 *     DelegatingSubscriptionListener.create(
 *         start -> {},
 *         data -> {},
 *         failure > {},
 *         () -> {}
 *     );
 * </pre>
 * @param <T> Type of data found on subscription
 */
public final class DelegatingApiSubscriptionListener<T> extends ApiSubscriptionListener<T> {
    private final Consumer<String> onStart;
    private final Consumer<GraphQLResponse<T>> onNext;
    private final Consumer<ApiException> onFailure;
    private final Action onComplete;

    private DelegatingApiSubscriptionListener(
            Consumer<String> onStart,
            Consumer<GraphQLResponse<T>> onNext,
            Consumer<ApiException> onFailure,
            Action onComplete) {
        this.onStart = onStart;
        this.onNext = onNext;
        this.onFailure = onFailure;
        this.onComplete = onComplete;
    }

    /**
     * Creates a new {@link DelegatingApiSubscriptionListener}.
     * @param onStart Called for {@link ApiSubscriptionListener#onSubscriptionStarted(String)}
     * @param onNext Called for {@link ApiSubscriptionListener#onSubscriptionData(GraphQLResponse)}
     * @param onFailure Called for {@link ApiSubscriptionListener#onSubscriptionFailure(ApiException)}
     * @param onComplete Called for {@link ApiSubscriptionListener#onSubscriptionComplete()}
     * @param <T> The type of application data found on the subscription
     * @return A subscription listener which delegates to the provided lambda arguments.
     */
    public static <T> DelegatingApiSubscriptionListener<T> create(
            Consumer<String> onStart,
            Consumer<GraphQLResponse<T>> onNext,
            Consumer<ApiException> onFailure,
            Action onComplete) {
        Objects.requireNonNull(onStart);
        Objects.requireNonNull(onNext);
        Objects.requireNonNull(onFailure);
        Objects.requireNonNull(onComplete);
        return new DelegatingApiSubscriptionListener<>(onStart, onNext, onFailure, onComplete);
    }

    @Override
    public void onSubscriptionStarted(String subscriptionToken) {
        onStart.accept(subscriptionToken);
    }

    @Override
    public void onSubscriptionData(GraphQLResponse<T> subscriptionData) {
        onNext.accept(subscriptionData);
    }

    @Override
    public void onSubscriptionComplete() {
        onComplete.call();
    }

    @Override
    public void onSubscriptionFailure(ApiException subscriptionFailure) {
        onFailure.accept(subscriptionFailure);
    }
}
