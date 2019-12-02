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

package com.amplifyframework.testmodels.commentsblog;

import com.amplifyframework.testutils.RandomString;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests the code-generated {@link BlogOwner}.
 */
public class BlogOwnerTest {
    /**
     * Two {@link BlogOwner} that were built using identical fields should return true for
     * {@link BlogOwner#equals(Object)}.
     */
    @Test
    public void blogOwnerEqualsReturnsTrueForSameValueObjects() {
        String name = RandomString.string();
        String id = RandomString.string();

        BlogOwner one = BlogOwner.builder()
            .name(name)
            .id(id)
            .build();

        BlogOwner two = BlogOwner.builder()
            .name(name)
            .id(id)
            .build();

        // First, are even the individual fields equals()?
        assertEquals(one.getBlog(), two.getBlog());
        assertEquals(one.getName(), two.getName());
        assertEquals(one.getId(), two.getId());
        assertEquals(one.getWea(), two.getWea());

        // Okay, then the objects are too, right?
        assertEquals(one, two);
    }


    /**
     * When you construct a {@link BlogOwner} twice in the same fashion,
     * the {@link BlogOwner#hashCode()} should be the same for both instances.
     */
    @Test
    public void hashCodeProducesStableResult() {
        final String name = RandomString.string();
        final String id = RandomString.string();

        int oneHash = BlogOwner.builder()
            .name(name)
            .id(id)
            .build()
            .hashCode();

        int twoHash = BlogOwner.builder()
            .name(name)
            .id(id)
            .build()
            .hashCode();

        assertEquals(
            String.format("hashCode() is broken for BlowOwner. Wanted %d, but got %d.", oneHash, twoHash),
            oneHash,
            twoHash
        );
    }
}
