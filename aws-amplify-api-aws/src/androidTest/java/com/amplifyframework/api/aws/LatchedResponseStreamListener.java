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

package com.amplifyframework.api.aws;

import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Immutable;
import com.amplifyframework.core.StreamListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of the {@link StreamListener} which can also await a number
 * of responses, and await a completion callback, sort of like resolving a promise.
 *
 * Construct an instance of this {@link LatchedResponseStreamListener} by providing the number
 * of expected items, in the call to {@link #LatchedResponseStreamListener(int)}.
 *
 * This listener can be useful when it is provided to the
 * {@link ApiCategoryBehavior#subscribe(String, GraphQLRequest, StreamListener)}
 * call, in a test.
 *
 * You can await results and completion by calling {@link #awaitItems()}
 * and/or {@link #awaitCompletion()}.
 *
 * @param <T> The type of data expected in the {@link GraphQLResponse}s.
 */
public final class LatchedResponseStreamListener<T> implements StreamListener<GraphQLResponse<T>> {
    private static final int RESULT_TIMEOUT_MS = 5_000 /* ms */; // 5 is chosen arbitrarily

    private final CountDownLatch allItemsLatch;
    private final CountDownLatch completedLatch;
    private final List<GraphQLResponse<T>> items;
    private final long latchTimeoutMs;
    private Throwable error;

    /**
     * Constructs a new LatchedResponseStreamListener,
     * which will block until a certain number of responses are received.
     * The latch will wait for 5 seconds, by default.
     * You can configure/override that value by using {@link #LatchedResponseStreamListener(int, long)}.
     * @param countOfItemsExpected The number of responses that are expected before timeout
     */
    public LatchedResponseStreamListener(int countOfItemsExpected) {
        this(countOfItemsExpected, RESULT_TIMEOUT_MS);
    }

    /**
     * Constructs a new LatchedResposneStreamListener,
     * which will block until a certain number of responses are received.
     * The latch will wait for the provided number of milliseconds.
     * @param countOfItemsExpected Number of responses expected within timeout window
     * @param latchTimeoutMs The amount of time to await the responses
     */
    public LatchedResponseStreamListener(int countOfItemsExpected, long latchTimeoutMs) {
        this.allItemsLatch = new CountDownLatch(countOfItemsExpected);
        this.completedLatch = new CountDownLatch(1);
        this.items = new ArrayList<>();
        this.latchTimeoutMs = latchTimeoutMs;
        this.error = null;
    }

    @Override
    public void onNext(final GraphQLResponse<T> item) {
        items.add(item);
        allItemsLatch.countDown();
    }

    @Override
    public void onComplete() {
        completedLatch.countDown();
    }

    @Override
    public void onError(final Throwable error) {
        this.error = error;
        while (0 != allItemsLatch.getCount()) {
            allItemsLatch.countDown();
        }
    }

    /**
     * Waits for all responses to arrive. The number of responses that will be
     * expected are the number provided at {@link #LatchedResponseStreamListener(int)}.
     * @return A list of all responses received, if at least as many were received as requested
     * @throws Throwable If fewer than the expected number of responses were obtained
     */
    @NonNull // Possibly empty, though
    public List<GraphQLResponse<T>> awaitItems() throws Throwable {
        if (!allItemsLatch.await(latchTimeoutMs, TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException("Items count down latch did not count down.");
        }
        if (error != null) {
            throw error;
        }
        return Immutable.of(items);
    }

    /**
     * Waits for the {@link #onComplete()} callback to be invoked.
     * @throws Throwable If {@link #onComplete()} is not invoked before the timeout.
     */
    public void awaitCompletion() throws Throwable {
        if (!completedLatch.await(latchTimeoutMs, TimeUnit.MICROSECONDS)) {
            throw new IllegalStateException("Completion latch did not count down.");
        }
    }
}
