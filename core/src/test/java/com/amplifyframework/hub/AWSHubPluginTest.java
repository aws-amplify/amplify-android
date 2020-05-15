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

import com.amplifyframework.testutils.Latch;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Validates the functionality of the {@link AWSHubPlugin}.
 */
public final class AWSHubPluginTest {
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(1);

    private AWSHubPlugin hub;

    /**
     * Creates an instance of {@link AWSHubPlugin}, to test.
     */
    @Before
    public void setup() {
        this.hub = new AWSHubPlugin();
    }

    /**
     * Validates that a subscribe can subscribe, receive an event, and then unsubscribe.
     */
    @Test
    public void basicSubscribeReceiveUnsubscribe() {
        CountDownLatch latch = new CountDownLatch(1);
        SubscriptionToken token = hub.subscribe(HubChannel.DATASTORE, event -> latch.countDown());
        hub.publish(HubChannel.DATASTORE, HubEvent.create("hello!"));
        Latch.await(latch);
        hub.unsubscribe(token);
    }

    /**
     * Validates that a subscriber will not continue to receive events
     * from the hub, once it has unsubscribed.
     * @throws InterruptedException when waiting for CountDownLatch to
     *                              meet the desired condition is interrupted.
     */
    @Test
    public void noEventReceivedAfterUnsubscribe() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        SubscriptionToken token = hub.subscribe(HubChannel.STORAGE, event -> latch.countDown());
        hub.unsubscribe(token);

        hub.publish(HubChannel.STORAGE, HubEvent.create("Storage event"));
        assertFalse(
            latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        );
    }

    /**
     * Tests publishing, subscribing, receiving events, etc., from many threads.
     *
     * For each country musician, we'll make a thread that published a "(musician) is great" event,
     * and a thread that receives it, and counts down a latch, to acknowledge its receipt.
     * Then, we'll create *another* set of threads that await on the latches. When this awaiting
     * set of threads joins, the test ends successfully.
     *
     * @throws InterruptedException If a latch is interrupted
     */
    @Test
    public void multithreadedInteractions() throws InterruptedException {
        final Set<Thread> pubSubPool = new CopyOnWriteArraySet<>();
        final ConcurrentHashMap<Musician, CountDownLatch> receivedPublication = new ConcurrentHashMap<>();
        final Set<SubscriptionToken> tokens = new CopyOnWriteArraySet<>();

        for (Musician musician : Musician.values()) {
            // Add some publication threads
            pubSubPool.add(new Thread(() ->
                hub.publish(HubChannel.HUB, HubEvent.create(musician, musician + " is great!")))
            );
            // And some subscribe threads
            pubSubPool.add(new Thread(() -> {
                CountDownLatch latch = new CountDownLatch(1);
                SubscriptionToken token = hub.subscribe(
                    HubChannel.HUB,
                    event -> musician.equals(Musician.valueOf(event.getName())),
                    event -> latch.countDown()
                );
                receivedPublication.put(musician, latch);
                tokens.add(token);
            }));
        }
        // Start and then join them all, in parallel
        for (Thread thread : pubSubPool) {
            thread.start();
        }
        for (Thread thread : pubSubPool) {
            thread.join();
        }
        // Verify that all latches counted down (all musicians received)
        for (Musician musician : Musician.values()) {
            CountDownLatch latch = receivedPublication.get(musician);
            assertNotNull(latch);
            latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }
        for (SubscriptionToken token : tokens) {
            hub.unsubscribe(token);
        }
    }

    enum Musician {
        JON_PARDI,
        MEMPHIS_SLIM,
        CHARLEY_CROCKETT
    }
}
