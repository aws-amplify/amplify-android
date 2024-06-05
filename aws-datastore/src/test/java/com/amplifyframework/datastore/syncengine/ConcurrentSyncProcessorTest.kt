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
package com.amplifyframework.datastore.syncengine

import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.core.model.temporal.Temporal
import com.amplifyframework.datastore.DataStoreConfiguration
import com.amplifyframework.datastore.DataStoreConfigurationProvider
import com.amplifyframework.datastore.DataStoreErrorHandler
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.appsync.AppSync
import com.amplifyframework.datastore.appsync.AppSyncMocking
import com.amplifyframework.datastore.appsync.ModelMetadata
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.datastore.model.SystemModelsProviderFactory
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter
import com.amplifyframework.testmodels.flat.AmplifyModelProvider
import com.amplifyframework.testmodels.flat.Model1
import com.amplifyframework.testmodels.flat.Model2
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.observers.TestObserver
import java.util.concurrent.TimeUnit
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlin.random.Random
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

/**
 * Tests the [SyncProcessor] with concurrency enabled.
 */
@RunWith(RobolectricTestRunner::class)
class ConcurrentSyncProcessorTest {
    private val appSync = mock(AppSync::class.java) // using Mockito due to existing AppSyncMocking setup
    private val errorHandler = mockk<DataStoreErrorHandler>(relaxed = true)

    private lateinit var modelProvider: ModelProvider
    private lateinit var storageAdapter: SynchronousStorageAdapter

    private lateinit var syncProcessor: SyncProcessor

    @Before
    fun setup() {
        modelProvider = AmplifyModelProvider.getInstance()
        val schemaRegistry = SchemaRegistry.instance().apply {
            clear()
            register(modelProvider.models())
        }

        val dataStoreConfiguration = DataStoreConfiguration.builder()
            .errorHandler(errorHandler)
            .syncInterval(1, TimeUnit.MINUTES)
            .syncPageSize(100)
            .syncMaxRecords(1000)
            .syncMaxConcurrentModels(3)
            .build()

        val sqliteStorageAdapter = SQLiteStorageAdapter.forModels(
            schemaRegistry,
            modelProvider
        )

        storageAdapter = SynchronousStorageAdapter.delegatingTo(sqliteStorageAdapter).apply {
            initialize(ApplicationProvider.getApplicationContext(), dataStoreConfiguration)
        }

        val syncTimeRegistry = SyncTimeRegistry(sqliteStorageAdapter)
        val mutationOutbox: MutationOutbox = PersistentMutationOutbox(sqliteStorageAdapter)
        val versionRepository = VersionRepository(sqliteStorageAdapter)
        val merger = Merger(mutationOutbox, versionRepository, sqliteStorageAdapter)

        val dataStoreConfigurationProvider = DataStoreConfigurationProvider { dataStoreConfiguration }

        this.syncProcessor = SyncProcessor.builder()
            .modelProvider(modelProvider)
            .schemaRegistry(schemaRegistry)
            .syncTimeRegistry(syncTimeRegistry)
            .appSync(appSync)
            .merger(merger)
            .dataStoreConfigurationProvider(dataStoreConfigurationProvider)
            .queryPredicateProvider(
                QueryPredicateProvider(dataStoreConfigurationProvider).apply {
                    resolvePredicates()
                }
            )
            .retryHandler(RetryHandler())
            .isSyncRetryEnabled(false)
            .build()
    }

    /**
     * Test Cleanup.
     * @throws DataStoreException On storage adapter terminate failure
     */
    @After
    fun tearDown() {
        storageAdapter.terminate()
    }

