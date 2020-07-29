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
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.AppSyncMocking;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.model.SimpleModelProvider;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.util.Time;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

import static com.amplifyframework.datastore.syncengine.TestHubEventFilters.publicationOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests the {@link Orchestrator}.
 */
@RunWith(RobolectricTestRunner.class)
public final class OrchestratorTest {
    /**
     * When an item is placed into storage, a cascade of
     * things happen which should ultimately result in a mutation call
     * to the API category, with an {@link MutationType} corresponding to the type of
     * modification that was made to the storage.
     * @throws AmplifyException On failure to load model schema into registry
     */
    @SuppressWarnings("unchecked") // Casting ? in HubEvent<?> to PendingMutation<? extends Model>
    @Test
    public void itemsPlacedInStorageArePublishedToNetwork() throws AmplifyException {
        ShadowLog.stream = System.out;

        // Arrange: create a BlogOwner
        final BlogOwner susan = BlogOwner.builder()
            .name("Susan Quimby")
            .build();

        HubAccumulator accumulator =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(susan), 1)
                .start();

        // Mock behaviors from AppSync
        AppSync appSync = mock(AppSync.class);
        ModelMetadata metadata = new ModelMetadata(susan.getId(), false, 1, Time.now());
        ModelWithMetadata<BlogOwner> modelWithMetadata = new ModelWithMetadata<>(susan, metadata);
        AppSyncMocking.create(appSync).mockResponse(susan);
        AppSyncMocking.sync(appSync).mockSuccessResponse(BlogOwner.class, modelWithMetadata);
        AppSyncMocking.onCreate(appSync).callOnStart();
        AppSyncMocking.onDelete(appSync).callOnStart();
        AppSyncMocking.onUpdate(appSync).callOnStart();

        InMemoryStorageAdapter localStorageAdapter = InMemoryStorageAdapter.create();
        ModelProvider modelProvider = SimpleModelProvider.withRandomVersion(BlogOwner.class);
        ModelSchemaRegistry modelSchemaRegistry = ModelSchemaRegistry.instance();
        modelSchemaRegistry.clear();
        modelSchemaRegistry.load(modelProvider.models());

        Orchestrator orchestrator =
            new Orchestrator(modelProvider,
                modelSchemaRegistry,
                localStorageAdapter,
                appSync,
                DataStoreConfiguration::defaults,
                () -> Orchestrator.Mode.SYNC_VIA_API
            );

        // Arrange: orchestrator is running
        orchestrator.start();

        // Act: Put BlogOwner into storage, and wait for it to complete.
        SynchronousStorageAdapter.delegatingTo(localStorageAdapter).save(susan);

        // Assert that the event is published out to the API
        assertEquals(
            Collections.singletonList(susan),
            Observable.fromIterable(accumulator.await(10, TimeUnit.SECONDS))
                .map(HubEvent::getData)
                .map(data -> (PendingMutation<BlogOwner>) data)
                .map(PendingMutation::getMutatedItem)
                .toList()
                .blockingGet()
        );

        assertTrue(orchestrator.stop().blockingAwait(5, TimeUnit.SECONDS));
    }
}
