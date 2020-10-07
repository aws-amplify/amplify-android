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

package com.amplifyframework.datastore.syncengine;

import androidx.annotation.NonNull;

import com.amplifyframework.api.graphql.GraphQLLocation;
import com.amplifyframework.api.graphql.GraphQLPathSegment;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreConfigurationProvider;
import com.amplifyframework.datastore.DataStoreConflictData;
import com.amplifyframework.datastore.DataStoreConflictHandler;
import com.amplifyframework.datastore.DataStoreConflictHandlerResult;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.AppSyncMocking;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.Latch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.amplifyframework.datastore.syncengine.TestHubEventFilters.outboxIsEmpty;
import static com.amplifyframework.datastore.syncengine.TestHubEventFilters.publicationOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link MutationProcessor}.
 */
@RunWith(RobolectricTestRunner.class)
public final class MutationProcessorTest {
    private static final long TIMEOUT_SECONDS = 5;

    private SynchronousStorageAdapter synchronousStorageAdapter;
    private MutationOutbox mutationOutbox;
    private AppSync appSync;
    private MutationProcessor mutationProcessor;
    private DataStoreConfigurationProvider configurationProvider;

    /**
     * A {@link MutationProcessor} is being tested. To do so, we arrange mutations into
     * an {@link MutationOutbox}. Fake responses are returned from a mock {@link AppSync}.
     */
    @Before
    public void setup() {
        ShadowLog.stream = System.out;
        LocalStorageAdapter localStorageAdapter = InMemoryStorageAdapter.create();
        this.synchronousStorageAdapter = SynchronousStorageAdapter.delegatingTo(localStorageAdapter);
        this.mutationOutbox = new PersistentMutationOutbox(localStorageAdapter);
        VersionRepository versionRepository = new VersionRepository(localStorageAdapter);
        Merger merger = new Merger(mutationOutbox, versionRepository, localStorageAdapter);
        this.appSync = mock(AppSync.class);
        SyncTimeRegistry syncTimeRegistry = new SyncTimeRegistry(localStorageAdapter);
        this.configurationProvider = mock(DataStoreConfigurationProvider.class);
        this.mutationProcessor = MutationProcessor.builder()
            .merger(merger)
            .versionRepository(versionRepository)
            .syncTimeRegistry(syncTimeRegistry)
            .mutationOutbox(mutationOutbox)
            .appSync(appSync)
            .dataStoreConfigurationProvider(configurationProvider)
            .build();
    }