    @Test
    fun `sync with concurrency`() {
        // Arrange a subscription to the storage adapter. We're going to watch for changes.
        // We expect to see content here as a result of the SyncProcessor applying updates.
        val adapterObserver = storageAdapter.observe().test()

        // Arrange: return some responses for the sync() call on the RemoteModelState
        val configurator = AppSyncMocking.sync(appSync)

        val model1 = Model1.builder().name("M1_1").build()
        val model1Metadata = ModelMetadata(model1.id, null, Random.nextInt(), Temporal.Timestamp.now())
        val model1WithMetadata = ModelWithMetadata(model1, model1Metadata)
        val expectedModel1Response = mutableListOf(model1WithMetadata)

        val model2 = Model2.builder().name("M2_1").build()
        val model2Metadata = ModelMetadata(model2.id, null, Random.nextInt(), Temporal.Timestamp.now())
        val model2WithMetadata = ModelWithMetadata(model2, model2Metadata)
        val expectedModel2Response = mutableListOf(model2WithMetadata)

        val allExpectedModels = expectedModel1Response + expectedModel2Response

        configurator.mockSuccessResponse(Model1::class.java, model1WithMetadata)
        configurator.mockSuccessResponse(Model2::class.java, model2WithMetadata)

        // Act: Call hydrate, and await its completion - assert it completed without error
        val hydrationObserver = TestObserver.create<ModelWithMetadata<out Model>>()
        syncProcessor.hydrate().subscribe(hydrationObserver)

        assertTrue(hydrationObserver.await(2, TimeUnit.SECONDS))
        hydrationObserver.assertNoErrors()
        hydrationObserver.assertComplete()

        // Since hydrate() completed, the storage adapter observer should see some values.
        // The number should match expectedResponseItems * 2 (1 for model, 1 for metadata)
        // Additionally, there should be 1 LastSyncMetadata record for each model in the provider
        adapterObserver.awaitCount(allExpectedModels.size * 2 + AmplifyModelProvider.getInstance().models().size)

        // Validate the changes emitted from the storage adapter's observe(). Sorted to compare lists
        assertEquals(
            allExpectedModels.flatMap {
                listOf(it.model, it.syncMetadata)
            }.sortedBy { it.primaryKeyString },
            adapterObserver.values()
                .map { it.item() }
                .filter { !LastSyncMetadata::class.java.isAssignableFrom(it.javaClass) }
                .sortedBy { it.primaryKeyString }
        )

        // Lastly: validate the current contents of the storage adapter.
        val itemsInStorage = storageAdapter.query(modelProvider)
        assertEquals(allExpectedModels.size, itemsInStorage.size)

        val expectedModels = allExpectedModels.map { it.model }.sortedBy { it.primaryKeyString }
        val expectedMetadata = allExpectedModels.map { it.syncMetadata }.sortedBy { it.primaryKeyString }
        // system model size excluding metadata should = number of models and
        // + 1 (LastSyncMetadata for each + PersistentModelVersion)
        val expectedSystemModelsSize = modelProvider.models().size + 1

        val actualModels = itemsInStorage
            .filter { !LastSyncMetadata::class.java.isAssignableFrom(it.javaClass) }
            .sortedBy { it.primaryKeyString }
        val actualMetadata =
            storageAdapter.query(SystemModelsProviderFactory.create())
                .filter { ModelMetadata::class.java.isAssignableFrom(it.javaClass) }
                .sortedBy { it.primaryKeyString }

        val actualSystemModels = storageAdapter.query(SystemModelsProviderFactory.create()).filter {
            !ModelMetadata::class.java.isAssignableFrom(it.javaClass)
        }

        assertEquals(expectedModels, actualModels)
        assertEquals(expectedMetadata, actualMetadata)
        assertEquals(expectedSystemModelsSize, actualSystemModels.size)

        adapterObserver.dispose()
        hydrationObserver.dispose()
    }

