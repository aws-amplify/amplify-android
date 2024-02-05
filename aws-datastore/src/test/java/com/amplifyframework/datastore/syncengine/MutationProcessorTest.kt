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
package com.amplifyframework.datastore.syncengine

import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.AmplifyException
import com.amplifyframework.api.graphql.GraphQLLocation
import com.amplifyframework.api.graphql.GraphQLOperation
import com.amplifyframework.api.graphql.GraphQLPathSegment
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.core.model.temporal.Temporal
import com.amplifyframework.datastore.DataStoreChannelEventName
import com.amplifyframework.datastore.DataStoreConfiguration
import com.amplifyframework.datastore.DataStoreConfigurationProvider
import com.amplifyframework.datastore.DataStoreConflictHandler.ConflictData
import com.amplifyframework.datastore.DataStoreConflictHandler.ConflictResolutionDecision
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.DataStoreException.GraphQLResponseException
import com.amplifyframework.datastore.appsync.AppSync
import com.amplifyframework.datastore.appsync.AppSyncMocking
import com.amplifyframework.datastore.appsync.ModelMetadata
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.datastore.storage.LocalStorageAdapter
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider
import com.amplifyframework.testmodels.commentsblog.BlogOwner
import com.amplifyframework.testutils.HubAccumulator
import com.amplifyframework.testutils.Latch
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

/**
 * Tests the [MutationProcessor].
 */
@RunWith(RobolectricTestRunner::class)
class MutationProcessorTest {
    private lateinit var schemaRegistry: SchemaRegistry
    private lateinit var synchronousStorageAdapter: SynchronousStorageAdapter
    private lateinit var mutationOutbox: MutationOutbox
    private lateinit var appSync: AppSync
    private lateinit var mutationProcessor: MutationProcessor
    private lateinit var configurationProvider: DataStoreConfigurationProvider

    /**
     * A [MutationProcessor] is being tested. To do so, we arrange mutations into
     * an [MutationOutbox]. Fake responses are returned from a mock [AppSync].
     * @throws AmplifyException When loading SchemaRegistry
     */
    @Before
    @Throws(AmplifyException::class)
    fun setup() {
        ShadowLog.stream = System.out
        schemaRegistry = SchemaRegistry.instance()
        schemaRegistry.register(
            setOf<Class<out Model>>(
                BlogOwner::class.java
            )
        )
        val localStorageAdapter: LocalStorageAdapter = SQLiteStorageAdapter.forModels(
            schemaRegistry,
            AmplifyModelProvider.getInstance()
        )
        synchronousStorageAdapter = SynchronousStorageAdapter.delegatingTo(localStorageAdapter)
        synchronousStorageAdapter.initialize(
            ApplicationProvider.getApplicationContext(),
            DataStoreConfiguration.defaults()
        )
        mutationOutbox = PersistentMutationOutbox(localStorageAdapter)
        val versionRepository = VersionRepository(localStorageAdapter)
        val merger = Merger(mutationOutbox, versionRepository, localStorageAdapter)
        appSync = Mockito.mock(AppSync::class.java)
        configurationProvider = Mockito.mock(
            DataStoreConfigurationProvider::class.java
        )
        val retryHandler = RetryHandler(0, Duration.ofMinutes(1).toMillis())
        mutationProcessor = MutationProcessor.builder()
            .merger(merger)
            .versionRepository(versionRepository)
            .schemaRegistry(schemaRegistry)
            .mutationOutbox(mutationOutbox)
            .appSync(appSync)
            .dataStoreConfigurationProvider(configurationProvider)
            .retryHandler(retryHandler)
            .build()
    }

    /**
     * Processing a mutation should publish current outbox status.
     */
    @Test
    fun outboxStatusIsPublishedToHubOnProcess() {
        val raphael = BlogOwner.builder()
            .name("Raphael Kim")
            .build()
        val schema = schemaRegistry.getModelSchemaForModelClass(
            BlogOwner::class.java
        )
        val createRaphael = PendingMutation.creation(raphael, schema)

        // Mock up a response from AppSync and enqueue a mutation.
        AppSyncMocking.create(appSync).mockSuccessResponse(raphael)
        assertTrue(
            mutationOutbox.enqueue(createRaphael)
                .blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        )

        // Start listening for publication events.
        // outbox should be empty after processing its only mutation
        val statusAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, TestHubEventFilters.isOutboxEmpty(true), 1)
                .start()

