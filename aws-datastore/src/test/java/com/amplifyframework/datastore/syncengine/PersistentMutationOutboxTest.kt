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

import com.amplifyframework.AmplifyException
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.core.model.SerializedModel
import com.amplifyframework.core.model.query.Where
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter
import com.amplifyframework.datastore.syncengine.MutationOutbox.OutboxEvent
import com.amplifyframework.datastore.syncengine.PendingMutation.PersistentRecord
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.testmodels.commentsblog.Author
import com.amplifyframework.testmodels.commentsblog.BlogOwner
import com.amplifyframework.testmodels.commentsblog.Post
import com.amplifyframework.testmodels.commentsblog.PostStatus
import com.amplifyframework.testutils.HubAccumulator
import com.amplifyframework.testutils.random.RandomString
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests the [MutationOutbox].
 */
class PersistentMutationOutboxTest {
    private lateinit var schema: ModelSchema
    private lateinit var mutationOutbox: PersistentMutationOutbox
    private lateinit var converter: PendingMutation.Converter
    private lateinit var storage: SynchronousStorageAdapter

    /**
     * Set up the object under test.
     * @throws AmplifyException on failure to build schema
     */
    @Before
    @Throws(AmplifyException::class)
    fun setup() {
        schema = ModelSchema.fromModelClass(BlogOwner::class.java)
        val inMemoryStorageAdapter = InMemoryStorageAdapter.create()
        storage = SynchronousStorageAdapter.delegatingTo(inMemoryStorageAdapter)
        mutationOutbox = PersistentMutationOutbox(inMemoryStorageAdapter)
        converter = GsonPendingMutationConverter()
    }

    /**
     * Enqueueing a mutation should publish current outbox status.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(InterruptedException::class)
    fun outboxStatusIsPublishedToHubOnEnqueue() {
        val raphael = BlogOwner.builder()
            .name("Raphael Kim")
            .build()
        val createRaphael = PendingMutation.creation(raphael, schema)

        // Start listening for publication events.
        // outbox should not be empty
        val statusAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, TestHubEventFilters.isOutboxEmpty(false), 1)
                .start()

        // Enqueue an save for a Raphael BlogOwner object,
        // and make sure that outbox status is published to hub.
        val saveObserver = mutationOutbox.enqueue(createRaphael).test()
        saveObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        saveObserver.assertNoErrors().assertComplete()
        saveObserver.dispose()
        statusAccumulator.await()
    }

    /**
     * Enqueueing a mutation should have the result of persisting
     * the mutation to storage, and notifying any observers that
     * a new mutation has been enqueued.
     * @throws DataStoreException On failure to query results, for assertions
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(DataStoreException::class, InterruptedException::class)
    fun enqueuePersistsMutationAndNotifiesObserver() {
        // Observe the queue
        val queueObserver = mutationOutbox.events().test()
        val jameson = BlogOwner.builder()
            .name("Jameson Williams")
            .build()
        val createJameson = PendingMutation.creation(jameson, schema)
        val savedMutationsAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, TestHubEventFilters.isEnqueued(jameson), 1)
                .start()

        // Enqueue an save for a Jameson BlogOwner object,
        // and make sure that it calls back onComplete().
        val saveObserver = mutationOutbox.enqueue(createJameson).test()
        saveObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        saveObserver.assertNoErrors().assertComplete()
        saveObserver.dispose()

        // Wait for a Hub event telling us that the model got successfully enqueued.
        savedMutationsAccumulator.await()

        // Expected to observe the mutation on the subject
        queueObserver.awaitCount(1)
        queueObserver.assertValue(OutboxEvent.CONTENT_AVAILABLE)
        queueObserver.dispose()

        // Assert that the storage contains the mutation
        assertEquals(
            listOf(converter.toRecord(createJameson)),
            storage.query(PersistentRecord::class.java)
        )
        assertTrue(hasPendingMutation(jameson.id, jameson.javaClass.simpleName))
        assertEquals(createJameson, mutationOutbox.peek())
    }

    /**
     * The enqueue() returns a Completable, but that Completable doesn't actually invoke
     * any behavior until it is subscribed.
     * @throws DataStoreException On failure to query results, for assertions
     */
    @Test
    @Throws(DataStoreException::class)
    fun enqueueDoesNothingBeforeSubscription() {
        // Watch for notifications on the observe() API.
        val testObserver = mutationOutbox.events().test()

        // Enqueue something, but don't subscribe to the observable just yet.
        val tony = BlogOwner.builder()
            .name("Tony Daniels")
            .build()
        mutationOutbox.enqueue(PendingMutation.creation(tony, schema))
        // .subscribe() is NOT called on the enqueue() above This is the point!

        // Note that nothing has actually happened yet --
        // Nothing was put out on the observable ...
        testObserver.assertNoValues()
        testObserver.assertNotComplete()
        testObserver.dispose()

        // And nothing is in storage.
        assertTrue(storage.query(PersistentRecord::class.java).isEmpty())

        // And nothing is peek()ed.
        assertNull(mutationOutbox.peek())
    }

