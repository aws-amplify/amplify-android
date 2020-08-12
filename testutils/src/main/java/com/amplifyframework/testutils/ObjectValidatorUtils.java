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

package com.amplifyframework.testutils;

import org.junit.Assert;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class that can be used to test core
 * Object behavior of a class that implements its own
 * equals, hashCode and toString methods.
 */
public final class ObjectValidatorUtils {

    /**
     * Hidding default constructor of utility class.
     */
    private ObjectValidatorUtils() {}

    /**
     * Function to make some basic assertions as it related to the
     * proper implementation of equals, hashCode and toString.
     * @param subject1 An instance of T
     * @param suject2 An instance of T that is not equal to subject1.
     * @param subject3ThatEqualsSubject1 An instance of T such that when passed as
     *                                   a parameter to subject1.equals, will return true.
     * @param <T> The type being tested.
     */
    public static <T> void assertCoreObjectBehavior(T subject1, T suject2, T subject3ThatEqualsSubject1) {
        Set<T> set = new HashSet<>();
        set.add(subject1);
        set.add(suject2);
        set.add(subject3ThatEqualsSubject1);

        Assert.assertNotEquals(subject1, suject2);
        // Check that subject1 and subject3ThatEqualsSubject1 are equivalent
        // as far as the equals method is concerned.
        Assert.assertEquals(subject1, subject3ThatEqualsSubject1);
        // Check that subject1 and subject3ThatEqualsSubject1 are not the same instance.
        Assert.assertFalse(subject1 == subject3ThatEqualsSubject1);

        // Since subject1 and subject3ThatEqualsSubject1 are equals,
        // we should only have two items in the set.
        Assert.assertEquals(2, set.size());
        Assert.assertTrue(set.contains(subject1));
        Assert.assertTrue(set.contains(suject2));
        Assert.assertTrue(set.contains(subject3ThatEqualsSubject1));

        Assert.assertNotEquals(subject1.toString(), suject2.toString());
        Assert.assertEquals(subject1.toString(), subject3ThatEqualsSubject1.toString());
    }
}
