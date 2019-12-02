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

import android.util.Log;
import androidx.annotation.NonNull;

import com.amplifyframework.core.ResultListener;

import org.junit.Assert;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An implementation of a {@link ResultListener} which can also await
 * rececipt of a result/error, kind of like resolving a promise. Provide
 * an instance of this {@link LatchedResultListener} to an asynchronous
 * method invocation, and then wait for a result by calling
 * {@link LatchedResultListener#awaitTerminalEvent()}.
 * @param <T> The type of the result data
 */
public final class LatchedResultListener<T> implements ResultListener<T> {
    private static final long REASONABLE_WAIT_TIME_MS = 500 /* ms */;

    private final AtomicReference<T> resultReference;
    private final AtomicReference<Throwable> errorReference;
    private final CountDownLatch completionsPending;
    private final long waitTimeMs;

    /**
     * Constructs a new LatchedResultListener with a provided latch timeout.
     * If you don't want to choose a latch timeout, prefer {@link #instance()}.
     * @param waitTimeMs Latch will timeout after this many milliseconds
     */
    private LatchedResultListener(long waitTimeMs) {
        this.waitTimeMs = waitTimeMs;
        this.resultReference = new AtomicReference<>();
        this.errorReference = new AtomicReference<>();
        this.completionsPending = new CountDownLatch(1);
    }


    /**
     * Creates an instance of {@link LatchedResultListener} that awaits
     * reciept of a result or error, until the provided timeout (in
     * milliseconds) has elapsed.
     * @param milliseconds Time to wait for a result, in milliseconds
     * @param <T> Type of result being awaited
     * @return A LatchedResultListener configured to wait for the provided number of milliseconds
     */
    @NonNull
    public static <T> LatchedResultListener<T> waitFor(long milliseconds) {
        return new LatchedResultListener<>(milliseconds);
    }

    /**
     * Creates an instance of {@link LatchedResultListener} that awaits
     * reciept of a result or error, until a default timeout of 500s has
     * elapsed.
     * @param <T> The type of result being waited
     * @return An instance of a LatchedResultListener, configure to await a result
     *         for a default waiting time.
     */
    @NonNull
    public static <T> LatchedResultListener<T> instance() {
        return new LatchedResultListener<>(REASONABLE_WAIT_TIME_MS);
    }

    @Override
    public void onResult(@NonNull T result) {
        resultReference.set(result);
        completionsPending.countDown();
    }

    @Override
    public void onError(@NonNull Throwable error) {
        errorReference.set(error);
        completionsPending.countDown();
    }

    /**
     * Awaits a terminal event, either a result or an error.
     * @return Current instance of the {@link LatchedResultListener}, for
     *         fluent method chaining
     */
    @NonNull
    public LatchedResultListener<T> awaitTerminalEvent() {
        boolean didCountDown = false;
        try {
            didCountDown = completionsPending.await(waitTimeMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interruptedException) {
            Assert.fail(Log.getStackTraceString(interruptedException));
        }
        Assert.assertTrue("Listener did not count down...", didCountDown);

        return this;
    }

    /**
     * Awaits a successful result.
     * If the listener did not get a result, and/or if it got an error,
     * this will throw an {@link AssertionError}.
     * @return The result value
     */
    @NonNull
    public T awaitResult() {
        awaitTerminalEvent();
        final Throwable error = errorReference.get();
        Assert.assertNull("Listener got error: " + Log.getStackTraceString(error), error);
        T result = resultReference.get();
        Assert.assertNotNull("Listener has no result data.", result);
        return result;
    }

    /**
     * Awaits receipt of an error.
     * @return The error received by the listener.
     */
    @NonNull
    public Throwable awaitError() {
        awaitTerminalEvent();
        final T data = resultReference.get();
        Assert.assertNull("Got data, but expected error: " + data, data);
        final Throwable error = errorReference.get();
        Assert.assertNotNull("Wanted error, but it was null.", error);
        return error;
    }
}