    /**
     * Calling load() will populate the outbox with content from disk.
     * @throws DataStoreException On failure to arrange models into storage before test action
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(DataStoreException::class, InterruptedException::class)
    fun loadPreparesOutbox() {
        // Arrange: some mutations.
        val tony = BlogOwner.builder()
            .name("Tony Daniels")
            .build()
        val updateTony = PendingMutation.update(tony, schema)
        val sam = BlogOwner.builder()
            .name("Sam Watson")
            .build()
        val insertSam = PendingMutation.creation(sam, schema)
        storage.save(converter.toRecord(updateTony), converter.toRecord(insertSam))

        // Act: load the outbox.
        val loadObserver = mutationOutbox.load().test()

        // Assert: load worked.
        loadObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        loadObserver.assertNoErrors().assertComplete()
        loadObserver.dispose()

        // Assert: items are in the outbox.
        assertTrue(hasPendingMutation(tony.id, tony.javaClass.simpleName))
        assertTrue(hasPendingMutation(sam.id, sam.javaClass.simpleName))

        // Tony is first, since he is the older of the two mutations.
        assertEquals(updateTony, mutationOutbox.peek())
    }

    /**
     * Tests [MutationOutbox.remove].
     * @throws DataStoreException On failure to query results, for assertions
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(DataStoreException::class, InterruptedException::class)
    fun removeRemovesChangesFromQueue() {
        // Arrange: there is a change in the queue.
        val bill = BlogOwner.builder()
            .name("Bill Gates")
            .build()
        val deleteBillGates = PendingMutation.deletion(bill, schema)
        storage.save(converter.toRecord(deleteBillGates))
        val completed = mutationOutbox.load().blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed)
        val testObserver = mutationOutbox.remove(deleteBillGates.mutationId).test()
        testObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        testObserver.assertNoErrors().assertComplete()
        testObserver.dispose()
        assertEquals(0, storage.query(PersistentRecord::class.java).size.toLong())
        assertNull(mutationOutbox.peek())
        assertFalse(hasPendingMutation(bill.id, bill.javaClass.simpleName))
    }

    /**
     * When there are multiple pending mutations in the outbox, and one is removed,
     * we will see a [OutboxEvent.CONTENT_AVAILABLE] after the removal. This
     * notifies the system that there is more work to be done, even though we've successfully
     * processed an event. The system will continue processing items from the outbox until
     * all have been processed.
     * @throws InterruptedException If thread interrupted while waiting for events
     */
    @Test
    @Throws(InterruptedException::class)
    fun notifiesWhenContentAvailableAfterDelete() {
        // Start watching the events stream. We'll expect a notification here 3 times:
        // after the first enqueue, after the second enqueue, after the first deletion.
        val enqueueEventObserver = mutationOutbox.events().test()

        // Arrange a few mutations into the queue.
        val senatorBernie = BlogOwner.builder()
            .name("Senator Bernard Sanders")
            .build()
        val createSenatorBernie = PendingMutation.creation(senatorBernie, schema)
        val createCompleted = mutationOutbox.enqueue(createSenatorBernie)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(createCompleted)
        val sam = BlogOwner.builder()
            .name("Sam Watson")
            .build()
        val insertSam = PendingMutation.creation(sam, schema)
        val updateCompleted = mutationOutbox.enqueue(insertSam)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(updateCompleted)
        enqueueEventObserver
            .awaitCount(2)
            .assertValues(OutboxEvent.CONTENT_AVAILABLE, OutboxEvent.CONTENT_AVAILABLE)
            .assertNoErrors()
        val firstRemoveEventObserver = mutationOutbox.events().test()

        // Remove first item.
        val firstRemoval = mutationOutbox.remove(createSenatorBernie.mutationId).test()
        firstRemoval.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        firstRemoval
            .assertNoErrors()
            .assertComplete()
            .dispose()

        // One event is observed on events(), since there are still some pending mutations
        // that need to be processed.
        firstRemoveEventObserver
            .awaitCount(1)
            .assertValues(OutboxEvent.CONTENT_AVAILABLE)
            .assertNoErrors()

        // Get ready to watch the events() again.
        val secondRemoveEventObserver = mutationOutbox.events().test()

        // Remove the next item.
        val secondRemoval = mutationOutbox.remove(insertSam.mutationId).test()
        secondRemoval.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        secondRemoval
            .assertNoErrors()
            .assertComplete()
            .dispose()

        // This time, we don't see any event on events(), since the outbox has become empty.
        secondRemoveEventObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        secondRemoveEventObserver.assertNoValues().assertNoErrors()
    }

