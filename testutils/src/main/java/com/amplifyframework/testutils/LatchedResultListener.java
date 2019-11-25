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
    public static <T> LatchedResultListener<T> instance() {
        return new LatchedResultListener<>(REASONABLE_WAIT_TIME_MS);
    }

    @Override
    public void onResult(T result) {
        resultReference.set(result);
        completionsPending.countDown();
    }

    @Override
    public void onError(Throwable error) {
        errorReference.set(error);
        completionsPending.countDown();
    }

    /**
     * Await a terminal event in the listener. A terminal event
     * is either an invocation of {@link ResultListener#onError(Throwable)},
     * or of {@link ResultListener#onResult(Object)}, whichever shall occur
     * first.
     * @return The current instance of the {@link LatchedResultListener},
     *         for the utility of fluent method chaining
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

        return LatchedResultListener.this;
    }

    /**
     * Assert that no errors have been received by the listener.
     * This should be called only after {@link #awaitTerminalEvent()};
     * It is a usage error to call this before calling {@link #awaitTerminalEvent()}.
     * @return The current instance of the {@link LatchedResultListener},
     *         for the utility of fluent method chaining
     */
    @NonNull
    public LatchedResultListener<T> assertNoError() {
        final Throwable error = errorReference.get();
        Assert.assertNull("Had error: " + Log.getStackTraceString(error), error);
        return LatchedResultListener.this;
    }

    /**
     * Asserts that an error was received by the listener.
     * This should be called only after {@link #awaitTerminalEvent()}.
     * It is a usage error to call this before calling {@link #awaitTerminalEvent()}.
     * @return The current instance of the {@link LatchedResultListener},
     *         for the purpose of fluent method chaining
     */
    public LatchedResultListener<T> assertError() {
        Assert.assertTrue("Expected an error, but had none.", hasError());
        return LatchedResultListener.this;
    }

    /**
     * Asserts that the listener has received a non-null result.
     * @return True if the listener has received a non-null result; false, otherwise
     */
    @NonNull
    public LatchedResultListener<T> assertResult() {
        final T result = resultReference.get();
        Assert.assertNotNull("Expected a result, but had null.", result);
        return LatchedResultListener.this;
    }

    /**
     * Gets the value of the of the result, as received int the {@link ResultListener#onResult(Object)}
     * callback. It is a usage error to call this method before {@link #awaitTerminalEvent()} has been called.
     * @return The value which the listener received in {@link ResultListener#onResult(Object)}.
     */
    @NonNull
    public T getResult() {
        Assert.assertTrue(hasResult());
        return resultReference.get();
    }

    /**
     * Checks if the listener has received a result.
     * It is a usage error to call this before first calling {@link #awaitTerminalEvent()}.
     * @return True if the listener has received a result; false, otherwise
     */
    public boolean hasResult() {
        return resultReference.get() != null;
    }

    /**
     * Gets the value of the error that was obtained via {@link #onError(Throwable)},
     * if an error was received, there.
     * It is a usage error to call this before {@link #awaitTerminalEvent()}.
     * @return The throwable that had been obtained via the onError(Throwable) callback.
     */
    @NonNull
    public Throwable getError() {
        Assert.assertTrue(hasError());
        return errorReference.get();
    }

    /**
     * Checks if the listener has received an error.
     * It is a usage error to call this before first calling {@link #awaitTerminalEvent()}.
     * @return true if the listener has received an error, false otherwise
     */
    public boolean hasError() {
        return errorReference.get() != null;
    }
}
