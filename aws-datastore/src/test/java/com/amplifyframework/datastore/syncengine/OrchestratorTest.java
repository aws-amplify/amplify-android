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
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.DataStoreConfiguration;
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

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

import static com.amplifyframework.datastore.syncengine.TestHubEventFilters.isProcessed;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link Orchestrator}.
 */
@RunWith(RobolectricTestRunner.class)
public final class OrchestratorTest {

    private Orchestrator orchestrator;
    private HubAccumulator orchestratorInitObserver;
    private final GraphQLBehavior mockApi = mock(GraphQLBehavior.class);
    private final InMemoryStorageAdapter localStorageAdapter = InMemoryStorageAdapter.create();
    private final BlogOwner susan = BlogOwner.builder().name("Susan Quimby").build();

    private final ReachabilityMonitor reachabilityMonitor = mock(ReachabilityMonitor.class);

    /**
     * Setup mocks and other common elements.
     * @throws AmplifyException Not expected.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws AmplifyException {
        ShadowLog.stream = System.out;

        // SYNC_QUERIES_READY indicates that the sync queries have completed.
        orchestratorInitObserver =
            HubAccumulator.create(HubChannel.DATASTORE, DataStoreChannelEventName.SYNC_QUERIES_READY, 1)
                .start();

        ModelMetadata metadata = new ModelMetadata(
            susan.getId(),
            false,
            1,
            Temporal.Timestamp.now()
        );
        ModelWithMetadata<BlogOwner> modelWithMetadata = new ModelWithMetadata<>(susan, metadata);
        // Mock behaviors from for the API category
        ApiMocking.mockSubscriptionStart(mockApi);
        ApiMocking.mockSuccessfulMutation(mockApi, susan.getId(), modelWithMetadata);
        ApiMocking.mockSuccessfulQuery(mockApi, modelWithMetadata);
        AppSyncClient appSync = AppSyncClient.via(mockApi);

        ModelProvider modelProvider = SimpleModelProvider.withRandomVersion(BlogOwner.class);
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();
        schemaRegistry.clear();
        schemaRegistry.register(modelProvider.models());

        when(reachabilityMonitor.getObservable()).thenReturn(Observable.just(true));

        orchestrator = new Orchestrator(
            modelProvider,
            schemaRegistry,
            localStorageAdapter,
            appSync,
            DataStoreConfiguration::defaults,
            () -> Orchestrator.State.SYNC_VIA_API,
            reachabilityMonitor,
            true
        );
    }

    /**
     * When an item is placed into storage, a cascade of things happen which should ultimately result in a mutation call
     * to the API category, with an {@link MutationType} corresponding to the type of modification that was made to the
     * storage.
     * @throws AmplifyException On failure to load model schema into registry
     */
    @SuppressWarnings("unchecked") // Casting ? in HubEvent<?> to PendingMutation<? extends Model>
    @Test
    public void itemsPlacedInStorageArePublishedToNetwork() throws AmplifyException {
        // Arrange: orchestrator is running
        orchestrator.start().test().assertComplete();

        orchestratorInitObserver.await(10, TimeUnit.SECONDS);
        HubAccumulator accumulator = HubAccumulator.create(HubChannel.DATASTORE, isProcessed(susan), 1).start();

        // Act: Put BlogOwner into storage, and wait for it to complete.
        SynchronousStorageAdapter.delegatingTo(localStorageAdapter).save(susan);

        // Assert that the event is published out to the API
        assertEquals(
            Collections.singletonList(susan),
            Observable.fromIterable(accumulator.await(10, TimeUnit.SECONDS))
                .map(HubEvent::getData)
                .map(data -> (OutboxMutationEvent<BlogOwner>) data)
                .map(OutboxMutationEvent::getElement)
                .map(OutboxMutationEvent.OutboxMutationEventElement::getModel)
                .toList()
                .blockingGet()
        );

        assertTrue(orchestrator.stop().blockingAwait(5, TimeUnit.SECONDS));
    }

    /**
     * Verify preventing concurrent state transitions from happening.
     */
    @Test
    public void preventConcurrentStateTransitions() {
        // Arrange: orchestrator is running
        orchestrator.start().test().assertComplete();

        // Try to start it in a new thread.
        boolean success = Completable.create(emitter -> {
            new Thread(() -> orchestrator.start()
                                 .subscribe(emitter::onComplete, emitter::onError)
            ).start();

            // Try to start it again on the current thread.
            orchestrator.start().test().assertComplete();
        }).blockingAwait(5, TimeUnit.SECONDS);
        assertTrue("Failed to start orchestrator on a background thread", success);

        orchestratorInitObserver.await(10, TimeUnit.SECONDS);
        verify(mockApi, times(1)).query(any(), any(), any());

        assertTrue(orchestrator.stop().blockingAwait(5, TimeUnit.SECONDS));
    }

    /**
     * Verify that an error that occurs during a start/stop still releases the semaphore and allows retrying.
     */
    @Test
    public void preventLockupOnStartStopError() {
        IllegalStateException exception = new IllegalStateException("Simulated failure");

        // Not a particularly realistic failure, but the exact cause isn't important.
        when(reachabilityMonitor.getObservable()).thenThrow(exception);

        // This will fail, but it should not leave the orchestrator in a bad state.
        orchestrator.start().test().assertError(exception);

        // Reset the mock so that it won't throw an error on second attempt
        reset(reachabilityMonitor);
        when(reachabilityMonitor.getObservable()).thenReturn(Observable.just(true));

        // Now we should be able to start successfully
        orchestrator.start().test().assertComplete();
    }
}
