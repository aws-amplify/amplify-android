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

/**
 * A utility to combine a collection of {@link Consumer}s - for stream data, and
 * terminating completion/error events.
 *
 * A stream of zero or more items may be passed to the StreamListener
 * via the {@see #onNext(T item)} callback. After this,
 * at most one of the {@see #onComplete()} or {@see #onError(Throwable error)}
 * callbacks are invoked, but never both.
 *
 * See also the {@see ResultListener}, which expects a single result object
 * in its callbacks, as opposed to a stream of 0..n items.
 *
 * The StreamListener is modeled after the RxJava2 {@link io.reactivex.Observer}.
 *
 * @param <T> The type of item(s) that can be emitted via {@link #onNext(T)}
 * @param <E> The type of error(s) that can be emitted via {@link #onError(E)}.
 * @see <a href="http://reactivex.io/documentation/contract.html">The Rx Observable Contract</a>
 */
public final class StreamListener<T, E extends AmplifyException> {
    private final Consumer<T> itemConsumer;
    private final Consumer<E> errorConsumer;
    private final Action completionAction;

    private StreamListener(
            @NonNull Consumer<T> itemConsumer,
            @NonNull Consumer<E> errorConsumer,
            @NonNull Action completionAction) {
        this.itemConsumer = itemConsumer;
        this.errorConsumer = errorConsumer;
        this.completionAction = completionAction;
    }

    /**
     * Creates a StreamListener.
     * @param itemConsumer Consumer of stream items
     * @param errorConsumer Consumer of terminating errors
     * @param completionAction Action to perform on end of stream
     * @param <T> Type of items found in stream
     * @param <E> Type of error that terminates the stream
     * @return A stream listener
     */
    @NonNull
    public static <T, E extends AmplifyException> StreamListener<T, E> instance(
            @NonNull Consumer<T> itemConsumer,
            @NonNull Consumer<E> errorConsumer,
            @NonNull Action completionAction) {
        return new StreamListener<>(itemConsumer, errorConsumer, completionAction);
    }

    /**
     * Called zero or more times, once for each new item
     * that appears on the stream of items. This callback
     * will be invoked before {@see #onComplete()} or
     * {@see #onError(Throwable error)} may be called.
     * @param item Next item in the stream
    */
    public void onNext(@NonNull T item) {
        itemConsumer.accept(item);
    }

    /**
     * Called when the stream has completed emitting items.
     * No other callback will be received once this is invoked.
     */
    public void onComplete() {
        completionAction.call();
    }

    /**
     * Called when an error is encountered while processing
     * an item stream. No other callback will be received once
     * this is invoked.
     * @param error An error encountered while evaluating a stream
     */
    public void onError(@NonNull E error) {
        errorConsumer.accept(error);
    }
}

