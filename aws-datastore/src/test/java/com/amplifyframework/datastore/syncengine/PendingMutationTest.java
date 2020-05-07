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

import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Tests the {@link PendingMutation}.
 */
public final class PendingMutationTest {
    private Random random;

    /**
     * Sets up test dependencies.
     */
    @Before
    public void setup() {
        random = new Random();
    }

    /**
     * It is possible to sort pending mutations according to their TimeBasedUUID field.
     */
    @Test
    public void pendingMutationsAreComparable() {
        // First, create a bunch of pending mutations.
        List<PendingMutation<BlogOwner>> expectedOrder = new ArrayList<>();
        final int desiredQuantity = 10;
        for (int index = 0; index < desiredQuantity; index++) {
            BlogOwner blogger = BlogOwner.builder()
                .name(String.format(Locale.US, "Blogger #%d", index))
                .build();
            expectedOrder.add(PendingMutation.creation(blogger, BlogOwner.class));
        }

        // Okay! Now, scatter them.
        List<PendingMutation<BlogOwner>> outOfOrder = new ArrayList<>(expectedOrder);
        //noinspection ComparatorMethodParameterNotUsed Intentional; result is random
        Collections.sort(outOfOrder, (one, two) -> random.nextInt());
        assertNotEquals(expectedOrder, outOfOrder);

        // Now sort them according to the item comparator, {@link PendingMutation#compareTo(Object)}.
        List<PendingMutation<BlogOwner>> actualOrder = new ArrayList<>(outOfOrder);
        Collections.sort(actualOrder);

        assertEquals(expectedOrder, actualOrder);
    }
}
