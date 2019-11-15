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

package com.amplifyframework.datastore.network;

import com.amplifyframework.datastore.MutationEvent;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import io.reactivex.observers.TestObserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link OutgoingMutationsQueue}.
 */
public class OutgoingMutationsQueueTest {

    private InMemoryStorageAdapter inMemoryStorageAdapter;
    private OutgoingMutationsQueue outgoingMutationsQueue;

    /**
     * Set up the object under test.
     */
    @Before
    public void setup() {
        inMemoryStorageAdapter = InMemoryStorageAdapter.create();
        outgoingMutationsQueue = new OutgoingMutationsQueue(inMemoryStorageAdapter);
    }

    /**
     * Enqueueing a mutation should have the result of persisting
     * the mutation to storage, and notifying any observers that
     * there is a new mutation available.
     * @throws InterruptedException If CountDownLatch used to await
     *         subscription results times out
     */
    @SuppressWarnings("WhitespaceAround")
    @Test
    public void enqueuePersistsMutationAndNotifiesObserver() throws InterruptedException {
        // Observe the queue
        TestObserver<MutationEvent<?>> testObserver = TestObserver.create();
        outgoingMutationsQueue.observe().subscribe(testObserver);

        MutationEvent<Person> insertJamesonMutation = MutationEvent.<Person>builder()
            .mutationType(MutationEvent.MutationType.INSERT)
            .dataClass(Person.class)
            .data(Person.named("Jameson"))
            .source(MutationEvent.Source.DATA_STORE)
            .build();

        // Enqueue an insert for a Jameson person object,
        // and make sure that it calls back onSuccess().
        final CountDownLatch mutationWasEnqueued = new CountDownLatch(1);
        //noinspection ResultOfMethodCallIgnored
        outgoingMutationsQueue.enqueue(insertJamesonMutation).subscribe(
            $ -> mutationWasEnqueued.countDown(),
            $ -> {}
        );
        mutationWasEnqueued.await();

        // Expected to observe the mutation on the subject
        testObserver.assertValue(insertJamesonMutation);

        // Assert that the storage contains the mutation
        assertEquals(1, inMemoryStorageAdapter.items().size());
        assertEquals(insertJamesonMutation, inMemoryStorageAdapter.items().get(0));

        // Dispose of the test observer, since we're done with it.
        testObserver.dispose();
    }

    /**
     * The enqueue() returns a Single, but that Single doesn't actually invoke
     * any behavior until you subscribe to it.
     */
    @Test
    public void enqueueDoesNothingBeforeSubscription() {
        // Watch for notifications on the observe() API.
        TestObserver<MutationEvent<?>> testObserver = TestObserver.create();
        outgoingMutationsQueue.observe().subscribe(testObserver);

        // Enqueue something, but don't subscribe to the observable just yet.
        outgoingMutationsQueue.enqueue(MutationEvent.<Person>builder()
            .dataClass(Person.class)
            .data(Person.named("tony"))
            .mutationType(MutationEvent.MutationType.UPDATE)
            .source(MutationEvent.Source.DATA_STORE)
            .build());
        // .subscribe() is NOT called.

        // Note that nothing has actually happened yet --
        // Nothing was put out on the observable ...
        testObserver.assertNoValues();
        testObserver.assertNotTerminated();

        // And nothing is in the data store.
        assertTrue(inMemoryStorageAdapter.items().isEmpty());
    }

    /**
     * If there are some mutations that haven't been processed,
     * you'll hear about them when you subscribe, even if nothing new
     * has entered the sync engine recently.
     */
    @Test
    public void observeReplaysUnprocessedMutationsOnSubscribe() {

        // Arrange: some mutations.
        MutationEvent<Person> updateTony = MutationEvent.<Person>builder()
            .mutationType(MutationEvent.MutationType.UPDATE)
            .data(Person.named("Tony"))
            .dataClass(Person.class)
            .source(MutationEvent.Source.DATA_STORE)
            .build();
        MutationEvent<Person> insertSam = MutationEvent.<Person>builder()
            .mutationType(MutationEvent.MutationType.INSERT)
            .data(Person.named("Sam"))
            .dataClass(Person.class)
            .source(MutationEvent.Source.DATA_STORE)
            .build();
        MutationEvent<Person> deleteBetty = MutationEvent.<Person>builder()
            .mutationType(MutationEvent.MutationType.DELETE)
            .data(Person.named("Betty"))
            .dataClass(Person.class)
            .source(MutationEvent.Source.DATA_STORE)
            .build();

        // Arrange: when no-one is observing, enqueue some mutations.
        outgoingMutationsQueue.enqueue(updateTony).subscribe();
        outgoingMutationsQueue.enqueue(insertSam).subscribe();
        outgoingMutationsQueue.enqueue(deleteBetty).subscribe();

        // Act: observe the queue.
        TestObserver<MutationEvent<?>> testObserver = TestObserver.create();
        outgoingMutationsQueue.observe().subscribe(testObserver);

        // Assert: we got some stuff.
        testObserver.awaitCount(Arrays.asList(updateTony, insertSam, deleteBetty).size());
        testObserver.assertValues(updateTony, insertSam, deleteBetty);
    }

    /**
     * Tests {@link OutgoingMutationsQueue#remove(MutationEvent)}.
     */
    @Test
    public void removeRemovesMutationFromQueue() {
        // Arrange: there is a mutation in the queue.
        MutationEvent<Person> deleteBillGates = MutationEvent.<Person>builder()
            .mutationType(MutationEvent.MutationType.DELETE)
            .dataClass(Person.class)
            .data(Person.named("Bill Gates"))
            .source(MutationEvent.Source.DATA_STORE)
            .build();
        inMemoryStorageAdapter.items().add(deleteBillGates);

        TestObserver<MutationEvent<Person>> testObserver = TestObserver.create();
        outgoingMutationsQueue.remove(deleteBillGates).subscribe(testObserver);

        testObserver.assertValue(deleteBillGates);

        assertEquals(0, inMemoryStorageAdapter.items().size());
    }
}