    /**
     * Processing a mutation should publish current outbox status.
     */
    @Test
    public void outboxStatusIsPublishedToHubOnProcess() {
        BlogOwner raphael = BlogOwner.builder()
                .name("Raphael Kim")
                .build();
        PendingMutation<BlogOwner> createRaphael = PendingMutation.creation(raphael, BlogOwner.class);

        // Mock up a response from AppSync and enqueue a mutation.
        AppSyncMocking.create(appSync).mockSuccessResponse(raphael);
        assertTrue(mutationOutbox.enqueue(createRaphael)
            .blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        // Start listening for publication events.
        HubAccumulator statusAccumulator = HubAccumulator.create(
                HubChannel.DATASTORE,
                outboxIsEmpty(true), // outbox should be empty after processing its only mutation
                1
        ).start();

        // Start draining the outbox which has one mutation enqueued,
        // and make sure that outbox status is published to hub.
        mutationProcessor.startDrainingMutationOutbox();
        statusAccumulator.await();
    }

    /**
     * Tests the {@link MutationProcessor#startDrainingMutationOutbox()}. After this method
     * is called, any content in the {@link MutationOutbox} should be published via the {@link AppSync}
     * and then removed.
     * @throws DataStoreException On failure to interact with storage adapter during arrangement
     *                            and verification
     */
    @Test
    public void canDrainMutationOutbox() throws DataStoreException {
        // We will attempt to "sync" this model.
        BlogOwner tony = BlogOwner.builder()
            .name("Tony Daniels")
            .build();
        synchronousStorageAdapter.save(tony);

        // Arrange a cooked response from AppSync.
        AppSyncMocking.create(appSync).mockSuccessResponse(tony);

        // Start listening for publication events.
        HubAccumulator accumulator =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(tony), 1)
                .start();

        PendingMutation<BlogOwner> createTony = PendingMutation.creation(tony, BlogOwner.class);
        assertTrue(mutationOutbox.enqueue(createTony)
            .blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        // Act! Start draining the outbox.
        mutationProcessor.startDrainingMutationOutbox();

        // Assert: the event was published
        List<HubEvent<?>> events = accumulator.await();
        assertEquals(1, events.size());
        @SuppressWarnings("unchecked")
        PendingMutation<BlogOwner> mutation = (PendingMutation<BlogOwner>) events.get(0).getData();
        assertEquals(createTony, mutation);

        // And that it is no longer in the outbox.
        assertFalse(mutationOutbox.hasPendingMutation(tony.getId()));

        // And that it was passed to AppSync for publication.
        verify(appSync).create(eq(tony), any(), any());
    }

    /**
     * If the AppSync response to the mutation contains a ConflictUnhandled
     * error in the GraphQLResponse error list, then the user-provided
     * conflict handler should be invoked.
     * @throws DataStoreException On failure to obtain configuration from the provider
     */
    @Test
    public void conflictHandlerInvokedForUnhandledConflictError() throws DataStoreException {
        // Arrange a user-provided conflict handler.
        CountDownLatch handlerInvocationsRemainingCount = new CountDownLatch(1);
        DataStoreConflictHandler handler = new DataStoreConflictHandler() {
            @Override
            public <T extends Model> void resolveConflict(
                    @NonNull DataStoreConflictData<T> conflictData,
                    @NonNull Consumer<DataStoreConflictHandlerResult> onResult) {
                handlerInvocationsRemainingCount.countDown();
            }
        };
        when(configurationProvider.getConfiguration())
            .thenReturn(DataStoreConfiguration.builder()
                .dataStoreConflictHandler(handler)
                .build()
            );

        // Save a model, its metadata, and its last sync data.
        BlogOwner model = BlogOwner.builder()
            .name("Exceptional Blogger")
            .build();
        ModelMetadata metadata =
            new ModelMetadata(model.getId(), false, 1, Temporal.Timestamp.now());
        LastSyncMetadata lastSyncMetadata = LastSyncMetadata.baseSyncedAt(BlogOwner.class, 1_000L);
        synchronousStorageAdapter.save(model, metadata, lastSyncMetadata);

        // Enqueue an update in the mutation outbox
        assertTrue(mutationOutbox
            .enqueue(PendingMutation.update(model, BlogOwner.class))
            .blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        // Fields that represent the "server's" understanding of the model state
        Map<String, Object> serverModelData = new HashMap<>();
        serverModelData.put("id", model.getId());
        serverModelData.put("name", "Server blogger name");
        serverModelData.put("_version", 1);
        serverModelData.put("_deleted", false);
        serverModelData.put("_lastChangedAt", 1_000);

        // When AppSync receives that update, have it respond
        // with a ConflictUnhandledError.
        String message = "Conflict resolver rejects mutation.";
        List<GraphQLPathSegment> paths = Collections.singletonList(new GraphQLPathSegment("updateBlogOwner"));
        List<GraphQLLocation> locations = Collections.singletonList(new GraphQLLocation(2, 3));
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("errorType", "ConflictUnhandled");
        extensions.put("data", serverModelData);
        GraphQLResponse.Error error =
            new GraphQLResponse.Error(message, locations, paths, extensions);

        AppSyncMocking.update(appSync).mockErrorResponse(model, 1, error);

        // Start the mutation processor.
        mutationProcessor.startDrainingMutationOutbox();

        // Wait for the conflict handler to be called.
        Latch.await(handlerInvocationsRemainingCount);
    }
}
