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
import com.amplifyframework.datastore.storage.GsonStorageItemChangeConverter;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import io.reactivex.observers.TestObserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link MutationOutbox}.
 */
public class MutationOutboxTest {
    private InMemoryStorageAdapter inMemoryStorageAdapter;
    private MutationOutbox mutationOutbox;
    private GsonStorageItemChangeConverter storageItemChangeConverter;

    /**
     * Set up the object under test.
     */
    @Before
    public void setup() {
        inMemoryStorageAdapter = InMemoryStorageAdapter.create();
        mutationOutbox = new MutationOutbox(inMemoryStorageAdapter);
        storageItemChangeConverter = new GsonStorageItemChangeConverter();
    }

    /**
     * Enqueueing a change should have the result of persisting
     * the change to storage, and notifying any observers that
     * there is a new change available.
     */
    @Test
    public void enqueuePersistsChangeAndNotifiesObserver() {
        // Observe the queue
        TestObserver<StorageItemChange<? extends Model>> queueObserver = TestObserver.create();
        mutationOutbox.observe().subscribe(queueObserver);

        StorageItemChange<BlogOwner> createJameson = StorageItemChange.<BlogOwner>builder()
            .type(StorageItemChange.Type.CREATE)
            .itemClass(BlogOwner.class)
            .item(BlogOwner.builder()
                .name("Jameson Williams")
                .build())
            .initiator(StorageItemChange.Initiator.DATA_STORE_API)
            .build();

        // Enqueue an save for a Jameson BlogOwner object,
        // and make sure that it calls back onSuccess().
        TestObserver<StorageItemChange<BlogOwner>> saveObserver = TestObserver.create();
        mutationOutbox.enqueue(createJameson).subscribe(saveObserver);
        saveObserver.awaitTerminalEvent();
        saveObserver.dispose();

        // Expected to observe the mutation on the subject
        queueObserver.awaitCount(1);
        queueObserver.assertValue(createJameson);
        queueObserver.dispose();

        // Assert that the storage contains the mutation
        assertEquals(1, inMemoryStorageAdapter.items().size());
        assertEquals(
            createJameson.toRecord(storageItemChangeConverter),
            inMemoryStorageAdapter.items().get(0)
        );
    }

    /**
     * The enqueue() returns a Single, but that Single doesn't actually invoke
     * any behavior until you subscribe to it.
     */
    @Test
    public void enqueueDoesNothingBeforeSubscription() {
        // Watch for notifications on the observe() API.
        TestObserver<StorageItemChange<?>> testObserver = TestObserver.create();
        mutationOutbox.observe().subscribe(testObserver);

        // Enqueue something, but don't subscribe to the observable just yet.
        mutationOutbox.enqueue(StorageItemChange.<BlogOwner>builder()
            .itemClass(BlogOwner.class)
            .item(BlogOwner.builder()
                .name("Tony Daniels")
                .build())
            .type(StorageItemChange.Type.CREATE)
            .initiator(StorageItemChange.Initiator.DATA_STORE_API)
            .build());
        // .subscribe() is NOT called.

        // Note that nothing has actually happened yet --
        // Nothing was put out on the observable ...
        testObserver.assertNoValues();
        testObserver.assertNotTerminated();

        // And nothing is in storage.
        assertTrue(inMemoryStorageAdapter.items().isEmpty());
    }

    /**
     * If there are some changes that haven't been processed,
     * you'll hear about them when you subscribe, even if nothing new
     * has entered the sync engine recently.
     */
    @Test
    public void observeReplaysUnprocessedChangesOnSubscribe() {

        // Arrange: some mutations.
        StorageItemChange<BlogOwner> updateTony = StorageItemChange.<BlogOwner>builder()
            .type(StorageItemChange.Type.UPDATE)
            .item(BlogOwner.builder()
                .name("Tony Daniels")
                .build())
            .itemClass(BlogOwner.class)
            .initiator(StorageItemChange.Initiator.DATA_STORE_API)
            .build();
        StorageItemChange<BlogOwner> insertSam = StorageItemChange.<BlogOwner>builder()
            .type(StorageItemChange.Type.CREATE)
            .item(BlogOwner.builder()
                .name("Sam Watson")
                .build())
            .itemClass(BlogOwner.class)
            .initiator(StorageItemChange.Initiator.DATA_STORE_API)
            .build();
        StorageItemChange<BlogOwner> deleteBetty = StorageItemChange.<BlogOwner>builder()
            .type(StorageItemChange.Type.DELETE)
            .item(BlogOwner.builder()
                .name("Betty Smith")
                .build())
            .itemClass(BlogOwner.class)
            .initiator(StorageItemChange.Initiator.DATA_STORE_API)
            .build();

        // Arrange: when no-one is observing, enqueue some changes.
        mutationOutbox.enqueue(updateTony).subscribe();
        mutationOutbox.enqueue(insertSam).subscribe();
        mutationOutbox.enqueue(deleteBetty).subscribe();

        // Act: observe the queue.
        TestObserver<StorageItemChange<?>> testObserver = TestObserver.create();
        mutationOutbox.observe().subscribe(testObserver);

        // Assert: we got some stuff.
        testObserver.awaitCount(Arrays.asList(updateTony, insertSam, deleteBetty).size());
        testObserver.assertValues(updateTony, insertSam, deleteBetty);
    }

    /**
     * Tests {@link MutationOutbox#remove(StorageItemChange)}.
     */
    @Test
    public void removeRemovesChangesFromQueue() {
        // Arrange: there is a change in the queue.
        StorageItemChange<BlogOwner> deleteBillGates = StorageItemChange.<BlogOwner>builder()
            .type(StorageItemChange.Type.DELETE)
            .itemClass(BlogOwner.class)
            .item(BlogOwner.builder()
                .name("Bill Gates")
                .build())
            .initiator(StorageItemChange.Initiator.DATA_STORE_API)
            .build();
        inMemoryStorageAdapter.items().add(deleteBillGates.toRecord(storageItemChangeConverter));

        TestObserver<StorageItemChange<BlogOwner>> testObserver = TestObserver.create();
        mutationOutbox.remove(deleteBillGates).subscribe(testObserver);

        testObserver.assertValue(deleteBillGates);

        assertEquals(0, inMemoryStorageAdapter.items().size());
    }
}
