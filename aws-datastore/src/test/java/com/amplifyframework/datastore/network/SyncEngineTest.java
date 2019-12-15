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

import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.EmptyConsumer;
import com.amplifyframework.testutils.LatchedConsumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Tests the {@link SyncEngine}.
 */
@Config(sdk = Build.VERSION_CODES.P, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class SyncEngineTest {
    // A "reasonable" amount of time our test(s) will wait for async operations to complete
    private static final long OPERATIONS_TIMEOUT_MS = 5_000L /* ms */;

    /**
     * When an item is placed into storage, a cascade of
     * things happen which should ultimately result in a mutation call
     * to the API category, with an {@link MutationType} corresponding to the type of
     * modification that was made to the storage.
     * @throws InterruptedException If our own mock API response doesn't get generated
     */
    @Test
    public void itemsPlacedInStorageArePublishedToNetwork() throws InterruptedException {
        ShadowLog.stream = System.out;
        AppSyncEndpoint endpoint = mock(AppSyncEndpoint.class);
        LocalStorageAdapter localStorageAdapter = InMemoryStorageAdapter.create();
        ModelProvider modelProvider = mock(ModelProvider.class);
        SyncEngine syncEngine = new SyncEngine(modelProvider, localStorageAdapter, endpoint);

        // Arrange: storage engine is running
        syncEngine.start();

        // Arrange: create a BlogOwner
        final BlogOwner susan = BlogOwner.builder()
            .name("Susan Quimby")
            .build();

        CountDownLatch apiInvoked = new CountDownLatch(1);
        doAnswer(invocation -> {
            apiInvoked.countDown();
            return null;
        }).when(endpoint).create(any(), any());

        // Act: Put BlogOwner into storage, and wait for it to complete.
        LatchedConsumer<StorageItemChange.Record> saveConsumer = LatchedConsumer.instance(OPERATIONS_TIMEOUT_MS);
        ResultListener<StorageItemChange.Record> listener =
            ResultListener.instance(saveConsumer::accept, EmptyConsumer.of(Throwable.class));
        localStorageAdapter.save(susan, StorageItemChange.Initiator.DATA_STORE_API, listener);
        saveConsumer.awaitValue();

        // Wait for the mock network callback to occur on the IO scheduler ...
        assertTrue(apiInvoked.await(OPERATIONS_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
