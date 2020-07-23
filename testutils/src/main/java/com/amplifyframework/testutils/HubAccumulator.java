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
import com.amplifyframework.util.Immutable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Accumulates {@link HubEvent}s received on a {@link HubChannel}, into an in-memory
 * buffer. These may be queried by tests in a synchronous way to check if they have arrived.
 */
public final class HubAccumulator {
    private final HubChannel channel;
    private final HubEventFilter filter;
    private final CountDownLatch latch;
    private final long quantity;
    private final CopyOnWriteArrayList<HubEvent<?>> events;
    private final AtomicReference<SubscriptionToken> token;

    private HubAccumulator(
            @NonNull HubChannel channel, @NonNull HubEventFilter filter, int quantity) {
        this.channel = channel;
        this.filter = filter;
        this.latch = new CountDownLatch(quantity);
        this.quantity = quantity;
        this.events = new CopyOnWriteArrayList<>();
        this.token = new AtomicReference<>();
    }

    /**
     * Creates an {@link HubAccumulator} that accumulates events arriving
     * on a particular channel.
     * @param channel Events will be accumulated for this channel only
     * @param quantity Number of events to accumulate
     * @return A HubAccumulator for the requested channel
     */
    @NonNull
    public static HubAccumulator create(@NonNull HubChannel channel, int quantity) {
        return create(channel, HubEventFilters.always(), quantity);
    }

    /**
     * Creates an {@link HubAccumulator} that accumulates events arriving
     * on a particular channel.
     * @param channel Events will be accumulated for this channel only
     * @param filter Filter to apply to accumulating events
     * @param quantity Number of events to accumulate
     * @return A HubAccumulator for the requested channel
     */
    @NonNull
    public static HubAccumulator create(
            @NonNull HubChannel channel, @NonNull HubEventFilter filter, int quantity) {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(filter);
        return new HubAccumulator(channel, filter, quantity);
    }

    /**
     * Creates a {@link HubAccumulator} that accumulates events arriving on a particular channel,
     * whose event name is the given enum value (as string). For example, the accumulator
     * created as below will match all events with name {@link DataStoreChannelEventName#PUBLISHED_TO_CLOUD}:
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
     * @throws IllegalStateException If the accumulator has already been started
     */
    @NonNull
    public HubAccumulator start() {
        if (token.get() != null) {
            throw new IllegalStateException("Already started.");
        }
        this.token.set(Amplify.Hub.subscribe(channel, filter, event -> {
            synchronized (events) {
                if (events.size() < quantity) {
                    events.add(event);
                    latch.countDown();
                    if (latch.getCount() == 0) {
                        Amplify.Hub.unsubscribe(this.token.get());
                    }
                }
            }
        }));
        return this;
    }

    /**
     * Wait for the desired quantity of events to be accumulated.
     * If there are fewer than this many, right now, this method will
     * block until the rest show up. If there are enough, they will
     * be returned immediately.
     * @return A list of as many items as requested when the accumulator was created
     * @throws RuntimeException On failure to attain the requested number of events
     *                          within a reasonable waiting period
     */
    @NonNull
    public List<HubEvent<?>> await() {
        Latch.await(latch);
        return Immutable.of(events);
    }

    /**
     * Wait for the desired quantity of events to be accumulated.
     * Waits for a specified amount of time.
     * If there are fewer than the desired amount of events right now, this method will
     * block until the rest show up. If there are enough, they will
     * be returned immediately.
     * @param amount Amount of time, e.g. 5 seconds
     * @param unit Unit attached to the amount, e.g. {@link TimeUnit#SECONDS}
     * @return A list of as many items as requested when the accumulator was created
     * @throws RuntimeException On failure to attain the requested number of events
     *                          within a reasonable waiting period
     */
    public List<HubEvent<?>> await(int amount, TimeUnit unit) {
        Latch.await(latch, unit.toMillis(amount));
        return Immutable.of(events);
    }
}
