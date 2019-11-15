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

    @Override
    public void publish(@NonNull HubChannel hubChannel, @NonNull HubEvent hubEvent)
            throws HubException {
        getSelectedPlugin().publish(hubChannel, hubEvent);
    }

    @Override
    public SubscriptionToken subscribe(@NonNull HubChannel hubChannel,
                                       @NonNull HubSubscriber hubSubscriber) throws HubException {
        return getSelectedPlugin().subscribe(hubChannel, hubSubscriber);
    }

    @Override
    public SubscriptionToken subscribe(@NonNull HubChannel hubChannel,
                                       @Nullable HubEventFilter hubEventFilter,
                                       @NonNull HubSubscriber hubSubscriber) throws HubException {
        return getSelectedPlugin().subscribe(hubChannel, hubEventFilter, hubSubscriber);
    }

    @Override
    public void unsubscribe(@NonNull SubscriptionToken subscriptionToken) throws HubException {
        getSelectedPlugin().unsubscribe(subscriptionToken);
    }

    @Override
    public CategoryType getCategoryType() {
        return CategoryType.HUB;
    }

    /**
     * Convenience method to allow callers to subscribe to Hub events
     * for a particular operation.  Internally, the subscription
     * transforms the HubEvent into the Operation's expected AsyncEvent
     * type, so callers may re-use their `subscriber`s.
     *
     * @param operation The operation to subscribe to events for
     * @param eventListener The Operation-specific listener to be invoked
     *                 when an AsyncEvent for that operation is received.
     * @param <E> The type of the event that the event listener will receive
     * @param <R> The type of the request object of the {@link AmplifyOperation}
     * @return A subscription token
     */
    public <E, R> SubscriptionToken subscribe(
            @NonNull final AmplifyOperation<R> operation,
            @NonNull final EventListener<E> eventListener) {
        HubChannel channel = HubChannel.forCategoryType(operation.getCategoryType());
        HubSubscriber transformingListener = event -> {
            // TODO: check for casting of Object to E and
            // see if it can be prevented.
            // eventListener.onEvent(event.getData());
        };

        return subscribe(channel, event -> true, transformingListener);
    }
}
