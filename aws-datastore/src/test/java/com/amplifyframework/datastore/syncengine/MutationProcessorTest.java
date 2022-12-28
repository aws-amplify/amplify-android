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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLLocation;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLPathSegment;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreConfigurationProvider;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.AppSyncMocking;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.Latch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.amplifyframework.datastore.syncengine.TestHubEventFilters.isOutboxEmpty;
import static com.amplifyframework.datastore.syncengine.TestHubEventFilters.isProcessed;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
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

    private SchemaRegistry schemaRegistry;
    private SynchronousStorageAdapter synchronousStorageAdapter;
    private MutationOutbox mutationOutbox;
    private AppSync appSync;
    private MutationProcessor mutationProcessor;
    private DataStoreConfigurationProvider configurationProvider;

    /**
     * A {@link MutationProcessor} is being tested. To do so, we arrange mutations into
     * an {@link MutationOutbox}. Fake responses are returned from a mock {@link AppSync}.
     * @throws AmplifyException When loading SchemaRegistry
     */
    @Before
    public void setup() throws AmplifyException {
        ShadowLog.stream = System.out;
        LocalStorageAdapter localStorageAdapter = InMemoryStorageAdapter.create();
        this.synchronousStorageAdapter = SynchronousStorageAdapter.delegatingTo(localStorageAdapter);
        this.mutationOutbox = new PersistentMutationOutbox(localStorageAdapter);
        VersionRepository versionRepository = new VersionRepository(localStorageAdapter);
        Merger merger = new Merger(mutationOutbox, versionRepository, localStorageAdapter);
        this.appSync = mock(AppSync.class);
        this.configurationProvider = mock(DataStoreConfigurationProvider.class);
        RetryHandler retryHandler = new RetryHandler(0, Duration.ofMinutes(1).toMillis());
        schemaRegistry = SchemaRegistry.instance();
        schemaRegistry.register(Collections.singleton(BlogOwner.class));
        this.mutationProcessor = MutationProcessor.builder()
                .merger(merger)
                .versionRepository(versionRepository)
                .schemaRegistry(schemaRegistry)
                .mutationOutbox(mutationOutbox)
                .appSync(appSync)
                .dataStoreConfigurationProvider(configurationProvider)
                .retryHandler(retryHandler)
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
        ModelSchema schema = schemaRegistry.getModelSchemaForModelClass(BlogOwner.class);
        PendingMutation<BlogOwner> createRaphael = PendingMutation.creation(raphael, schema);

        // Mock up a response from AppSync and enqueue a mutation.
        AppSyncMocking.create(appSync).mockSuccessResponse(raphael);
        assertTrue(mutationOutbox.enqueue(createRaphael)
            .blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        // Start listening for publication events.
        // outbox should be empty after processing its only mutation
        HubAccumulator statusAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, isOutboxEmpty(true), 1)
                .start();

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
            HubAccumulator.create(HubChannel.DATASTORE, isProcessed(tony), 1)
                .start();

        ModelSchema schema = schemaRegistry.getModelSchemaForModelClass(BlogOwner.class);
        PendingMutation<BlogOwner> createTony = PendingMutation.creation(tony, schema);
        assertTrue(mutationOutbox.enqueue(createTony)
            .blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        // Act! Start draining the outbox.
        mutationProcessor.startDrainingMutationOutbox();

        // Assert: the event was published
        assertEquals(1, accumulator.await().size());

        // And that it is no longer in the outbox.
        assertFalse(mutationOutbox.hasPendingMutation(tony.getPrimaryKeyString()));

        // And that it was passed to AppSync for publication.
        verify(appSync).create(eq(tony), any(), any(), any());
    }

    /**
     * If the AppSync response to the mutation contains a ConflictUnhandled
     * error in the GraphQLResponse error list, then the user-provided
     * conflict handler should be invoked.
     * @throws DataStoreException On failure to obtain configuration from the provider
     * @throws AmplifyException On failure to build {@link ModelSchema}
     */
    @Test
    public void conflictHandlerInvokedForUnhandledConflictError() throws AmplifyException {
        // Arrange a user-provided conflict handler.
        CountDownLatch handlerInvocationsRemainingCount = new CountDownLatch(1);
        when(configurationProvider.getConfiguration())
            .thenReturn(DataStoreConfiguration.builder()
                .conflictHandler((conflictData, onDecision) ->
                    handlerInvocationsRemainingCount.countDown()
                )
                .build()
            );

        // Save a model, its metadata, and its last sync data.
        BlogOwner model = BlogOwner.builder()
            .name("Exceptional Blogger")
            .build();
        ModelMetadata metadata =
            new ModelMetadata(model.getModelName() + "|" + model.getPrimaryKeyString(), false, 1,
                    Temporal.Timestamp.now());
        ModelSchema schema = schemaRegistry.getModelSchemaForModelClass(BlogOwner.class);
        LastSyncMetadata lastSyncMetadata = LastSyncMetadata.baseSyncedAt(schema.getName(), 1_000L);
        synchronousStorageAdapter.save(model, metadata, lastSyncMetadata);

        // Enqueue an update in the mutation outbox
        assertTrue(mutationOutbox
            .enqueue(PendingMutation.update(model, schema))
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
        mutationProcessor.stopDrainingMutationOutbox();
    }

    /**
     * If the AppSync response to the mutation contains not-empty GraphQLResponse error
     * list without any ConflictUnhandled error, then
     * {@link DataStoreChannelEventName#OUTBOX_MUTATION_FAILED} event is published via Hub.
     * @throws DataStoreException On failure to save model and metadata
     */
    @Test
    public void hubEventPublishedForPublicationError() throws DataStoreException {
        // Save a model, its metadata, and its last sync data.
        BlogOwner model = BlogOwner.builder()
                .name("Average Joe")
                .build();
        ModelMetadata metadata =
                new ModelMetadata(model.getModelName() + "|" + model.getPrimaryKeyString(), false, 1,
                        Temporal.Timestamp.now());
        ModelSchema schema = schemaRegistry.getModelSchemaForModelClass(BlogOwner.class);
        synchronousStorageAdapter.save(model, metadata);

        // Enqueue an update in the mutation outbox
        assertTrue(mutationOutbox
                .enqueue(PendingMutation.update(model, schema))
                .blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        // When AppSync receives that update, have it respond with an error.
        AppSyncMocking.update(appSync).mockErrorResponse(model, 1);

        // Start listening for publication events.
        HubAccumulator errorAccumulator = HubAccumulator.create(
                HubChannel.DATASTORE,
                DataStoreChannelEventName.OUTBOX_MUTATION_FAILED,
                1
        ).start();

        // Start the mutation processor and wait for hub event.
        mutationProcessor.startDrainingMutationOutbox();
        errorAccumulator.await();
    }

    /**
     * If error is caused by AppSync response, then the mutation outbox continues to
     * drain without getting blocked.
     * @throws DataStoreException On failure to save models
     */
    @Test
    public void canDrainMutationOutboxOnPublicationError() throws DataStoreException {
        ModelSchema schema = schemaRegistry.getModelSchemaForModelClass(BlogOwner.class);

        // We will attempt to "sync" 10 models.
        final int maxAttempts = 10;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            BlogOwner model = BlogOwner.builder()
                .name("Blogger #" + attempt)
                .build();
            synchronousStorageAdapter.save(model);

            // Every other model triggers an AppSync error response.
            if (attempt % 2 == 0) {
                AppSyncMocking.create(appSync).mockErrorResponse(model);
            } else {
                AppSyncMocking.create(appSync).mockSuccessResponse(model);
            }

            // Enqueue a creation in the mutation outbox
            assertTrue(mutationOutbox
                .enqueue(PendingMutation.creation(model, schema))
                .blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }

        // Start listening for Mutation Outbox Empty event.
        HubAccumulator accumulator = HubAccumulator.create(
            HubChannel.DATASTORE,
            isOutboxEmpty(true),
            1
        ).start();

        // Start draining the outbox.
        mutationProcessor.startDrainingMutationOutbox();
        accumulator.await();
    }

    /**
     * If an error is caused by AppSync response, then the error handler gets invoked.
     * @throws DataStoreException On failure to save models
     */
    @Test
    public void callsErrorHandlerOnError() throws DataStoreException {
        CountDownLatch errorHandlerInvocationsLatch = new CountDownLatch(1);
        when(configurationProvider.getConfiguration()).thenReturn(DataStoreConfiguration.builder()
                .errorHandler(ignore -> errorHandlerInvocationsLatch.countDown())
                .build()
        );

        ModelSchema schema = schemaRegistry.getModelSchemaForModelClass(BlogOwner.class);

        BlogOwner model = BlogOwner.builder()
            .name("Blogger #1")
            .build();
        synchronousStorageAdapter.save(model);

        DataStoreException.GraphQLResponseException error = new DataStoreException.GraphQLResponseException(
                "Some exception.",
                Collections.emptyList()
        );

        AppSyncMocking.create(appSync).mockResponseFailure(model, error);

        // Enqueue a creation in the mutation outbox
        assertTrue(mutationOutbox
            .enqueue(PendingMutation.creation(model, schema))
            .blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        // Start listening for Outbox Mutation Failed event.
        HubAccumulator accumulator = HubAccumulator.create(
            HubChannel.DATASTORE,
            DataStoreChannelEventName.OUTBOX_MUTATION_FAILED,
            1
        ).start();

        // Start draining the outbox.
        mutationProcessor.startDrainingMutationOutbox();
        accumulator.await();

        try {
            assertTrue("Error handler wasn't invoked",
                    errorHandlerInvocationsLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            );
        } catch (InterruptedException exception) {
            fail(exception.getMessage());
        }
    }


    /**
     * If the AppSync response to the mutation contains a retry able
     * error in the GraphQLResponse error list, then the request is retried until successful.
     *
     * @throws InterruptedException Latch
     * @throws AmplifyException   On failure to build {@link ModelSchema}
     */
    @Test
    public void retryStrategyAppliedAfterRecoverableError()
            throws AmplifyException, InterruptedException {
        CountDownLatch retryHandlerInvocationCount = new CountDownLatch(2);
        // Save a model, its metadata, and its last sync data.
        BlogOwner model = BlogOwner.builder()
                .name("Exceptional Blogger")
                .build();
        ModelMetadata metadata =
                new ModelMetadata(model.getModelName() + "|" + model.getPrimaryKeyString(), false, 1,
                        Temporal.Timestamp.now());
        doAnswer(invocation -> {
            int indexOfResponseConsumer = 5;
            Consumer<DataStoreException> onError =
                    invocation.getArgument(indexOfResponseConsumer);
            retryHandlerInvocationCount.countDown();
            onError.accept(new DataStoreException("Error", "Retryable error."));
            return mock(GraphQLOperation.class);
        }).doAnswer(invocation -> {
            //When mutate is called on the appsync for the second time success response is returned
            int indexOfResponseConsumer = 4;
            Consumer<GraphQLResponse<ModelWithMetadata<BlogOwner>>> onResponse =
                    invocation.getArgument(indexOfResponseConsumer);
            ModelMetadata modelMetadata = new ModelMetadata(model.getId(), false, 1,
                    Temporal.Timestamp.now());

            ModelWithMetadata<BlogOwner> modelWithMetadata = new ModelWithMetadata<>(model,
                    modelMetadata);
            retryHandlerInvocationCount.countDown();
            onResponse.accept(new GraphQLResponse<>(modelWithMetadata, Collections.emptyList()));
            // latch makes sure success response is returned.
            return mock(GraphQLOperation.class);
        }).when(appSync).update(ArgumentMatchers.any(),
                any(ModelSchema.class),
                anyInt(),
                any(QueryPredicate.class),
                ArgumentMatchers.<Consumer<GraphQLResponse<ModelWithMetadata<BlogOwner>>>>any(),
                ArgumentMatchers.any());

        ModelSchema schema = schemaRegistry.getModelSchemaForModelClass(BlogOwner.class);
        LastSyncMetadata lastSyncMetadata = LastSyncMetadata.baseSyncedAt(schema.getName(),
                1_000L);
        synchronousStorageAdapter.save(model, metadata, lastSyncMetadata);

        // Enqueue an update in the mutation outbox
        assertTrue(mutationOutbox
                .enqueue(PendingMutation.update(model, schema))
                .blockingAwait(30, TimeUnit.SECONDS));

        // Start the mutation processor.
        mutationProcessor.startDrainingMutationOutbox();
        // Wait for the retry handler to be called.
        assertTrue(retryHandlerInvocationCount.await(300, TimeUnit.SECONDS));
        mutationProcessor.stopDrainingMutationOutbox();
    }
}
