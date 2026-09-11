/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.async.NoOpCancelable
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.datastore.DataStoreConfiguration
import com.amplifyframework.datastore.DataStoreConfigurationProvider
import com.amplifyframework.datastore.DataStoreErrorHandler
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.appsync.AppSync
import com.amplifyframework.datastore.appsync.AppSyncMocking
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider
import com.amplifyframework.testmodels.commentsblog.BlogOwner
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.mockk
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner

/**
 * Regression tests for the base-sync disposal race: when the subscriber is disposed while
 * a sync network call is in flight, a late error must be delivered through emitter.tryOnError so it is
 * dropped on the disposed emitter rather than crashing the app with an RxJava UndeliverableException.
 */
@RunWith(RobolectricTestRunner::class)
class SyncProcessorDisposalTest {
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

        val configuration = DataStoreConfiguration.builder()
            .errorHandler(errorHandler)
            .syncInterval(1, TimeUnit.MINUTES)
            .syncPageSize(1000)
            .syncMaxRecords(10_000)
            .syncMaxConcurrentModels(1)
            .build()

        val sqliteStorageAdapter = SQLiteStorageAdapter.forModels(schemaRegistry, modelProvider)
        storageAdapter = SynchronousStorageAdapter.delegatingTo(sqliteStorageAdapter).apply {
            initialize(ApplicationProvider.getApplicationContext(), configuration)
        }

        val configurationProvider = DataStoreConfigurationProvider { configuration }

        syncProcessor = SyncProcessor.builder()
            .modelProvider(modelProvider)
            .schemaRegistry(schemaRegistry)
            .syncTimeRegistry(SyncTimeRegistry(sqliteStorageAdapter))
            .appSync(appSync)
            .merger(
                Merger(
                    PersistentMutationOutbox(sqliteStorageAdapter),
                    VersionRepository(sqliteStorageAdapter),
                    sqliteStorageAdapter
                )
            )
            .dataStoreConfigurationProvider(configurationProvider)
            .queryPredicateProvider(
                QueryPredicateProvider(configurationProvider).apply { resolvePredicates() }
            )
            .retryHandler(RetryHandler())
            .isSyncRetryEnabled(false)
            .build()
    }

    @After
    fun tearDown() {
        storageAdapter.terminate()
    }

    @Test
    fun `failure delivered after dispose does not throw UndeliverableException`() {
        // Arrange: capture the failure callback without invoking it yet, keeping the sync in flight.
        val capturedOnFailure = AtomicReference<Consumer<DataStoreException>>()
        AppSyncMocking.sync(appSync)
        doAnswer { invocation ->
            capturedOnFailure.set(invocation.getArgument(2))
            NoOpCancelable()
        }.`when`(appSync).sync<BlogOwner>(any(), any(), any())

        val undelivered = CopyOnWriteArrayList<Throwable>()
        val previousHandler = RxJavaPlugins.getErrorHandler()
        RxJavaPlugins.setErrorHandler { undelivered.add(it) }
        try {
            // Arrange: run the sync, wait for the callback, then dispose the subscriber.
            val testObserver = syncProcessor.hydrate().test()
            verify(appSync, timeout(TIMEOUT_MS).atLeastOnce()).sync<BlogOwner>(any(), any(), any())
            val onFailure = capturedOnFailure.get().shouldNotBeNull()
            testObserver.dispose()

            // Act: deliver the failure to the now-disposed emitter.
            onFailure.accept(DataStoreException("Sync failed after stop.", "Expected in this race."))

            // Assert: no error reaches the global handler and the subscriber receives none.
            undelivered.shouldBeEmpty()
            testObserver.assertNoErrors()
        } finally {
            RxJavaPlugins.setErrorHandler(previousHandler)
        }
    }

    @Test
    fun `no-data response after dispose does not throw UndeliverableException`() {
        // Arrange: capture the response callback without invoking it yet, keeping the sync in flight.
        val capturedOnResponse =
            AtomicReference<Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<BlogOwner>>>>>()
        AppSyncMocking.sync(appSync)
        doAnswer { invocation ->
            capturedOnResponse.set(invocation.getArgument(1))
            NoOpCancelable()
        }.`when`(appSync).sync<BlogOwner>(any(), any(), any())

        val undelivered = CopyOnWriteArrayList<Throwable>()
        val previousHandler = RxJavaPlugins.getErrorHandler()
        RxJavaPlugins.setErrorHandler { undelivered.add(it) }
        try {
            // Arrange: run the sync, wait for the callback, then dispose the subscriber.
            val testObserver = syncProcessor.hydrate().test()
            verify(appSync, timeout(TIMEOUT_MS).atLeastOnce()).sync<BlogOwner>(any(), any(), any())
            val onResponse = capturedOnResponse.get().shouldNotBeNull()
            testObserver.dispose()

            // Act: deliver a no-data (errors-only) response to the now-disposed emitter.
            val noDataResponse = GraphQLResponse<PaginatedResult<ModelWithMetadata<BlogOwner>>>(
                null,
                listOf(GraphQLResponse.Error("Errors from AppSync after stop.", null, null, null))
            )
            onResponse.accept(noDataResponse)

            // Assert: no error reaches the global handler and the subscriber receives none.
            undelivered.shouldBeEmpty()
            testObserver.assertNoErrors()
        } finally {
            RxJavaPlugins.setErrorHandler(previousHandler)
        }
    }

    companion object {
        private const val TIMEOUT_MS = 10_000L
    }
}
