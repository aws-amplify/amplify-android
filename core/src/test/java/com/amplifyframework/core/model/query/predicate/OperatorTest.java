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

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests operator evaluation.
 */
@SuppressWarnings("MagicNumber")
public final class OperatorTest {

    /**
     * Test the accuracy of Equal operator evaluation.
     */
    @Test
    public void testEqualOperator() {
        final EqualQueryOperator numericalOperator = new EqualQueryOperator(123);

        Assert.assertTrue(numericalOperator.evaluate(123));
        Assert.assertFalse(numericalOperator.evaluate(1234));
        Assert.assertFalse(numericalOperator.evaluate("123"));

        final EqualQueryOperator stringOperator = new EqualQueryOperator("Hello");

        Assert.assertTrue(stringOperator.evaluate("Hello"));
        Assert.assertFalse(stringOperator.evaluate("HELLO"));
        Assert.assertFalse(stringOperator.evaluate("World"));
    }

    /**
     * Test the accuracy of Not Equal operator evaluation.
     */
    @Test
    public void testNotEqualOperator() {
        final NotEqualQueryOperator numericalOperator = new NotEqualQueryOperator(123);

        Assert.assertTrue(numericalOperator.evaluate(124));
        Assert.assertTrue(numericalOperator.evaluate("123"));
        Assert.assertFalse(numericalOperator.evaluate(123));

        final NotEqualQueryOperator stringOperator = new NotEqualQueryOperator("Hello");

        Assert.assertTrue(stringOperator.evaluate("World"));
        Assert.assertFalse(stringOperator.evaluate("Hello"));
    }

    /**
     * Test the accuracy of Greater Than operator evaluation.
     */
    @Test
    public void testGreaterThanOperator() {
        final GreaterThanQueryOperator<Integer> integerOperator = new GreaterThanQueryOperator<>(123);

        Assert.assertFalse(integerOperator.evaluate(122));
        Assert.assertFalse(integerOperator.evaluate(123));
        Assert.assertTrue(integerOperator.evaluate(124));

        final GreaterThanQueryOperator<Float> floatOperator = new GreaterThanQueryOperator<>(123f);

        Assert.assertFalse(floatOperator.evaluate(122f));
        Assert.assertFalse(floatOperator.evaluate(123f));
        Assert.assertTrue(floatOperator.evaluate(124f));

        final GreaterThanQueryOperator<String> stringOperator = new GreaterThanQueryOperator<>("abc");

        Assert.assertFalse(stringOperator.evaluate("abb"));
        Assert.assertFalse(stringOperator.evaluate("abc"));
        Assert.assertTrue(stringOperator.evaluate("abd"));
    }

    /**
     * Test the accuracy of Greater Or Equal operator evaluation.
     */
    @Test
    public void testGreaterOrEqualOperator() {
        final GreaterOrEqualQueryOperator<Integer> integerOperator = new GreaterOrEqualQueryOperator<>(123);

        Assert.assertFalse(integerOperator.evaluate(122));
        Assert.assertTrue(integerOperator.evaluate(123));
        Assert.assertTrue(integerOperator.evaluate(124));

        final GreaterOrEqualQueryOperator<Float> floatOperator = new GreaterOrEqualQueryOperator<>(123f);

        Assert.assertFalse(floatOperator.evaluate(122f));
        Assert.assertTrue(floatOperator.evaluate(123f));
        Assert.assertTrue(floatOperator.evaluate(124f));

        final GreaterOrEqualQueryOperator<String> stringOperator = new GreaterOrEqualQueryOperator<>("abc");

        Assert.assertFalse(stringOperator.evaluate("abb"));
        Assert.assertTrue(stringOperator.evaluate("abc"));
        Assert.assertTrue(stringOperator.evaluate("abd"));
    }

