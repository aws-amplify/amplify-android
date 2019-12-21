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

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.ResultListener;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link LatchedResponseConsumer} test-utility.
 * (A test for a test (util)? Wow.)
 */
public final class LatchedResponseConsumerTest {
    private static final long TEST_OP_TIMEOUT_MS = 500L;

    /**
     * When the response consumer is used as a back-end to an {@link ResultListener},
     * a result value is proxied through to the consumer and may be awaited, from it.
     */
    @Test
    public void valuesAreConsumedThroughResultListener() {
        LatchedResponseConsumer<String> resultConsumer = LatchedResponseConsumer.instance();
        ResultListener<GraphQLResponse<String>> resultListener =
            ResultListener.instance(resultConsumer, EmptyConsumer.of(Throwable.class));

        String expectedResponseData = "Hello, World.";
        resultListener.onResult(new GraphQLResponse<>(expectedResponseData, Collections.emptyList()));

        assertEquals(expectedResponseData, resultConsumer.awaitResponseData());
    }

    /**
     * When the consumer has accepted a bunch of values, and then we await their receipt,
     * we should get back all of the same values.
     */
    @Test
    public void awaitItemsAfterAlreadyKnownToBeAccepted() {
        final LatchedResponseConsumer<String> latchedResponseConsumer =
            LatchedResponseConsumer.instance(TEST_OP_TIMEOUT_MS);

        acceptValues(latchedResponseConsumer, ExpectedData.responses());

        // Act: await some items.
        List<String> responseDataItems =
            latchedResponseConsumer.awaitResponseData(ExpectedData.responses().size());

        // Assert: returned data are the the items from the accepted responses
        assertEquals(ExpectedData.items(), responseDataItems);
    }

    /**
     * If we await values before the consumer has accepted any value, then
     * the thread should block until they do arrive _after_ that call.
     */
    @Test
    public void awaitItemsCalledBeforeConsumerHasAcceptedAnyValue() {
        final LatchedResponseConsumer<String> latchedResponseConsumer =
            LatchedResponseConsumer.instance(TEST_OP_TIMEOUT_MS);

        final CountDownLatch awaitStarted = new CountDownLatch(1);
        final AtomicReference<List<String>> receivedValues = new AtomicReference<>();
        final CountDownLatch awaitCompleted = new CountDownLatch(1);
        final Thread awaitingValuesThread = new Thread(() -> {
            awaitStarted.countDown();
            receivedValues.set(latchedResponseConsumer.awaitResponseData(ExpectedData.responses().size()));
            awaitCompleted.countDown();
        });
        awaitingValuesThread.start();
        awaitMs(awaitStarted, TEST_OP_TIMEOUT_MS);

        acceptValues(latchedResponseConsumer, ExpectedData.responses());

        awaitMs(awaitCompleted, TEST_OP_TIMEOUT_MS);

        assertEquals(ExpectedData.items(), receivedValues.get());
    }

    private <T> void acceptValues(Consumer<T> consumer, Collection<T> values) {
        final CountDownLatch allProvided = new CountDownLatch(1);
        final Thread valueAcceptingThread = new Thread(() -> {
            for (T value : values) {
                consumer.accept(value);
            }
            allProvided.countDown();
        });
        // Okay, now actually accept the values.
        valueAcceptingThread.start();
        awaitMs(allProvided, TEST_OP_TIMEOUT_MS);
        assertEquals(0, allProvided.getCount());
    }

    @SuppressWarnings("SameParameterValue")
    private static void awaitMs(final CountDownLatch latch, long waitTimeMs) {
        try {
            latch.await(waitTimeMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException awaitInterruptedException) {
            // About to check, one sec ....
        } finally {
            assertEquals(0, latch.getCount());
        }
    }

    /**
     * A class to bundle together some expected values, as List of String,
     * and as List of GraphQLResponse of String.
     */
    static final class ExpectedData {
        @SuppressWarnings("checkstyle:all") private ExpectedData() {}

        static List<String> items() {
            return Collections.unmodifiableList(Arrays.asList("Hola", "Mundo"));
        }

        static List<GraphQLResponse<String>> responses() {
            final List<GraphQLResponse<String>> responses = new ArrayList<>();
            for (String expectedItem : items()) {
                responses.add(new GraphQLResponse<>(expectedItem, Collections.emptyList()));
            }
            return responses;
        }
    }
}
