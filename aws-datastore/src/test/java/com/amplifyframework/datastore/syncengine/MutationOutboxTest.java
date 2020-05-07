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

import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

import io.reactivex.observers.TestObserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link MutationOutbox}.
 */
@RunWith(RobolectricTestRunner.class)
public final class MutationOutboxTest {
    private MutationOutbox mutationOutbox;
    private PendingMutation.Converter converter;
    private SynchronousStorageAdapter storage;

    /**
     * Set up the object under test.
     */
    @Before
    public void setup() {
        InMemoryStorageAdapter inMemoryStorageAdapter = InMemoryStorageAdapter.create();
        storage = SynchronousStorageAdapter.delegatingTo(inMemoryStorageAdapter);
        mutationOutbox = new MutationOutbox(inMemoryStorageAdapter);
        converter = new GsonPendingMutationConverter();
    }

    /**
     * Enqueueing a mutation should have the result of persisting
     * the mutation to storage, and notifying any observers that
     * there is a new muation available.
     * @throws DataStoreException On failure to query results, for assertions
     */
    @Test
    public void enqueuePersistsMutationAndNotifiesObserver() throws DataStoreException {
        // Observe the queue
        TestObserver<PendingMutation<? extends Model>> queueObserver = mutationOutbox.observe().test();

        BlogOwner jameson = BlogOwner.builder()
            .name("Jameson Williams")
            .build();
        PendingMutation<BlogOwner> createJameson = PendingMutation.creation(jameson, BlogOwner.class);

        // Enqueue an save for a Jameson BlogOwner object,
        // and make sure that it calls back onComplete().
        TestObserver<Void> saveObserver = mutationOutbox.enqueue(createJameson).test();
        saveObserver.awaitTerminalEvent();
        saveObserver.assertNoErrors().assertComplete();
        saveObserver.dispose();

        // Expected to observe the mutation on the subject
        queueObserver.awaitCount(1);
        queueObserver.assertValue(createJameson);
        queueObserver.dispose();

        // Assert that the storage contains the mutation
        List<PendingMutation.PersistentRecord> recordsInStorage =
            storage.query(PendingMutation.PersistentRecord.class);
        assertEquals(1, recordsInStorage.size());
        assertEquals(
            converter.toRecord(createJameson),
            recordsInStorage.get(0)
        );
    }

    /**
     * The enqueue() returns a Single, but that Single doesn't actually invoke
     * any behavior until you subscribe to it.
     * @throws DataStoreException On failure to query results, for assertions
     */
    @Test
    public void enqueueDoesNothingBeforeSubscription() throws DataStoreException {
        // Watch for notifications on the observe() API.
        TestObserver<PendingMutation<? extends Model>> testObserver = mutationOutbox.observe().test();

        // Enqueue something, but don't subscribe to the observable just yet.
        BlogOwner tony = BlogOwner.builder()
            .name("Tony Daniels")
            .build();
        mutationOutbox.enqueue(PendingMutation.creation(tony, BlogOwner.class));
        // .subscribe() is NOT called on the enqueue() above!! This is the point!!!

        // Note that nothing has actually happened yet --
        // Nothing was put out on the observable ...
        testObserver.assertNoValues();
        testObserver.assertNotTerminated();
        testObserver.dispose();

        // And nothing is in storage.
        assertTrue(storage.query(PendingMutation.PersistentRecord.class).isEmpty());
    }

    /**
     * If there are some mutations that haven't been processed,
     * you'll hear about them when you subscribe, even if nothing new
     * has entered the outbox recently.
     */
    @Test
    public void observeReplaysUnprocessedChangesOnSubscribe() {
        // Arrange: some mutations.
        BlogOwner tony = BlogOwner.builder()
            .name("Tony Daniels")
            .build();
        PendingMutation<BlogOwner> updateTony = PendingMutation.update(tony, BlogOwner.class);

        BlogOwner sam = BlogOwner.builder()
            .name("Sam Watson")
            .build();
        PendingMutation<BlogOwner> insertSam = PendingMutation.creation(sam, BlogOwner.class);

        BlogOwner betty = BlogOwner.builder()
            .name("Betty Smith")
            .build();
        PendingMutation<BlogOwner> deleteBetty = PendingMutation.deletion(betty, BlogOwner.class);

        // Arrange: when no-one is observing, enqueue some changes.
        mutationOutbox.enqueue(updateTony).blockingAwait();
        mutationOutbox.enqueue(insertSam).blockingAwait();
        mutationOutbox.enqueue(deleteBetty).blockingAwait();

        // Act: observe the queue.
        TestObserver<PendingMutation<?>> testObserver = TestObserver.create();
        mutationOutbox.observe().subscribe(testObserver);

        // Assert: we got some stuff, even though we subscribed after.
        // These values came from *storage*.
        testObserver.awaitCount(Arrays.asList(updateTony, insertSam, deleteBetty).size());
        testObserver.assertValues(updateTony, insertSam, deleteBetty);
        testObserver.dispose();
    }

    /**
     * Tests {@link MutationOutbox#remove(PendingMutation)}.
     * @throws DataStoreException On failure to query results, for assertions
     */
    @Test
    public void removeRemovesChangesFromQueue() throws DataStoreException {
        // Arrange: there is a change in the queue.
        BlogOwner bill = BlogOwner.builder()
            .name("Bill Gates")
            .build();
        PendingMutation<BlogOwner> deleteBillGates = PendingMutation.deletion(bill, BlogOwner.class);
        storage.save(converter.toRecord(deleteBillGates));

        TestObserver<Void> testObserver = mutationOutbox.remove(deleteBillGates).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoErrors().assertComplete();
        testObserver.dispose();

        assertEquals(0, storage.query(PendingMutation.PersistentRecord.class).size());
    }
}