    /**
     * Test the accuracy of Less Than operator evaluation.
     */
    @Test
    public void testLessThanOperator() {
        final LessThanQueryOperator<Integer> integerOperator = new LessThanQueryOperator<>(123);

        Assert.assertTrue(integerOperator.evaluate(122));
        Assert.assertFalse(integerOperator.evaluate(123));
        Assert.assertFalse(integerOperator.evaluate(124));

        final LessThanQueryOperator<Float> floatOperator = new LessThanQueryOperator<>(123f);

        Assert.assertTrue(floatOperator.evaluate(122f));
        Assert.assertFalse(floatOperator.evaluate(123f));
        Assert.assertFalse(floatOperator.evaluate(124f));

        final LessThanQueryOperator<String> stringOperator = new LessThanQueryOperator<>("abc");

        Assert.assertTrue(stringOperator.evaluate("abb"));
        Assert.assertFalse(stringOperator.evaluate("abc"));
        Assert.assertFalse(stringOperator.evaluate("abd"));
    }

    /**
     * Test the accuracy of Less Or Equal operator evaluation.
     */
    @Test
    public void testLessOrEqualOperator() {
        final LessOrEqualQueryOperator<Integer> integerOperator = new LessOrEqualQueryOperator<>(123);

        Assert.assertTrue(integerOperator.evaluate(122));
        Assert.assertTrue(integerOperator.evaluate(123));
        Assert.assertFalse(integerOperator.evaluate(124));

        final LessOrEqualQueryOperator<Float> floatOperator = new LessOrEqualQueryOperator<>(123f);

        Assert.assertTrue(floatOperator.evaluate(122f));
        Assert.assertTrue(floatOperator.evaluate(123f));
        Assert.assertFalse(floatOperator.evaluate(124f));

        final LessOrEqualQueryOperator<String> stringOperator = new LessOrEqualQueryOperator<>("abc");

        Assert.assertTrue(stringOperator.evaluate("abb"));
        Assert.assertTrue(stringOperator.evaluate("abc"));
        Assert.assertFalse(stringOperator.evaluate("abd"));
    }

    /**
     * Test the accuracy of Between operator evaluation.
     */
    @Test
    public void testBetweenOperator() {
        final BetweenQueryOperator<Integer> numericalOperator = new BetweenQueryOperator<>(2, 4);

        Assert.assertFalse(numericalOperator.evaluate(1));
        Assert.assertFalse(numericalOperator.evaluate(2));
        Assert.assertTrue(numericalOperator.evaluate(3));
        Assert.assertFalse(numericalOperator.evaluate(4));
        Assert.assertFalse(numericalOperator.evaluate(5));

        final BetweenQueryOperator<String> stringOperator = new BetweenQueryOperator<>("2", "4");

        Assert.assertFalse(stringOperator.evaluate("1"));
        Assert.assertFalse(stringOperator.evaluate("2"));
        Assert.assertTrue(stringOperator.evaluate("3"));
        Assert.assertFalse(stringOperator.evaluate("4"));
        Assert.assertFalse(stringOperator.evaluate("5"));
    }

    /**
     * Test the accuracy of Begins With operator evaluation.
     */
    @Test
    public void testBeginsWithOperator() {
        final BeginsWithQueryOperator operator = new BeginsWithQueryOperator("Raph");

        Assert.assertTrue(operator.evaluate("Raphael"));
        Assert.assertTrue(operator.evaluate("Raph"));
        Assert.assertFalse(operator.evaluate("R"));
        Assert.assertFalse(operator.evaluate("RRaphael"));
    }

    /**
     * Test the accuracy of Contains operator evaluation.
     */
    @Test
    public void testContainsOperator() {
        final ContainsQueryOperator operator = new ContainsQueryOperator("e");

        Assert.assertTrue(operator.evaluate("Hello"));
        Assert.assertTrue(operator.evaluate("e"));
        Assert.assertFalse(operator.evaluate("World"));
        Assert.assertFalse(operator.evaluate(""));
    }
}

