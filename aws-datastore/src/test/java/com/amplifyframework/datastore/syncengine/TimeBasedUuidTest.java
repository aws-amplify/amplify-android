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

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;

/**
 * Tests the {@link TimeBasedUuid}.
 */
public final class TimeBasedUuidTest {
    private Random random;

    /**
     * Sets up test dependencies.
     */
    @Before
    public void setup() {
        this.random = new Random();
    }

    /**
     * Time-based UUIDs should be comparable, and they are compared according
     * to their timestamp. An out-of-order sequence of these UUIDs should be sort-able.
     * Doing so should restore them to the ordering in which they were created.
     */
    @Test
    public void timeBasedUuidsMayBeSortedByCreationTime() {
        // First, make a bunch of TimeBasedUuids.
        List<TimeBasedUuid> expectedOrder = new ArrayList<>();
        final int amountDesired = 10;
        for (int index = 0; index < amountDesired; index++) {
            expectedOrder.add(TimeBasedUuid.create());
        }

        // Now, scatter them, into a new array.
        List<TimeBasedUuid> randomOrder = new ArrayList<>(expectedOrder);
        Collections.sort(randomOrder, (one, two) -> random.nextInt());
        // Sanity check: this is actually out of order, now, right?
        assertNotEquals(expectedOrder, randomOrder);

        // Act! sort them using the *default* comparator for the items.
        List<TimeBasedUuid> actualSortedOrder = new ArrayList<>(randomOrder);
        Collections.sort(actualSortedOrder);

        assertEquals(expectedOrder, actualSortedOrder);
    }

    /**
     * Two {@link TimeBasedUuid} with the same string representation
     * should be equals().
     */
    @Test
    public void timeBasedUNIDsHasSaneEqualsImplementation() {
        // Create a record, get its value as String.
        TimeBasedUuid first = TimeBasedUuid.create();
        String firstUuidAsString = first.toString();

        // Now, reconstruct it.
        TimeBasedUuid second = TimeBasedUuid.fromString(firstUuidAsString);

        // This is a different object instance:
        assertNotSame(first, second);
        // But, they are considered to be equals() based on the content of their character!
        assertEquals(first, second);
    }

    /**
     * Two {@link TimeBasedUuid}s with the same string contents should both evaluate
     * to the same hash-code. This way they can be included into collections safely, etc.
     */
    @Test
    public void timeBasedUuidHasSanHashCode() {
        TimeBasedUuid first = TimeBasedUuid.create();
        TimeBasedUuid second = TimeBasedUuid.fromString(first.toString());

        final Set<TimeBasedUuid> uuids = new HashSet<>();
        uuids.add(first);
        uuids.add(second);
        assertEquals(1, uuids.size());
    }
}
