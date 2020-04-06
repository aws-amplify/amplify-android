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

package com.amplifyframework.datastore.syncengine;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.NoOpCancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.datastore.AWSDataStorePluginConfiguration;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.SimpleModelProvider;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.Await;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Tests the {@link Orchestrator}.
 */
@SuppressWarnings("unchecked") // Mockito matchers, i.e. any(Raw.class), etc.
@RunWith(RobolectricTestRunner.class)
public final class OrchestratorTest {
    // A "reasonable" amount of time our test(s) will wait for async operations to complete
    private static final long OPERATIONS_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5) /* ms */;

    /**
     * When an item is placed into storage, a cascade of
     * things happen which should ultimately result in a mutation call
     * to the API category, with an {@link MutationType} corresponding to the type of
     * modification that was made to the storage.
     * @throws InterruptedException If our own mock API response doesn't get generated
     * @throws AmplifyException On failure to load model schema into registry
     */
    @Test
    public void itemsPlacedInStorageArePublishedToNetwork() throws InterruptedException, AmplifyException {
        AppSync appSync = mock(AppSync.class);

        // Arrange: create a BlogOwner
        final BlogOwner susan = BlogOwner.builder()
            .name("Susan Quimby")
            .build();

        CountDownLatch apiInvocationsPending = new CountDownLatch(1);
        doAnswer(invocation -> {
            // Count down our latch, to indicate that this code did run.
            apiInvocationsPending.countDown();

            // Simulate a successful response from the API.
            int positionOfCreationItem = 0;
            int positionOfResponseConsumer = 1;
            Model createdItem = invocation.getArgument(positionOfCreationItem);
            Consumer<GraphQLResponse<Model>> onResponse = invocation.getArgument(positionOfResponseConsumer);
            onResponse.accept(new GraphQLResponse<>(createdItem, Collections.emptyList()));

            // Technically, the AppSync create() returns a Cancelable of some kind.
            return new NoOpCancelable();
        }).when(appSync)
            .create(eq(susan), any(Consumer.class), any(Consumer.class));

        InMemoryStorageAdapter localStorageAdapter = InMemoryStorageAdapter.create();
        ModelProvider modelProvider = SimpleModelProvider.withRandomVersion();
        ModelSchemaRegistry modelSchemaRegistry = ModelSchemaRegistry.instance();
        modelSchemaRegistry.clear();
        modelSchemaRegistry.load(modelProvider.models());

        Orchestrator orchestrator =
            new Orchestrator(modelProvider, modelSchemaRegistry, localStorageAdapter, appSync,
                () -> AWSDataStorePluginConfiguration.DEFAULT_BASE_SYNC_INTERVAL_MS
            );

        // Arrange: storage engine is running
        orchestrator.start().blockingAwait();

        // Act: Put BlogOwner into storage, and wait for it to complete.
        Await.result(
            (Consumer<StorageItemChange.Record> onResult, Consumer<DataStoreException> onError) ->
                localStorageAdapter.save(
                    susan,
                    StorageItemChange.Initiator.DATA_STORE_API,
                    onResult,
                    onError
                )
        );

        // Wait for the mock network callback to occur on the IO scheduler ...
        apiInvocationsPending.await(OPERATIONS_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(0, apiInvocationsPending.getCount());
    }
}
