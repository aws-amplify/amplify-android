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

package com.amplifyframework.testutils;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A consumer which counts down a latch when a value is accepted.
 * You can call {@link #awaitValue()} to block execution on the calling thread,
 * until a value has been consumed.
 * @param <T> Type of value being consumed
 */
public final class LatchedConsumer<T> implements Consumer<T> {
    private static final long DEFAULT_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(5);

    private final long waitTimeMs;
    private final List<T> values;
    private CountDownLatch latch;

    private LatchedConsumer(long waitTimeMs) {
        this.waitTimeMs = waitTimeMs;
        this.values = new ArrayList<>();
        this.latch = null;
    }

    /**
     * Creates an latched consumer instance, using a default latch timeout.
     * @param <T> Type of value being consumed
     * @return A latched consumer
     */
    @NonNull
    public static <T> LatchedConsumer<T> instance() {
        return new LatchedConsumer<>(DEFAULT_WAIT_TIME_MS);
    }

    /**
     * Creates a latched consumer instance, using a provided latch timeout.
     * @param waitTimeMs Amount of time to wait for a value when calling {@link #awaitValue()}
     * @param <T> Type of value being awaited
     * @return A latched consumer
     */
    @NonNull
    public static <T> LatchedConsumer<T> instance(long waitTimeMs) {
        return new LatchedConsumer<>(waitTimeMs);
    }

    @Override
    public void accept(@NonNull T value) {
        values.add(value);
        if (latch != null) {
            latch.countDown();
        }
    }

    /**
     * Wait until the consumer has accepted a value.
     * @return The accepted value
     * @throws RuntimeException if no value was accepted
     */
    @NonNull
    public T awaitValue() throws RuntimeException {
        return awaitValues(1).get(0);
    }

    /**
     * Await the given number of values to arrive.
     * @param count Count of values to await
     * @return The count-many values
     */
    @NonNull
    public List<T> awaitValues(int count) {
        if (latch == null) {
            latch = new CountDownLatch(count - values.size());
        }

        try {
            latch.await(waitTimeMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interruptedException) {
            // One sec ...
        }

        if (latch.getCount() != 0) {
            throw new RuntimeException("Result consumer latch did not count down.");
        }

        return Immutable.of(values);
    }
}