    @Test
    fun `sync with concurrency continues when single model fails`() {
        // Arrange a subscription to the storage adapter. We're going to watch for changes.
        // We expect to see content here as a result of the SyncProcessor applying updates.
        val adapterObserver = storageAdapter.observe().test()

        // Arrange: return some responses for the sync() call on the RemoteModelState
        val configurator = AppSyncMocking.sync(appSync)

        val expectedModel1Exception = DataStoreException("Failed to sync Model1", "Failed to sync Model1")

        val model2Item1 = Model2.builder().name("M2_1").build()
        val model2Item1Metadata = ModelMetadata(model2Item1.id, null, Random.nextInt(), Temporal.Timestamp.now())
        val model2Item1WithMetadata = ModelWithMetadata(model2Item1, model2Item1Metadata)
        val expectedModel2Response1 = mutableListOf(model2Item1WithMetadata)

        val model2Item2 = Model2.builder().name("M2_2").build()
        val model2Item2Metadata = ModelMetadata(model2Item2.id, null, Random.nextInt(), Temporal.Timestamp.now())
        val model2Item2WithMetadata = ModelWithMetadata(model2Item2, model2Item2Metadata)
        val expectedModel2Response2 = mutableListOf(model2Item2WithMetadata)

        val allExpectedModels = expectedModel2Response1 + expectedModel2Response2

        configurator.mockFailure<Model1>(expectedModel1Exception)
        configurator.mockSuccessResponse(Model2::class.java, null, "page2", model2Item1WithMetadata)
        configurator.mockSuccessResponse(Model2::class.java, "page2", null, model2Item2WithMetadata)

        // Act: Call hydrate, and await its completion - assert it completed without error
        val hydrationObserver = TestObserver.create<ModelWithMetadata<out Model>>()
        syncProcessor.hydrate().subscribe(hydrationObserver)

        assertTrue(hydrationObserver.await(2, TimeUnit.SECONDS))
        hydrationObserver.assertError { it == expectedModel1Exception }
        verify {
            errorHandler.accept(
                DataStoreException(
                    "Initial cloud sync failed for Model1.",
                    expectedModel1Exception,
                    "Check your internet connection."
                )
            )
        }

        // Since hydrate() completed, the storage adapter observer should see some values.
        // The number should match expectedResponseItems * 2 (1 for model, 1 for metadata)
        // Additionally, there should be 1 LastSyncMetadata for model2 sync success.
        // Model1 will not have a last sync record due to failure
        adapterObserver.awaitCount(allExpectedModels.size * 2 + 1)

        // Validate the changes emitted from the storage adapter's observe(). Sorted to compare lists
        assertEquals(
            allExpectedModels.flatMap {
                listOf(it.model, it.syncMetadata)
            }.sortedBy { it.primaryKeyString },
            adapterObserver.values()
                .map { it.item() }
                .filter { !LastSyncMetadata::class.java.isAssignableFrom(it.javaClass) }
                .sortedBy { it.primaryKeyString }
        )

        // Lastly: validate the current contents of the storage adapter.
        val itemsInStorage = storageAdapter.query(modelProvider)
        assertEquals(allExpectedModels.size, itemsInStorage.size)

        val expectedModels = allExpectedModels.map { it.model }.sortedBy { it.primaryKeyString }
        val expectedMetadata = allExpectedModels.map { it.syncMetadata }.sortedBy { it.primaryKeyString }
        // LastSyncMetadata and PersistentModel version for Model2 success
        val expectedSystemModelsSize = 2

        val actualModels = itemsInStorage
            .filter { !LastSyncMetadata::class.java.isAssignableFrom(it.javaClass) }
            .sortedBy { it.primaryKeyString }
        val actualMetadata =
            storageAdapter.query(SystemModelsProviderFactory.create())
                .filter { ModelMetadata::class.java.isAssignableFrom(it.javaClass) }
                .sortedBy { it.primaryKeyString }

        val actualSystemModels = storageAdapter.query(SystemModelsProviderFactory.create()).filter {
            !ModelMetadata::class.java.isAssignableFrom(it.javaClass)
        }

        assertEquals(expectedModels, actualModels)
        assertEquals(expectedMetadata, actualMetadata)
        assertEquals(expectedSystemModelsSize, actualSystemModels.size)

        adapterObserver.dispose()
        hydrationObserver.dispose()
    }
}
