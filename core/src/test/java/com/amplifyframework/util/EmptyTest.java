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

package com.amplifyframework.util;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link Empty} utility.
 */
public final class EmptyTest {
    /**
     * An empty collection should be evaluated as empty!.
     */
    @Test
    public void emptyCollectionIsEmpty() {
        assertTrue(Empty.check(Collections.emptyList()));
    }

    /**
     * A null list reference should evaluated as empty.
     */
    @Test
    public void nullCollectionIsEmpty() {
        final List<Void> nullList = null;
        //noinspection ConstantConditions
        assertTrue(Empty.check(nullList));

    }

    /**
     * An empty map should evaluated as empty.
     */
    @Test
    public void emptyMapIsEmpty() {
        assertTrue(Empty.check(Collections.emptyMap()));
    }

    /**
     * A null map should evaluate as empty.
     */
    @Test
    public void nullMapIsEmpty() {
        Map<Void, Void> nullMap = null;
        //noinspection ConstantConditions
        assertTrue(Empty.check(nullMap));
    }
}
