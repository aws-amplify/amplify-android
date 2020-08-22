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

package com.amplifyframework.rx;

import androidx.annotation.NonNull;

import com.amplifyframework.hub.HubCategoryBehavior;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.hub.HubException;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

/**
 * An Rx-idiomatic version of Amplify's {@link HubCategoryBehavior}.
 */
public interface RxHubCategoryBehavior {

    /**
     * Publish an event to the specified channel.
     * @param hubChannel The channel on which to dispatch the event
     * @param hubEvent The event to send
     * @return A Completable which terminates with {@link HubException} on publication failure
     * @param <T> Type of data stored in the event
     */
    @NonNull
    <T> Completable publish(
            @NonNull HubChannel hubChannel,
            @NonNull HubEvent<T> hubEvent
    );

    /**
     * Observe Hub events that arrive on a particular channel.
     * @param hubChannel A channel on which to subscribe to events
     * @return An {@link Observable} stream of {@link HubEvent}
     */
    @NonNull
    Observable<HubEvent<?>> on(
        @NonNull HubChannel hubChannel
    );
}
