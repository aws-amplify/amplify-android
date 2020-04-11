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
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.hub.HubEventFilter;
import com.amplifyframework.hub.HubEventFilters;
import com.amplifyframework.hub.SubscriptionToken;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private final CountDownLatch latch;
    private SubscriptionToken token;

    private HubAccumulator(HubChannel channel, HubEventFilter filter, int quantity) {
        this.channel = channel;
        this.filter = filter;
        this.events = new CopyOnWriteArrayList<>();
        this.latch = new CountDownLatch(quantity);
        this.token = null;
    }

    /**
     * Gets an {@link HubAccumulator} that accumulates events arriving
     * on a particular channel.
     * @param channel Events will be accumulated for this channel only
     * @param quantity Number of events to await
     * @return A HubAccumulator for the requested channel
     */
    @NonNull
    public static HubAccumulator create(@NonNull HubChannel channel, int quantity) {
        Objects.requireNonNull(channel);
        return create(channel, HubEventFilters.always(), quantity);
    }

    /**
     * Gets an {@link HubAccumulator} that accumulates events arriving
     * on a particular channel.
     * @param channel Events will be accumulated for this channel only
     * @param filter Filter to apply to accumulating events
     * @param quantity Number of events to await
     * @return A HubAccumulator for the requested channel
     */
    @NonNull
    public static HubAccumulator create(@NonNull HubChannel channel, @NonNull HubEventFilter filter, int quantity) {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(filter);
        return new HubAccumulator(channel, filter, quantity);
    }

    /**
     * Gets an {@link HubAccumulator} that accumulates events arriving on a particular channel,
     * whose event name is the given enum value (as string). For example, an accumulator
     * created in this way will match all events with name {@link DataStoreChannelEventName#PUBLISHED_TO_CLOUD}:
     *
     *   HubAccumulator.create(HubChannel.DATASTORE, DataStoreChannelEventName.PUBLISH_TO_CLOUD);
     *
     * @param channel Channel on which to listen for events
     * @param enumeratedEventName A enum value, the toString() of which is expected as the name of
     *                             a hub event. Only events with this name will be accumulated.
     * @param quantity Number of events to accumulate
     * @param <E> The type of enumeration
     * @return A HubAccumulator
     */
    @NonNull
    public static <E extends Enum<E>> HubAccumulator create(
            @NonNull HubChannel channel, @NonNull E enumeratedEventName, int quantity) {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(enumeratedEventName);
        HubEventFilter filter = event -> enumeratedEventName.toString().equals(event.getName());
        return new HubAccumulator(channel, filter, quantity);
    }

    /**
     * Start accumulating events.
     * @return HubAccumulator instance for fluent chaining
     */
    @NonNull
    public HubAccumulator start() {
        this.token = Amplify.Hub.subscribe(channel, filter, event -> {
            events.add(event);
            latch.countDown();
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
     * @return A list of desiredQuantity many items
     * @throws RuntimeException On failure to attain the requested number of events
     *                          within a reasonable waiting period
     */
    @NonNull
    public List<HubEvent<?>> takeAll() {
        Latch.await(latch);

        // If we already had the right number of events,
        // or if our latch counted down as a result of receiving the remaining number of events,
        // return the requested number of events.
        CopyOnWriteArrayList<HubEvent<?>> returning = new CopyOnWriteArrayList<>(events);

        // Also, clear those events out of the events list so that next call to #takeAll().
        // returns (a) unique value(s).
        Iterator<HubEvent<?>> iterator = events.iterator();
        while (iterator.hasNext()) {
            if (returning.contains(iterator.next())) {
                iterator.remove();
            }
        }

        return returning;
    }
}

