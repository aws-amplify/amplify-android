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

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;

/**
 * Interface that defines the actions to be implemented by a class that is
 * going to be used as a GraphQL subscription endpoint.
 */
public interface SubscriptionEndpoint {
    /**
     * Given a GraphQL subscription request, an implementation of this function should abstract the
     * steps needed to establish/manage subscriptions with the backend service.
     * @param request the GraphQL request.
     * @param onSubscriptionStarted A callback to be invoked when a subscription starts.
     * @param onNextItem A callback invoked when the subscription receives data.
     * @param onSubscriptionError A callback invoked when an error occurs.
     * @param onSubscriptionComplete A callback invoked when the subscription is completed.
     * @param <T> The type of data in the request/response.
     */
    <T> void requestSubscription(
        @NonNull GraphQLRequest<T> request,
        @NonNull Consumer<String> onSubscriptionStarted,
        @NonNull Consumer<GraphQLResponse<T>> onNextItem,
        @NonNull Consumer<ApiException> onSubscriptionError,
        @NonNull Action onSubscriptionComplete);

    /**
     * Given a subscriptionId, an implementation of this function should perform the necessary steps
     * to clean up any resources pertaining to the subscription being released.
     * @param subscriptionId The id of the subscription to be released.
     * @throws ApiException If an error occurs while releasing the subscription.
     */
    void releaseSubscription(String subscriptionId) throws ApiException;
}
