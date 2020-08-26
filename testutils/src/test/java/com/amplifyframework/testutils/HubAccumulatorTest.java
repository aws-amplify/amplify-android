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

import com.amplifyframework.core.Amplify;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.core.Observable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link HubAccumulator} test utility.
 * (A test for a test? Ya, bud.)
 */
public final class HubAccumulatorTest {
    /**
     * The main point of the {@link HubAccumulator} is to capture values that arrive
     * after the accumulator is started. Does it work?
     */
    @Test
    public void accumulatorReceivesValuesArrivingAfterCreation() {
        // Accumulate Apple events, only.
        HubAccumulator fruitCatcher =
            HubAccumulator.create(HubChannel.HUB, Fruit.APPLE, 2);
        fruitCatcher.start();

        // Post some miscellaneous fruit events.
        publish(Fruit.APPLE, "Apple 1");
        publish(Fruit.BANANA, "Banana 2");
        publish(Fruit.KIWI, "Kiwi 3");
        publish(Fruit.APPLE, "Apple 6");

        // Await the values.
        List<HubEvent<?>> apples = fruitCatcher.await();
        for (HubEvent<?> event : apples) {
            assertEquals(Fruit.APPLE, Fruit.valueOf(event.getName()));
        }
    }

    /**
     * If the accumulator fails to attain the requested number of values, then it
     * will time out.
     */
    @Test
    public void accumulatorTimesOutWhenNotEnoughValues() {
        final int desiredAmount = 3;
        HubAccumulator accumulator = HubAccumulator.create(HubChannel.HUB, desiredAmount);
        publish(Fruit.BANANA, "Bananas are so boss!");
        RuntimeException timeoutError = assertThrows(RuntimeException.class, accumulator::await);
        assertNotNull(timeoutError.getMessage());
        assertTrue(timeoutError.getMessage().contains("Failed to count down"));
    }

    /**
     * It's okay to call {@link HubAccumulator#await()} a bunch of times.
     * @throws InterruptedException On failure to join watching threads
     */
    @Test
    public void multithreadedAccumulation() throws InterruptedException {
        // Accumulate ONE FEWER that we will publish
        final int fruitCount = Fruit.values().length;
        HubAccumulator accumulator = HubAccumulator.create(HubChannel.HUB, fruitCount - 1);
        accumulator.start();

        // Create three threads that await values, and three that publish values.
        final Set<Thread> threadPool = new HashSet<>();
        for (Fruit fruit : Fruit.values()) {
            threadPool.add(new Thread(accumulator::await));
            threadPool.add(new Thread(() -> publish(fruit, fruit.toString() + " is the best.")));
        }

        // Start all threads in parallel, then join all threads in parallel.
        for (Thread thread : threadPool) {
            thread.start();
        }
        for (Thread thread : threadPool) {
            thread.join();
        }

        // We accumulated (fruitCount - 1) values, even though fruitCount were published.
        // Since this happened in no particular order, figure out which two we actually got?
        Set<Fruit> unseenFruits = new HashSet<>(Arrays.asList(Fruit.values()));
        Observable.fromIterable(accumulator.await())
            .map(event -> Fruit.valueOf(event.getName()))
            .blockingForEach(unseenFruits::remove);
        assertEquals(1, unseenFruits.size());
    }

    @SuppressWarnings("SameParameterValue")
    private <E extends Enum<E>, D> void publish(E eumName, D data) {
        Amplify.Hub.publish(HubChannel.HUB, HubEvent.create(eumName, data));
    }

    enum Fruit {
        APPLE,
        BANANA,
        KIWI
    }
}
