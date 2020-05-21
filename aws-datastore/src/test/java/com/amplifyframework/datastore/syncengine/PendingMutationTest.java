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

package com.amplifyframework.datastore.syncengine;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.commentsblog.PostStatus;

import org.junit.Test;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link PendingMutation}.
 */
public final class PendingMutationTest {
    /**
     * It is possible to sort pending mutations according to their TimeBasedUUID field.
     */
    @Test
    public void pendingMutationsAreComparable() {
        // First, create a bunch of pending mutations.
        final List<PendingMutation<? extends Model>> expectedOrder = new ArrayList<>();
        final int desiredQuantity = 10;
        for (int index = 0; index < desiredQuantity; index++) {
            // Populate a few different types of models, according to a random boolean value,
            // to make sure the comparator can work across model types
            if (new SecureRandom().nextBoolean()) {
                BlogOwner blogger = BlogOwner.builder()
                    .name(String.format(Locale.US, "Blogger #%d", index))
                    .build();
                expectedOrder.add(PendingMutation.creation(blogger, BlogOwner.class));
            } else {
                Post post = Post.builder()
                    .title(String.format(Locale.US, "Title #%d", index))
                    .status(PostStatus.ACTIVE)
                    .rating(5)
                    .build();
                expectedOrder.add(PendingMutation.creation(post, Post.class));
            }
        }

        // Okay! Now, shuffle them.
        List<PendingMutation<? extends Model>> outOfOrder = new ArrayList<>();
        Collections.shuffle(outOfOrder, new SecureRandom());

        // Now sort them according to the item comparator, {@link PendingMutation#compareTo(Object)}.
        List<PendingMutation<? extends Model>> actualOrder = new ArrayList<>(outOfOrder);
        Collections.sort(actualOrder);

        assertEquals(expectedOrder, actualOrder);
    }
}
