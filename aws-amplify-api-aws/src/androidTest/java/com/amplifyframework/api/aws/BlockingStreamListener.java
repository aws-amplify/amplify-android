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
 * Construct an instance of this {@link BlockingStreamListener} by providing the number
 * of expected items, in the call to {@link BlockingStreamListener#BlockingStreamListener(int)}.
 *
 * This listener can be useful when it is provided to the
 * {@link ApiCategoryBehavior#subscribe(String, GraphQLRequest, StreamListener)}
 * call, in a test.
 *
 * You can await results and completion by calling {@link BlockingStreamListener#awaitItems()}
 * and/or {@link BlockingStreamListener#awaitCompletion()}.
 *
 * @param <T> The type of data expected in the {@link GraphQLResponse}s.
 */
final class BlockingStreamListener<T> implements StreamListener<GraphQLResponse<T>> {
    private static final int RESULT_TIMEOUT = 5 /* seconds */; // 5 is chosen arbitrarily

    private final CountDownLatch allItemsLatch;
    private final CountDownLatch completedLatch;
    private final List<GraphQLResponse<T>> items;
    private Throwable error;

    BlockingStreamListener(int countOfItemsExpected) {
        this.allItemsLatch = new CountDownLatch(countOfItemsExpected);
        this.completedLatch = new CountDownLatch(1);
        this.items = new ArrayList<>();
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

    List<GraphQLResponse<T>> awaitItems() throws Throwable {
        if (!allItemsLatch.await(RESULT_TIMEOUT, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Items count down latch did not count down.");
        }
        if (error != null) {
            throw error;
        }
        return Immutable.of(items);
    }

    void awaitCompletion() throws Throwable {
        if (!completedLatch.await(RESULT_TIMEOUT, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Completion latch did not count down.");
        }
    }
}
