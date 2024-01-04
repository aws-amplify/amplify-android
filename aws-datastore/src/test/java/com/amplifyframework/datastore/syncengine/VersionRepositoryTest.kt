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
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.core.model.temporal.Temporal
import com.amplifyframework.datastore.DataStoreConfiguration
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.appsync.ModelMetadata
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider
import com.amplifyframework.testmodels.commentsblog.BlogOwner
import java.util.Locale
import java.util.Random
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests the [VersionRepository].
 */
@RunWith(RobolectricTestRunner::class)
class VersionRepositoryTest {
    private lateinit var storageAdapter: SynchronousStorageAdapter
    private lateinit var versionRepository: VersionRepository

    @Before
    @Throws(DataStoreException::class)
    fun setup() {
        val sqliteStorageAdapter = SQLiteStorageAdapter.forModels(
            SchemaRegistry.instance(),
            AmplifyModelProvider.getInstance()
        )
        storageAdapter = SynchronousStorageAdapter.delegatingTo(sqliteStorageAdapter)
        storageAdapter.initialize(
            ApplicationProvider.getApplicationContext(),
            DataStoreConfiguration.defaults()
        )
        versionRepository = VersionRepository(sqliteStorageAdapter)
    }

    @After
    @Throws(DataStoreException::class)
    fun tearDown() {
        storageAdapter.terminate()
    }

    /**
     * When you try to get a model version, but there's no metadata for that model,
     * this should fail with an [DataStoreException].
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(InterruptedException::class)
    fun emitsErrorForNoMetadataInRepo() {
        // Arrange: no metadata is in the repo.
        val blogOwner = BlogOwner.builder()
            .name("Jameson Williams")
            .build()
        // Note that this line is NOT executed in arrangement.
        // ModelMetadata metadata = new ModelMetadata(blogOwner.getId(), false, 1, Time.now());
        // putInStore(blogOwner blogOwner);

        // Act: try to lookup the metadata. Is it going to work? Duh.
        val observer = versionRepository.findModelVersion(blogOwner).test()
        Assert.assertTrue(observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS))

        // Assert: this failed. There was no version available.
        observer.assertError { error: Throwable ->
            if (error !is DataStoreException) {
                return@assertError false
            }
            val expectedMessage = String.format(
                Locale.US,
                "Wanted 1 metadata for item with id = %s, but had 0.",
                blogOwner.id
            )
            expectedMessage == error.message
        }
    }

    /**
     * When you try to get the version for a model, and there is metadata for the model
     * in the DataStore, BUT the version info is not populated, this should return an
     * [DataStoreException].
     * @throws DataStoreException
     * NOT EXPECTED. This happens on failure to arrange data before test action.
     * The expected DataStoreException is communicated via callback, not thrown
     * on the calling thread. It's a different thing than this.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(DataStoreException::class, InterruptedException::class)
    fun emitsErrorWhenMetadataHasNullVersion() {
        // Arrange a model an metadata into the store, but the metadtaa doesn't contain a valid version
        val blogOwner = BlogOwner.builder()
            .name("Jameson")
            .build()
        val metadata = ModelMetadata(
            blogOwner.modelName + "|" + blogOwner.id, null,
            null, null
        )
        storageAdapter.save(blogOwner, metadata)

        // Act: try to get the version.
        val observer = versionRepository.findModelVersion(blogOwner).test()
        Assert.assertTrue(observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS))

        // Assert: the single emitted a DataStoreException.
        observer.assertError { error: Throwable ->
            if (error !is DataStoreException) {
                return@assertError false
            }
            val expectedMessage = String.format(
                Locale.US,
                "Metadata for item with id = %s had null version.",
                blogOwner.id
            )
            expectedMessage == error.message
        }
    }

    /**
     * When there is metadata for a model in the store, and that metadata includes a version -
     * for heaven's sake, man - do please emit the dang thing.
     * @throws DataStoreException On failure to arrange data into store
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(DataStoreException::class, InterruptedException::class)
    fun emitsSuccessWithValueWhenVersionInStore() {
        // Arrange versioning info into the store.
        val owner = BlogOwner.builder()
            .name("Jameson")
            .build()
        val maxRandomVersion = 1000
        val expectedVersion = Random().nextInt(maxRandomVersion)
        storageAdapter.save(
            ModelMetadata(
                owner.modelName + "|" + owner.id,
                false,
                expectedVersion,
                Temporal.Timestamp.now()
            )
        )

        // Act! Try to obtain it via the Versioning Repository.
        val observer = versionRepository.findModelVersion(owner).test()
        Assert.assertTrue(observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS))

        // Assert: we got a version.
        observer
            .assertNoErrors()
            .assertComplete()
            .assertValue(expectedVersion)
    }

    @Test
    fun fetchModelVersionsReturnsExpectedValues() = runTest {
        val owner1 = BlogOwner.builder().name("Owner1").build()
        val owner1Metadata = ModelMetadata(owner1.modelName + "|" + owner1.id, false, 5, null)
        val owner1ModelWithMetadata = ModelWithMetadata(owner1, owner1Metadata)
        val owner2 = BlogOwner.builder().name("Owner2").build()
        val owner2Metadata = ModelMetadata(owner2.modelName + "|" + owner2.id, false, null, null)
        val owner2ModelWithMetadata = ModelWithMetadata(owner2, owner2Metadata)
        val owner3 = BlogOwner.builder().name("Owner3").build()
        val owner3Metadata = ModelMetadata(owner3.modelName + "|" + owner3.id, false, 10, null)
        storageAdapter.save(owner1Metadata)
        storageAdapter.save(owner2Metadata)
        storageAdapter.save(owner3Metadata)

        // Purposefully omitting model 3 to ensure it does not return in result
        val modelsWithMetadata = listOf(owner1ModelWithMetadata, owner2ModelWithMetadata)
        val result = versionRepository.fetchModelVersions(modelsWithMetadata)

        assertEquals(2, result.size)
        assertEquals(5, result[owner1Metadata.primaryKeyString])
        assertEquals(-1, result[owner2Metadata.primaryKeyString])
    }

    @Test
    // This test ensures the 950 chunking works
    fun fetchModelVersionReturnsMoreThan950() = runTest(timeout = 50.seconds) {
        val modelsWithMetadata = mutableListOf<ModelWithMetadata<BlogOwner>>()
        for (i in 0 until 1000) {
            val owner = BlogOwner.builder().name("Owner$i").build()
            val metadata = ModelMetadata(owner.modelName + "|" + owner.id, false, i, null)
            modelsWithMetadata.add(ModelWithMetadata(owner, metadata))
            storageAdapter.save(metadata)
        }

        val result = versionRepository.fetchModelVersions(modelsWithMetadata)

        assertEquals(1000, result.size)
        for (i in 0 until 1000) {
            assertEquals(i, result[modelsWithMetadata[i].syncMetadata.primaryKeyString])
        }
    }

    companion object {
        private val REASONABLE_WAIT_TIME = TimeUnit.SECONDS.toMillis(1)
    }
}
