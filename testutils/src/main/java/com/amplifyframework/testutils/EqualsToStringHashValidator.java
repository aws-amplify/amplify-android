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
 * Class that can be used to test core
 * object behavior of a class that implements its own
 * equals, hashCode and toString methods.
 */
public final class EqualsToStringHashValidator {

    /**
     * Hidding default constructor of utility class.
     */
    private EqualsToStringHashValidator() {}

    /**
     * Function to make some basic assertions as it related to the
     * proper implementation of equals, hashCode and toString.
     * @param reference An instance of T
     * @param valueAndReferenceNotEquals An instance of T that is not equal to the reference parameter.
     * @param valueEqualsReferenceNotEquals An instance of T such that when passed as
     *                                   a parameter to reference.equals, will return true.
     * @param <T> The type being tested.(T reference, T equalsReference, T notEqualsReference)
     */
    public static <T> void validate(T reference, T valueAndReferenceNotEquals, T valueEqualsReferenceNotEquals) {
        Set<T> set = new HashSet<>();
        set.add(reference);
        set.add(valueAndReferenceNotEquals);
        set.add(valueEqualsReferenceNotEquals);

        Assert.assertNotEquals(reference, valueAndReferenceNotEquals);
        // Check that reference and valueEqualsReferenceNotEquals are equivalent
        // as far as the equals method is concerned.
        Assert.assertEquals(reference, valueEqualsReferenceNotEquals);
        // Check that reference and valueEqualsReferenceNotEquals are not the same instance.
        Assert.assertFalse(reference == valueEqualsReferenceNotEquals);

        // Since reference and valueEqualsReferenceNotEquals are equals,
        // we should only have two items in the set.
        Assert.assertEquals(2, set.size());
        Assert.assertTrue(set.contains(reference));
        Assert.assertTrue(set.contains(valueAndReferenceNotEquals));
        Assert.assertTrue(set.contains(valueEqualsReferenceNotEquals));

        Assert.assertNotEquals(reference.toString(), valueAndReferenceNotEquals.toString());
        Assert.assertEquals(reference.toString(), valueEqualsReferenceNotEquals.toString());
    }
}
