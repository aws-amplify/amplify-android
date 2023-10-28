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
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.datastore.syncengine.MutationOutbox.OutboxEvent;
import com.amplifyframework.datastore.syncengine.PendingMutation.PersistentRecord;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.testmodels.commentsblog.Author;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.commentsblog.PostStatus;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.TestObserver;

import static com.amplifyframework.datastore.syncengine.TestHubEventFilters.isEnqueued;
import static com.amplifyframework.datastore.syncengine.TestHubEventFilters.isOutboxEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link MutationOutbox}.
 */
@RunWith(RobolectricTestRunner.class)
public final class PersistentMutationOutboxTest {

    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(1);

    private ModelSchema schema;
    private PersistentMutationOutbox mutationOutbox;
    private PendingMutation.Converter converter;
    private SynchronousStorageAdapter storage;

    /**
     * Set up the object under test.
     * @throws AmplifyException on failure to build schema
     */
    @Before
    public void setup() throws AmplifyException {
        schema = ModelSchema.fromModelClass(BlogOwner.class);
        InMemoryStorageAdapter inMemoryStorageAdapter = InMemoryStorageAdapter.create();
        storage = SynchronousStorageAdapter.delegatingTo(inMemoryStorageAdapter);
        mutationOutbox = new PersistentMutationOutbox(inMemoryStorageAdapter);
        converter = new GsonPendingMutationConverter();
    }

    /**
     * Enqueueing a mutation should publish current outbox status.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void outboxStatusIsPublishedToHubOnEnqueue() throws InterruptedException {
        BlogOwner raphael = BlogOwner.builder()
                .name("Raphael Kim")
                .build();
        PendingMutation<BlogOwner> createRaphael = PendingMutation.creation(raphael, schema);

        // Start listening for publication events.
        // outbox should not be empty
        HubAccumulator statusAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, isOutboxEmpty(false), 1)
                .start();

        // Enqueue an save for a Raphael BlogOwner object,
        // and make sure that outbox status is published to hub.
        TestObserver<Void> saveObserver = mutationOutbox.enqueue(createRaphael).test();
        saveObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        saveObserver.assertNoErrors().assertComplete();
        saveObserver.dispose();
        statusAccumulator.await();
    }

    /**
     * Enqueueing a mutation should have the result of persisting
     * the mutation to storage, and notifying any observers that
     * a new mutation has been enqueued.
     * @throws DataStoreException On failure to query results, for assertions
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void enqueuePersistsMutationAndNotifiesObserver() throws DataStoreException, InterruptedException {
        // Observe the queue
        TestObserver<OutboxEvent> queueObserver = mutationOutbox.events().test();

        BlogOwner jameson = BlogOwner.builder()
            .name("Jameson Williams")
            .build();
        PendingMutation<BlogOwner> createJameson = PendingMutation.creation(jameson, schema);
        HubAccumulator savedMutationsAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, isEnqueued(jameson), 1)
                .start();

        // Enqueue an save for a Jameson BlogOwner object,
        // and make sure that it calls back onComplete().
        TestObserver<Void> saveObserver = mutationOutbox.enqueue(createJameson).test();
        saveObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        saveObserver.assertNoErrors().assertComplete();
        saveObserver.dispose();

        // Wait for a Hub event telling us that the model got successfully enqueued.
        savedMutationsAccumulator.await();

        // Expected to observe the mutation on the subject
        queueObserver.awaitCount(1);
        queueObserver.assertValue(OutboxEvent.CONTENT_AVAILABLE);
        queueObserver.dispose();

        // Assert that the storage contains the mutation
        assertEquals(
            Collections.singletonList(converter.toRecord(createJameson)),
            storage.query(PersistentRecord.class)
        );
        assertTrue(mutationOutbox.hasPendingMutation(jameson.getId(), jameson.getClass().getName()));
        assertEquals(createJameson, mutationOutbox.peek());
    }

    /**
     * The enqueue() returns a Completable, but that Completable doesn't actually invoke
     * any behavior until it is subscribed.
     * @throws DataStoreException On failure to query results, for assertions
     */
    @Test
    public void enqueueDoesNothingBeforeSubscription() throws DataStoreException {
        // Watch for notifications on the observe() API.
        TestObserver<OutboxEvent> testObserver = mutationOutbox.events().test();

        // Enqueue something, but don't subscribe to the observable just yet.
        BlogOwner tony = BlogOwner.builder()
            .name("Tony Daniels")
            .build();
        mutationOutbox.enqueue(PendingMutation.creation(tony, schema));
        // .subscribe() is NOT called on the enqueue() above!! This is the point!!!

        // Note that nothing has actually happened yet --
        // Nothing was put out on the observable ...
        testObserver.assertNoValues();
        testObserver.assertNotComplete();
        testObserver.dispose();

        // And nothing is in storage.
        assertTrue(storage.query(PersistentRecord.class).isEmpty());

        // And nothing is peek()ed.
        assertNull(mutationOutbox.peek());
    }