        // Start draining the outbox which has one mutation enqueued,
        // and make sure that outbox status is published to hub.
        mutationProcessor.startDrainingMutationOutbox()
        statusAccumulator.await()
    }

    /**
     * Tests the [MutationProcessor.startDrainingMutationOutbox]. After this method
     * is called, any content in the [MutationOutbox] should be published via the [AppSync]
     * and then removed.
     * @throws DataStoreException On failure to interact with storage adapter during arrangement
     * and verification
     */
    @Test
    @Throws(DataStoreException::class)
    fun canDrainMutationOutbox() {
        // We will attempt to "sync" this model.
        val tony = BlogOwner.builder()
            .name("Tony Daniels")
            .build()
        synchronousStorageAdapter.save(tony)

        // Arrange a cooked response from AppSync.
        AppSyncMocking.create(appSync).mockSuccessResponse(tony)

        // Start listening for publication events.
        val accumulator =
            HubAccumulator.create(HubChannel.DATASTORE, TestHubEventFilters.isProcessed(tony), 1)
                .start()
        val schema = schemaRegistry.getModelSchemaForModelClass(
            BlogOwner::class.java
        )
        val createTony = PendingMutation.creation(tony, schema)
        assertTrue(
            mutationOutbox.enqueue(createTony)
                .blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        )

        // Act! Start draining the outbox.
        mutationProcessor.startDrainingMutationOutbox()

        // Assert: the event was published
        assertEquals(1, accumulator.await().size.toLong())

        // And that it is no longer in the outbox.
        assertFalse(
            hasPendingMutation(
                tony,
                tony.javaClass.simpleName
            )
        )

        // And that it was passed to AppSync for publication.
        Mockito.verify(appSync).create(
            ArgumentMatchers.eq(tony),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()
        )
    }

    /**
     * If the AppSync response to the mutation contains a ConflictUnhandled
     * error in the GraphQLResponse error list, then the user-provided
     * conflict handler should be invoked.
     * @throws DataStoreException On failure to obtain configuration from the provider
     * @throws AmplifyException On failure to build [ModelSchema]
     */
    @Test
    @Throws(AmplifyException::class)
    fun conflictHandlerInvokedForUnhandledConflictError() {
        // Arrange a user-provided conflict handler.
        val handlerInvocationsRemainingCount = CountDownLatch(1)
        Mockito.`when`(configurationProvider.configuration)
            .thenReturn(
                DataStoreConfiguration.builder()
                    .conflictHandler { _: ConflictData<out Model>, _: Consumer<ConflictResolutionDecision<out Model>> ->
                        handlerInvocationsRemainingCount.countDown()
                    }
                    .build()
            )

        // Save a model, its metadata, and its last sync data.
        val model = BlogOwner.builder()
            .name("Exceptional Blogger")
            .build()
        val metadata = ModelMetadata(
            model.modelName + "|" + model.primaryKeyString, false, 1,
            Temporal.Timestamp.now()
        )
        val schema = schemaRegistry.getModelSchemaForModelClass(
            BlogOwner::class.java
        )
        val lastSyncMetadata = LastSyncMetadata.baseSyncedAt<Model>(schema.name, 1000L)
        synchronousStorageAdapter.save(model, metadata, lastSyncMetadata)

        // Enqueue an update in the mutation outbox
        assertTrue(
            mutationOutbox
                .enqueue(PendingMutation.update(model, schema))
                .blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        )

        // Fields that represent the "server's" understanding of the model state
        val serverModelData: MutableMap<String, Any> = HashMap()
        serverModelData["id"] = model.id
        serverModelData["name"] = "Server blogger name"
        serverModelData["_version"] = 1
        serverModelData["_deleted"] = false
        serverModelData["_lastChangedAt"] = 1000

        // When AppSync receives that update, have it respond
        // with a ConflictUnhandledError.
        val message = "Conflict resolver rejects mutation."
        val paths = listOf(GraphQLPathSegment("updateBlogOwner"))
        val locations = listOf(GraphQLLocation(2, 3))
        val extensions: MutableMap<String, Any> = HashMap()
        extensions["errorType"] = "ConflictUnhandled"
        extensions["data"] = serverModelData
        val error = GraphQLResponse.Error(message, locations, paths, extensions)
        AppSyncMocking.update(appSync).mockErrorResponse(model, 1, error)

        // Start the mutation processor.
        mutationProcessor.startDrainingMutationOutbox()

        // Wait for the conflict handler to be called.
        Latch.await(handlerInvocationsRemainingCount)
        mutationProcessor.stopDrainingMutationOutbox()
    }

    /**
     * If the AppSync response to the mutation contains not-empty GraphQLResponse error
     * list without any ConflictUnhandled error, then
     * [DataStoreChannelEventName.OUTBOX_MUTATION_FAILED] event is published via Hub.
     * @throws DataStoreException On failure to save model and metadata
     */
    @Test
    @Throws(DataStoreException::class)
    fun hubEventPublishedForPublicationError() {
        // Save a model, its metadata, and its last sync data.
        val model = BlogOwner.builder()
            .name("Average Joe")
            .build()
        val metadata = ModelMetadata(
            model.modelName + "|" + model.primaryKeyString, false, 1,
            Temporal.Timestamp.now()
        )
        val schema = schemaRegistry.getModelSchemaForModelClass(
            BlogOwner::class.java
        )
        synchronousStorageAdapter.save(model, metadata)

        // Enqueue an update in the mutation outbox
        assertTrue(
            mutationOutbox
                .enqueue(PendingMutation.update(model, schema))
                .blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        )

        // When AppSync receives that update, have it respond with an error.
        AppSyncMocking.update(appSync).mockErrorResponse(model, 1)

        // Start listening for publication events.
        val errorAccumulator = HubAccumulator.create(
            HubChannel.DATASTORE,
            DataStoreChannelEventName.OUTBOX_MUTATION_FAILED,
            1
        ).start()

        // Start the mutation processor and wait for hub event.
        mutationProcessor.startDrainingMutationOutbox()
        errorAccumulator.await()
    }

    /**
     * If error is caused by AppSync response, then the mutation outbox continues to
     * drain without getting blocked.
     * @throws DataStoreException On failure to save models
     */
    @Test
    @Throws(DataStoreException::class)
    fun canDrainMutationOutboxOnPublicationError() {
        val schema = schemaRegistry.getModelSchemaForModelClass(
            BlogOwner::class.java
        )

        // We will attempt to "sync" 10 models.
        val maxAttempts = 10
        for (attempt in 0 until maxAttempts) {
            val model = BlogOwner.builder()
                .name("Blogger #$attempt")
                .build()
            synchronousStorageAdapter.save(model)

            // Every other model triggers an AppSync error response.
            if (attempt % 2 == 0) {
                AppSyncMocking.create(appSync).mockErrorResponse(model)
            } else {
                AppSyncMocking.create(appSync).mockSuccessResponse(model)
            }

            // Enqueue a creation in the mutation outbox
            assertTrue(
                mutationOutbox
                    .enqueue(PendingMutation.creation(model, schema))
                    .blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            )
        }

        // Start listening for Mutation Outbox Empty event.
        val accumulator = HubAccumulator.create(
            HubChannel.DATASTORE,
            TestHubEventFilters.isOutboxEmpty(true),
            1
        ).start()

        // Start draining the outbox.
        mutationProcessor.startDrainingMutationOutbox()
        accumulator.await()
    }

    /**
     * If an error is caused by AppSync response, then the error handler gets invoked.
     * @throws DataStoreException On failure to save models
     */
    @Test
    @Throws(DataStoreException::class)
    fun callsErrorHandlerOnError() {
        val errorHandlerInvocationsLatch = CountDownLatch(1)
        Mockito.`when`(configurationProvider.configuration)
            .thenReturn(
                DataStoreConfiguration.builder()
                    .errorHandler { ignore: DataStoreException? -> errorHandlerInvocationsLatch.countDown() }
                    .build()
            )
        val schema = schemaRegistry.getModelSchemaForModelClass(
            BlogOwner::class.java
        )
        val model = BlogOwner.builder()
            .name("Blogger #1")
            .build()
        synchronousStorageAdapter.save(model)
        val error = GraphQLResponseException(
            "Some exception.", emptyList()
        )
        AppSyncMocking.create(appSync).mockResponseFailure(model, error)

        // Enqueue a creation in the mutation outbox
        assertTrue(
            mutationOutbox
                .enqueue(PendingMutation.creation(model, schema))
                .blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        )

        // Start listening for Outbox Mutation Failed event.
        val accumulator = HubAccumulator.create(
            HubChannel.DATASTORE,
            DataStoreChannelEventName.OUTBOX_MUTATION_FAILED,
            1
        ).start()

        // Start draining the outbox.
        mutationProcessor.startDrainingMutationOutbox()
        accumulator.await()
        try {
            assertTrue(
                "Error handler wasn't invoked",
                errorHandlerInvocationsLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            )
        } catch (exception: InterruptedException) {
            fail(exception.message)
        }
    }

    /**
     * If the AppSync response to the mutation contains a retry able
     * error in the GraphQLResponse error list, then the request is retried until successful.
     *
     * @throws InterruptedException Latch
     * @throws AmplifyException   On failure to build [ModelSchema]
     */
    @Test
    @Throws(AmplifyException::class, InterruptedException::class)
    fun retryStrategyAppliedAfterRecoverableError() {
        val retryHandlerInvocationCount = CountDownLatch(2)
        // Save a model, its metadata, and its last sync data.
        val model = BlogOwner.builder()
            .name("Exceptional Blogger")
            .build()
        val metadata = ModelMetadata(
            model.modelName + "|" + model.primaryKeyString, false, 1,
            Temporal.Timestamp.now()
        )
        Mockito.doAnswer { invocation: InvocationOnMock ->
            val indexOfResponseConsumer = 5
            val onError =
                invocation.getArgument<Consumer<DataStoreException>>(indexOfResponseConsumer)
            retryHandlerInvocationCount.countDown()
            onError.accept(DataStoreException("Error", "Retryable error."))
            Mockito.mock<GraphQLOperation<*>>(GraphQLOperation::class.java)
        }.doAnswer { invocation: InvocationOnMock ->
            // When mutate is called on the appsync for the second time success response is returned
            val indexOfResponseConsumer = 4
            val onResponse =
                invocation.getArgument<Consumer<GraphQLResponse<ModelWithMetadata<BlogOwner>>>>(
                    indexOfResponseConsumer
                )
            val modelMetadata = ModelMetadata(
                model.id, false, 1,
                Temporal.Timestamp.now()
            )
            val modelWithMetadata = ModelWithMetadata(
                model,
                modelMetadata
            )
            retryHandlerInvocationCount.countDown()
            onResponse.accept(GraphQLResponse(modelWithMetadata, emptyList()))
            Mockito.mock<GraphQLOperation<*>>(GraphQLOperation::class.java)
        }.`when`<AppSync?>(appSync).update<BlogOwner>(
            ArgumentMatchers.any<BlogOwner>(),
            ArgumentMatchers.any<ModelSchema>(ModelSchema::class.java),
            ArgumentMatchers.anyInt(),
            ArgumentMatchers.any<QueryPredicate>(QueryPredicate::class.java),
            ArgumentMatchers.any<Consumer<GraphQLResponse<ModelWithMetadata<BlogOwner>>>>(),
            ArgumentMatchers.any<Consumer<DataStoreException>>()
        )
        val schema = schemaRegistry.getModelSchemaForModelClass(
            BlogOwner::class.java
        )
        val lastSyncMetadata = LastSyncMetadata.baseSyncedAt<Model>(
            schema.name,
            1000L
        )
        synchronousStorageAdapter.save(model, metadata, lastSyncMetadata)

        // Enqueue an update in the mutation outbox
        assertTrue(
            mutationOutbox
                .enqueue(PendingMutation.update(model, schema))
                .blockingAwait(30, TimeUnit.SECONDS)
        )

        // Start the mutation processor.
        mutationProcessor.startDrainingMutationOutbox()
        // Wait for the retry handler to be called.
        assertTrue(retryHandlerInvocationCount.await(300, TimeUnit.SECONDS))
        mutationProcessor.stopDrainingMutationOutbox()
    }

    private fun <T : Model> hasPendingMutation(model: T, modelClass: String): Boolean {
        val results = mutationOutbox.fetchPendingMutations(listOf(model), modelClass, true)
        return results.contains(model.primaryKeyString)
    }

    companion object {
        private const val TIMEOUT_SECONDS: Long = 5
    }
}
