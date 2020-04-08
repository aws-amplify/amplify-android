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

package com.amplifyframework.predictions.aws;

import com.amplifyframework.predictions.models.Feature;

import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Utility class to assert that two features have matching values
 * without regarding their IDs or confidence scores.
 */
final class FeatureAssert {
    private FeatureAssert() {}

    /**
     * Assert that a given value equals the feature's assigned value.
     * @param expectedValue the value to expect in a feature
     * @param actual the actual feature
     * @param <T> the type of expected value
     */
    static <T> void assertMatches(T expectedValue, Feature<T> actual) {
        assertNotNull(expectedValue);
        assertNotNull(actual);
        assertEquals(expectedValue, actual.getValue());
    }

    /**
     * Assert that a list of features all match the expected values.
     * @param expected the list of feature values to expect
     * @param actual the list of actual features
     * @param <F> the data type of the feature
     * @param <T> the data type of the value of the feature
     */
    static <F extends Feature<T>, T> void assertMatches(Collection<T> expected, Collection<F> actual) {
        assertNotNull(expected);
        assertNotNull(actual);
        assertEquals("Size mismatch:", expected.size(), actual.size());
        Iterator<T> expectedIterator = expected.iterator();
        Iterator<F> actualIterator = actual.iterator();
        while (expectedIterator.hasNext()) {
            assertMatches(expectedIterator.next(), actualIterator.next());
        }
    }
}