    /**
     * Calling load() will populate the outbox with content from disk.
     * @throws DataStoreException On failure to arrange models into storage before test action
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void loadPreparesOutbox() throws DataStoreException, InterruptedException {
        // Arrange: some mutations.
        BlogOwner tony = BlogOwner.builder()
            .name("Tony Daniels")
            .build();
        PendingMutation<BlogOwner> updateTony = PendingMutation.update(tony, schema);
        BlogOwner sam = BlogOwner.builder()
            .name("Sam Watson")
            .build();
        PendingMutation<BlogOwner> insertSam = PendingMutation.creation(sam, schema);
        storage.save(converter.toRecord(updateTony), converter.toRecord(insertSam));

        // Act: load the outbox.
        TestObserver<Void> loadObserver = mutationOutbox.load().test();

        // Assert: load worked.
        loadObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        loadObserver.assertNoErrors().assertComplete();
        loadObserver.dispose();

        // Assert: items are in the outbox.
        assertTrue(mutationOutbox.hasPendingMutation(tony.getId(), tony.getClass().getName()));
        assertTrue(mutationOutbox.hasPendingMutation(sam.getId(), sam.getClass().getName()));

        // Tony is first, since he is the older of the two mutations.
        assertEquals(updateTony, mutationOutbox.peek());
    }

    /**
     * Tests {@link MutationOutbox#remove(TimeBasedUuid)}.
     * @throws DataStoreException On failure to query results, for assertions
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void removeRemovesChangesFromQueue() throws DataStoreException, InterruptedException {
        // Arrange: there is a change in the queue.
        BlogOwner bill = BlogOwner.builder()
            .name("Bill Gates")
            .build();
        PendingMutation<BlogOwner> deleteBillGates = PendingMutation.deletion(bill, schema);
        storage.save(converter.toRecord(deleteBillGates));
        boolean completed = mutationOutbox.load().blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(completed);

        TestObserver<Void> testObserver = mutationOutbox.remove(deleteBillGates.getMutationId()).test();

        testObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        testObserver.assertNoErrors().assertComplete();
        testObserver.dispose();

        assertEquals(0, storage.query(PersistentRecord.class).size());

        assertNull(mutationOutbox.peek());
        assertFalse(mutationOutbox.hasPendingMutation(bill.getId(), bill.getClass().getName()));
    }

    /**
     * When there are multiple pending mutations in the outbox, and one is removed,
     * we will see a {@link OutboxEvent#CONTENT_AVAILABLE} after the removal. This
     * notifies the system that there is more work to be done, even though we've successfully
     * processed an event. The system will continue processing items from the outbox until
     * all have been processed.
     * @throws InterruptedException If thread interrupted while waiting for events
     */
    @Test
    public void notifiesWhenContentAvailableAfterDelete() throws InterruptedException {
        // Start watching the events stream. We'll expect a notification here 3 times:
        // after the first enqueue, after the second enqueue, after the first deletion.
        TestObserver<OutboxEvent> enqueueEventObserver = mutationOutbox.events().test();

        // Arrange a few mutations into the queue.
        BlogOwner senatorBernie = BlogOwner.builder()
            .name("Senator Bernard Sanders")
            .build();
        PendingMutation<BlogOwner> createSenatorBernie = PendingMutation.creation(senatorBernie, schema);
        boolean createCompleted = mutationOutbox.enqueue(createSenatorBernie)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(createCompleted);

        BlogOwner sam = BlogOwner.builder()
                .name("Sam Watson")
                .build();
        PendingMutation<BlogOwner> insertSam = PendingMutation.creation(sam, schema);
        boolean updateCompleted = mutationOutbox.enqueue(insertSam)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(updateCompleted);

        enqueueEventObserver
                .awaitCount(2)
                .assertValues(OutboxEvent.CONTENT_AVAILABLE, OutboxEvent.CONTENT_AVAILABLE)
                .assertNoErrors();

        TestObserver<OutboxEvent> firstRemoveEventObserver = mutationOutbox.events().test();

        // Remove first item.
        TestObserver<Void> firstRemoval = mutationOutbox.remove(createSenatorBernie.getMutationId()).test();
        firstRemoval.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        firstRemoval
            .assertNoErrors()
            .assertComplete()
            .dispose();

        // One event is observed on events(), since there are still some pending mutations
        // that need to be processed.
        firstRemoveEventObserver
            .awaitCount(1)
            .assertValues(OutboxEvent.CONTENT_AVAILABLE)
            .assertNoErrors();

        // Get ready to watch the events() again.
        TestObserver<OutboxEvent> secondRemoveEventObserver = mutationOutbox.events().test();

        // Remove the next item.
        TestObserver<Void> secondRemoval = mutationOutbox.remove(insertSam.getMutationId()).test();
        secondRemoval.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        secondRemoval
            .assertNoErrors()
            .assertComplete()
            .dispose();

        // This time, we don't see any event on events(), since the outbox has become empty.
        secondRemoveEventObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        secondRemoveEventObserver.assertNoValues().assertNoErrors();
    }

    /**
     * When there is a pending mutation for a particular model ID
     * {@link MutationOutbox#hasPendingMutation(String)} must say "yes!".
     */
    @Test
    public void hasPendingMutationReturnsTrueForExistingModelMutation() {
        String modelId = RandomString.string();
        BlogOwner joe = BlogOwner.builder()
            .name("Joe")
            .id(modelId)
            .build();
        TimeBasedUuid mutationId = TimeBasedUuid.create();
        PendingMutation<BlogOwner> pendingMutation = PendingMutation.instance(
            mutationId, joe, schema, PendingMutation.Type.CREATE, QueryPredicates.all()
        );
        boolean completed = mutationOutbox.enqueue(pendingMutation).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);

        assertTrue(completed);
        assertTrue(mutationOutbox.hasPendingMutation(modelId, joe.getClass().getName()));
        assertFalse(mutationOutbox.hasPendingMutation(mutationId.toString(), mutationId.getClass().getName()));
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
    public void hasPendingMutationReturnsFalseForItemNotInStore() throws DataStoreException {
        String joeId = RandomString.string();
        BlogOwner joe = BlogOwner.builder()
            .name("Joe Swanson III")
            .id(joeId)
            .build();
        storage.save(joe);

        TimeBasedUuid mutationId = TimeBasedUuid.create();
        PendingMutation<BlogOwner> unrelatedMutation = PendingMutation.instance(
            mutationId, joe, schema, PendingMutation.Type.CREATE, QueryPredicates.all()
        );

        assertFalse(mutationOutbox.hasPendingMutation(joeId, joe.getClass().getName()));
        assertFalse(mutationOutbox.hasPendingMutation(unrelatedMutation.getMutationId().toString(),
                unrelatedMutation.getClass().getName()));
    }

