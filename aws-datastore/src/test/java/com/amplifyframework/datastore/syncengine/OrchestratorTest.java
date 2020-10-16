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
import com.amplifyframework.api.graphql.GraphQLBehavior;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSyncClient;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.model.SimpleModelProvider;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.mocks.ApiMocking;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;

import static com.amplifyframework.datastore.syncengine.TestHubEventFilters.publicationOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests the {@link Orchestrator}.
 */
@RunWith(RobolectricTestRunner.class)
public final class OrchestratorTest {
    private Orchestrator orchestrator;
    private HubAccumulator orchestratorInitObserver;
    private GraphQLBehavior mockApi;
    private InMemoryStorageAdapter localStorageAdapter;
    private BlogOwner susan;

    /**
     * Setup mocks and other common elements.
     * @throws AmplifyException Not expected.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws AmplifyException {
        ShadowLog.stream = System.out;
        // Arrange: create a BlogOwner
        susan = BlogOwner.builder().name("Susan Quimby").build();

        // SUBSCRIPTIONS_ESTABLISHED indicates that the orchestrator is up and running.
        orchestratorInitObserver =
            HubAccumulator.create(HubChannel.DATASTORE, DataStoreChannelEventName.SUBSCRIPTIONS_ESTABLISHED, 1)
                          .start();

        ModelMetadata metadata = new ModelMetadata(susan.getId(),
                                                   false,
                                                   1,
                                                   Temporal.Timestamp.now());
        ModelWithMetadata<BlogOwner> modelWithMetadata = new ModelWithMetadata<>(susan, metadata);
        // Mock behaviors from for the API category
        mockApi = mock(GraphQLBehavior.class);
        ApiMocking.mockSubscriptionStart(mockApi);
        ApiMocking.mockSuccessfulMutation(mockApi, susan.getId(), modelWithMetadata);
        ApiMocking.mockSuccessfulQuery(mockApi, modelWithMetadata);
        AppSyncClient appSync = AppSyncClient.via(mockApi);

        localStorageAdapter = InMemoryStorageAdapter.create();
        ModelProvider modelProvider = SimpleModelProvider.withRandomVersion(BlogOwner.class);
        ModelSchemaRegistry modelSchemaRegistry = ModelSchemaRegistry.instance();
        modelSchemaRegistry.clear();
        modelSchemaRegistry.load(modelProvider.models());

        orchestrator =
            new Orchestrator(modelProvider,
                modelSchemaRegistry,
                localStorageAdapter,
                appSync,
                DataStoreConfiguration::defaults,
                () -> Orchestrator.Mode.SYNC_VIA_API
            );
    }

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
        // Arrange: orchestrator is running
        orchestrator.start();

        orchestratorInitObserver.await(10, TimeUnit.SECONDS);
        HubAccumulator accumulator =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(susan), 1)
                          .start();
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

    /**
     * Verify preventing concurrent state transitions from happening.
     * @throws AmplifyException Not expected.
     */
    @SuppressWarnings("unchecked") // Casting any(GraphQLRequest.class)
    @Test
    public void preventConcurrentStateTransitions() throws AmplifyException {

        // Arrange: orchestrator is running
        orchestrator.start();

        // Try to start it in a new thread.
        new Thread(() -> {
            try {
                orchestrator.start();
            } catch (DataStoreException exception) {
                fail("Error occurred starting the orchestrator." + exception);
            }
        }).start();
        // Try to start it again on a current thread.
        orchestrator.start();

        orchestratorInitObserver.await(10, TimeUnit.SECONDS);
        verify(mockApi, times(1)).query(any(GraphQLRequest.class), any(), any());

        assertTrue(orchestrator.stop().blockingAwait(5, TimeUnit.SECONDS));
    }
}