    /**
     * When there is a pending mutation for a particular model ID
     * [hasPendingMutation] must say "yes!".
     */
    @Test
    fun hasPendingMutationReturnsTrueForExistingModelMutation() {
        val modelId = RandomString.string()
        val joe = BlogOwner.builder()
            .name("Joe")
            .id(modelId)
            .build()
        val mutationId = TimeBasedUuid.create()
        val pendingMutation = PendingMutation.instance(
            mutationId, joe, schema, PendingMutation.Type.CREATE, QueryPredicates.all()
        )
        val completed = mutationOutbox.enqueue(pendingMutation)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed)
        assertTrue(hasPendingMutation(modelId, joe.javaClass.simpleName))
        assertFalse(
            hasPendingMutation(
                mutationId.toString(),
                mutationId.javaClass.simpleName
            )
        )
    }

    /**
     * When the mutation outbox is asked if there is a pending mutation, and there is no
     * corresponding mutation, then the mutation outbox shall say "heck no!".
     *
     * To throw a wrench in things, make sure the model is in storage -- just that there is
     * no pending mutation for it.
     *
     * @throws DataStoreException On failure to save the model item into storage
     */
    @Test
    @Throws(DataStoreException::class)
    fun hasPendingMutationReturnsFalseForItemNotInStore() {
        val joeId = RandomString.string()
        val joe = BlogOwner.builder()
            .name("Joe Swanson III")
            .id(joeId)
            .build()
        storage.save(joe)
        val mutationId = TimeBasedUuid.create()
        val unrelatedMutation = PendingMutation.instance(
            mutationId, joe, schema, PendingMutation.Type.CREATE, QueryPredicates.all()
        )
        assertFalse(hasPendingMutation(joeId, joe.javaClass.simpleName))
        assertFalse(
            hasPendingMutation(
                unrelatedMutation.mutationId.toString(),
                unrelatedMutation.javaClass.simpleName
            )
        )
    }

    /**
     * When queuing record to mutationOutbox for a model, hasPendingMutation should return false
     * for other existing models with same primary key values.
     *
     * @throws AmplifyException On failure to convert the modelClass item to ModelSchema
     */
    @Test
    @Throws(AmplifyException::class)
    fun hasPendingMutationReturnsFalseForModelMutationWithSamePrimaryKeyForDifferentModels() {
        val modelId = RandomString.string()
        val blogOwner = BlogOwner.builder()
            .name("Sample BlogOwner")
            .id(modelId)
            .build()
        val mutationId = TimeBasedUuid.create()
        val pendingBlogOwnerMutation = PendingMutation.instance(
            mutationId, blogOwner, schema, PendingMutation.Type.CREATE, QueryPredicates.all()
        )

        // Act & Assert: Enqueue and verify BlogOwner
        assertTrue(
            mutationOutbox.enqueue(pendingBlogOwnerMutation).blockingAwait(
                TIMEOUT_MS, TimeUnit.MILLISECONDS
            )
        )
        assertTrue(hasPendingMutation(modelId, blogOwner.javaClass.simpleName))

        // Act & Assert: Enqueue and verify Author
        val author = Author.builder()
            .name("Sample Author")
            .id(modelId)
            .build()

        // Check hasPendingMutation returns False for Author with same Primary Key (id) as BlogOwner
        assertFalse(hasPendingMutation(modelId, author.javaClass.simpleName))
        val pendingAuthorMutation = PendingMutation.instance(
            mutationId, author, ModelSchema.fromModelClass(Author::class.java),
            PendingMutation.Type.CREATE, QueryPredicates.all()
        )
        assertTrue(
            mutationOutbox.enqueue(pendingAuthorMutation)
                .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        )

        // Make sure Author Mutation is stored
        assertTrue(hasPendingMutation(modelId, author.javaClass.simpleName))

        // Act & Assert: Enqueue and verify Author
        val post = Post.builder()
            .title("Sample Author")
            .status(PostStatus.ACTIVE)
            .rating(1)
            .id(modelId)
            .build()

        // Check hasPendingMutation returns False for Post with same Primary Key (id) as BlogOwner
        assertFalse(hasPendingMutation(modelId, post.javaClass.simpleName))
        val pendingPostMutation = PendingMutation.instance(
            mutationId, post, ModelSchema.fromModelClass(Post::class.java),
            PendingMutation.Type.CREATE, QueryPredicates.all()
        )
        assertTrue(
            mutationOutbox.enqueue(pendingPostMutation)
                .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        )

        // Make sure Post Mutation is stored
        assertTrue(hasPendingMutation(modelId, post.javaClass.simpleName))
    }

    /**
     * Validates that attempting to enqueue a mutation for a model with a duplicate primary key results
     * in a DataStoreException. Also checks that the original mutation is still in the outbox.
     *
     * @throws DataStoreException On failure to query storage to assert post-action value of mutation
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     * @throws AmplifyException If schema cannot be found in the registry
     */
    @Test
    @Throws(AmplifyException::class, InterruptedException::class)
    fun mutationEnqueueForModelWithDuplicatePrimaryKeyThrowsDatastoreException() {

        // Arrange: Create and enqueue an initial BlogOwner mutation
        val modelId = RandomString.string()
        val existingBlogOwner = BlogOwner.builder()
            .name("Sample BlogOwner")
            .id(modelId)
            .build()
        val existingCreation = PendingMutation.creation(existingBlogOwner, schema)
        val existingCreationId = existingCreation.mutationId.toString()
        assertTrue(
            mutationOutbox.enqueue(existingCreation)
                .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        )

        // Arrange: Create a new BlogOwner with the same ID as the existing one
        val duplicateBlogOwner = BlogOwner.builder()
            .name("Sample BlogOwner")
            .id(modelId)
            .build()
        val duplicateMutation = PendingMutation.creation(duplicateBlogOwner, schema)
        val duplicateMutationId = duplicateMutation.mutationId.toString()

        // Act: Attempt to enqueue the duplicate mutation
        val enqueueObserver = mutationOutbox.enqueue(duplicateMutation).test()

        // Assert: Verify that a DataStoreException is thrown
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        enqueueObserver.assertError { throwable: Throwable? -> throwable is DataStoreException }

        // Assert: original mutation is present, but the new one isn't.
        val storedMutation = storage.query(
            PersistentRecord::class.java,
            Where.identifier(PersistentRecord::class.java, existingCreationId)
        )[0]
        assertEquals(
            existingBlogOwner,
            converter.fromRecord<Model>(storedMutation).mutatedItem
        )
        assertTrue(
            storage.query(
                PersistentRecord::class.java,
                Where.identifier(PersistentRecord::class.java, duplicateMutationId)
            ).isEmpty()
        )

        // Additional Checks: Peek the Mutation outbox, existing mutation should be present.
        assertTrue(
            hasPendingMutation(
                existingBlogOwner.primaryKeyString,
                existingBlogOwner.javaClass.simpleName
            )
        )
        assertEquals(existingCreation, mutationOutbox.peek())
    }

    /**
     * When there is an existing creation for a model, and a new creation for that
     * model comes in, an error should be returned. In other words, it is illegal to
     * create a mutation twice.
     * @throws DataStoreException On failure to query storage to assert post-action value of mutation
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    @Throws(AmplifyException::class, InterruptedException::class)
    fun existingCreationIncomingCreationYieldsError() {
        // Arrange an existing creation mutation
        val modelInExistingMutation = BlogOwner.builder()
            .name("The Real Papa Tony")
            .build()
        val existingCreation = PendingMutation.creation(modelInExistingMutation, schema)
        val existingCreationId = existingCreation.mutationId.toString()
        val completed = mutationOutbox.enqueue(existingCreation)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed)

        // Act: try to create the blog owner again -- but there's already a pending creation
        val modelInIncomingMutation = modelInExistingMutation.copyOfBuilder()
            .name("Someone Posing as Papa Tony Who isn't \uD83D\uDCAF legit.")
            .build()
        val incomingCreation = PendingMutation.creation(modelInIncomingMutation, schema)
        val incomingCreationId = incomingCreation.mutationId.toString()
        val enqueueObserver = mutationOutbox.enqueue(incomingCreation).test()

        // Assert: caused a failure.
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        enqueueObserver.assertError { throwable: Throwable? -> throwable is DataStoreException }

        // Assert: original mutation is present, but the new one isn't.
        val storedMutation = storage.query(
            PersistentRecord::class.java,
            Where.identifier(
                PersistentRecord::class.java,
                existingCreationId
            )
        )[0]
        assertEquals(
            modelInExistingMutation,
            converter.fromRecord<Model>(storedMutation).mutatedItem
        )
        assertTrue(
            storage.query(
                PersistentRecord::class.java,
                Where.identifier(PersistentRecord::class.java, incomingCreationId)
            ).isEmpty()
        )

        // Existing mutation still attainable as next mutation (right now, its the ONLY mutation in outbox)
        assertTrue(
            hasPendingMutation(
                modelInExistingMutation.primaryKeyString,
                modelInExistingMutation.javaClass.simpleName
            )
        )
        assertEquals(existingCreation, mutationOutbox.peek())
    }

    /**
     * When there is an existing update for a model, and a new creation for that
     * model comes in, an error should be returned. In other words, you can't create
     * something that already exists and is being updated.
     * @throws DataStoreException On failure to to query which mutations are in storage
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    @Throws(AmplifyException::class, InterruptedException::class)
    fun existingUpdateIncomingCreationYieldsError() {
        // Arrange an existing update mutation
        val modelInExistingMutation = BlogOwner.builder()
            .name("Tony with improvements applied")
            .build()
        val existingUpdate = PendingMutation.update(modelInExistingMutation, schema)
        val exitingUpdateId = existingUpdate.mutationId.toString()
        val completed = mutationOutbox.enqueue(existingUpdate)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed)

        // Act: try to CREATE tony again -- but isn't he already created, if there's an update?
        val modelInIncomingMutation = modelInExistingMutation.copyOfBuilder()
            .name("Brand new tony")
            .build()
        val incomingCreation = PendingMutation.creation(modelInIncomingMutation, schema)
        val incomingCreationId = incomingCreation.mutationId.toString()
        val enqueueObserver = mutationOutbox.enqueue(incomingCreation).test()

        // Assert: caused a failure.
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        enqueueObserver.assertError { throwable: Throwable? -> throwable is DataStoreException }

        // Assert: original mutation is present, but the new one isn't.
        val storedMutation = storage.query(
            PersistentRecord::class.java,
            Where.identifier(
                PersistentRecord::class.java,
                exitingUpdateId
            )
        )[0]
        assertEquals(
            modelInExistingMutation,
            converter.fromRecord<Model>(storedMutation).mutatedItem
        )
        assertTrue(
            storage.query(
                PersistentRecord::class.java,
                Where.identifier(
                    PersistentRecord::class.java,
                    incomingCreationId
                )
            ).isEmpty()
        )

        // Existing mutation still attainable as next mutation (right now, its the ONLY mutation in outbox)
        assertTrue(
            hasPendingMutation(
                modelInExistingMutation.primaryKeyString,
                modelInExistingMutation.javaClass.simpleName
            )
        )
        assertEquals(existingUpdate, mutationOutbox.peek())
    }

    /**
     * When there is an existing deletion for a model, and a new creation for that
     * model comes in, an error should be returned. Even though the model may be staged
     * for deletion, that deletion hasn't happened yet. So, it doesn't make sense to create()
     * something that currently already exists. That's like an "update."
     * @throws DataStoreException On failure to query which mutation is present in storage
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    @Throws(AmplifyException::class, InterruptedException::class)
    fun existingDeletionIncomingCreationYieldsError() {
        // Arrange an existing deletion mutation
        val modelInExistingMutation = BlogOwner.builder()
            .name("Papa Tony")
            .build()
        val existingDeletion = PendingMutation.deletion(modelInExistingMutation, schema)
        val existingDeletionId = existingDeletion.mutationId.toString()
        val completed = mutationOutbox.enqueue(existingDeletion)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed)

        // Act: try to create tony, but wait -- if we're already deleting him...
        val modelInIncomingMutation = modelInExistingMutation.copyOfBuilder()
            .name("Tony Jr.")
            .build()
        val incomingCreation = PendingMutation.creation(modelInIncomingMutation, schema)
        val incomingCreationId = incomingCreation.mutationId.toString()
        val enqueueObserver = mutationOutbox.enqueue(incomingCreation).test()

        // Assert: caused a failure.
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        enqueueObserver.assertError { throwable: Throwable? -> throwable is DataStoreException }

        // Assert: original mutation is present, but the new one isn't.
        val storedMutation = storage.query(
            PersistentRecord::class.java,
            Where.identifier(PersistentRecord::class.java, existingDeletionId)
        )[0]
        assertEquals(
            modelInExistingMutation,
            converter.fromRecord<Model>(storedMutation).mutatedItem
        )
        assertTrue(
            storage.query(
                PersistentRecord::class.java,
                Where.identifier(PersistentRecord::class.java, incomingCreationId)
            ).isEmpty()
        )

        // Existing mutation still attainable as next mutation (right now, its the ONLY mutation in outbox)
        assertTrue(
            hasPendingMutation(
                modelInExistingMutation.primaryKeyString,
                modelInExistingMutation.javaClass.simpleName
            )
        )
        assertEquals(existingDeletion, mutationOutbox.peek())
    }

    /**
     * If there is a pending deletion, enqueuing an update will fail, since the thing being
     * updated is not meant to exist.
     * @throws DataStoreException On failure to query storage, for the purpose of asserting the
     * state of mutations after the test action.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer.
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    @Throws(AmplifyException::class, InterruptedException::class)
    fun existingDeletionIncomingUpdateYieldsError() {
        // Arrange an existing deletion mutation
        val modelInExistingMutation = BlogOwner.builder()
            .name("Papa Tony")
            .build()
        val existingDeletion = PendingMutation.deletion(modelInExistingMutation, schema)
        val existingDeletionId = existingDeletion.mutationId.toString()
        val completed = mutationOutbox.enqueue(existingDeletion)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed)

        // Act: try to update tony, but wait ... aren't we deleting tony?
        val modelInIncomingMutation = modelInExistingMutation.copyOfBuilder()
            .name("Tony Jr.")
            .build()
        val incomingUpdate = PendingMutation.update(modelInIncomingMutation, schema)
        val incomingUpdateId = incomingUpdate.mutationId.toString()
        val enqueueObserver = mutationOutbox.enqueue(incomingUpdate).test()

        // Assert: caused a failure.
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        enqueueObserver.assertError { throwable: Throwable? -> throwable is DataStoreException }

        // Assert: original mutation is present, but the new one isn't.
        val storedMutation = storage.query(
            PersistentRecord::class.java,
            Where.identifier(PersistentRecord::class.java, existingDeletionId)
        )[0]
        assertEquals(
            modelInExistingMutation,
            converter.fromRecord<Model>(storedMutation).mutatedItem
        )
        assertTrue(
            storage.query(
                PersistentRecord::class.java,
                Where.identifier(
                    PersistentRecord::class.java,
                    incomingUpdateId
                )
            ).isEmpty()
        )

        // Existing mutation still attainable as next mutation (right now, its the ONLY mutation in outbox)
        assertTrue(
            hasPendingMutation(
                modelInExistingMutation.primaryKeyString,
                modelInExistingMutation.javaClass.simpleName
            )
        )
        assertEquals(existingDeletion, mutationOutbox.peek())
    }

    /**
     * When there is an existing update mutation, and a new update mutation with condition
     * comes in, then the existing one should remain and the new one should be appended.
     * @throws DataStoreException On failure to query storage for current mutations state.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer.
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    @Throws(AmplifyException::class, InterruptedException::class)
    fun existingUpdateIncomingUpdateWithConditionAppendsMutation() {
        // Arrange an existing update mutation
        val modelInExistingMutation = BlogOwner.builder()
            .name("Papa Tony")
            .build()
        val existingUpdate = PendingMutation.update(modelInExistingMutation, schema)
        val existingUpdateId = existingUpdate.mutationId.toString()
        val completed = mutationOutbox.enqueue(existingUpdate)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed)

        // Act: try to enqueue a new update mutation when there already is one
        val modelInIncomingMutation = modelInExistingMutation.copyOfBuilder()
            .name("Tony Jr.")
            .build()
        val incomingUpdate = PendingMutation.update(
            modelInIncomingMutation,
            schema,
            BlogOwner.NAME.eq("Papa Tony")
        )
        val incomingUpdateId = incomingUpdate.mutationId.toString()
        val enqueueObserver = mutationOutbox.enqueue(incomingUpdate).test()

        // Assert: OK. The new mutation is accepted
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        enqueueObserver.assertComplete()

        // Assert: the existing mutation is still there, by id ....
        val recordsForExistingMutationId = storage.query(
            PersistentRecord::class.java,
            Where.identifier(PersistentRecord::class.java, existingUpdateId)
        )
        assertEquals(1, recordsForExistingMutationId.size.toLong())

        // Assert: And the new one is also there
        val recordsForIncomingMutationId = storage.query(
            PersistentRecord::class.java,
            Where.identifier(
                PersistentRecord::class.java, incomingUpdateId
            )
        )
        assertEquals(1, recordsForIncomingMutationId.size.toLong())

        // The original mutation should remain as is
        val existingStoredMutation =
            converter.fromRecord<BlogOwner>(recordsForExistingMutationId[0])
        // This is the name from the second model, not the first
        assertEquals(modelInExistingMutation.name, existingStoredMutation.mutatedItem.name)
        var next = mutationOutbox.peek()
        assertNotNull(next)
        // The first one should be the existing mutation
        assertEquals(
            existingUpdate,
            next
        )
        // Remove the first one from the queue
        val removeCompleted = mutationOutbox.remove(existingUpdate.mutationId)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(removeCompleted)

        // Get the next one
        next = mutationOutbox.peek()
        assertNotNull(next)
        // The first one should be the existing mutation
        assertEquals(
            incomingUpdate,
            next
        )
    }

    /**
     * When there is an existing update mutation, and a new update mutation comes in,
     * then we need to remove any existing mutations for that modelId and create the new one.
     * @throws DataStoreException On failure to query storage for current mutations state.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer.
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    @Throws(AmplifyException::class, InterruptedException::class)
    fun existingUpdateIncomingUpdateWithoutConditionRewritesExistingMutation() {
        // Arrange an existing update mutation
        val modelInExistingMutation = BlogOwner.builder()
            .name("Papa Tony")
            .build()
        val existingUpdate = PendingMutation.update(modelInExistingMutation, schema)
        val existingUpdateId = existingUpdate.mutationId.toString()
        mutationOutbox.enqueue(existingUpdate).blockingAwait()

        // Act: try to enqueue a new update mutation when there already is one
        val modelInIncomingMutation = modelInExistingMutation.copyOfBuilder()
            .name("Tony Jr.")
            .build()
        val incomingUpdate = PendingMutation.update(modelInIncomingMutation, schema)
        val incomingUpdateId = incomingUpdate.mutationId.toString()
        val enqueueObserver = mutationOutbox.enqueue(incomingUpdate).test()

        // Assert: OK. The new mutation is accepted
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        enqueueObserver.assertComplete()

        // Assert: the existing mutation has been removed
        assertRecordCountForMutationId(existingUpdateId, 0)

        // And the new one has been added to the queue
        assertRecordCountForMutationId(incomingUpdateId, 1)

        // Ensure the new one is in storage.
        val storedMutation =
            converter.fromRecord<BlogOwner>(getPendingMutationRecordFromStorage(incomingUpdateId)[0])
        // This is the name from the second model, not the first
        assertEquals(modelInIncomingMutation.name, storedMutation.mutatedItem.name)

        // The mutation in the outbox is the incoming one.
        assertEquals(
            incomingUpdate,
            mutationOutbox.peek()
        )
    }

    /**
     * When there is an existing SerializedModel update mutation, and a new SerializedModel update mutation comes in,
     * then we need to merge any existing mutations for that modelId and create the new one of type Update.
     * @throws AmplifyException On failure to find the serializedModel difference.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(AmplifyException::class, InterruptedException::class)
    fun existingSerializedModelUpdateIncomingUpdateWithoutConditionMergesWithExistingMutation() {
        // Arrange an existing update mutation
        val modelInSqlLite = BlogOwner.builder()
            .name("Papa Tony")
            .wea("Something")
            .build()
        val initialUpdate = BlogOwner.builder()
            .name("Tony Jr")
            .id(modelInSqlLite.primaryKeyString)
            .build()
        val initialUpdatePendingMutation = PendingMutation.update(
            SerializedModel.difference(
                initialUpdate,
                modelInSqlLite,
                schema
            ),
            schema
        )
        val existingUpdateId = initialUpdatePendingMutation.mutationId.toString()
        mutationOutbox.enqueue(initialUpdatePendingMutation).blockingAwait()

        // Act: try to enqueue a new update mutation when there already is one
        val incomingUpdatedModel = BlogOwner.builder()
            .name("Papa Tony")
            .wea("something else")
            .id(modelInSqlLite.primaryKeyString)
            .build()
        val incomingUpdate = PendingMutation.update(
            SerializedModel.difference(incomingUpdatedModel, modelInSqlLite, schema),
            schema
        )
        val incomingUpdateId = incomingUpdate.mutationId.toString()
        val enqueueObserver = mutationOutbox.enqueue(incomingUpdate).test()

        // Assert: OK. The new mutation is accepted
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        enqueueObserver.assertComplete()

        // Assert: the existing mutation has been removed
        assertRecordCountForMutationId(existingUpdateId, 0)

        // And the new one has been added to the queue
        assertRecordCountForMutationId(incomingUpdateId, 0)
        val pendingMutationsFromStorage = allPendingMutationRecordFromStorage
        for (record in pendingMutationsFromStorage) {
            if (record.containedModelId != incomingUpdate.mutatedItem.resolveIdentifier()) {
                pendingMutationsFromStorage.remove(record)
            }
        }
        // Ensure the new one is in storage.
        val storedMutation = converter.fromRecord<SerializedModel>(pendingMutationsFromStorage[0])
        // This is the name from the second model, not the first
        assertEquals(
            initialUpdate.name,
            storedMutation.mutatedItem.serializedData["name"]
        )
        // wea got merged from existing model
        assertEquals(
            incomingUpdatedModel.wea,
            storedMutation.mutatedItem.serializedData["wea"]
        )
        assertEquals(
            PendingMutation.Type.UPDATE,
            storedMutation.mutationType
        )
    }

    /**
     * When there is an existing SerializedModel create mutation, and a new SerializedModel update mutation comes in,
     * then we need to merge any existing mutations for that modelId and create the new one of type Create.
     * @throws AmplifyException On failure to find the serializedModel difference.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(AmplifyException::class, InterruptedException::class)
    fun existingSerializedModelCreateIncomingUpdateMergesWithExistingMutation() {
        // Arrange an existing update mutation
        val modelInSqlLite = BlogOwner.builder()
            .name("Papa Tony")
            .wea("Something")
            .build()
        val initialUpdate = BlogOwner.builder()
            .name("Tony Jr")
            .id(modelInSqlLite.primaryKeyString)
            .build()
        val initialUpdatePendingMutation = PendingMutation.creation(
            SerializedModel.difference(
                initialUpdate,
                modelInSqlLite,
                schema
            ),
            schema
        )
        val existingUpdateId = initialUpdatePendingMutation.mutationId.toString()
        mutationOutbox.enqueue(initialUpdatePendingMutation).blockingAwait()

        // Act: try to enqueue a new update mutation when there already is one
        val incomingUpdatedModel = BlogOwner.builder()
            .name("Papa Tony")
            .wea("something else")
            .id(modelInSqlLite.primaryKeyString)
            .build()
        val incomingUpdate = PendingMutation.update(
            SerializedModel.difference(incomingUpdatedModel, modelInSqlLite, schema),
            schema
        )
        val incomingUpdateId = incomingUpdate.mutationId.toString()
        val enqueueObserver = mutationOutbox.enqueue(incomingUpdate).test()

        // Assert: OK. The new mutation is accepted
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        enqueueObserver.assertComplete()

        // Assert: the existing mutation has been removed
        assertRecordCountForMutationId(existingUpdateId, 0)

        // And the new one has been added to the queue
        assertRecordCountForMutationId(incomingUpdateId, 0)
        val pendingMutationsFromStorage = allPendingMutationRecordFromStorage
        for (record in pendingMutationsFromStorage) {
            if (record.containedModelId != incomingUpdate.mutatedItem.resolveIdentifier()) {
                pendingMutationsFromStorage.remove(record)
            }
        }
        // Ensure the new one is in storage.
        val storedMutation = converter.fromRecord<SerializedModel>(pendingMutationsFromStorage[0])
        // This is the name from the second model, not the first
        assertEquals(
            initialUpdate.name,
            storedMutation.mutatedItem.serializedData["name"]
        )
        // wea got merged from existing model
        assertEquals(
            incomingUpdatedModel.wea,
            storedMutation.mutatedItem.serializedData["wea"]
        )
        assertEquals(
            PendingMutation.Type.CREATE,
            storedMutation.mutationType
        )
    }

    /**
     * When there is an existing creation mutation, and an update comes in,
     * the exiting creation should be updated with the contents of the incoming
     * mutation. The original creation mutation ID should be retained, for ordering.
     * @throws DataStoreException On failure to query the storage to examine which mutations were saved.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer.
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    @Throws(AmplifyException::class, InterruptedException::class)
    fun existingCreationIncomingUpdateRewritesExitingMutation() {
        // Arrange an existing creation mutation
        val modelInExistingMutation = BlogOwner.builder()
            .name("Papa Tony")
            .build()
        val existingCreation = PendingMutation.creation(modelInExistingMutation, schema)
        val existingCreationId = existingCreation.mutationId.toString()
        val completed = mutationOutbox.enqueue(existingCreation)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed)

        // Act: try to enqueue an update even whilst the creation is pending
        val modelInIncomingMutation = modelInExistingMutation.copyOfBuilder()
            .name("Tony Jr.")
            .build()
        val incomingUpdate = PendingMutation.update(modelInIncomingMutation, schema)
        val incomingUpdateId = incomingUpdate.mutationId.toString()
        val enqueueObserver = mutationOutbox.enqueue(incomingUpdate).test()

        // Assert: OK. The new mutation is accepted
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        enqueueObserver.assertComplete()

        // Assert: the existing mutation is still there, by id ....
        val recordsForExistingMutationId = storage.query(
            PersistentRecord::class.java,
            Where.identifier(
                PersistentRecord::class.java, existingCreationId
            )
        )
        assertEquals(1, recordsForExistingMutationId.size.toLong())

        // And the new one is not, by ID...
        val recordsForIncomingMutationId = storage.query(
            PersistentRecord::class.java,
            Where.identifier(
                PersistentRecord::class.java, incomingUpdateId
            )
        )
        assertEquals(0, recordsForIncomingMutationId.size.toLong())

        // However, the original mutation has been updated to include the contents of the
        // incoming mutation. This is true even whilst the mutation retains its original ID.
        val storedMutation = converter.fromRecord<BlogOwner>(recordsForExistingMutationId[0])
        // This is the name from the second model, not the first!
        assertEquals("Tony Jr.", storedMutation.mutatedItem.name)

        // There is a mutation in the outbox, it has the original ID.
        // This is STILL a creation, just using the new model data.
        assertEquals(
            PendingMutation.instance(
                existingCreation.mutationId,
                modelInIncomingMutation,
                schema,
                PendingMutation.Type.CREATE,
                QueryPredicates.all()
            ),
            mutationOutbox.peek()
        )
    }

    /**
     * When there is already a creation pending, and then we get a deletion for the same model ID,
     * we should just remove the creation. It means like "never mind, don't actually create."
     * @throws DataStoreException On failure to query storage for mutations state after test action.
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    @Throws(AmplifyException::class)
    fun existingCreationIncomingDeletionRemovesExisting() {
        val joe = BlogOwner.builder()
            .name("Original Joe")
            .build()
        val existingCreation = PendingMutation.creation(joe, schema)
        val existingCreationId = existingCreation.mutationId.toString()
        val completed = mutationOutbox.enqueue(existingCreation)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed)
        val incomingDeletion = PendingMutation.deletion(joe, schema)
        val incomingDeletionId = incomingDeletion.mutationId.toString()
        val otherEnqueueCompleted = mutationOutbox.enqueue(incomingDeletion)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(otherEnqueueCompleted)
        assertTrue(
            storage.query(
                PersistentRecord::class.java,
                Where.identifier(PersistentRecord::class.java, existingCreationId)
            ).isEmpty()
        )
        assertTrue(
            storage.query(
                PersistentRecord::class.java,
                Where.identifier(
                    PersistentRecord::class.java,
                    incomingDeletionId
                )
            ).isEmpty()
        )

        // There are no pending mutations.
        assertNull(mutationOutbox.peek())
    }

    /**
     * When there is already an existing update, and then a deletion comes in, we should
     * use the deletion, not the update. No sense in updating the record if you're just going to
     * delete it.
     * @throws DataStoreException On failure to query storage to inspect mutation records after test action.
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    @Throws(AmplifyException::class)
    fun existingUpdateIncomingDeletionOverwritesExisting() {
        val joe = BlogOwner.builder()
            .name("Original Joe")
            .build()
        val exitingUpdate = PendingMutation.update(joe, schema)
        val existingUpdateId = exitingUpdate.mutationId.toString()
        val completed =
            mutationOutbox.enqueue(exitingUpdate).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed)
        val incomingDeletion = PendingMutation.deletion(joe, schema)
        val incomingDeletionId = incomingDeletion.mutationId.toString()
        val otherEnqueueCompleted = mutationOutbox.enqueue(incomingDeletion)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(otherEnqueueCompleted)

        // The original mutation ID is preserved.
        val existingMutationRecords = storage.query(
            PersistentRecord::class.java,
            Where.identifier(
                PersistentRecord::class.java, existingUpdateId
            )
        )
        assertEquals(1, existingMutationRecords.size.toLong())

        // The new ID was discarded ....
        val incomingMutationRecords = storage.query(
            PersistentRecord::class.java,
            Where.identifier(
                PersistentRecord::class.java, incomingDeletionId
            )
        )
        assertEquals(0, incomingMutationRecords.size.toLong())

        // HOWEVER,
        // The stored mutation has the original ID, but it has become a deletion, not an update
        assertEquals(
            PendingMutation.Type.DELETE,
            converter.fromRecord<Model>(existingMutationRecords[0]).mutationType
        )

        // Able to get next mutation, it has the original ID
        // The model data doesn't really matter, since it only matches on model ID, anyway.
        // Importantly, the type is NOT update, but instead has become a deletion.
        assertEquals(
            PendingMutation.instance(
                exitingUpdate.mutationId,
                joe,
                schema,
                PendingMutation.Type.DELETE,
                QueryPredicates.all()
            ),
            mutationOutbox.peek()
        )
    }

    /**
     * If there is an existing deletion mutation, and then we get another one, update the original
     * with the new one.
     * @throws DataStoreException On failure to query storage for records.
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    @Throws(AmplifyException::class)
    fun existingDeletionIncomingDeletionOverwritesExisting() {
        val sammy = BlogOwner.builder()
            .name("Sammy Swanson")
            .build()
        val exitingDeletion = PendingMutation.deletion(sammy, schema)
        val incomingDeletion = PendingMutation.deletion(sammy, schema)
        assertNotEquals(exitingDeletion.mutationId, incomingDeletion.mutationId)
        val completed = mutationOutbox.enqueue(exitingDeletion)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed)
        val otherEnqueueCompleted = mutationOutbox.enqueue(incomingDeletion)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(otherEnqueueCompleted)

        // Existing record is still there
        val existingMutationRecords = storage.query(
            PersistentRecord::class.java,
            Where.identifier(
                PersistentRecord::class.java,
                exitingDeletion.mutationId.toString()
            )
        )
        assertEquals(1, existingMutationRecords.size.toLong())

        // Incoming is not present
        val incomingMutationRecords = storage.query(
            PersistentRecord::class.java,
            Where.identifier(
                PersistentRecord::class.java,
                incomingDeletion.mutationId.toString()
            )
        )
        assertEquals(0, incomingMutationRecords.size.toLong())

        // Still a deletion, as the next outbox item
        assertEquals(exitingDeletion, mutationOutbox.peek())
    }

    /**
     * When an already-pending mutation is updated, then the [Observable] returned by
     * [MutationOutbox.events] should emit an [OutboxEvent.CONTENT_AVAILABLE] event.
     */
    @Test
    fun updateEventPostedWhenExistingOutboxItemUpdate() {
        // Watch for events.
        val eventsObserver = mutationOutbox.events().test()

        // Create tony.
        val tonyWrongName = BlogOwner.builder()
            .name("Tony Jon Swanssssssssson yee-haw!")
            .build()
        val originalCreation = PendingMutation.creation(tonyWrongName, schema)
        val completed = mutationOutbox.enqueue(originalCreation)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed)

        // Update tony - we spelled his name wrong originally
        val tonySpelledRight = tonyWrongName.copyOfBuilder()
            .name("Tony Jon (\"TJ\") Swanson")
            .build()
        val otherEnqueueCompleted =
            mutationOutbox.enqueue(PendingMutation.update(tonySpelledRight, schema))
                .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(otherEnqueueCompleted)

        // Assert: an event for the original creation, then another for the update
        eventsObserver.awaitCount(2)
            .assertNoErrors()
            .assertNotComplete()
            .assertValues(OutboxEvent.CONTENT_AVAILABLE, OutboxEvent.CONTENT_AVAILABLE)
    }

    /**
     * When an new mutation is enqueued into the outbox, the [Observable] made available by
     * [MutationOutbox.events] should emit an [OutboxEvent.CONTENT_AVAILABLE] event.
     */
    @Test
    fun enqueueEventPostedWhenNewOutboxItemAdded() {
        // Watch for events.
        val eventsObserver = mutationOutbox.events().test()

        // Enqueue one
        val completed = mutationOutbox.enqueue(
            PendingMutation.deletion(
                BlogOwner.builder()
                    .name("Tony Swanson")
                    .build(),
                schema
            )
        ).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed)

        // Assert: we got an event!
        eventsObserver.awaitCount(1)
            .assertNoErrors()
            .assertNotComplete()
            .assertValue(OutboxEvent.CONTENT_AVAILABLE)
    }

    /**
     * If the queue contains multiple items, then
     * [MutationOutbox.getMutationForModelId]
     * returns the first one.
     * @throws DataStoreException On failure to arrange content into storage
     */
    @Test
    @Throws(DataStoreException::class)
    fun nextItemForModelIdReturnsFirstEnqueued() {
        val originalJoe = BlogOwner.builder()
            .name("Joe Swanson")
            .build()
        val firstMutation = PendingMutation.update(originalJoe, schema)
        storage.save(originalJoe, converter.toRecord(firstMutation))
        val updatedJoe = originalJoe.copyOfBuilder()
            .name("Joe Swanson, MD. (He finished med school, I guess?)")
            .build()
        val secondMutation = PendingMutation.update(updatedJoe, schema)
        storage.save(updatedJoe, converter.toRecord(secondMutation))
        val completed = mutationOutbox.load().blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed)
        assertEquals(
            firstMutation,
            mutationOutbox.getMutationForModelId(originalJoe.id, originalJoe.javaClass.simpleName)
        )
    }

    /**
     * Ordinarily, a DELETE would remote a CREATE, in front of it. But if that
     * create is marked in flight, we can't remove it. We have to enqueue the new
     * mutation.
     */
    @Test
    fun mutationEnqueuedIfExistingMutationIsInFlight() {
        // Arrange an existing mutation.
        val joe = BlogOwner.builder()
            .name("Joe")
            .build()
        val creation = PendingMutation.creation(joe, schema)
        val completed =
            mutationOutbox.enqueue(creation) // Act: mark it as in-flight, after enqueue.
                .andThen(mutationOutbox.markInFlight(creation.mutationId))
                .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed)

        // Now, look at what happens when we enqueue a new mutation.
        val deletion = PendingMutation.deletion(joe, schema)
        val otherEnqueueCompleted = mutationOutbox.enqueue(deletion)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(otherEnqueueCompleted)
        var next = mutationOutbox.peek()!!
        assertNotNull(next)
        assertEquals(creation, next)
        val removeCompleted = mutationOutbox.remove(next.mutationId)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(removeCompleted)
        next = mutationOutbox.peek()!!
        assertNotNull(next)
        assertEquals(deletion, next)
        val otherRemoveCompleted = mutationOutbox.remove(next.mutationId)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(otherRemoveCompleted)
        assertNull(mutationOutbox.peek())
    }

    /**
     * It is an error to mark an item as in-flight, if it isn't even in the dang queue.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(InterruptedException::class)
    fun errorWhenMarkingItemNotInQueue() {
        // Enqueue and remove a mutation.
        val tabby = BlogOwner.builder()
            .name("Tabitha Stevens of Beaver Falls, Idaho")
            .build()
        val creation = PendingMutation.creation(tabby, schema)
        val enqueueCompleted = mutationOutbox.enqueue(creation)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(enqueueCompleted)
        val removeCompleted = mutationOutbox.remove(creation.mutationId)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(removeCompleted)

        // Now, if we try to make that mutation as in-flight, its an error, since its already processed.
        val observer = mutationOutbox.markInFlight(creation.mutationId).test()
        observer.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        observer
            .assertError(DataStoreException::class.java)
            .assertError { error: Throwable ->
                error.message != null &&
                    error.message!!.contains("there was no mutation with that ID in the outbox")
            }
    }

    /**
     * When two creations for the same model are enqueued, the second should fail.  This is similar to
     * [.existingCreationIncomingCreationYieldsError], except that the Completable's from the two enqueue calls
     * are concatenated into the same stream.   The second enqueue should not check if an item exists in the queue
     * until the first enqueue is completed.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(InterruptedException::class)
    fun enqueueIsSynchronized() {
        // Arrange an existing creation mutation
        val modelInExistingMutation = BlogOwner.builder()
            .name("The Real Papa Tony")
            .build()
        val firstCreation = PendingMutation.creation(modelInExistingMutation, schema)
        val secondCreation = PendingMutation.creation(modelInExistingMutation, schema)
        val enqueueObserver = mutationOutbox.enqueue(firstCreation)
            .andThen(mutationOutbox.enqueue(secondCreation))
            .test()

        // Assert: caused a failure.
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        enqueueObserver.assertError { throwable: Throwable? -> throwable is DataStoreException }
    }

    /**
     * Attempting to remove an item from the queue which doesn't exist should throw an error.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(InterruptedException::class)
    fun removeIsSynchronized() {
        // Enqueue and remove a mutation.
        val tabby = BlogOwner.builder()
            .name("Tabitha Stevens of Beaver Falls, Idaho")
            .build()
        val creation = PendingMutation.creation(tabby, schema)
        val completed = mutationOutbox.enqueue(creation)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed)
        val observer = mutationOutbox.remove(creation.mutationId)
            .andThen(mutationOutbox.remove(creation.mutationId))
            .test()
        observer.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        observer
            .assertError(DataStoreException::class.java)
            .assertError { error: Throwable ->
                error.message != null &&
                    error.message!!.contains("there was no mutation with that ID in the outbox")
            }
    }

    /**
     * Marking an item in flight should throw an error if the item is already removed from the queue.  This is similar
     * to [.errorWhenMarkingItemNotInQueue], except that the removal and marking in flight Completables are
     * concatenated into the same stream.  This validates that markInFlight does not check if the item is in the queue
     * until after the removal is complete.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    @Throws(InterruptedException::class)
    fun markInFlightIsSynchronized() {
        // Enqueue and remove a mutation.
        val tabby = BlogOwner.builder()
            .name("Tabitha Stevens of Beaver Falls, Idaho")
            .build()
        val creation = PendingMutation.creation(tabby, schema)
        val completed = mutationOutbox.enqueue(creation)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed)
        val observer = mutationOutbox.remove(creation.mutationId)
            .andThen(mutationOutbox.markInFlight(creation.mutationId)).test()

        // Now, we should see an error since we can't mark a mutation as in-flight that has already been removed.
        observer.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        observer
            .assertError(DataStoreException::class.java)
            .assertError { error: Throwable ->
                error.message != null &&
                    error.message!!.contains("there was no mutation with that ID in the outbox")
            }
    }

    @Test
    fun fetchPendingMutationsReturnsExpectedIds() {
        val blogOwners = mutableListOf<BlogOwner>()
        for (i in 0 until 975) {
            val blogOwner = BlogOwner.builder()
                .name("Name$i")
                .id("ID$i")
                .build()
            blogOwners.add(blogOwner)
            val creation = PendingMutation.creation(blogOwner, schema)
            val completed = mutationOutbox.enqueue(creation).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            assertTrue(completed)
        }
        val expectedResult = blogOwners.map { it.id }.toSet()

        val result = mutationOutbox.fetchPendingMutations(blogOwners, blogOwners[0].javaClass.name, true)

        assertEquals(975, result.size)
        assertEquals(expectedResult, result)
    }

    @Test
    fun fetchPendingMutationsExcludesInFlight() {
        val b1 = BlogOwner.builder()
            .name("Name1")
            .id("ID1")
            .build()
        val p1 = PendingMutation.creation(b1, schema)
        val completed1 = mutationOutbox.enqueue(p1)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed1)
        val markedInFlight = mutationOutbox.markInFlight(p1.mutationId).blockingAwait(1, TimeUnit.SECONDS)
        assertTrue(markedInFlight)

        val b2 = BlogOwner.builder()
            .name("Name2")
            .id("ID2")
            .build()
        val completed2 = mutationOutbox.enqueue(PendingMutation.creation(b2, schema))
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(completed2)
        val expectedResult = setOf(b2.id)

        val result = mutationOutbox.fetchPendingMutations(listOf(b1, b2), BlogOwner::class.java.name, true)

        assertEquals(1, result.size)
        assertEquals(expectedResult, result)
    }

    private fun hasPendingMutation(modelId: String, modelClass: String): Boolean {
        return mutationOutbox.getMutationForModelId(modelId, modelClass) != null
    }

    private fun assertRecordCountForMutationId(mutationId: String, expectedCount: Int) {
        val recordsForExistingMutationId = getPendingMutationRecordFromStorage(mutationId)
        assertEquals(expectedCount.toLong(), recordsForExistingMutationId.size.toLong())
    }

    private fun getPendingMutationRecordFromStorage(mutationId: String): List<PersistentRecord> {
        return storage.query(
            PersistentRecord::class.java,
            Where.identifier(
                PersistentRecord::class.java, mutationId
            )
        )
    }

    private val allPendingMutationRecordFromStorage: MutableList<PersistentRecord>
        get() = storage.query(PersistentRecord::class.java, Where.matchesAll())

    companion object {
        private val TIMEOUT_MS = TimeUnit.SECONDS.toMillis(1)
    }
}
