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

import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.AmplifyException
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.core.model.query.Where
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.core.model.temporal.Temporal
import com.amplifyframework.datastore.DataStoreConfiguration
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.appsync.ModelMetadata
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.datastore.storage.LocalStorageAdapter
import com.amplifyframework.datastore.storage.StorageItemChange
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider
import com.amplifyframework.testmodels.commentsblog.Blog
import com.amplifyframework.testmodels.commentsblog.BlogOwner
import com.amplifyframework.testutils.random.RandomString
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

/**
 * Tests the [Merger].
 */
@RunWith(RobolectricTestRunner::class)
class MergerTest {
    private lateinit var spiedStorageAdapter: LocalStorageAdapter
    private lateinit var storageAdapter: SynchronousStorageAdapter
    private lateinit var mutationOutbox: MutationOutbox
    private lateinit var merger: Merger

    /**
     * Sets up the test. A [Merger] is being tested. To construct one, several
     * intermediary objects are needed. A reference is held to a [MutationOutbox],
     * to arrange state. A [SynchronousStorageAdapter] is crated to facilitate
     * arranging model data into the [SQLiteStorageAdapter] which backs the various
     * components.
     */
    @Before
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
        spiedStorageAdapter = Mockito.spy(sqliteStorageAdapter)
        mutationOutbox = PersistentMutationOutbox(sqliteStorageAdapter)
        val versionRepository = VersionRepository(sqliteStorageAdapter)
        merger = Merger(mutationOutbox, versionRepository, sqliteStorageAdapter)
    }

    /**
     * Assume there is a item A in the store. Then, we try to merge
     * a mutation to delete item A. This should succeed. After the
     * merge, A should NOT be in the store anymore.
     * @throws DataStoreException On failure to arrange test data into store,
     * or on failure to query results for test assertions
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(DataStoreException::class, InterruptedException::class)
    fun mergeDeletionForExistingItem() {
        // Arrange: A blog owner, and some metadata about it, are in the store.
        val blogOwner = BlogOwner.builder()
            .name("Jameson")
            .build()
        val originalMetadata = ModelMetadata(blogOwner.id, false, 1, Temporal.Timestamp.now())
        storageAdapter.save(blogOwner, originalMetadata)
        // Just to be sure, our arrangement worked, and that thing is in there, right? Good.
        assertEquals(
            listOf(blogOwner),
            storageAdapter.query(
                BlogOwner::class.java
            )
        )

        // Act: merge a model deletion.
        val deletionMetadata = ModelMetadata(blogOwner.id, true, 2, Temporal.Timestamp.now())
        val observer = merger.merge(ModelWithMetadata(blogOwner, deletionMetadata)).test()
        assertTrue(observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS))
        observer.assertNoErrors().assertComplete()

        // Assert: the blog owner is no longer in the store.
        assertEquals(0, storageAdapter.query(BlogOwner::class.java).size.toLong())
    }

    /**
     * Assume there is NOT an item in the store. Then, we try to
     * merge a mutation to delete item A. This should succeed, since
     * there was no work to be performed (it was already deleted.) After
     * the merge, there should STILL be no matching item in the store.
     * @throws DataStoreException On failure to query results for assertions
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(DataStoreException::class, InterruptedException::class)
    fun mergeDeletionForNotExistingItem() {
        // Arrange, to start, there are no items matching the incoming deletion request.
        val blogOwner = BlogOwner.builder()
            .name("Jameson")
            .build()
        // Note that storageAdapter.save(...) does NOT happen!
        // storageAdapter.save(blogOwner, new ModelMetadata(blogOwner.getId(), false, 1, Time.now()));

        // Act: try to merge a deletion that refers to an item not in the store
        val deletionMetadata = ModelMetadata(blogOwner.id, true, 1, Temporal.Timestamp.now())
        val observer = merger.merge(ModelWithMetadata(blogOwner, deletionMetadata)).test()
        assertTrue(observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS))
        observer.assertNoErrors().assertComplete()

        // Assert: there is still nothing in the store.
        assertEquals(0, storageAdapter.query(BlogOwner::class.java).size.toLong())
    }

    /**
     * Assume there is NO item A. Then, we try to merge a save for a
     * item A. This should succeed, with A being in the store, at the end.
     * @throws DataStoreException On failure to query results for assertions
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(DataStoreException::class, InterruptedException::class)
    fun mergeSaveForNotExistingItem() {
        // Arrange: nothing in the store, to start.
        val blogOwner = BlogOwner.builder()
            .name("Jameson")
            .build()
        val metadata = ModelMetadata(
            blogOwner.modelName + "|" + blogOwner.id, false, 1,
            Temporal.Timestamp.now()
        )
        // Note that storageAdapter.save(...) is NOT called!
        // storageAdapter.save(blogOwner, metadata);

        // Act: merge a creation for an item
        val observer = merger.merge(ModelWithMetadata(blogOwner, metadata)).test()
        assertTrue(observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS))
        observer.assertNoErrors().assertComplete()

        // Assert: the item & its associated metadata are now in the store.
        assertEquals(
            listOf(blogOwner),
            storageAdapter.query(
                BlogOwner::class.java
            )
        )
        assertEquals(
            listOf(metadata),
            storageAdapter.query(
                ModelMetadata::class.java
            )
        )
    }

    /**
     * Assume there is an item A in the store. We try to merge a save for A.
     * This should succeed, and it should be treated as an update. After the merge,
     * A should have the updates from the merge.
     * @throws DataStoreException On failure to arrange data into store
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(DataStoreException::class, InterruptedException::class)
    fun mergeSaveForExistingItem() {
        // Arrange: an item is already in the store.
        val originalModel = BlogOwner.builder()
            .name("Jameson The Original")
            .build()
        val originalMetadata = ModelMetadata(
            originalModel.modelName + "|" + originalModel.id,
            false,
            1,
            Temporal.Timestamp.now()
        )
        storageAdapter.save(originalModel, originalMetadata)

        // Act: merge a save.
        val updatedModel = originalModel.copyOfBuilder()
            .name("Jameson The New and Improved")
            .build()
        val updatedMetadata =
            ModelMetadata(originalMetadata.resolveIdentifier(), false, 2, Temporal.Timestamp.now())
        val observer = merger.merge(ModelWithMetadata(updatedModel, updatedMetadata)).test()
        assertTrue(observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS))
        observer.assertComplete().assertNoErrors()

        // Assert: the *UPDATED* stuff is in the store, *only*.
        assertEquals(
            listOf(updatedModel),
            storageAdapter.query(
                BlogOwner::class.java
            )
        )
        assertEquals(
            listOf(updatedMetadata),
            storageAdapter.query(
                ModelMetadata::class.java
            )
        )
    }

    /**
     * When an item comes into the merger to be merged,
     * if there is a pending mutation in the outbox, for a model of the same ID,
     * then that item shall NOT be merged.
     * @throws DataStoreException On failure to arrange data into store
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     * @throws AmplifyException On failure to arrange model schema
     */
    @Test
    @Throws(AmplifyException::class, InterruptedException::class)
    fun itemIsNotMergedWhenOutboxHasPendingMutation() {
        // Arrange: some model with a well known ID exists on the system.
        // We pretend that the user has recently updated it via the DataStore update() API.
        val knownId = RandomString.string()
        val blogOwner = BlogOwner.builder()
            .name("Jameson")
            .id(knownId)
            .build()
        val localMetadata = ModelMetadata(blogOwner.id, false, 1, Temporal.Timestamp.now())
        storageAdapter.save(blogOwner, localMetadata)
        val schema = ModelSchema.fromModelClass(
            BlogOwner::class.java
        )
        val pendingMutation = PendingMutation.instance(
            blogOwner, schema, PendingMutation.Type.CREATE, QueryPredicates.all()
        )
        val enqueueObserver = mutationOutbox.enqueue(pendingMutation).test()
        enqueueObserver.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS)
        enqueueObserver.assertNoErrors().assertComplete()

        // Act: now, cloud sync happens, and the sync engine tries to apply an update
        // for the same model ID, into the store. According to the cloud, this same
        // item should be DELETED.
        val cloudMetadata = ModelMetadata(knownId, true, 2, Temporal.Timestamp.now())
        val mergeObserver = merger.merge(ModelWithMetadata(blogOwner, cloudMetadata)).test()
        mergeObserver.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS)
        mergeObserver.assertNoErrors().assertComplete()

        // Assert: the item is NOT deleted from the local store.
        // The original is still there.
        // Or in other words, the cloud data was NOT merged.
        val blogOwnersInStorage = storageAdapter.query(
            BlogOwner::class.java
        )
        assertEquals(1, blogOwnersInStorage.size.toLong())
        assertEquals(blogOwner, blogOwnersInStorage[0])
    }

    /**
     * When processing a mutation response, the pending mutation should be removed from the outbox, and the mutation
     * should be merged to local storage.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     * @throws AmplifyException On failure to arrange model schema
     */
    @Test
    @Throws(AmplifyException::class, InterruptedException::class)
    fun itemIsMergedAfterPendingMutationRemovedFromOutbox() {
        // Arrange: some model with a well known ID exists on the system.
        // We pretend that the user has recently updated it via the DataStore update() API.
        val knownId = RandomString.string()
        val blogOwner = BlogOwner.builder()
            .name("Jameson")
            .id(knownId)
            .build()
        val localMetadata = ModelMetadata(blogOwner.id, false, 1, Temporal.Timestamp.now())
        storageAdapter.save(blogOwner, localMetadata)
        val schema = ModelSchema.fromModelClass(
            BlogOwner::class.java
        )
        val pendingMutation = PendingMutation.instance(
            blogOwner, schema, PendingMutation.Type.DELETE, QueryPredicates.all()
        )
        val enqueueObserver = mutationOutbox.enqueue(pendingMutation).test()
        enqueueObserver.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS)
        enqueueObserver.assertNoErrors().assertComplete()

        // Act: now, cloud sync happens, and the sync engine tries to apply an update
        // for the same model ID, into the store. According to the cloud, this same
        // item should be DELETED.
        val cloudMetadata = ModelMetadata(knownId, true, 2, Temporal.Timestamp.now())
        val observer = mutationOutbox.remove(pendingMutation.mutationId)
            .andThen(merger.merge(ModelWithMetadata(blogOwner, cloudMetadata)))
            .test()
        observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS)
        observer.assertNoErrors().assertComplete()

        // Assert: the item IS deleted from the local store.
        // Or in other words, the cloud data WAS merged.
        val blogOwnersInStorage = storageAdapter.query(
            BlogOwner::class.java
        )
        assertEquals(0, blogOwnersInStorage.size.toLong())
    }

    /**
     * An incoming mutation whose model has a LOWER version than an already existing model
     * shall be rejected from the merger.
     * @throws DataStoreException On failure interacting with local store during test arrange/verify.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    @Throws(AmplifyException::class, InterruptedException::class)
    fun itemWithLowerVersionIsNotMerged() {
        // Arrange a model and metadata into storage.
        val existingModel = BlogOwner.builder()
            .name("Cornelius Daniels")
            .build()
        val existingMetadata = ModelMetadata(
            existingModel.id, false, 55,
            Temporal.Timestamp.now()
        )
        storageAdapter.save(existingModel, existingMetadata)

        // Act: try to merge, but specify a LOWER version.
        val incomingModel = existingModel.copyOfBuilder()
            .name("Cornelius Daniels, but woke af, now.")
            .build()
        val lowerVersionMetadata =
            ModelMetadata(incomingModel.id, false, 33, Temporal.Timestamp.now())
        val modelWithLowerVersionMetadata = ModelWithMetadata(existingModel, lowerVersionMetadata)
        val mergeObserver = merger.merge(modelWithLowerVersionMetadata).test()
        mergeObserver.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS)
        mergeObserver.assertNoErrors().assertComplete()

        // Assert: Joey is still the same old Joey.
        assertEquals(
            listOf(existingModel),
            storageAdapter.query(
                BlogOwner::class.java
            )
        )

        // And his metadata is the still the same.
        assertEquals(
            listOf(existingMetadata),
            storageAdapter.query(
                ModelMetadata::class.java,
                Where.identifier(
                    ModelMetadata::class.java, existingModel.primaryKeyString
                )
            )
        )
    }

    /**
     * If the incoming change has the SAME version as the data currently in the DB, we refuse to update it.
     * The user may have updated the data locally via the DataStore API. So we would clobber it.
     * The version must be strictly HIGHER than the current version, in order for the merge to succeed.
     * @throws DataStoreException On failure to interact with storage during arrange/verify
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    @Throws(AmplifyException::class, InterruptedException::class)
    fun itemWithSameVersionIsNotMerged() {
        // Arrange a model and metadata into storage.
        val existingModel = BlogOwner.builder()
            .name("Cornelius Daniels")
            .build()
        val existingMetadata = ModelMetadata(
            existingModel.modelName + "|" + existingModel.id,
            false,
            55,
            Temporal.Timestamp.now()
        )
        storageAdapter.save(existingModel, existingMetadata)

        // Act: try to merge, but specify a LOWER version.
        val incomingModel = existingModel.copyOfBuilder()
            .name("Cornelius Daniels, but woke af, now.")
            .build()
        val lowerVersionMetadata =
            ModelMetadata(incomingModel.id, false, 33, Temporal.Timestamp.now())
        val modelWithLowerVersionMetadata = ModelWithMetadata(incomingModel, lowerVersionMetadata)
        val mergeObserver = merger.merge(modelWithLowerVersionMetadata).test()
        mergeObserver.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS)
        mergeObserver.assertNoErrors().assertComplete()

        // Assert: Joey is still the same old Joey.
        val actualBlogOwners = storageAdapter.query(
            BlogOwner::class.java
        )
        assertEquals(1, actualBlogOwners.size.toLong())
        assertEquals(existingModel, actualBlogOwners[0])

        // And his metadata is the still the same.
        assertEquals(
            listOf(existingMetadata),
            storageAdapter.query(
                ModelMetadata::class.java,
                Where.identifier(
                    ModelMetadata::class.java,
                    existingModel.modelName + "|" + existingModel.id
                )
            )
        )
    }

    /**
     * Gray-box, we know that "no version" evaluates to a version of 0.
     * So, this test should always behave like [.itemWithLowerVersionIsNotMerged].
     * But, it the inputs to the system are technically different, so it is
     * a distinct test in terms of system input/output.
     * @throws DataStoreException On failure to interact with storage during arrange/verification.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer.
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    @Throws(AmplifyException::class, InterruptedException::class)
    fun itemWithoutVersionIsNotMerged() {
        // Arrange a model and metadata into storage.
        val existingModel = BlogOwner.builder()
            .name("Cornelius Daniels")
            .build()
        val existingMetadata = ModelMetadata(
            existingModel.id, false,
            1, Temporal.Timestamp.now()
        )
        storageAdapter.save(existingModel, existingMetadata)

        // Act: try to merge, but don't specify a version in the metadata being used to merge.
        val incomingModel = existingModel.copyOfBuilder()
            .name("Cornelius Daniels, but woke af, now.")
            .build()
        val metadataWithoutVersion = ModelMetadata(
            incomingModel.id, null, null,
            null
        )
        val incomingModelWithMetadata = ModelWithMetadata(existingModel, metadataWithoutVersion)
        val mergeObserver = merger.merge(incomingModelWithMetadata).test()
        mergeObserver.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS)
        mergeObserver.assertNoErrors().assertComplete()

        // Assert: Joey is still the same old Joey.
        assertEquals(
            listOf(existingModel),
            storageAdapter.query(
                BlogOwner::class.java
            )
        )

        // And his metadata is the still the same.
        assertEquals(
            listOf(existingMetadata),
            storageAdapter.query(
                ModelMetadata::class.java,
                Where.identifier(
                    ModelMetadata::class.java, existingModel.id
                )
            )
        )
    }

    /**
     * Assume item A is dependent on item B, but the remote store has an
     * orphaned item A without item B. Then, we try to merge a save for a
     * item A. This should gracefully fail, with A not being in the local
     * store, at the end.
     * @throws DataStoreException On failure to query results for assertions
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(DataStoreException::class, InterruptedException::class)
    fun orphanedItemIsNotMerged() {
        // Arrange: an item and its parent are not in the local store
        val badOwner = BlogOwner.builder()
            .name("Raphael")
            .build()
        val orphanedBlog = Blog.builder()
            .name("How Not To Save Blogs")
            .owner(badOwner)
            .build()
        val metadata = ModelMetadata(
            orphanedBlog.id, false, 1,
            Temporal.Timestamp.now()
        )

        // Enforce foreign key constraint on in-memory storage adapter
        Mockito.doThrow(SQLiteConstraintException::class.java)
            .`when`(spiedStorageAdapter)
            .save(
                ArgumentMatchers.eq(orphanedBlog),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            )

        // Act: merge a creation for an item
        val observer = merger.merge(ModelWithMetadata(orphanedBlog, metadata)).test()
        assertTrue(observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS))
        observer.assertNoErrors().assertComplete()

        // Assert: orphaned model was not merged locally
        val blogsInStorage = storageAdapter.query(
            Blog::class.java
        )
        assertTrue(blogsInStorage.isEmpty())
    }

    /**
     * b1 for insert with no existing record
     * b2 for delete with no existing record
     * b3 for delete with existing record + pending mutation
     * b4 for update with existing record
     * b5 for ignored update due to higher version in existing record
     * b6 for delete with existing record
     */
    @Test
    fun testBatchedMerger() {
        // GIVEN

        // Timestamp isn't important in these tests but helpful to know for equality checks
        val ignoredTimestamp = Temporal.Timestamp.now()

        // Capture Storage Changes Returned
        var capturedStorageItemChanges = mutableListOf<StorageItemChange.Type>()
        val changeTypeConsumer = Consumer<StorageItemChange.Type> {
            capturedStorageItemChanges.add(it)
        }

        // Hydrate Step for b3
        val blog3ModelWithMetadata = ModelWithMetadata(
            Blog.builder().name("blog3Name").id("b3").build(),
            ModelMetadata("Blog|b3", false, 1, Temporal.Timestamp.now())
        )
        storageAdapter.save(blog3ModelWithMetadata.model, blog3ModelWithMetadata.syncMetadata)
        val pendingMutation = PendingMutation.instance(
            blog3ModelWithMetadata.model,
            ModelSchema.fromModelClass(Blog::class.java),
            PendingMutation.Type.CREATE,
            QueryPredicates.all()
        )
        val enqueueObserver = mutationOutbox.enqueue(pendingMutation).test()
        enqueueObserver.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS)
        enqueueObserver.assertNoErrors().assertComplete()

        // Hydrate Step for b4
        val blog4ModelWithMetadata = ModelWithMetadata(
            Blog.builder().name("blog4Name").id("b4").build(),
            ModelMetadata("Blog|b4", false, 1, ignoredTimestamp)
        )
        storageAdapter.save(blog4ModelWithMetadata.model, blog4ModelWithMetadata.syncMetadata)

        // Hydrate Step for b5
        val blog5ModelWithMetadata = ModelWithMetadata(
            Blog.builder().name("blog5Name").id("b5").build(),
            ModelMetadata("Blog|b5", false, 10, ignoredTimestamp)
        )
        storageAdapter.save(blog5ModelWithMetadata.model, blog5ModelWithMetadata.syncMetadata)

        // Hydrate Step for b6
        val blog6ModelWithMetadata = ModelWithMetadata(
            Blog.builder().name("blog6Name").id("b6").build(),
            ModelMetadata("Blog|b6", false, 1, ignoredTimestamp)
        )
        storageAdapter.save(blog6ModelWithMetadata.model, blog6ModelWithMetadata.syncMetadata)

        // Models to Merge
        val blog1ModelWithMetadata = ModelWithMetadata(
            Blog.builder().name("blog1Name").id("b1").build(),
            ModelMetadata("Blog|b1", false, 1, ignoredTimestamp)
        )
        val blog2ModelWithMetadata = ModelWithMetadata(
            Blog.builder().name("blog2Name").id("b2").build(),
            ModelMetadata("Blog|b2", true, 1, ignoredTimestamp)
        )
        val blog3ToUpdate = ModelWithMetadata(
            blog3ModelWithMetadata.model.copyOfBuilder().name("blog3NameUpdated").build(),
            ModelMetadata("Blog|b3", true, 2, ignoredTimestamp)
        )
        val blog4ToUpdate = ModelWithMetadata(
            blog4ModelWithMetadata.model.copyOfBuilder().name("blog4NameUpdated").build(),
            ModelMetadata("Blog|b4", false, 2, ignoredTimestamp)
        )
        val blog5ToUpdate = ModelWithMetadata(
            blog5ModelWithMetadata.model.copyOfBuilder().name("blog5NameUpdated").id("b5").build(),
            ModelMetadata("Blog|b5", false, 5, ignoredTimestamp)
        )
        val blog6ToUpdate = ModelWithMetadata(
            blog6ModelWithMetadata.model,
            ModelMetadata("Blog|b6", true, 2, ignoredTimestamp)
        )

        // Expected Blog table result
        val expectedBlogResult = listOf(
            blog1ModelWithMetadata.model,
            blog3ModelWithMetadata.model,
            blog4ToUpdate.model,
            blog5ModelWithMetadata.model
        )
        // Expected ModelMutation table result
        val expectedMetadataResult = listOf(
            blog1ModelWithMetadata.syncMetadata,
            blog2ModelWithMetadata.syncMetadata,
            blog3ToUpdate.syncMetadata,
            blog4ToUpdate.syncMetadata,
            blog5ModelWithMetadata.syncMetadata,
            blog6ToUpdate.syncMetadata
        )
        // Expected Storage Item Changes
        val expectedStorageItemChanges = listOf(
            StorageItemChange.Type.CREATE,
            StorageItemChange.Type.DELETE,
            StorageItemChange.Type.UPDATE,
            StorageItemChange.Type.DELETE
        )

        // WHEN: Merge Models
        val observer = merger.merge(
            listOf(
                blog1ModelWithMetadata,
                blog2ModelWithMetadata,
                blog3ToUpdate,
                blog4ToUpdate,
                blog5ToUpdate,
                blog6ToUpdate
            ),
            changeTypeConsumer
        ).test()

        assertTrue(observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS))
        observer.assertNoErrors().assertComplete()

        // THEN
        val blogResult = storageAdapter.query(Blog::class.java)
        val metadataResult = storageAdapter.query(ModelMetadata::class.java)

        assertEquals(4, blogResult.size)
        assertEquals(6, metadataResult.size)
        assertEquals(4, expectedStorageItemChanges.size)

        assertEquals(expectedBlogResult, blogResult.sortedBy { it.id })
        assertEquals(expectedMetadataResult, metadataResult.sortedBy { it.primaryKeyString })
        assertEquals(expectedStorageItemChanges, capturedStorageItemChanges)
    }

    companion object {
        private val REASONABLE_WAIT_TIME = TimeUnit.SECONDS.toMillis(2)
    }
}
