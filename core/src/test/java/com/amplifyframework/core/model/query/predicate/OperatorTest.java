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

package com.amplifyframework.core.model.query.predicate;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests operator evaluation.
 */
public final class OperatorTest {

    /**
     * Test the accuracy of Equal operator evaluation.
     */
    @Test
    public void testEqualOperator() {
        final EqualQueryOperator numericalOperator = new EqualQueryOperator(123);

        assertTrue(numericalOperator.evaluate(123));
        assertFalse(numericalOperator.evaluate(1234));
        assertFalse(numericalOperator.evaluate("123"));

        final EqualQueryOperator stringOperator = new EqualQueryOperator("Hello");

        assertTrue(stringOperator.evaluate("Hello"));
        assertFalse(stringOperator.evaluate("HELLO"));
        assertFalse(stringOperator.evaluate("World"));
    }

    /**
     * Test the accuracy of Not Equal operator evaluation.
     */
    @Test
    public void testNotEqualOperator() {
        final NotEqualQueryOperator numericalOperator = new NotEqualQueryOperator(123);

        assertTrue(numericalOperator.evaluate(124));
        assertTrue(numericalOperator.evaluate("123"));
        assertFalse(numericalOperator.evaluate(123));

        final NotEqualQueryOperator stringOperator = new NotEqualQueryOperator("Hello");

        assertTrue(stringOperator.evaluate("World"));
        assertFalse(stringOperator.evaluate("Hello"));
    }

    /**
     * Test the accuracy of Greater Than operator evaluation.
     */
    @Test
    public void testGreaterThanOperator() {
        final GreaterThanQueryOperator<Integer> integerOperator = new GreaterThanQueryOperator<>(123);

        assertFalse(integerOperator.evaluate(122));
        assertFalse(integerOperator.evaluate(123));
        assertTrue(integerOperator.evaluate(124));

        final GreaterThanQueryOperator<Float> floatOperator = new GreaterThanQueryOperator<>(123f);

        assertFalse(floatOperator.evaluate(122f));
        assertFalse(floatOperator.evaluate(123f));
        assertTrue(floatOperator.evaluate(124f));

        final GreaterThanQueryOperator<String> stringOperator = new GreaterThanQueryOperator<>("abc");

        assertFalse(stringOperator.evaluate("abb"));
        assertFalse(stringOperator.evaluate("abc"));
        assertTrue(stringOperator.evaluate("abd"));
    }

    /**
     * Test the accuracy of Greater Or Equal operator evaluation.
     */
    @Test
    public void testGreaterOrEqualOperator() {
        final GreaterOrEqualQueryOperator<Integer> integerOperator = new GreaterOrEqualQueryOperator<>(123);

        assertFalse(integerOperator.evaluate(122));
        assertTrue(integerOperator.evaluate(123));
        assertTrue(integerOperator.evaluate(124));

        final GreaterOrEqualQueryOperator<Float> floatOperator = new GreaterOrEqualQueryOperator<>(123f);

        assertFalse(floatOperator.evaluate(122f));
        assertTrue(floatOperator.evaluate(123f));
        assertTrue(floatOperator.evaluate(124f));

        final GreaterOrEqualQueryOperator<String> stringOperator = new GreaterOrEqualQueryOperator<>("abc");

        assertFalse(stringOperator.evaluate("abb"));
        assertTrue(stringOperator.evaluate("abc"));
        assertTrue(stringOperator.evaluate("abd"));
    }

    /**
     * Test the accuracy of Less Than operator evaluation.
     */
    @Test
    public void testLessThanOperator() {
        final LessThanQueryOperator<Integer> integerOperator = new LessThanQueryOperator<>(123);

        assertTrue(integerOperator.evaluate(122));
        assertFalse(integerOperator.evaluate(123));
        assertFalse(integerOperator.evaluate(124));

        final LessThanQueryOperator<Float> floatOperator = new LessThanQueryOperator<>(123f);

        assertTrue(floatOperator.evaluate(122f));
        assertFalse(floatOperator.evaluate(123f));
        assertFalse(floatOperator.evaluate(124f));

        final LessThanQueryOperator<String> stringOperator = new LessThanQueryOperator<>("abc");

        assertTrue(stringOperator.evaluate("abb"));
        assertFalse(stringOperator.evaluate("abc"));
        assertFalse(stringOperator.evaluate("abd"));
    }

    /**
     * Test the accuracy of Less Or Equal operator evaluation.
     */
    @Test
    public void testLessOrEqualOperator() {
        final LessOrEqualQueryOperator<Integer> integerOperator = new LessOrEqualQueryOperator<>(123);

        assertTrue(integerOperator.evaluate(122));
        assertTrue(integerOperator.evaluate(123));
        assertFalse(integerOperator.evaluate(124));

        final LessOrEqualQueryOperator<Float> floatOperator = new LessOrEqualQueryOperator<>(123f);

        assertTrue(floatOperator.evaluate(122f));
        assertTrue(floatOperator.evaluate(123f));
        assertFalse(floatOperator.evaluate(124f));

        final LessOrEqualQueryOperator<String> stringOperator = new LessOrEqualQueryOperator<>("abc");

        assertTrue(stringOperator.evaluate("abb"));
        assertTrue(stringOperator.evaluate("abc"));
        assertFalse(stringOperator.evaluate("abd"));
    }

    /**
     * Test the accuracy of Between operator evaluation.
     */
    @Test
    public void testBetweenOperator() {
        final BetweenQueryOperator<Integer> numericalOperator = new BetweenQueryOperator<>(2, 4);

        assertFalse(numericalOperator.evaluate(1));
        assertTrue(numericalOperator.evaluate(2));
        assertTrue(numericalOperator.evaluate(3));
        assertTrue(numericalOperator.evaluate(4));
        assertFalse(numericalOperator.evaluate(5));

        final BetweenQueryOperator<String> stringOperator = new BetweenQueryOperator<>("2", "4");

        assertFalse(stringOperator.evaluate("1"));
        assertTrue(stringOperator.evaluate("2"));
        assertTrue(stringOperator.evaluate("3"));
        assertTrue(stringOperator.evaluate("4"));
        assertFalse(stringOperator.evaluate("5"));
    }

    /**
     * Test the accuracy of Begins With operator evaluation.
     */
    @Test
    public void testBeginsWithOperator() {
        final BeginsWithQueryOperator operator = new BeginsWithQueryOperator("Raph");

        assertTrue(operator.evaluate("Raphael"));
        assertTrue(operator.evaluate("Raph"));
        assertFalse(operator.evaluate("R"));
        assertFalse(operator.evaluate("RRaphael"));
    }

    /**
     * Test the accuracy of Contains operator evaluation.
     */
    @Test
    public void testContainsOperator() {
        final ContainsQueryOperator operator = new ContainsQueryOperator("e");

        assertTrue(operator.evaluate("Hello"));
        assertTrue(operator.evaluate("e"));
        assertFalse(operator.evaluate("World"));
        assertFalse(operator.evaluate(""));
    }
}

