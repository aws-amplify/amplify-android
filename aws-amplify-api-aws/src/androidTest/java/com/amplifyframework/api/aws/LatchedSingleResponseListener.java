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
import androidx.annotation.Nullable;

import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.testutils.LatchedResultListener;

/**
 * An {@link ResultListener} that can await results.
 * This implementation expected the result type to be the {@link GraphQLResponse} as
 * found in the template argument in several callbacks of the {@link ApiCategoryBehavior}.
 * @param <T> The type of data contained in the response object.
 */
public final class LatchedSingleResponseListener<T> implements ResultListener<GraphQLResponse<T>> {
    private static final long DEFAULT_TIMEOUT_MS = 5_000 /* ms */;

    private final LatchedResultListener<GraphQLResponse<T>> latchedResultListener;

    /**
     * Constructs a new LatchedSingleResponseListener with a provided latch timeout.
     * @param waitTimeMs Latch will timeout after this many milliseconds
     */
    public LatchedSingleResponseListener(long waitTimeMs) {
        this.latchedResultListener = LatchedResultListener.waitFor(waitTimeMs);
    }

    /**
     * Constructs a new LatchedSingleResponseListener using a default latch timeout of 5 seconds.
     */
    public LatchedSingleResponseListener() {
        this(DEFAULT_TIMEOUT_MS);
    }

    @Override
    public void onResult(GraphQLResponse<T> result) {
        latchedResultListener.onResult(result);
    }

    @Override
    public void onError(Throwable error) {
        latchedResultListener.onError(error);
    }

    /**
     * Await a terminal event, either {@link #onResult(GraphQLResponse)} is called,
     * or {@link #onError(Throwable)} is called.
     * @return Current {@link LatchedSingleResponseListener} instance for fluent call chaining
     */
    @NonNull
    public LatchedSingleResponseListener<T> awaitTerminalEvent() {
        latchedResultListener.awaitTerminalEvent();
        return this;
    }

    /**
     * Assert that no error was observed at {@link #onError(Throwable)}.
     * It is a usage error to call this method before calling {@link #awaitTerminalEvent()}.
     * @return Current {@link LatchedSingleResponseListener} instance for fluent call chaining
     */
    @NonNull
    public LatchedSingleResponseListener<T> assertNoError() {
        latchedResultListener.assertNoError();
        return this;
    }

    /**
     * Asserts that this listener received a response.
     * It is an usage error to call this before calling {@link #awaitTerminalEvent()}.
     * @return Current {@link LatchedSingleResponseListener} instance for fluent call chaining
     */
    @NonNull
    public LatchedSingleResponseListener<T> assertResponse() {
        latchedResultListener.assertResult();
        return this;
    }

    /**
     * Gets the value of the response, if present, as it had been received in
     * {@link #onResult(GraphQLResponse)}.
     * It is a usage error to call this before {@link #awaitTerminalEvent()}.
     * @return the value of the response that was received in the result callback.
     */
    @NonNull
    public GraphQLResponse<T> getResponse() {
        return latchedResultListener.getResult();
    }

    /**
     * Gets the error that had been received in {@link #onError(Throwable)}.
     * It is a usage error to call this before calling {@link #awaitTerminalEvent()}.
     * @return The error that had been received in the error callback.
     */
    @Nullable
    public Throwable getError() {
        return latchedResultListener.getError();
    }
}
