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

import android.util.Log;
import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;

/**
 * An instance of the {@link HubSubscriber} may be passed to the
 * {@link HubCategory#subscribe(HubChannel, HubSubscriber)} or the
 * {@link HubCategory#subscribe(HubChannel, HubEventFilter, HubSubscriber)}
 * methods to subscribe to various types of Hub events.
 */
public interface HubSubscriber {
    /**
     * Called to notify that there is a new event available in the Hub.
     * @param hubEvent A hub event
     */
    void onEvent(@NonNull HubEvent<?> hubEvent);

    /**
     * Factory method that provides a cleaner way for a "strongly"-typed
     * subscriber to be created.
     * @param eventDataHandler A function that processes the provided event data.
     * @param <T> An implementation of {@link HubEvent.Data} interface that represents the event payload.
     * @return An implementation of the {@link HubSubscriber} interface that attempts to
     *          cast the event payload into the desired type and then invokes the
     *          function provided in the eventDataHandler parameter.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static <T extends HubEvent.Data> HubSubscriber create(@NonNull Consumer<T> eventDataHandler) {
        return hubEvent -> {
            try {
                eventDataHandler.accept((T) hubEvent.getData());
            } catch (Exception exception) {
                Log.w("amplify:aws-hub", "Unable to cast event data for event type " + hubEvent.getName(), exception);
            }
        };
    }
}
