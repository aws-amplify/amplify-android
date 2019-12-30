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

package com.amplifyframework.core;

import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;

import java.util.Objects;

/**
 * A ResultListener is used internally to combine two {@link Consumer} into a single
 * collection of consumers: one for a result object, and one for an thrown error.
 *
 * A listener which will be notified of a single result, or alternately,
 * a single error, if a result could not be obtained.  Exactly one of
 * {@see #onResult(R result)} or {@see onError(Throwable error)} is
 * expected to be called.
 *
 * See also the {@see StreamListener}, which expects to receive 0..n items
 * in its callback(s), instead of just a single result.
 *
 * An {@link ResultListener} is modeled after an RxJava2 {@link io.reactivex.SingleObserver}.
 *
 * @param <R> The type of result being returned to the listener
 * @param <E> The type of error expected instead of a result
 */
public final class ResultListener<R, E extends AmplifyException> {
    private final Consumer<R> resultConsumer;
    private final Consumer<E> errorConsumer;

    private ResultListener(
            @NonNull final Consumer<R> resultConsumer,
            @NonNull final Consumer<E> errorConsumer) {
        this.resultConsumer = resultConsumer;
        this.errorConsumer = errorConsumer;
    }

    /**
     * Creates a ResultListener.
     * @param resultConsumer Consumer of result
     * @param errorConsumer Consumer of error
     * @param <R> The result type
     * @param <E> The expected type of error
     * @return A result listener
     */
    @NonNull
    public static <R, E extends AmplifyException> ResultListener<R, E> instance(
            @NonNull final Consumer<R> resultConsumer,
            @NonNull final Consumer<E> errorConsumer) {
        return new ResultListener<>(
            Objects.requireNonNull(resultConsumer),
            Objects.requireNonNull(errorConsumer)
        );
    }

    /**
     * Called back when a result is available.
     * @param result A result object
     */
    public void onResult(@NonNull R result) {
        resultConsumer.accept(result);
    }

    /**
     * Called if a result cannot be obtained, because an
     * error has occurred.
     * @param error An error that prevents determination of a result.
     */
    public void onError(@NonNull E error) {
        errorConsumer.accept(error);
    }
}
