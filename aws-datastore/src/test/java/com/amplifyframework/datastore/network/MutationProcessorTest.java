/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.NoOpCancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.GsonStorageItemChangeConverter;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.Await;
import com.amplifyframework.testutils.RandomString;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Tests the {@link MutationProcessor}.
 */
@SuppressWarnings("unchecked") // Mockito's argument matchers, e.g. any()
@RunWith(RobolectricTestRunner.class)
public final class MutationProcessorTest {
    private static final long REASONABLE_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(1);

    private LocalStorageAdapter localStorageAdapter;
    private AppSyncEndpoint appSyncEndpoint;
    private MutationProcessor mutationProcessor;
    private StorageItemChange.RecordFactory recordConverter;

    @Before
    public void setup() {
        this.localStorageAdapter = InMemoryStorageAdapter.create();
        this.appSyncEndpoint = mock(AppSyncEndpoint.class);
        this.mutationProcessor = new MutationProcessor(new MutationOutbox(localStorageAdapter), appSyncEndpoint);
        this.recordConverter = new GsonStorageItemChangeConverter();
        ShadowLog.stream = System.out;
    }

    /**
     * Validates that the {@link MutationProcessor} will read form the {@link MutationOutbox},
     * and will publish any items there-in to the {@link AppSyncEndpoint}.
     * @throws DataStoreException On failure to arrange items in to the MutationOutbox
     * @throws InterruptedException If the latch is interrupted while waiting for AppSync API to be invoked
     */
    @Test
    public void canDrainMutationOutbox() throws DataStoreException, InterruptedException {
        // Arrange a CountDownLatch, which will count down when the AppSyncEndpoint API is hit.
        final CountDownLatch apiInvocationsPending = new CountDownLatch(2);
        doAnswer(invocation -> {
            // Count down our latch, to signal that the create() API was hit.
            apiInvocationsPending.countDown();

            // Simulate a successful response callback from the create() method.
            final int indexOfModelBeingCreated = 0;
            final int indexOfResultConsumer = 1;
            Model result = invocation.getArgument(indexOfModelBeingCreated);
            Consumer<GraphQLResponse<Model>> onResult = invocation.getArgument(indexOfResultConsumer);
            onResult.accept(new GraphQLResponse<>(result, Collections.emptyList()));

            // Technically, create() returns a Cancelable...
            return new NoOpCancelable();
        }).when(appSyncEndpoint)
            .create(any(Model.class), any(Consumer.class), any(Consumer.class));

        // Put some stuff in the mutation outbox.
        // (Actually, we're directly putting records into the storage adapter.
        // technically, this is an implementation detail of them mutation outbox, oops!.)
        populateStorageItemChangeRecords(
            StorageItemChange.<BlogOwner>builder()
                .itemClass(BlogOwner.class)
                .item(BlogOwner.builder()
                    .name("First")
                    .wea(RandomString.string())
                    .build())
                .initiator(StorageItemChange.Initiator.DATA_STORE_API)
                .type(StorageItemChange.Type.SAVE)
                .build()
                .toRecord(recordConverter),
            StorageItemChange.<Blog>builder()
                .type(StorageItemChange.Type.SAVE)
                .initiator(StorageItemChange.Initiator.DATA_STORE_API)
                .itemClass(Blog.class)
                .item(Blog.builder()
                    .name("Cool blog")
                    .owner(BlogOwner.builder()
                        .name("Cool owner")
                        .build())
                    .build())
                .build()
                .toRecord(recordConverter)
        );

        // Okay, neat. Now, try to start the mutation processor.
        mutationProcessor.startDrainingMutationOutbox();

        // Wait for the API to be invoked.
        apiInvocationsPending.await(REASONABLE_WAIT_TIME_MS, TimeUnit.MILLISECONDS);

        // As a result, the mutation processor should try to dispatch them to the
        // AppSyncEndpoint.
        assertEquals(0, apiInvocationsPending.getCount());
    }

    private void populateStorageItemChangeRecords(StorageItemChange.Record... records) throws DataStoreException {
        for (StorageItemChange.Record record : records) {
            Await.<StorageItemChange.Record, DataStoreException>result((onResult, onError) ->
                localStorageAdapter.save(
                    record, StorageItemChange.Initiator.DATA_STORE_API, onResult, onError
                )
            );
        }
    }
}
