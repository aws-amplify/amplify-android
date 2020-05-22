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

package com.amplifyframework.core.model.temporal;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link Temporal.Timestamp}.
 */
public final class TemporalTimestampTest {
    private static final long MS_SINCE_EPOCH = 1_588_703_119_659L;
    private static final long SEC_SINCE_EPOCH = 1_588_703_119L;

    /**
     * The {@link Temporal.Timestamp#Timestamp()} simple constructor will store and use
     * the current time. The current time is retrievable via {@link Temporal.Timestamp#getSecondsSinceEpoch()}.
     */
    @Test
    public void correctValueStoredFromDefaultConstructor() {
        Temporal.Timestamp timestamp = new Temporal.Timestamp();
        long evaluationTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        assertTrue(timestamp.getSecondsSinceEpoch() <= evaluationTimeInSeconds + 1);
    }

    /**
     * The {@link Temporal.Timestamp#Timestamp(Date)} can construct an {@link Temporal.Timestamp}.
     * It does this by pulling the {@link Date#getTime()} and converting it to seconds since
     * the epoch. This value is available via {@link Temporal.Timestamp#getSecondsSinceEpoch()}.
     */
    @Test
    public void correctValueStoredWhenConstructedFromDate() {
        Date date = new Date(MS_SINCE_EPOCH);
        Temporal.Timestamp timestamp = new Temporal.Timestamp(date);
        assertEquals(SEC_SINCE_EPOCH, timestamp.getSecondsSinceEpoch());
    }

    /**
     * The {@link Temporal.Timestamp#Timestamp(long, TimeUnit)} is able to construct
     * a valid timestamp. The {@link Temporal.Timestamp#getSecondsSinceEpoch()} will return
     * the correct number of seconds since the epoch.
     */
    @Test
    public void correctValueStoredWhenConstructedFromLong() {
        Temporal.Timestamp timestamp = new Temporal.Timestamp(SEC_SINCE_EPOCH, TimeUnit.SECONDS);
        assertEquals(SEC_SINCE_EPOCH, timestamp.getSecondsSinceEpoch());
    }

    /**
     * {@link Temporal.Timestamp} has a sane implementation of {@link Object#equals(Object)}
     * and {@link Object#hashCode()}, such like-valued instances are considered equal to
     * one another, and produce the same hash.
     */
    @Test
    public void testEqualsAndHash() {
        // Arrange two values that are logically the same.
        Temporal.Timestamp first = new Temporal.Timestamp(5_000, TimeUnit.SECONDS);
        Temporal.Timestamp second = new Temporal.Timestamp(5_000_000, TimeUnit.MILLISECONDS);

        // Validate that the two are equals(), and shared a common hashCode().
        Set<Temporal.Timestamp> times = new HashSet<>(Arrays.asList(first, second));
        assertEquals(1, times.size());
        assertEquals(first, second);
    }
}
