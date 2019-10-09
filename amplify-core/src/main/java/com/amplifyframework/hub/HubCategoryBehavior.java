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
import androidx.annotation.Nullable;

/**
 * Defines the client behavior (client API) consumed
 * by the app for collection and sending of Analytics
 * events.
 */
public interface HubCategoryBehavior {
    /**
     * Dispatch a Hub message on the specified channel
     * @param hubChannel The channel to send the message on
     * @param hubpayload The payload to send
     */
    void publish(@NonNull final HubChannel hubChannel,
                 @NonNull final HubPayload hubpayload) throws HubException;

    /**
     * Listen to Hub messages on a particular channel,
     *
     * @param hubChannel The channel to listen for messages on
     * @param listener The callback to invoke with the received message
     * @return the token which serves as an identifier for the listener
     *          registered. The token can be used with
     *          {@link #unsubscribe(SubscriptionToken)}
     *          to de-register the listener.
     */
    SubscriptionToken subscribe(@NonNull final HubChannel hubChannel,
                                @Nullable final HubListener listener) throws HubException;

    /**
     * Listen to Hub messages on a particular channel,
     *
     * @param hubChannel The channel to listen for messages on
     * @param hubPayloadFilter candidate messages will be passed to this closure prior to dispatching to
     *                  the {@link HubListener}. Only messages for which the closure returns
     *                  `true` will be dispatched.
     * @param listener The callback to invoke with the received message
     * @return the token which serves as an identifier for the listener
     *          registered. The token can be used with #unsubscribe(SubscriptionToken)
     *          to de-register the listener.
     */
    SubscriptionToken subscribe(@NonNull final HubChannel hubChannel,
                                @Nullable final HubPayloadFilter hubPayloadFilter,
                                @Nullable final HubListener listener) throws HubException;

    /**
     * The registered listener can be removed from the Hub system by passing the
     * token received from {@link #subscribe(HubChannel, HubListener)} or
     * {@link #subscribe(HubChannel, HubPayloadFilter, HubListener)}.
     *
     * @param subscriptionToken the token which serves as an identifier for the listener
     *                 {@link HubListener} registered
     */
    void unsubscribe(@NonNull final SubscriptionToken subscriptionToken) throws HubException;
}
