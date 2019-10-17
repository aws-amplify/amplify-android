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

import com.amplifyframework.core.async.AmplifyOperation;
import com.amplifyframework.core.async.EventListener;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;

/**
 * Amplify has a local eventing system called Hub. It is a lightweight implementation of
 * Publisher-Subscriber pattern, and is used to share data between modules and components
 * in your app. Amplify uses Hub for different categories to communicate with one another
 * when specific events occur, such as authentication events like a user sign-in or
 * notification of a file download.
 */
public final class HubCategory extends Category<HubPlugin<?>> implements HubCategoryBehavior {

    /**
     * Publish a Hub payload on the specified channel.
     * @param hubChannel The channel on which to send the payload
     * @param hubPayload The payload to send
     * @throws HubException on publication failure
     */
    @Override
    public void publish(@NonNull HubChannel hubChannel, @NonNull HubPayload hubPayload)
            throws HubException {
        getSelectedPlugin().publish(hubChannel, hubPayload);
    }

    /**
     * Subscribe to Hub payloads on a particular channel.
     * @param hubChannel The channel on which to listen for payloads
     * @param listener   The callback to invoke with the received payload
     * @return the token which serves as an identifier for the listener
     * registered. The token can be used with
     * {@link #unsubscribe(SubscriptionToken)}
     * to de-register the listener.
     * @throws HubException on failure to subscribe
     */
    @Override
    public SubscriptionToken subscribe(@NonNull HubChannel hubChannel,
                                       @NonNull HubListener listener) throws HubException {
        return getSelectedPlugin().subscribe(hubChannel, listener);
    }

    /**
     * Subscribe to Hub payloads on a particular channel; payloads will be received
     * only if the provided payload filter matches a given payload.
     * @param hubChannel The channel to listen for payloads on
     * @param hubPayloadFilter candidate payload will be passed to this closure prior to dispatching to
     *                   the {@link HubListener}. Only payloads for which the closure returns
     *                   `true` will be dispatched.
     * @param listener   The callback to invoke with the received payload
     * @return the token which serves as an identifier for the listener
     * registered. The token can be used with #unsubscribe(SubscriptionToken)
     * to de-register the listener.
     */
    @Override
    public SubscriptionToken subscribe(@NonNull HubChannel hubChannel,
                                       @Nullable HubPayloadFilter hubPayloadFilter,
                                       @NonNull HubListener listener) throws HubException {
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
    public CategoryType getCategoryType() {
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
     * @param <E> The type of the event that the event listener will receive
     * @return A subscription token
     */
    public <E> SubscriptionToken subscribe(
            @NonNull final AmplifyOperation operation,
            @NonNull final EventListener<E> eventListener) {
        HubChannel channel = HubChannel.forCategoryType(operation.getCategoryType());
        HubPayloadFilter filter = HubFilters.hubPayloadFilter(operation);
        HubListener transformingListener = payload -> {
            // TODO: check for casting of Object to E and
            // see if it can be prevented.
            // eventListener.onEvent(payload.getEventData());
        };

        return subscribe(channel, filter, transformingListener);
    }
}