    /**
     * When queuing record to mutationOutbox for a model, hasPendingMutation should return false
     * for other existing models with same primary key values.
     *
     * @throws AmplifyException On failure to convert the modelClass item to ModelSchema
     */
    @Test
    public void hasPendingMutationReturnsFalseForModelMutationWithSamePrimaryKeyForDifferentModels()
            throws AmplifyException {

        String modelId = RandomString.string();
        BlogOwner blogOwner = BlogOwner.builder()
                .name("Sample BlogOwner")
                .id(modelId)
                .build();
        TimeBasedUuid mutationId = TimeBasedUuid.create();

        PendingMutation<BlogOwner> pendingBlogOwnerMutation = PendingMutation.instance(
                mutationId, blogOwner, schema, PendingMutation.Type.CREATE, QueryPredicates.all()
        );

        // Act & Assert: Enqueue and verify BlogOwner
        assertTrue(mutationOutbox.enqueue(pendingBlogOwnerMutation).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mutationOutbox.hasPendingMutation(modelId, blogOwner.getClass().getName()));

        // Act & Assert: Enqueue and verify Author
        Author author = Author.builder()
                .name("Sample Author")
                .id(modelId)
                .build();

        // Check hasPendingMutation returns False for Author with same Primary Key (id) as BlogOwner
        assertFalse(mutationOutbox.hasPendingMutation(modelId, author.getClass().getName()));

        PendingMutation<Author> pendingAuthorMutation = PendingMutation.instance(
                mutationId, author, ModelSchema.fromModelClass(Author.class),
                PendingMutation.Type.CREATE, QueryPredicates.all()
        );
        assertTrue(mutationOutbox.enqueue(pendingAuthorMutation).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Make sure Author Mutation is stored
        assertTrue(mutationOutbox.hasPendingMutation(modelId, author.getClass().getName()));

        // Act & Assert: Enqueue and verify Author
        Post post = Post.builder()
                .title("Sample Author")
                .status(PostStatus.ACTIVE)
                .rating(1)
                .id(modelId)
                .build();

        // Check hasPendingMutation returns False for Post with same Primary Key (id) as BlogOwner
        assertFalse(mutationOutbox.hasPendingMutation(modelId, post.getClass().getName()));

        PendingMutation<Post> pendingPostMutation = PendingMutation.instance(
                mutationId, post, ModelSchema.fromModelClass(Post.class),
                PendingMutation.Type.CREATE, QueryPredicates.all()
        );
        assertTrue(mutationOutbox.enqueue(pendingPostMutation).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Make sure Post Mutation is stored
        assertTrue(mutationOutbox.hasPendingMutation(modelId, post.getClass().getName()));
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
    public void mutationEnqueueForModelWithDuplicatePrimaryKeyThrowsDatastoreException() throws
            AmplifyException, InterruptedException {

        // Arrange: Create and enqueue an initial BlogOwner mutation
        String modelId = RandomString.string();
        BlogOwner existingBlogOwner = BlogOwner.builder()
                .name("Sample BlogOwner")
                .id(modelId)
                .build();

        PendingMutation<BlogOwner> existingCreation = PendingMutation.creation(existingBlogOwner, schema);
        String existingCreationId = existingCreation.getMutationId().toString();
        assertTrue(mutationOutbox.enqueue(existingCreation).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Arrange: Create a new BlogOwner with the same ID as the existing one
        BlogOwner duplicateBlogOwner = BlogOwner.builder()
                .name("Sample BlogOwner")
                .id(modelId)
                .build();
        PendingMutation<BlogOwner> duplicateMutation =
                PendingMutation.creation(duplicateBlogOwner, schema);
        String duplicateMutationId = duplicateMutation.getMutationId().toString();

        // Act: Attempt to enqueue the duplicate mutation
        TestObserver<Void> enqueueObserver = mutationOutbox.enqueue(duplicateMutation).test();

        // Assert: Verify that a DataStoreException is thrown
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        enqueueObserver.assertError(throwable -> throwable instanceof DataStoreException);

        // Assert: original mutation is present, but the new one isn't.
        PersistentRecord storedMutation = storage.query(PersistentRecord.class,
                Where.identifier(PersistentRecord.class, existingCreationId)).get(0);
        assertEquals(existingBlogOwner, converter.fromRecord(storedMutation).getMutatedItem());
        assertTrue(storage.query(PersistentRecord.class,
                Where.identifier(PersistentRecord.class, duplicateMutationId)).isEmpty());

        // Additional Checks: Peek the Mutation outbox, existing mutation should be present.
        assertTrue(mutationOutbox.hasPendingMutation(existingBlogOwner.getPrimaryKeyString(),
                existingBlogOwner.getClass().getName()));
        assertEquals(existingCreation, mutationOutbox.peek());
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
    public void existingCreationIncomingCreationYieldsError() throws AmplifyException, InterruptedException {
        // Arrange an existing creation mutation
        BlogOwner modelInExistingMutation = BlogOwner.builder()
            .name("The Real Papa Tony")
            .build();
        PendingMutation<BlogOwner> existingCreation =
            PendingMutation.creation(modelInExistingMutation, schema);
        String existingCreationId = existingCreation.getMutationId().toString();
        boolean completed = mutationOutbox.enqueue(existingCreation).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(completed);

        // Act: try to create the blog owner again -- but there's already a pending creation
        BlogOwner modelInIncomingMutation = modelInExistingMutation.copyOfBuilder()
            .name("Someone Posing as Papa Tony Who isn't \uD83D\uDCAF legit.")
            .build();
        PendingMutation<BlogOwner> incomingCreation =
            PendingMutation.creation(modelInIncomingMutation, schema);
        String incomingCreationId = incomingCreation.getMutationId().toString();
        TestObserver<Void> enqueueObserver = mutationOutbox.enqueue(incomingCreation).test();

        // Assert: caused a failure.
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        enqueueObserver.assertError(throwable -> throwable instanceof DataStoreException);

        // Assert: original mutation is present, but the new one isn't.
        PersistentRecord storedMutation = storage.query(PersistentRecord.class, Where.identifier(PersistentRecord.class,
                existingCreationId)).get(0);
        assertEquals(modelInExistingMutation, converter.fromRecord(storedMutation).getMutatedItem());
        assertTrue(storage.query(PersistentRecord.class,
                Where.identifier(PersistentRecord.class, incomingCreationId)).isEmpty());

        // Existing mutation still attainable as next mutation (right now, its the ONLY mutation in outbox)
        assertTrue(mutationOutbox.hasPendingMutation(modelInExistingMutation.getPrimaryKeyString(),
                modelInExistingMutation.getClass().getName()));
        assertEquals(existingCreation, mutationOutbox.peek());
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
    public void existingUpdateIncomingCreationYieldsError() throws AmplifyException, InterruptedException {
        // Arrange an existing update mutation
        BlogOwner modelInExistingMutation = BlogOwner.builder()
            .name("Tony with improvements applied")
            .build();
        PendingMutation<BlogOwner> existingUpdate =
            PendingMutation.update(modelInExistingMutation, schema);
        String exitingUpdateId = existingUpdate.getMutationId().toString();
        boolean completed = mutationOutbox.enqueue(existingUpdate).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(completed);

        // Act: try to CREATE tony again -- but isn't he already created, if there's an update?
        BlogOwner modelInIncomingMutation = modelInExistingMutation.copyOfBuilder()
            .name("Brand new tony")
            .build();
        PendingMutation<BlogOwner> incomingCreation =
            PendingMutation.creation(modelInIncomingMutation, schema);
        String incomingCreationId = incomingCreation.getMutationId().toString();
        TestObserver<Void> enqueueObserver = mutationOutbox.enqueue(incomingCreation).test();

        // Assert: caused a failure.
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        enqueueObserver.assertError(throwable -> throwable instanceof DataStoreException);

        // Assert: original mutation is present, but the new one isn't.
        PersistentRecord storedMutation = storage.query(PersistentRecord.class, Where.identifier(PersistentRecord.class,
                exitingUpdateId)).get(0);
        assertEquals(modelInExistingMutation, converter.fromRecord(storedMutation).getMutatedItem());
        assertTrue(storage.query(PersistentRecord.class, Where.identifier(PersistentRecord.class,
                incomingCreationId)).isEmpty());

        // Existing mutation still attainable as next mutation (right now, its the ONLY mutation in outbox)
        assertTrue(mutationOutbox.hasPendingMutation(modelInExistingMutation.getPrimaryKeyString(),
                modelInExistingMutation.getClass().getName()));
        assertEquals(existingUpdate, mutationOutbox.peek());
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
    public void existingDeletionIncomingCreationYieldsError() throws AmplifyException, InterruptedException {
        // Arrange an existing deletion mutation
        BlogOwner modelInExistingMutation = BlogOwner.builder()
            .name("Papa Tony")
            .build();
        PendingMutation<BlogOwner> existingDeletion =
            PendingMutation.deletion(modelInExistingMutation, schema);
        String existingDeletionId = existingDeletion.getMutationId().toString();
        boolean completed = mutationOutbox.enqueue(existingDeletion).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(completed);

        // Act: try to create tony, but wait -- if we're already deleting him...
        BlogOwner modelInIncomingMutation = modelInExistingMutation.copyOfBuilder()
            .name("Tony Jr.")
            .build();
        PendingMutation<BlogOwner> incomingCreation =
            PendingMutation.creation(modelInIncomingMutation, schema);
        String incomingCreationId = incomingCreation.getMutationId().toString();
        TestObserver<Void> enqueueObserver = mutationOutbox.enqueue(incomingCreation).test();

        // Assert: caused a failure.
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        enqueueObserver.assertError(throwable -> throwable instanceof DataStoreException);

        // Assert: original mutation is present, but the new one isn't.
        PersistentRecord storedMutation = storage.query(PersistentRecord.class,
                Where.identifier(PersistentRecord.class, existingDeletionId)).get(0);
        assertEquals(modelInExistingMutation, converter.fromRecord(storedMutation).getMutatedItem());
        assertTrue(storage.query(PersistentRecord.class,
                Where.identifier(PersistentRecord.class, incomingCreationId)).isEmpty());

        // Existing mutation still attainable as next mutation (right now, its the ONLY mutation in outbox)
        assertTrue(mutationOutbox.hasPendingMutation(modelInExistingMutation.getPrimaryKeyString(),
                modelInExistingMutation.getClass().getName()));
        assertEquals(existingDeletion, mutationOutbox.peek());
    }

    /**
     * If there is a pending deletion, enqueuing an update will fail, since the thing being
     * updated is not meant to exist.
     * @throws DataStoreException On failure to query storage, for the purpose of asserting the
     *                            state of mutations after the test action.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer.
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    public void existingDeletionIncomingUpdateYieldsError() throws AmplifyException, InterruptedException {
        // Arrange an existing deletion mutation
        BlogOwner modelInExistingMutation = BlogOwner.builder()
            .name("Papa Tony")
            .build();
        PendingMutation<BlogOwner> existingDeletion =
            PendingMutation.deletion(modelInExistingMutation, schema);
        String existingDeletionId = existingDeletion.getMutationId().toString();
        boolean completed = mutationOutbox.enqueue(existingDeletion).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(completed);

        // Act: try to update tony, but wait ... aren't we deleting tony?
        BlogOwner modelInIncomingMutation = modelInExistingMutation.copyOfBuilder()
            .name("Tony Jr.")
            .build();
        PendingMutation<BlogOwner> incomingUpdate =
            PendingMutation.update(modelInIncomingMutation, schema);
        String incomingUpdateId = incomingUpdate.getMutationId().toString();
        TestObserver<Void> enqueueObserver = mutationOutbox.enqueue(incomingUpdate).test();

        // Assert: caused a failure.
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        enqueueObserver.assertError(throwable -> throwable instanceof DataStoreException);

        // Assert: original mutation is present, but the new one isn't.
        PersistentRecord storedMutation = storage.query(PersistentRecord.class,
                Where.identifier(PersistentRecord.class, existingDeletionId)).get(0);
        assertEquals(modelInExistingMutation, converter.fromRecord(storedMutation).getMutatedItem());
        assertTrue(storage.query(PersistentRecord.class, Where.identifier(PersistentRecord.class,
                incomingUpdateId)).isEmpty());

        // Existing mutation still attainable as next mutation (right now, its the ONLY mutation in outbox)
        assertTrue(mutationOutbox.hasPendingMutation(modelInExistingMutation.getPrimaryKeyString(),
                modelInExistingMutation.getClass().getName()));
        assertEquals(existingDeletion, mutationOutbox.peek());
    }

    /**
     * When there is an existing update mutation, and a new update mutation with condition
     * comes in, then the existing one should remain and the new one should be appended.
     * @throws DataStoreException On failure to query storage for current mutations state.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer.
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    public void existingUpdateIncomingUpdateWithConditionAppendsMutation()
            throws AmplifyException, InterruptedException {
        // Arrange an existing update mutation
        BlogOwner modelInExistingMutation = BlogOwner.builder()
            .name("Papa Tony")
            .build();
        PendingMutation<BlogOwner> existingUpdate =
            PendingMutation.update(modelInExistingMutation, schema);
        String existingUpdateId = existingUpdate.getMutationId().toString();
        boolean completed = mutationOutbox.enqueue(existingUpdate).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(completed);

        // Act: try to enqueue a new update mutation when there already is one
        BlogOwner modelInIncomingMutation = modelInExistingMutation.copyOfBuilder()
            .name("Tony Jr.")
            .build();
        PendingMutation<BlogOwner> incomingUpdate =
            PendingMutation.update(modelInIncomingMutation, schema, BlogOwner.NAME.eq("Papa Tony"));
        String incomingUpdateId = incomingUpdate.getMutationId().toString();
        TestObserver<Void> enqueueObserver = mutationOutbox.enqueue(incomingUpdate).test();

        // Assert: OK. The new mutation is accepted
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        enqueueObserver.assertComplete();

        // Assert: the existing mutation is still there, by id ....
        List<PersistentRecord> recordsForExistingMutationId = storage.query(PersistentRecord.class,
                Where.identifier(PersistentRecord.class, existingUpdateId));
        assertEquals(1, recordsForExistingMutationId.size());

        // Assert: And the new one is also there
        List<PendingMutation.PersistentRecord> recordsForIncomingMutationId =
            storage.query(PersistentRecord.class, Where.identifier(PersistentRecord.class, incomingUpdateId));
        assertEquals(1, recordsForIncomingMutationId.size());

        // The original mutation should remain as is
        PendingMutation<BlogOwner> existingStoredMutation = converter.fromRecord(recordsForExistingMutationId.get(0));
        // This is the name from the second model, not the first!!
        assertEquals(modelInExistingMutation.getName(), existingStoredMutation.getMutatedItem().getName());

        PendingMutation<? extends Model> next = mutationOutbox.peek();
        assertNotNull(next);
        // The first one should be the existing mutation
        assertEquals(
            existingUpdate,
            next
        );
        // Remove the first one from the queue
        boolean removeCompleted = mutationOutbox.remove(existingUpdate.getMutationId())
                .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(removeCompleted);

        // Get the next one
        next = mutationOutbox.peek();
        assertNotNull(next);
        // The first one should be the existing mutation
        assertEquals(
            incomingUpdate,
            next
        );
    }

    /**
     * When there is an existing update mutation, and a new update mutation comes in,
     * then we need to remove any existing mutations for that modelId and create the new one.
     * @throws DataStoreException On failure to query storage for current mutations state.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer.
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    public void existingUpdateIncomingUpdateWithoutConditionRewritesExistingMutation()
            throws AmplifyException, InterruptedException {
        // Arrange an existing update mutation
        BlogOwner modelInExistingMutation = BlogOwner.builder()
            .name("Papa Tony")
            .build();
        PendingMutation<BlogOwner> existingUpdate =
            PendingMutation.update(modelInExistingMutation, schema);
        String existingUpdateId = existingUpdate.getMutationId().toString();
        mutationOutbox.enqueue(existingUpdate).blockingAwait();

        // Act: try to enqueue a new update mutation when there already is one
        BlogOwner modelInIncomingMutation = modelInExistingMutation.copyOfBuilder()
            .name("Tony Jr.")
            .build();
        PendingMutation<BlogOwner> incomingUpdate =
            PendingMutation.update(modelInIncomingMutation, schema);
        String incomingUpdateId = incomingUpdate.getMutationId().toString();
        TestObserver<Void> enqueueObserver = mutationOutbox.enqueue(incomingUpdate).test();

        // Assert: OK. The new mutation is accepted
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        enqueueObserver.assertComplete();

        // Assert: the existing mutation has been removed
        assertRecordCountForMutationId(existingUpdateId, 0);

        // And the new one has been added to the queue
        assertRecordCountForMutationId(incomingUpdateId, 1);

        // Ensure the new one is in storage.
        PendingMutation<BlogOwner> storedMutation =
            converter.fromRecord(getPendingMutationRecordFromStorage(incomingUpdateId).get(0));
        // This is the name from the second model, not the first!!
        assertEquals(modelInIncomingMutation.getName(), storedMutation.getMutatedItem().getName());

        // The mutation in the outbox is the incoming one.
        assertEquals(
            incomingUpdate,
            mutationOutbox.peek()
        );
    }

    /**
     * When there is an existing SerializedModel update mutation, and a new SerializedModel update mutation comes in,
     * then we need to merge any existing mutations for that modelId and create the new one of type Update.
     * @throws AmplifyException On failure to find the serializedModel difference.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void
        existingSerializedModelUpdateIncomingUpdateWithoutConditionMergesWithExistingMutation()
            throws AmplifyException, InterruptedException {
        // Arrange an existing update mutation
        BlogOwner modelInSqlLite = BlogOwner.builder()
                .name("Papa Tony")
                .wea("Something")
                .build();

        BlogOwner initialUpdate = BlogOwner.builder()
                .name("Tony Jr")
                .id(modelInSqlLite.getPrimaryKeyString())
                .build();

        PendingMutation<SerializedModel> initialUpdatePendingMutation =
                PendingMutation.update(SerializedModel.difference(initialUpdate, modelInSqlLite, schema), schema);
        String existingUpdateId = initialUpdatePendingMutation.getMutationId().toString();
        mutationOutbox.enqueue(initialUpdatePendingMutation).blockingAwait();

        // Act: try to enqueue a new update mutation when there already is one
        BlogOwner incomingUpdatedModel = BlogOwner.builder()
                .name("Papa Tony")
                .wea("something else")
                .id(modelInSqlLite.getPrimaryKeyString())
                .build();
        PendingMutation<SerializedModel> incomingUpdate = PendingMutation.update(
                SerializedModel.difference(incomingUpdatedModel, modelInSqlLite, schema),
                schema);
        String incomingUpdateId = incomingUpdate.getMutationId().toString();
        TestObserver<Void> enqueueObserver = mutationOutbox.enqueue(incomingUpdate).test();

        // Assert: OK. The new mutation is accepted
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        enqueueObserver.assertComplete();

        // Assert: the existing mutation has been removed
        assertRecordCountForMutationId(existingUpdateId, 0);

        // And the new one has been added to the queue
        assertRecordCountForMutationId(incomingUpdateId, 0);

        List<PersistentRecord> pendingMutationsFromStorage = getAllPendingMutationRecordFromStorage();
        for (PersistentRecord record : pendingMutationsFromStorage) {
            if (!record.getContainedModelId().equals(incomingUpdate.getMutatedItem().resolveIdentifier())) {
                pendingMutationsFromStorage.remove(record);
            }
        }
        // Ensure the new one is in storage.
        PendingMutation<SerializedModel> storedMutation =
                converter.fromRecord(pendingMutationsFromStorage.get(0));
        // This is the name from the second model, not the first!!
        assertEquals(initialUpdate.getName(),
                storedMutation.getMutatedItem().getSerializedData().get("name"));
        // wea got merged from existing model!!
        assertEquals(incomingUpdatedModel.getWea(),
                storedMutation.getMutatedItem().getSerializedData().get("wea"));
        assertEquals(PendingMutation.Type.UPDATE,
                storedMutation.getMutationType());
    }

    /**
     * When there is an existing SerializedModel create mutation, and a new SerializedModel update mutation comes in,
     * then we need to merge any existing mutations for that modelId and create the new one of type Create.
     * @throws AmplifyException On failure to find the serializedModel difference.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void existingSerializedModelCreateIncomingUpdateMergesWithExistingMutation()
            throws AmplifyException, InterruptedException {
        // Arrange an existing update mutation
        BlogOwner modelInSqlLite = BlogOwner.builder()
                .name("Papa Tony")
                .wea("Something")
                .build();

        BlogOwner initialUpdate = BlogOwner.builder()
                .name("Tony Jr")
                .id(modelInSqlLite.getPrimaryKeyString())
                .build();

        PendingMutation<SerializedModel> initialUpdatePendingMutation =
                PendingMutation.creation(SerializedModel.difference(initialUpdate, modelInSqlLite, schema), schema);
        String existingUpdateId = initialUpdatePendingMutation.getMutationId().toString();
        mutationOutbox.enqueue(initialUpdatePendingMutation).blockingAwait();

        // Act: try to enqueue a new update mutation when there already is one
        BlogOwner incomingUpdatedModel = BlogOwner.builder()
                .name("Papa Tony")
                .wea("something else")
                .id(modelInSqlLite.getPrimaryKeyString())
                .build();
        PendingMutation<SerializedModel> incomingUpdate = PendingMutation.update(
                SerializedModel.difference(incomingUpdatedModel, modelInSqlLite, schema),
                schema);
        String incomingUpdateId = incomingUpdate.getMutationId().toString();
        TestObserver<Void> enqueueObserver = mutationOutbox.enqueue(incomingUpdate).test();

        // Assert: OK. The new mutation is accepted
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        enqueueObserver.assertComplete();

        // Assert: the existing mutation has been removed
        assertRecordCountForMutationId(existingUpdateId, 0);

        // And the new one has been added to the queue
        assertRecordCountForMutationId(incomingUpdateId, 0);

        List<PersistentRecord> pendingMutationsFromStorage = getAllPendingMutationRecordFromStorage();
        for (PersistentRecord record : pendingMutationsFromStorage) {
            if (!record.getContainedModelId().equals(incomingUpdate.getMutatedItem().resolveIdentifier())) {
                pendingMutationsFromStorage.remove(record);
            }
        }
        // Ensure the new one is in storage.
        PendingMutation<SerializedModel> storedMutation =
                converter.fromRecord(pendingMutationsFromStorage.get(0));
        // This is the name from the second model, not the first!!
        assertEquals(initialUpdate.getName(),
                storedMutation.getMutatedItem().getSerializedData().get("name"));
        // wea got merged from existing model!!
        assertEquals(incomingUpdatedModel.getWea(),
                storedMutation.getMutatedItem().getSerializedData().get("wea"));
        assertEquals(PendingMutation.Type.CREATE,
                storedMutation.getMutationType());
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
    public void existingCreationIncomingUpdateRewritesExitingMutation()
            throws AmplifyException, InterruptedException {
        // Arrange an existing creation mutation
        BlogOwner modelInExistingMutation = BlogOwner.builder()
            .name("Papa Tony")
            .build();
        PendingMutation<BlogOwner> existingCreation =
            PendingMutation.creation(modelInExistingMutation, schema);
        String existingCreationId = existingCreation.getMutationId().toString();
        boolean completed = mutationOutbox.enqueue(existingCreation).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(completed);

        // Act: try to enqueue an update even whilst the creation is pending
        BlogOwner modelInIncomingMutation = modelInExistingMutation.copyOfBuilder()
            .name("Tony Jr.")
            .build();
        PendingMutation<BlogOwner> incomingUpdate =
            PendingMutation.update(modelInIncomingMutation, schema);
        String incomingUpdateId = incomingUpdate.getMutationId().toString();
        TestObserver<Void> enqueueObserver = mutationOutbox.enqueue(incomingUpdate).test();

        // Assert: OK. The new mutation is accepted
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        enqueueObserver.assertComplete();

        // Assert: the existing mutation is still there, by id ....
        List<PendingMutation.PersistentRecord> recordsForExistingMutationId =
            storage.query(PersistentRecord.class, Where.identifier(PersistentRecord.class, existingCreationId));
        assertEquals(1, recordsForExistingMutationId.size());

        // And the new one is not, by ID...
        List<PendingMutation.PersistentRecord> recordsForIncomingMutationId =
            storage.query(PersistentRecord.class, Where.identifier(PersistentRecord.class, incomingUpdateId));
        assertEquals(0, recordsForIncomingMutationId.size());

        // However, the original mutation has been updated to include the contents of the
        // incoming mutation. This is true even whilst the mutation retains its original ID.
        PendingMutation<BlogOwner> storedMutation = converter.fromRecord(recordsForExistingMutationId.get(0));
        // This is the name from the second model, not the first!
        assertEquals("Tony Jr.", storedMutation.getMutatedItem().getName());

        // There is a mutation in the outbox, it has the original ID.
        // This is STILL a creation, just using the new model data.
        assertEquals(
            PendingMutation.instance(
                existingCreation.getMutationId(),
                modelInIncomingMutation,
                schema,
                PendingMutation.Type.CREATE,
                QueryPredicates.all()
            ),
            mutationOutbox.peek()
        );
    }

    /**
     * When there is already a creation pending, and then we get a deletion for the same model ID,
     * we should just remove the creation. It means like "never mind, don't actually create."
     * @throws DataStoreException On failure to query storage for mutations state after test action.
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    public void existingCreationIncomingDeletionRemovesExisting() throws AmplifyException {
        BlogOwner joe = BlogOwner.builder()
            .name("Original Joe")
            .build();
        PendingMutation<BlogOwner> existingCreation = PendingMutation.creation(joe, schema);
        String existingCreationId = existingCreation.getMutationId().toString();
        boolean completed = mutationOutbox.enqueue(existingCreation)
                .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(completed);

        PendingMutation<BlogOwner> incomingDeletion = PendingMutation.deletion(joe, schema);
        String incomingDeletionId = incomingDeletion.getMutationId().toString();
        boolean otherEnqueueCompleted = mutationOutbox.enqueue(incomingDeletion)
                .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(otherEnqueueCompleted);

        assertTrue(storage.query(PersistentRecord.class,
                Where.identifier(PersistentRecord.class, existingCreationId)).isEmpty());
        assertTrue(storage.query(PersistentRecord.class, Where.identifier(PersistentRecord.class,
                incomingDeletionId)).isEmpty());

        // There are no pending mutations.
        assertNull(mutationOutbox.peek());
    }

    /**
     * When there is already an existing update, and then a deletion comes in, we should
     * use the deletion, not the update. No sense in updating the record if you're just going to
     * delete it.
     * @throws DataStoreException On failure to query storage to inspect mutation records after test action.
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    public void existingUpdateIncomingDeletionOverwritesExisting() throws AmplifyException {
        BlogOwner joe = BlogOwner.builder()
            .name("Original Joe")
            .build();
        PendingMutation<BlogOwner> exitingUpdate = PendingMutation.update(joe, schema);
        String existingUpdateId = exitingUpdate.getMutationId().toString();
        boolean completed = mutationOutbox.enqueue(exitingUpdate).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(completed);

        PendingMutation<BlogOwner> incomingDeletion = PendingMutation.deletion(joe, schema);
        String incomingDeletionId = incomingDeletion.getMutationId().toString();
        boolean otherEnqueueCompleted = mutationOutbox.enqueue(incomingDeletion)
                .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(otherEnqueueCompleted);

        // The original mutation ID is preserved.
        List<PendingMutation.PersistentRecord> existingMutationRecords =
            storage.query(PersistentRecord.class, Where.identifier(PersistentRecord.class, existingUpdateId));
        assertEquals(1, existingMutationRecords.size());

        // The new ID was discarded ....
        List<PendingMutation.PersistentRecord> incomingMutationRecords =
            storage.query(PersistentRecord.class, Where.identifier(PersistentRecord.class, incomingDeletionId));
        assertEquals(0, incomingMutationRecords.size());

        // HOWEVER,
        // The stored mutation has the original ID, but it has become a deletion, not an update
        assertEquals(
            PendingMutation.Type.DELETE,
            converter.fromRecord(existingMutationRecords.get(0)).getMutationType()
        );

        // Able to get next mutation, it has the original ID
        // The model data doesn't really matter, since it only matches on model ID, anyway.
        // Importantly, the type is NOT update, but instead has become a deletion.
        assertEquals(
            PendingMutation.instance(
                exitingUpdate.getMutationId(),
                joe,
                schema,
                PendingMutation.Type.DELETE,
                QueryPredicates.all()
            ),
            mutationOutbox.peek()
        );
    }

    /**
     * If there is an existing deletion mutation, and then we get another one, update the original
     * with the new one.
     * @throws DataStoreException On failure to query storage for records.
     * @throws AmplifyException If schema cannot be found in the registry.
     */
    @Test
    public void existingDeletionIncomingDeletionOverwritesExisting() throws AmplifyException {
        BlogOwner sammy = BlogOwner.builder()
            .name("Sammy Swanson")
            .build();
        PendingMutation<BlogOwner> exitingDeletion = PendingMutation.deletion(sammy, schema);
        PendingMutation<BlogOwner> incomingDeletion = PendingMutation.deletion(sammy, schema);
        assertNotEquals(exitingDeletion.getMutationId(), incomingDeletion.getMutationId());

        boolean completed = mutationOutbox.enqueue(exitingDeletion).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(completed);
        boolean otherEnqueueCompleted = mutationOutbox.enqueue(incomingDeletion)
                .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(otherEnqueueCompleted);

        // Existing record is still there
        List<PersistentRecord> existingMutationRecords =
            storage.query(PersistentRecord.class, Where.identifier(PersistentRecord.class,
                    exitingDeletion.getMutationId().toString()));
        assertEquals(1, existingMutationRecords.size());

        // Incoming is not present
        List<PersistentRecord> incomingMutationRecords =
            storage.query(PersistentRecord.class, Where.identifier(PersistentRecord.class,
                    incomingDeletion.getMutationId().toString()));
        assertEquals(0, incomingMutationRecords.size());

        // Still a deletion, as the next outbox item
        assertEquals(exitingDeletion, mutationOutbox.peek());
    }

    /**
     * When an already-pending mutation is updated, then the {@link Observable} returned by
     * {@link MutationOutbox#events()} should emit an {@link OutboxEvent#CONTENT_AVAILABLE} event.
     */
    @Test
    public void updateEventPostedWhenExistingOutboxItemUpdate() {
        // Watch for events.
        TestObserver<OutboxEvent> eventsObserver = mutationOutbox.events().test();

        // Create tony.
        BlogOwner tonyWrongName = BlogOwner.builder()
            .name("Tony Jon Swanssssssssson yee-haw!")
            .build();
        PendingMutation<BlogOwner> originalCreation = PendingMutation.creation(tonyWrongName, schema);
        boolean completed = mutationOutbox.enqueue(originalCreation).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(completed);

        // Update tony - we spelled his name wrong originally
        BlogOwner tonySpelledRight = tonyWrongName.copyOfBuilder()
            .name("Tony Jon (\"TJ\") Swanson")
            .build();
        boolean otherEnqueueCompleted = mutationOutbox.enqueue(PendingMutation.update(tonySpelledRight, schema))
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(otherEnqueueCompleted);

        // Assert: an event for the original creation, then another for the update
        eventsObserver.awaitCount(2)
            .assertNoErrors()
            .assertNotComplete()
            .assertValues(OutboxEvent.CONTENT_AVAILABLE, OutboxEvent.CONTENT_AVAILABLE);
    }

    /**
     * When an new mutation is enqueued into the outbox, the {@link Observable} made available by
     * {@link MutationOutbox#events()} should emit an {@link OutboxEvent#CONTENT_AVAILABLE} event.
     */
    @Test
    public void enqueueEventPostedWhenNewOutboxItemAdded() {
        // Watch for events.
        TestObserver<OutboxEvent> eventsObserver = mutationOutbox.events().test();

        // Enqueue one
        boolean completed = mutationOutbox.enqueue(PendingMutation.deletion(BlogOwner.builder()
            .name("Tony Swanson")
            .build(), schema
        )).blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(completed);

        // Assert: we got an event!
        eventsObserver.awaitCount(1)
            .assertNoErrors()
            .assertNotComplete()
            .assertValue(OutboxEvent.CONTENT_AVAILABLE);
    }

    /**
     * If the queue contains multiple items, then
     * {@link MutationOutbox#getMutationForModelId(String)}
     * returns the first one.
     * @throws DataStoreException On failure to arrange content into storage
     */
    @Test
    public void nextItemForModelIdReturnsFirstEnqueued() throws DataStoreException {
        BlogOwner originalJoe = BlogOwner.builder()
            .name("Joe Swanson")
            .build();
        PendingMutation<BlogOwner> firstMutation = PendingMutation.update(originalJoe, schema);
        storage.save(originalJoe, converter.toRecord(firstMutation));

        BlogOwner updatedJoe = originalJoe.copyOfBuilder()
            .name("Joe Swanson, MD. (He finished med school, I guess?)")
            .build();
        PendingMutation<BlogOwner> secondMutation = PendingMutation.update(updatedJoe, schema);
        storage.save(updatedJoe, converter.toRecord(secondMutation));

        boolean completed = mutationOutbox.load().blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);

        assertTrue(completed);
        assertEquals(
            firstMutation,
            mutationOutbox.getMutationForModelId(originalJoe.getId(), originalJoe.getClass().getName())
        );
    }

    /**
     * Ordinarily, a DELETE would remote a CREATE, in front of it. But if that
     * create is marked in flight, we can't remove it. We have to enqueue the new
     * mutation.
     */
    @Test
    public void mutationEnqueuedIfExistingMutationIsInFlight() {
        // Arrange an existing mutation.
        BlogOwner joe = BlogOwner.builder()
            .name("Joe")
            .build();
        PendingMutation<BlogOwner> creation = PendingMutation.creation(joe, schema);
        boolean completed = mutationOutbox.enqueue(creation)
            // Act: mark it as in-flight, after enqueue.
            .andThen(mutationOutbox.markInFlight(creation.getMutationId()))
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(completed);

        // Now, look at what happens when we enqueue a new mutation.
        PendingMutation<BlogOwner> deletion = PendingMutation.deletion(joe, schema);
        boolean otherEnqueueCompleted = mutationOutbox.enqueue(deletion)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(otherEnqueueCompleted);

        PendingMutation<? extends Model> next = mutationOutbox.peek();
        assertNotNull(next);
        assertEquals(creation, next);
        boolean removeCompleted = mutationOutbox.remove(next.getMutationId())
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(removeCompleted);

        next = mutationOutbox.peek();
        assertNotNull(next);
        assertEquals(deletion, next);
        boolean otherRemoveCompleted = mutationOutbox.remove(next.getMutationId())
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(otherRemoveCompleted);

        assertNull(mutationOutbox.peek());
    }

    /**
     * It is an error to mark an item as in-flight, if it isn't even in the dang queue.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void errorWhenMarkingItemNotInQueue() throws InterruptedException {
        // Enqueue and remove a mutation.
        BlogOwner tabby = BlogOwner.builder()
            .name("Tabitha Stevens of Beaver Falls, Idaho")
            .build();
        PendingMutation<BlogOwner> creation = PendingMutation.creation(tabby, schema);
        boolean enqueueCompleted = mutationOutbox.enqueue(creation)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(enqueueCompleted);
        boolean removeCompleted = mutationOutbox.remove(creation.getMutationId())
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(removeCompleted);

        // Now, if we try to make that mutation as in-flight, its an error, since its already processed.
        TestObserver<Void> observer = mutationOutbox.markInFlight(creation.getMutationId()).test();
        observer.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        observer
            .assertError(DataStoreException.class)
            .assertError(error ->
                error.getMessage() != null &&
                    error.getMessage().contains("there was no mutation with that ID in the outbox")
            );
    }

    /**
     * When two creations for the same model are enqueued, the second should fail.  This is similar to
     * {@link #existingCreationIncomingCreationYieldsError}, except that the Completable's from the two enqueue calls
     * are concatenated into the same stream.   The second enqueue should not check if an item exists in the queue
     * until the first enqueue is completed.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void enqueueIsSynchronized() throws InterruptedException {
        // Arrange an existing creation mutation
        BlogOwner modelInExistingMutation = BlogOwner.builder()
            .name("The Real Papa Tony")
            .build();
        PendingMutation<BlogOwner> firstCreation = PendingMutation.creation(modelInExistingMutation, schema);
        PendingMutation<BlogOwner> secondCreation = PendingMutation.creation(modelInExistingMutation, schema);

        TestObserver<Void> enqueueObserver = mutationOutbox.enqueue(firstCreation)
            .andThen(mutationOutbox.enqueue(secondCreation))
            .test();

        // Assert: caused a failure.
        enqueueObserver.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        enqueueObserver.assertError(throwable -> throwable instanceof DataStoreException);
    }

    /**
     * Attempting to remove an item from the queue which doesn't exist should throw an error.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void removeIsSynchronized() throws InterruptedException {
        // Enqueue and remove a mutation.
        BlogOwner tabby = BlogOwner.builder()
            .name("Tabitha Stevens of Beaver Falls, Idaho")
            .build();
        PendingMutation<BlogOwner> creation = PendingMutation.creation(tabby, schema);
        boolean completed = mutationOutbox.enqueue(creation)
            .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(completed);

        TestObserver<Void> observer = mutationOutbox.remove(creation.getMutationId())
            .andThen(mutationOutbox.remove(creation.getMutationId()))
            .test();

        observer.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        observer
            .assertError(DataStoreException.class)
            .assertError(error ->
                    error.getMessage() != null &&
                            error.getMessage().contains("there was no mutation with that ID in the outbox")
            );
    }

    /**
     * Marking an item in flight should throw an error if the item is already removed from the queue.  This is similar
     * to {@link #errorWhenMarkingItemNotInQueue}, except that the removal and marking in flight Completables are
     * concatenated into the same stream.  This validates that markInFlight does not check if the item is in the queue
     * until after the removal is complete.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void markInFlightIsSynchronized() throws InterruptedException {
        // Enqueue and remove a mutation.
        BlogOwner tabby = BlogOwner.builder()
                .name("Tabitha Stevens of Beaver Falls, Idaho")
                .build();
        PendingMutation<BlogOwner> creation = PendingMutation.creation(tabby, schema);
        boolean completed = mutationOutbox.enqueue(creation)
                .blockingAwait(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(completed);
        TestObserver<Void> observer = mutationOutbox.remove(creation.getMutationId())
                .andThen(mutationOutbox.markInFlight(creation.getMutationId())).test();

        // Now, we should see an error since we can't mark a mutation as in-flight that has already been removed.
        observer.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        observer
            .assertError(DataStoreException.class)
            .assertError(error ->
                error.getMessage() != null &&
                    error.getMessage().contains("there was no mutation with that ID in the outbox")
            );
    }

    private void assertRecordCountForMutationId(String mutationId, int expectedCount) throws AmplifyException {
        List<PersistentRecord> recordsForExistingMutationId = getPendingMutationRecordFromStorage(mutationId);
        assertEquals(expectedCount, recordsForExistingMutationId.size());
    }

    private List<PersistentRecord> getPendingMutationRecordFromStorage(String mutationId) throws AmplifyException {
        return storage.query(PersistentRecord.class, Where.identifier(PersistentRecord.class, mutationId));
    }

    private List<PersistentRecord> getAllPendingMutationRecordFromStorage() throws DataStoreException {
        return storage.query(PersistentRecord.class, Where.matchesAll());
    }
}
