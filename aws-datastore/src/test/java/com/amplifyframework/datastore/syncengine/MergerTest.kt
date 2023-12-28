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
import com.amplifyframework.AmplifyException
import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.core.model.query.Where
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.core.model.temporal.Temporal
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.appsync.ModelMetadata
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter
import com.amplifyframework.testmodels.commentsblog.Blog
import com.amplifyframework.testmodels.commentsblog.BlogOwner
import com.amplifyframework.testutils.random.RandomString
import java.util.concurrent.TimeUnit
import org.junit.Assert
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
    private lateinit var inMemoryStorageAdapter: InMemoryStorageAdapter
    private lateinit var storageAdapter: SynchronousStorageAdapter
    private lateinit var mutationOutbox: MutationOutbox
    private lateinit var merger: Merger

    /**
     * Sets up the test. A [Merger] is being tested. To construct one, several
     * intermediary objects are needed. A reference is held to a [MutationOutbox],
     * to arrange state. A [SynchronousStorageAdapter] is crated to facilitate
     * arranging model data into the [InMemoryStorageAdapter] which backs the various
     * components.
     */
    @Before
    fun setup() {
        inMemoryStorageAdapter = Mockito.spy(InMemoryStorageAdapter.create())
        storageAdapter = SynchronousStorageAdapter.delegatingTo(inMemoryStorageAdapter)
        mutationOutbox = PersistentMutationOutbox(inMemoryStorageAdapter)
        val versionRepository = VersionRepository(inMemoryStorageAdapter)
        merger = Merger(mutationOutbox, versionRepository, inMemoryStorageAdapter)
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
        Assert.assertEquals(
            listOf(blogOwner),
            storageAdapter.query(
                BlogOwner::class.java
            )
        )

        // Act: merge a model deletion.
        val deletionMetadata = ModelMetadata(blogOwner.id, true, 2, Temporal.Timestamp.now())
        val observer = merger.merge(ModelWithMetadata(blogOwner, deletionMetadata)).test()
        Assert.assertTrue(observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS))
        observer.assertNoErrors().assertComplete()

        // Assert: the blog owner is no longer in the store.
        Assert.assertEquals(0, storageAdapter.query(BlogOwner::class.java).size.toLong())
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
        Assert.assertTrue(observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS))
        observer.assertNoErrors().assertComplete()

        // Assert: there is still nothing in the store.
        Assert.assertEquals(0, storageAdapter.query(BlogOwner::class.java).size.toLong())
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
        Assert.assertTrue(observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS))
        observer.assertNoErrors().assertComplete()

        // Assert: the item & its associated metadata are now in the store.
        Assert.assertEquals(
            listOf(blogOwner),
            storageAdapter.query(
                BlogOwner::class.java
            )
        )
        Assert.assertEquals(
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
        Assert.assertTrue(observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS))
        observer.assertComplete().assertNoErrors()

        // Assert: the *UPDATED* stuff is in the store, *only*.
        Assert.assertEquals(
            listOf(updatedModel),
            storageAdapter.query(
                BlogOwner::class.java
            )
        )
        Assert.assertEquals(
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
        Assert.assertEquals(1, blogOwnersInStorage.size.toLong())
        Assert.assertEquals(blogOwner, blogOwnersInStorage[0])
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
        Assert.assertEquals(0, blogOwnersInStorage.size.toLong())
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
        Assert.assertEquals(
            listOf(existingModel),
            storageAdapter.query(
                BlogOwner::class.java
            )
        )

        // And his metadata is the still the same.
        Assert.assertEquals(
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
        Assert.assertEquals(1, actualBlogOwners.size.toLong())
        Assert.assertEquals(existingModel, actualBlogOwners[0])

        // And his metadata is the still the same.
        Assert.assertEquals(
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
        Assert.assertEquals(
            listOf(existingModel),
            storageAdapter.query(
                BlogOwner::class.java
            )
        )

        // And his metadata is the still the same.
        Assert.assertEquals(
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
            .`when`(inMemoryStorageAdapter)
            .save(
                ArgumentMatchers.eq(orphanedBlog),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            )

        // Act: merge a creation for an item
        val observer = merger.merge(ModelWithMetadata(orphanedBlog, metadata)).test()
        Assert.assertTrue(observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS))
        observer.assertNoErrors().assertComplete()

        // Assert: orphaned model was not merged locally
        val blogsInStorage = storageAdapter.query(
            Blog::class.java
        )
        Assert.assertTrue(blogsInStorage.isEmpty())
    }

    companion object {
        private val REASONABLE_WAIT_TIME = TimeUnit.SECONDS.toMillis(2)
    }
}
