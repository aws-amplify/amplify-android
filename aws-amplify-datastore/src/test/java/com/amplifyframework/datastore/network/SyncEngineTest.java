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

package com.amplifyframework.datastore.network;

import android.os.Build;

import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.datastore.MutationEvent;
import com.amplifyframework.datastore.RandomString;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests the {@link SyncEngine}.
 */
@Config(sdk = Build.VERSION_CODES.P, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class SyncEngineTest {
    // A "reasonable" amount of time our test(s) will wait for async operations to complete
    private static final int OPERATIONS_TIMEOUT_MS = 100;

    private ApiCategoryBehavior api;
    private String apiName;
    private LocalStorageAdapter localStorageAdapter;
    private SyncEngine syncEngine;

    /**
     * Configures the object under test, an {@link SyncEngine}.
     */
    @Before
    public void setup() {
        api = mock(ApiCategoryBehavior.class);
        apiName = RandomString.string();
        localStorageAdapter = InMemoryStorageAdapter.create();
        final OutgoingMutationsQueue outgoingMutationsQueue = new OutgoingMutationsQueue(localStorageAdapter);
        syncEngine = new SyncEngine(api, apiName, localStorageAdapter, outgoingMutationsQueue);
    }

    /**
     * When an item is placed into the storage adapter, a cascade
     * of things happen which should ultimately result in a mutation call to the
     * API category, with type {@link MutationEvent.MutationType#INSERT}.
     * @throws InterruptedException If our own mock API response doesn't get generated
     */
    @Test
    public void itemsPlacedInStorageArePublishedToNetwork() throws InterruptedException {
        // Arrange: storage engine is running
        syncEngine.start();

        // The latch can be used to block until response is received
        CountDownLatch responseLatch =
            awaitResponse(api, new GraphQLResponse<>(Person.named("Tony"), null));

        // Arrange: create a person
        final Person susan = Person.named("Susan");
        final MutationEvent<Person> insertSusan = MutationEvent.<Person>builder()
            .dataClass(Person.class)
            .data(susan)
            .mutationType(MutationEvent.MutationType.INSERT)
            .source(MutationEvent.Source.DATA_STORE)
            .build();

        // Act: Put person into storage.
        AwaitResultListener<MutationEvent<Person>> listener = AwaitResultListener.create();
        localStorageAdapter.save(susan, listener);
        listener.await(OPERATIONS_TIMEOUT_MS);

        // Wait for the network callback to occur on the IO scheduler ...
        assertTrue(responseLatch.await(OPERATIONS_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Assert: API was invoked to write the thing to the network
        verify(api).mutate(eq(apiName), anyString(), anyMap(), any(), any());
    }

    /**
     * Mock the API to prepare the provided response, and return a CountDownLatch
     * that will become zero when the response has been returned.
     * @param api API Category Behavior
     * @param response A response to send when mutate() is invoked
     * @param <T> The type of data in the response
     * @return A latch to wait on, which signals that response is returned
     */
    @SuppressWarnings("unchecked") // obtaining listener via invocation.getArgument() assumes template type
    private static <T> CountDownLatch awaitResponse(
            ApiCategoryBehavior api, GraphQLResponse<T> response) {
        CountDownLatch responseLatch = new CountDownLatch(1);

        // Arrange for the response listener to get invoked with a successful
        // response, whenever an API call is made. This effectively mocks
        // out all behavior of the API category.
        doAnswer(invocation -> {
            final int resultListenerParamIndex = 4; // The fifth, with api at .get(0)
            ResultListener<GraphQLResponse<T>> listener =
                invocation.getArgument(resultListenerParamIndex, ResultListener.class);
            listener.onResult(response);
            responseLatch.countDown();
            return null;
        }).when(api).mutate(
            anyString(),
            anyString(),
            anyMap(),
            any(),
            any()
        );

        return responseLatch;
    }

    /**
     * A listener that can be used like a latch. You can call {@link AwaitResultListener#await(long)}
     * to block execution until the listener receives a result/error of some kind.
     * @param <T> Type of result returned in the listener
     */
    static final class AwaitResultListener<T> implements ResultListener<T> {
        private final CountDownLatch latch;

        private AwaitResultListener() {
            this.latch = new CountDownLatch(1);
        }

        static <T> AwaitResultListener<T> create() {
            return new AwaitResultListener<>();
        }

        public void await(long waitTimeMillis) {
            boolean didCountDown;
            try {
                didCountDown = latch.await(waitTimeMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException interruptedException) {
                throw new RuntimeException(interruptedException);
            }
            if (!didCountDown) {
                throw new RuntimeException("Latch didn't count down...");
            }
        }

        @Override
        public void onResult(final T result) {
            latch.countDown();
        }

        @Override
        public void onError(final Throwable error) {
            latch.countDown();
        }
    }
}
