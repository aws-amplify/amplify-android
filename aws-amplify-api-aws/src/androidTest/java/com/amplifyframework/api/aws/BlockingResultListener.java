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

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.ResultListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of an {@link ResultListener} which can also await for a response,
 * kind of like resolving a promise. Provide an instance of this {@link BlockingResultListener}
 * to an API call, and then wait for a response by calling {@link BlockingResultListener#awaitResult()}.
 * @param <T> The type of data in the {@link GraphQLResponse}.
 */
final class BlockingResultListener<T> implements ResultListener<GraphQLResponse<T>> {
    private static final int DEFAULT_RESULT_TIMEOUT = 5 /* seconds */; // 5 is chosen arbitrarily

    private final CountDownLatch latch;
    private Throwable error;
    private GraphQLResponse<T> response;

    BlockingResultListener() {
        this.latch = new CountDownLatch(1);
    }

    @Override
    public void onResult(final GraphQLResponse<T> response) {
        this.response = response;
        latch.countDown();
    }

    @Override
    public void onError(final Throwable error) {
        this.error = error;
        latch.countDown();
    }

    GraphQLResponse<T> awaitResult() throws Throwable {
        if (!latch.await(DEFAULT_RESULT_TIMEOUT, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Latch never counted down.");
        }
        if (error != null) {
            throw error;
        }
        return response;
    }
}
