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

package com.amplifyframework.core.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link AWSTimestamp}.
 */
public final class AWSTimestampTest {
    private static final long MS_SINCE_EPOCH = 1_588_703_119_659L;
    private static final long SEC_SINCE_EPOCH = 1_588_703_119L;

    /**
     * The {@link AWSTimestamp#AWSTimestamp()} simple constructor will store and use
     * the current time. The current time is retrievable via {@link AWSTime#getOffsetTotalSeconds()}.
     */
    @Test
    public void correctValueStoredFromDefaultConstructor() {
        AWSTimestamp timestamp = new AWSTimestamp();
        long evaluationTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        assertTrue(timestamp.getSecondsSinceEpoch() <= evaluationTimeInSeconds + 1);
    }

    /**
     * The {@link AWSTimestamp#AWSTimestamp(Date)} can construct an {@link AWSTimestamp}.
     * It does this by pulling the {@link Date#getTime()} and converting it to seconds since
     * the epoch. This value is available via {@link AWSTimestamp#getSecondsSinceEpoch()}.
     */
    @Test
    public void correctValueStoredWhenConstructedFromDate() {
        Date date = new Date(MS_SINCE_EPOCH);
        AWSTimestamp timestamp = new AWSTimestamp(date);
        assertEquals(SEC_SINCE_EPOCH, timestamp.getSecondsSinceEpoch());
    }

    /**
     * The {@link AWSTimestamp#AWSTimestamp(long, TimeUnit)} is able to construct
     * a valid timestamp. The {@link AWSTimestamp#getSecondsSinceEpoch()} will return
     * the correct number of seconds since the epoch.
     */
    @Test
    public void correctValueStoredWhenConstructedFromLong() {
        AWSTimestamp timestamp = new AWSTimestamp(SEC_SINCE_EPOCH, TimeUnit.SECONDS);
        assertEquals(SEC_SINCE_EPOCH, timestamp.getSecondsSinceEpoch());
    }

    /**
     * {@link AWSTimestamp} has a sane implementation of {@link Object#equals(Object)}
     * and {@link Object#hashCode()}, such like-valued instances are considered equal to
     * one another, and produce the same hash.
     */
    @Test
    public void testEqualsAndHash() {
        // Arrange two values that are logically the same.
        AWSTimestamp first = new AWSTimestamp(5_000, TimeUnit.SECONDS);
        AWSTimestamp second = new AWSTimestamp(5_000_000, TimeUnit.MILLISECONDS);

        // Validate that the two are equals(), and shared a common hashCode().
        Set<AWSTimestamp> times = new HashSet<>(Arrays.asList(first, second));
        assertEquals(1, times.size());
        assertEquals(first, second);
    }
}
