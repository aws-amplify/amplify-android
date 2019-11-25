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
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.testmodels.Person;
import com.amplifyframework.testutils.LatchedResultListener;
import com.amplifyframework.testutils.RandomString;

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
        ApiCategoryBehavior api = mock(ApiCategoryBehavior.class);
        String apiName = RandomString.string();
        LocalStorageAdapter localStorageAdapter = InMemoryStorageAdapter.create();
        SyncEngine syncEngine = new SyncEngine(api, apiName, localStorageAdapter);

        // Arrange: storage engine is running
        syncEngine.start();

        // Arrange: create a person
        final Person susan = Person.builder()
            .firstName("Susan")
            .lastName("Quimby")
            .build();

        CountDownLatch apiInvoked = new CountDownLatch(1);
        doAnswer(invocation -> {
            apiInvoked.countDown();
            return null;
        }).when(api).mutate(
            any(),
            any(),
            any(),
            any(),
            any()
        );

        // Act: Put person into storage, and wait for it to complete.
        LatchedResultListener<StorageItemChange.Record> listener =
            LatchedResultListener.waitFor(OPERATIONS_TIMEOUT_MS);
        localStorageAdapter.save(susan, StorageItemChange.Initiator.DATA_STORE_API, listener);
        listener.awaitTerminalEvent().assertNoError().assertResult();

        // Wait for the mock network callback to occur on the IO scheduler ...
        assertTrue(apiInvoked.await(OPERATIONS_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
