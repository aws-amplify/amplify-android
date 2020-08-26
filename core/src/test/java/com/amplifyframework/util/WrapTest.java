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
 * Tests the {@link Wrap} utility.
 */
public final class WrapTest {
    /**
     * Validate wrapping a string in back ticks.
     */
    @Test
    public void wrapInBackTicksReturnsBackTickedString() {
        assertEquals("`Hamburger`", Wrap.inBackticks("Hamburger"));
    }

    /**
     * Don't try wrapping null into back ticks, just return null.
     */
    @Test
    public void passThroughNullWithoutBackTicks() {
        //noinspection ConstantConditions
        assertNull(Wrap.inBackticks(null));
    }

    /**
     * Validate wrapping a string in single quotes.
     */
    @Test
    public void wrapInSingleQuotesReturnsQuotedString() {
        assertEquals("'Hamburger'", Wrap.inSingleQuotes("Hamburger"));
    }

    /**
     * Don't try wrapping null into single quotes, just return null.
     */
    @Test
    public void passThroughNullWithoutSingleQuotes() {
        //noinspection ConstantConditions
        assertNull(Wrap.inSingleQuotes(null));
    }

    /**
     * Validate wrapping a string in double quotes.
     */
    @Test
    public void wrapInDoubleQuotesReturnsQuotedString() {
        assertEquals("\"Tomato\"", Wrap.inDoubleQuotes("Tomato"));
    }

    /**
     * Don't try wrapping null in double quotes, just let it pass through.
     */
    @Test
    public void passThroughNullWithoutDoubleQuotes() {
        //noinspection ConstantConditions
        assertNull(Wrap.inDoubleQuotes(null));
    }

    /**
     * Validate wrapping a string in braces.
     */
    @Test
    public void wrapInBracesReturnsBracedString() {
        assertEquals("{Tomato}", Wrap.inBraces("Tomato"));
    }

    /**
     * Don't try wrapping null in braces, just let it pass through.
     */
    @Test
    public void passThroughNullWithoutBraces() {
        //noinspection ConstantConditions
        assertNull(Wrap.inBraces(null));
    }

    /**
     * Validate wrapping a string in pretty braces.
     */
    @Test
    public void wrapInPrettyBracesReturnsBracedString() {
        assertEquals(" {\n      Tomato\n    }",
                Wrap.inPrettyBraces("Tomato", "    ", "  "));
    }

    /**
     * Don't try wrapping null in pretty braces, just let it pass through.
     */
    @Test
    public void passThroughNullWithoutPrettyBraces() {
        //noinspection ConstantConditions
        assertNull(Wrap.inPrettyBraces(null, "  ", "  "));
    }

    /**
     * Validate wrapping a string in parentheses.
     */
    @Test
    public void wrapInParenthesesReturnsParenthesizedString() {
        assertEquals("(Tomato)", Wrap.inParentheses("Tomato"));
    }

    /**
     * Don't try wrapping null in parentheses, just let it pass through.
     */
    @Test
    public void passThroughNullWithoutParentheses() {
        //noinspection ConstantConditions
        assertNull(Wrap.inParentheses(null));
    }
}
