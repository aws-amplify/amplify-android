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

package com.amplifyframework.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the range {@link Range} class.
 */
public class RangeTests {

    /**
     * Checks whether we can create a valid range of integers.
     */
    @Test
    public void validRangeInitialization() {
        Range<Integer> range = new Range<>(2, 4);
        Assert.assertNotNull("Range should be non null", range);
        Assert.assertTrue("Range should contain the given value", range.contains(3));
    }

    /**
     * Checks whether we get illegal argument exception.
     */
    @Test (expected = IllegalArgumentException.class)
    public void invalidRange() {
        new Range<>(4, 2);
    }

    /**
     * Checks whether comparable works for integers.
     */
    @Test
    public void compareRange() {
        Range<Integer> range = new Range<>(2, 10);
        Assert.assertNotNull("Range should be non null", range);
        Assert.assertTrue("Range should contain the given value", range.contains(3));
        Assert.assertTrue("Range should contain the given value", range.contains(10));
        Assert.assertFalse("Range should not contain the given value", range.contains(1));
    }
}
