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

import com.amplifyframework.core.Consumer;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A utility to await a value from an async function, in a synchronous way.
 */
public final class Await {

    private Await() {}

    /**
     * Await emission of either a result or an error.
     * Blocks the thread of execution until either the value is available,
     * or the timeout is reached.
     * @param <R> Type of result
     * @param <E> Type of error
     * @param resultErrorEmitter A function which emits result or error
     * @return The result
     * @throws E if error is emitted
     * @throws RuntimeException In all other situations where there is not a non-null result
     */
    @NonNull
    public static <R, E extends Throwable> R result(
        @NonNull ResultErrorEmitter<R, E> resultErrorEmitter) throws E {

        Objects.requireNonNull(resultErrorEmitter);

        AtomicReference<R> resultContainer = new AtomicReference<>();
        AtomicReference<E> errorContainer = new AtomicReference<>();

        await(resultErrorEmitter, resultContainer, errorContainer);

        R result = resultContainer.get();
        E error = errorContainer.get();
        if (error != null) {
            throw error;
        } else if (result != null) {
            return result;
        }

        throw new IllegalStateException("Latch counted down, but where's the value?");
    }

    /**
     * Awaits receipt of an error on an error callback.
     * Blocks the calling thread until it shows up, or until timeout elapses.
     * @param <R> Type of result
     * @param <E> Type of error
     * @param resultErrorEmitter A function which emits result or error
     * @return Error, if attained
     * @throws RuntimeException If no error is emitted by the emitter
     */
    @NonNull
    public static <R, E extends Throwable> E error(
        @NonNull ResultErrorEmitter<R, E> resultErrorEmitter) {

        Objects.requireNonNull(resultErrorEmitter);

        AtomicReference<R> resultContainer = new AtomicReference<>();
        AtomicReference<E> errorContainer = new AtomicReference<>();

        await(resultErrorEmitter, resultContainer, errorContainer);

        R result = resultContainer.get();
        E error = errorContainer.get();
        if (result != null) {
            throw new RuntimeException("Expected error, but had result = " + result);
        } else if (error != null) {
            return error;
        }

        throw new RuntimeException("Neither error nor result consumers accepted a value.");
    }

    private static <R, E extends Throwable> void await(
        @NonNull ResultErrorEmitter<R, E> resultErrorEmitter,
        @NonNull AtomicReference<R> resultContainer,
        @NonNull AtomicReference<E> errorContainer) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> unexpectedErrorContainer = new AtomicReference<>();
        final Thread thread = new Thread(() -> {
            try {
                resultErrorEmitter.emitTo(
                    result -> {
                        if (resultContainer.get() != null) {
                            throw new RuntimeException("Result callback called more than once with result: " + result);
                        }
                        resultContainer.set(result);
                        latch.countDown();
                    }, error -> {
                        if (errorContainer.get() != null) {
                            throw new RuntimeException("Error callback called more than once with error: " + error);
                        }
                        errorContainer.set(error);
                        latch.countDown();
                    }
                );
            } catch (Throwable unexpectedFailure) {
                unexpectedErrorContainer.set(unexpectedFailure);
                latch.countDown();
            }
        });
        thread.setDaemon(true);
        thread.start();

        Latch.await(latch);

        try {
            thread.join();
        } catch (InterruptedException threadJoinFailure) {
            throw new RuntimeException("Failed to join thread.", threadJoinFailure);
        }

        if (unexpectedErrorContainer.get() != null) {
            throw new RuntimeException("Unhandled error: " + unexpectedErrorContainer.get());
        }
    }

    /**
     * A function which, upon completion, either emits a single result,
     * or emits an error.
     * @param <R> Type of result
     * @param <E> Type of error
     */
    public interface ResultErrorEmitter<R, E extends Throwable> {
        /**
         * A function that emits a value upon completion, either as a
         * result or as an error.
         * @param onResult Callback invoked upon emission of result
         * @param onError Callback invoked upon emission of error
         */
        void emitTo(@NonNull Consumer<R> onResult, @NonNull Consumer<E> onError);
    }
}

