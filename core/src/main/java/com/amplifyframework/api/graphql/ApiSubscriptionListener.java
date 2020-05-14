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

import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.GraphQlBehavior;

/**
 * A listener to a GraphQL subscription. Used when registering for subscription data via the
 * {@link GraphQlBehavior}s in the {@link ApiCategory}. Extend this class or provide an anonymous
 * inline instance when registering with the
 * {@link GraphQlBehavior#subscribe(GraphQLRequest, ApiSubscriptionListener)} family of methods.
 * @param <T> Type of application data found on the subscription
 */
public /* extensible */ class ApiSubscriptionListener<T> {
    /**
     * Called when a subscription starts.
     * @param subscriptionToken A token which uniquely identifies this new subscription.
     */
    public void onSubscriptionStarted(String subscriptionToken) {
    }

    /**
     * Called when the next installment of data is available on the subscription.
     * This is always called after {@link #onSubscriptionStarted(String)}, and
     * may be called 0 or more times before either {@link #onSubscriptionFailure(ApiException)}
     * or {@link #onSubscriptionComplete()}.
     * @param subscriptionData Data found on the subscription, enveloped in an {@link GraphQLResponse}.
     */
    public void onSubscriptionData(GraphQLResponse<T> subscriptionData) {
    }

    /**
     * Called when the subscription terminates gracefully, perhaps because there
     * is no more data available on the subscription.
     */
    public void onSubscriptionComplete() {
    }

    /**
     * Called when either (1) a not-yet-started subscription fails to start, or (2) an already-started
     * subscription terminates, with an unrecoverable failure.
     * @param subscriptionFailure A failure to begin subscription, or a failure which terminates an
     *                            ongoing subscription
     */
    public void onSubscriptionFailure(ApiException subscriptionFailure) {
    }
}
