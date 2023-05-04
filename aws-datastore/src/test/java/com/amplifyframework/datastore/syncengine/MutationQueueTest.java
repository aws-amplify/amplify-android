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
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link MutationQueue}.
 */
@SuppressWarnings("deprecation")
@RunWith(RobolectricTestRunner.class)
public final class MutationQueueTest {
    private ModelSchema schema;
    private MutationQueue mutationQueue;

    /**
     * Set up the object under test.
     * @throws AmplifyException On failure to arrange model schema
     */
    @Before
    public void setup() throws AmplifyException {
        schema = ModelSchema.fromModelClass(BlogOwner.class);
        mutationQueue = new MutationQueue();
    }

    /**
     * Prepare a {@link PendingMutation} instance and insert into the tail of
     * the {@link MutationQueue}, verify the instance exist by checking its {@link TimeBasedUuid}.
     */
    @Test
    public void addPendingMutationTest() {
        mutationQueue.clear();
        BlogOwner qing = BlogOwner.builder()
                .name("Qing Zhong")
                .build();
        PendingMutation<BlogOwner> createQing = PendingMutation.creation(qing, schema);
        mutationQueue.add(createQing);

        assertEquals(mutationQueue.size(), 1);
        TimeBasedUuid id = createQing.getMutationId();
        assertEquals(mutationQueue.getMutationById(id), createQing);
    }

    /**
     * Prepare a {@link PendingMutation} instance and insert into the tail of
     * the {@link MutationQueue} and then remove it, verify the {@link MutationQueue}
     * is empty after the remove.
     */
    @Test
    public void removePendingMutationTest() {
        mutationQueue.clear();
        BlogOwner qing = BlogOwner.builder()
                .name("Qing Zhong")
                .build();
        PendingMutation<BlogOwner> createQing = PendingMutation.creation(qing, schema);
        mutationQueue.add(createQing);
        assertEquals(mutationQueue.size(), 1);
        mutationQueue.remove(createQing);
        assertTrue(mutationQueue.isEmpty());
    }

    /**
     * Prepare a {@link PendingMutation} instance and insert into the tail of
     * the {@link MutationQueue} and try to insert the same instance again,
     * the method should return false to indicate a failure on inserting.
     */
    @Test
    public void addingDuplicatePendingMutationsThrowsExceptionTest() {
        mutationQueue.clear();
        BlogOwner qing = BlogOwner.builder()
                .name("Qing Zhong")
                .build();
        PendingMutation<BlogOwner> createQing = PendingMutation.creation(qing, schema);

        mutationQueue.add(createQing);
        assertEquals(mutationQueue.size(), 1);
        assertFalse(mutationQueue.add(createQing));
    }

    /**
     * Prepare two {@link PendingMutation} instances and insert them into the tail of
     * the {@link MutationQueue}, verify the first element in the queue should be the
     * first element we insert, which is FIFO.
     */
    @Test
    public void peekingPendingMutationsTest() {
        mutationQueue.clear();
        BlogOwner qing = BlogOwner.builder()
                .name("Qing Zhong")
                .build();
        PendingMutation<BlogOwner> createQing = PendingMutation.creation(qing, schema);

        BlogOwner tony = BlogOwner.builder()
                .name("The Real Papa Tony")
                .build();
        PendingMutation<BlogOwner> createTony = PendingMutation.creation(tony, schema);

        mutationQueue.add(createQing);
        mutationQueue.add(createTony);
        assertEquals(createQing, mutationQueue.peek());
    }
}
