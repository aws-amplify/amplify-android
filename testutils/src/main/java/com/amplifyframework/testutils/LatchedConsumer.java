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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A consumer which counts down a latch when a value is accepted.
 * You can call {@link #awaitValue()} to block execution on the calling thread,
 * until a value has been consumed.
 * @param <T> Type of value being consumed
 */
public final class LatchedConsumer<T> implements Consumer<T> {
    private static final long DEFAULT_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(5);

    private final CountDownLatch latch;
    private final long waitTimeMs;
    private final AtomicReference<T> valueContainer;

    private LatchedConsumer(long waitTimeMs) {
        this.latch = new CountDownLatch(1);
        this.waitTimeMs = waitTimeMs;
        this.valueContainer = new AtomicReference<>();
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
        valueContainer.set(value);
        latch.countDown();
    }

    /**
     * Wait until the consumer has accepted a value.
     * @return The accepted value
     * @throws RuntimeException if no value was accepted
     */
    @NonNull
    public T awaitValue() throws RuntimeException {
        try {
            latch.await(waitTimeMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interruptedException) {
            // One sec ...
        }

        if (latch.getCount() != 0) {
            throw new RuntimeException("Result consumer latch did not count down.");
        }

        return valueContainer.get();
    }
}
