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

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A consumer of a stream of {@link GraphQLResponse}, with the additional ability
 * to block the current thread until a given number of responses have been accepted.
 * @param <T> Type of data in the GraphQLResponses
 */
public final class LatchedResponseConsumer<T> implements Consumer<GraphQLResponse<T>> {
    private static final long DEFAULT_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(5);
    private final long waitTimeMs;
    private final List<GraphQLResponse<T>> acceptedValues;
    private CountDownLatch countDownLatch;

    private LatchedResponseConsumer(long waitTimeMs) {
        this.waitTimeMs = waitTimeMs;
        this.acceptedValues = new ArrayList<>();
        this.countDownLatch = null;
    }

    /**
     * Creates an instance of a LatchedResponseConsumer, using a default latch timeout.
     * @param <T> Type of data in response
     * @return A latched response consumer
     */
    @NonNull
    public static <T> LatchedResponseConsumer<T> instance() {
        return instance(DEFAULT_WAIT_TIME_MS);
    }

    /**
     * Creates an instance of a LatchedResponseConsumer, using a provided latch timeout.
     * @param waitTimeMs Duration of time to await values -- wait time is for all values as
     *                   a whole, not each, individually
     * @param <T> Type of data in responses
     * @return A latched response consumer using the provided latch timeout
     */
    @NonNull
    public static <T> LatchedResponseConsumer<T> instance(long waitTimeMs) {
        return new LatchedResponseConsumer<>(waitTimeMs);
    }

    @Override
    public void accept(@NonNull GraphQLResponse<T> value) {
        acceptedValues.add(value);
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
    }

    /**
     * Awaits the first `count`-many responses. Validates that these
     * responses do not contain errors, and do contain non-null data.
     * If so, returns these data in a list.
     * @param count Count of responses being awaited
     * @return List of data from the received responses
     * @throws RuntimeException If fewer than count responses are received,
     *                          or if those responses contain errors or null-data
     */
    @NonNull
    public List<T> awaitResponseData(int count) {
        awaitResponses(count);
        final List<T> values = new ArrayList<>();
        for (GraphQLResponse<T> receivedResponse : acceptedValues) {
            if (receivedResponse.hasErrors()) {
                throw new RuntimeException("Response had errors: " + receivedResponse.getErrors());
            } else if (receivedResponse.getData() == null) {
                throw new RuntimeException("Response had null data.");
            } else {
                values.add(receivedResponse.getData());
            }
        }
        return Immutable.of(values);
    }

    private void awaitResponses(int count) {
        if (countDownLatch == null) {
            countDownLatch = new CountDownLatch(count - acceptedValues.size());
        }
        try {
            countDownLatch.await(waitTimeMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interruptionWhileAwaitingValues) {
            // Checking in a second ...
        }
        if (countDownLatch.getCount() != 0) {
            throw new RuntimeException(String.format(
                "Did not receive the expected number of values. Wanted %d, got %d.",
                count, acceptedValues.size()
            ));
        }
    }

    /**
     * Awaits a first response and, if no errors are found in the response,
     * extracts and returned the non-null response data.
     * @return Non-null response data from the first response
     * @throws RuntimeException If the response has errors, doesn't show up, or data is null
     */
    @NonNull
    public T awaitResponseData() {
        return awaitResponseData(1).get(0);
    }

    /**
     * Awaits a first response, and if the response contains errors, returns them.
     * @return A list of errors found in the first response
     */
    @NonNull
    public List<GraphQLResponse.Error> awaitErrorsInNextResponse() {
        awaitResponses(1);
        GraphQLResponse<T> response = acceptedValues.get(0);
        if (!response.hasErrors()) {
            throw new RuntimeException("No errors found in response.");
        }
        return response.getErrors();
    }
}
