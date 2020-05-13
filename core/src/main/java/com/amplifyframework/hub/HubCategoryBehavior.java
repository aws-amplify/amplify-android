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

package com.amplifyframework.hub;

import androidx.annotation.NonNull;

/**
 * Defines the client behavior (client API) consumed
 * by the app for collection and sending of Analytics
 * events.
 */
public interface HubCategoryBehavior {

    /**
     * Publish an event to the specified channel.
     * @param hubChannel The channel on which to dispatch the event
     * @param hubEvent The event to send
     * @throws RuntimeException If publication fails
     * @param <T> Type of data in the event
     */
    <T> void publish(@NonNull HubChannel hubChannel,
                     @NonNull HubEvent<T> hubEvent) throws RuntimeException;

    /**
     * Subscribe to Hub events that arrive on a particular channel.
     * @param hubChannel A channel on which to subscribe to events
     * @param hubSubscriber A subscriber who will receive hub events
     * @return A token which serves as an identifier for the subscription.
     *         The token can be used with {@link #unsubscribe(SubscriptionToken)}
     *         to cancel the subscription.
     */
    @NonNull
    SubscriptionToken subscribe(@NonNull HubChannel hubChannel,
                                @NonNull HubSubscriber hubSubscriber);

    /**
     * Subscribe to Hub events on a particular channel, and considering
     * the result of applying a provided {@link HubEventFilter}, to
     * further constrain the received events.
     * @param hubChannel A channel on which a subscriber will receive events
     * @param hubEventFilter
     *        Candidate messages will be passed to this closure prior to
     *        dispatching to the {@link HubSubscriber}. Only messages
     *        for which the closure returns `true` will be dispatched.
     * @param hubSubscriber A subscriber who will receive hub events
     * @return A token which serves as an identifier for the
     *         subscription. The token can be used with
     *         {@link #unsubscribe(SubscriptionToken)} to cancel the
     *         subscription.
     */
    @NonNull
    SubscriptionToken subscribe(@NonNull HubChannel hubChannel,
                                @NonNull HubEventFilter hubEventFilter,
                                @NonNull HubSubscriber hubSubscriber);

    /**
     * Unsubscribe a subscriber from the Hub system by passing the
     * token received from {@link #subscribe(HubChannel, HubSubscriber)} or
     * {@link #subscribe(HubChannel, HubEventFilter, HubSubscriber)}.
     * @param subscriptionToken A token which serves as an identifier for a subscription
     */
    void unsubscribe(@NonNull SubscriptionToken subscriptionToken);
}

