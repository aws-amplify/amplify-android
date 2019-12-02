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

import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.ResultListener;

import org.junit.Assert;

import java.util.List;

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
    public void onResult(@NonNull GraphQLResponse<T> result) {
        latchedResultListener.onResult(result);
    }

    @Override
    public void onError(@NonNull Throwable error) {
        latchedResultListener.onError(error);
    }

    /**
     * Awaits a terminal event - either an error, or a response.
     * @return Current instance of {@link LatchedSingleResponseListener}, for fluent chaining
     */
    private LatchedSingleResponseListener<T> awaitTerminalEvent() {
        latchedResultListener.awaitTerminalEvent();
        return this;
    }

    /**
     * Awaits a GraphQLResponse, and then validates that that response
     * had no GraphQLReponse.Error(s), and did contain non-null data.
     * @return The non-null data in a successful GraphQLResponse
     * @throws AssertionError In all other circumstances
     */
    @NonNull
    public T awaitSuccessResponse() {
        final GraphQLResponse<T> response = latchedResultListener.awaitResult();
        Assert.assertFalse(response.getErrors().toString(), response.hasErrors());
        Assert.assertNotNull("No data in GraphQLResponse", response.getData());
        return response.getData();
    }

    /**
     * Await a GraphQLResponse which contains (a) error(s).
     * @return The errors
     */
    @NonNull
    public List<GraphQLResponse.Error> awaitErrors() {
        final GraphQLResponse<T> response = latchedResultListener.awaitResult();
        Assert.assertTrue("Expected errors, but response had none.", response.hasErrors());
        return response.getErrors();
    }
}
