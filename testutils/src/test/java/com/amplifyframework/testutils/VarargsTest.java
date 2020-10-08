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

import com.amplifyframework.testutils.random.RandomString;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link Varargs} test utility.
 * (A test for a test? 😱)
 */
public final class VarargsTest {
    /**
     * A null array should become an empty list.
     */
    @Test
    public void nullArrayToEmptyList() {
        assertEquals(Collections.emptyList(), Varargs.toList(null));
    }

    /**
     * An empty array should become an empty list.
     */
    @Test
    public void emptyArrayToEmptyList() {
        assertEquals(Collections.emptyList(), Varargs.toList(new String[0]));
    }

    /**
     * A singleton array should become a singleton list.
     */
    @Test
    public void singletonArrayToSingletonList() {
        String sentinel = RandomString.string();
        String[] singletonArray = new String[] {
            sentinel
        };
        assertEquals(
            Collections.singletonList(sentinel),
            Varargs.toList(singletonArray)
        );
    }

    /**
     * An array of multiple items should become a list of the same
     * items in the same order.
     */
    @Test
    public void multipleArrayToMultipleList() {
        String valueOne = RandomString.string();
        String valueTwo = RandomString.string();
        String[] multipleValuesArray = new String[] {
            valueOne, valueTwo
        };
        assertEquals(Arrays.asList(valueOne, valueTwo), Varargs.toList(multipleValuesArray));
    }
}
