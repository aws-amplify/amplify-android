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

/**
 * A StreamListener behaves similarly to the Rx Observer.
 * A stream of zero or more items may be passed to the StreamListener
 * via the {@see #onNext(T item)} callback. After this,
 * at most one of the {@see #onComplete()} or {@see #onError(Throwable error)}
 * callbacks are invoked, but never both.
 *
 * See also the {@see ResultListener}, which expects a single result object
 * in its callbacks, as opposed to a stream of 0..n items.
 * @param <T> A common type for the item(s) found in the stream
 * @see <a href="http://reactivex.io/documentation/contract.html">The Rx Observable Contract</a>
 */
public interface StreamListener<T> {

    /**
     * Called zero or more times, once for each new item
     * that appears on the stream of items. This callback
     * will be invoked before {@see #onComplete()} or
     * {@see #onError(Throwable error)} may be called.
     * @param item Next item in the stream
    */
    void onNext(T item);

    /**
     * Called when the stream has completed emitting items.
     * No other callback will be received once this is invoked.
     */
    void onComplete();

    /**
     * Called when an error is encountered while processing
     * an item stream. No other callback will be received once
     * this is invoked.
     * @param error An error encountered while evaluating a stream
     */
    void onError(Throwable error);
}
