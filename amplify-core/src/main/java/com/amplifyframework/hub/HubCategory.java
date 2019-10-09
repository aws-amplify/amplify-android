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

import com.amplifyframework.core.async.AmplifyOperation;
import com.amplifyframework.core.async.AmplifyOperationRequest;
import com.amplifyframework.core.async.EventListener;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Amplify has a local eventing system called Hub. It is a lightweight implementation of
 * Publisher-Subscriber pattern, and is used to share data between modules and components
 * in your app. Amplify uses Hub for different categories to communicate with one another
 * when specific events occur, such as authentication events like a user sign-in or
 * notification of a file download.
 */
public class HubCategory extends Category<HubPlugin<?>> implements HubCategoryBehavior {

    /**
     * Dispatch a Hub message on the specified channel
     *
     * @param hubChannel The channel to send the message on
     * @param hubpayload The payload to send
     */
    @Override
    public void publish(@NonNull HubChannel hubChannel, @NonNull HubPayload hubpayload)
            throws HubException {
        getSelectedPlugin().publish(hubChannel, hubpayload);
    }

    /**
     * Listen to Hub messages on a particular channel,
     *
     * @param hubChannel The channel to listen for messages on
     * @param listener   The callback to invoke with the received message
     * @return the token which serves as an identifier for the listener
     * registered. The token can be used with
     * {@link #unsubscribe(SubscriptionToken)}
     * to de-register the listener.
     */
    @Override
    public SubscriptionToken subscribe(@NonNull HubChannel hubChannel,
                                       @Nullable HubListener listener) throws HubException {
        return getSelectedPlugin().subscribe(hubChannel, listener);
    }

    /**
     * Listen to Hub messages on a particular channel,
     *
     * @param hubChannel The channel to listen for messages on
     * @param hubPayloadFilter  candidate messages will be passed to this closure prior to dispatching to
     *                   the {@link HubListener}. Only messages for which the closure returns
     *                   `true` will be dispatched.
     * @param listener   The callback to invoke with the received message
     * @return the token which serves as an identifier for the listener
     * registered. The token can be used with #unsubscribe(SubscriptionToken)
     * to de-register the listener.
     */
    @Override
    public SubscriptionToken subscribe(@NonNull HubChannel hubChannel,
                                       @Nullable HubPayloadFilter hubPayloadFilter,
                                       @Nullable HubListener listener) throws HubException {
        return getSelectedPlugin().subscribe(hubChannel, hubPayloadFilter, listener);
    }

    /**
     * The registered listener can be removed from the Hub system by passing the
     * token received from {@link #subscribe(HubChannel, HubListener)} or
     * {@link #subscribe(HubChannel, HubPayloadFilter, HubListener)}.
     *
     * @param subscriptionToken the token which serves as an identifier for the listener
     *                          {@link HubListener} registered
     */
    @Override
    public void unsubscribe(@NonNull SubscriptionToken subscriptionToken) throws HubException {
        getSelectedPlugin().unsubscribe(subscriptionToken);
    }

    @Override
    public final CategoryType getCategoryType() {
        return CategoryType.HUB;
    }

    /**
     * Convenience method to allow callers to listen to Hub events for a particular operation.
     * Internally, the listener transforms the HubPayload into the Operation's expected AsyncEvent
     * type, so callers may re-use their `listener`s.
     *
     * @param operation The operation to listen to events for
     * @param eventListener The Operation-specific listener callback to be invoked
     *                 when an AsyncEvent for that operation is received.
     */
    public <R extends AmplifyOperationRequest<?>, E> SubscriptionToken subscribe(
            @NonNull final AmplifyOperation<R> operation,
            @NonNull final EventListener<E> eventListener) {
        HubChannel channel = HubChannel.forCategoryType(operation.getCategoryType());
        HubPayloadFilter filter = HubFilters.hubPayloadFilter(operation);
        HubListener transformingListener = new HubListener() {
            @Override
            public void onEvent(@NonNull HubPayload payload) {
                // TODO: check for casting of Object to E and
                // see if it can be prevented.
                // eventListener.onEvent(payload.getEventData());
            }
        };

        return subscribe(channel, filter, transformingListener);
    }
}
