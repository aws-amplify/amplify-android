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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests the {@link Quotes} utility.
 */
public final class QuotesTest {
    /**
     * Validate wrapping a string in single quotes.
     */
    @Test
    public void wrapInSingleQuotesReturnsQuotedString() {
        assertEquals("'Hamburger'", Quotes.wrapInSingle("Hamburger"));
    }

    /**
     * Don't try wrapping null into single quotes, just return null.
     */
    @Test
    public void passThroughNullWithoutSingleQuotes() {
        //noinspection ConstantConditions
        assertNull(Quotes.wrapInSingle(null));
    }

    /**
     * Validate wrapping a string in double quotes.
     */
    @Test
    public void wrapInDoubleQuotesReturnsQuotedString() {
        assertEquals("\"Tomato\"", Quotes.wrapInDouble("Tomato"));
    }

    /**
     * Don't try wrapping null in double quotes, just let it pass through.
     */
    @Test
    public void passThroughNullWithoutDoubleQuotes() {
        //noinspection ConstantConditions
        assertNull(Quotes.wrapInDouble(null));
    }
}
