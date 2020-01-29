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

package com.amplifyframework.testutils;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.hub.HubEventFilter;
import com.amplifyframework.hub.HubEventFilters;
import com.amplifyframework.hub.SubscriptionToken;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * Accumulates {@link HubEvent}s received on a {@link HubChannel}, into an in-memory
 * buffer. These may be queried by tests in a synchronous way to check if they have arrived.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class HubAccumulator {
    private final HubChannel channel;
    private final HubEventFilter filter;
    private final List<HubEvent<?>> events;
    private SubscriptionToken token;
    private CountDownLatch latch;

    private HubAccumulator(@NonNull HubChannel channel, @NonNull HubEventFilter filter) {
        this.channel = channel;
        this.filter = filter;
        this.events = new ArrayList<>();
    }

    /**
     * Gets an {@link HubAccumulator} that accumulates events arriving
     * on a particular channel.
     * @param channel Events will be accumulated for this channel only
     * @return A HubAccumulator for the requested channel
     */
    @NonNull
    public static HubAccumulator create(@NonNull HubChannel channel) {
        Objects.requireNonNull(channel);
        return new HubAccumulator(channel, HubEventFilters.always());
    }

    @NonNull
    public static HubAccumulator create(@NonNull HubChannel channel, @NonNull HubEventFilter filter) {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(filter);
        return new HubAccumulator(channel, filter);
    }

    /**
     * Start accumulating events.
     * @return HubAccumulator instance for fluent chaining
     */
    @NonNull
    public HubAccumulator start() {
        this.token = Amplify.Hub.subscribe(channel, filter, event -> {
            events.add(event);
            if (latch != null) {
                latch.countDown();
            }
        });
        return this;
    }

    /**
     * Stop accumulating events.
     * @return HubAccumulator instance for fluent chaining
     */
    @NonNull
    public HubAccumulator stop() {
        if (token != null) {
            Amplify.Hub.unsubscribe(token);
            token = null;
        }
        return this;
    }

    /**
     * Clear all events from the accumulator.
     * @return HubAccumulator instance for fluent chaining
     */
    @NonNull
    public HubAccumulator clear() {
        events.clear();
        return this;
    }

    /**
     * Wait for a quantity of events to be accumulated.
     * If there are fewer than this many, right now, this method will
     * block until the rest show up. If there are enough, they will
     * be returned immediately. The returned items will be cleared from the
     * accumulator.
     * @param desiredQuantity Number of items being awaited
     * @return A list of desiredQuantity many items
     * @throws RuntimeException On failure to attain the requested number of events
     *                          within a reasonable waiting period
     */
    @NonNull
    public List<HubEvent<?>> take(int desiredQuantity) {
        // If we haven't yet received the desired quantity of events on the subscription,
        // setup a latch to await the desired quantity, less the number of existing events.
        // For example: I desire 5, I already have 3, I wait for 2 more.
        if (events.size() < desiredQuantity) {
            latch = new CountDownLatch(desiredQuantity - events.size());
            Latch.await(latch);
            latch = null;
        }

        // If we already had the right number of events,
        // or if our latch counted down as a result of receiving the remaining number of events,
        // return the requested number of events.
        List<HubEvent<?>> returning = new ArrayList<>(events.subList(0, desiredQuantity));

        // Also, clear those events out of the events list so that next call to #take(int)
        // returns (a) unique value(s).
        Iterator<HubEvent<?>> iterator = events.iterator();
        while (iterator.hasNext()) {
            if (returning.contains(iterator.next())) {
                iterator.remove();
            }
        }

        return returning;
    }

    /**
     * Wait for the next event to show up, or pull the first one
     * in the accumulator, if there is one.
     * @return A HubEvent, either the first in the accumulator list, or
     *         the next one that shows up (in the future).
     */
    @NonNull
    public HubEvent<?> takeOne() {
        return take(1).get(0);
    }
}
