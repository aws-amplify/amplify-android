/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.core.model.Model;

import org.junit.Assert;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility for comparing {@class Model} objects.
 */
public final class ModelAssert {
    private ModelAssert() {}

    /**
     * Asserts whether two models are equal, ignoring the createdAt and updatedAt values.  This method modifies the
     * timestamp fields (createdAt and updatedAt) on the expected object, with the values, from the actual, does the
     * assert, and then sets them back to the original values at the end, so that this method doesn't have side effects.
     * @param expected expected value
     * @param actual the value to check against <code>expected</code>
     * @param <T> type of model
     */
    public static <T extends Model> void assertEqualsIgnoringTimestamps(T expected, T actual) {
        Map<String, Object> originalValues = new HashMap<>();
        for (String fieldName : Arrays.asList("createdAt", "updatedAt")) {
            try {
                Field privateField = actual.getClass().getDeclaredField(fieldName); // can throw NoSuchFieldException
                privateField.setAccessible(true);
                Object actualValue = privateField.get(actual); // can throw IllegalAccessException
                Object expectedValue = privateField.get(expected); // can throw IllegalAccessException
                privateField.set(expected, actualValue); // can throw IllegalAccessException
                // if we got here, we successfully updated the expected object, so we should remember the original
                // expected value, so we can restore it at the end.
                originalValues.put(fieldName, expectedValue);
            } catch (NoSuchFieldException exception) {
                // Field doesn't exist, just proceed with assertEquals
            } catch (IllegalAccessException exception) {
                // Field was not accessible, just proceed by calling assertEquals anyway.
            }
        }

        // Do the assert!
        Assert.assertEquals(expected, actual);

        // Reset values back to original, so that this method has no side effects.
        for (String fieldName : originalValues.keySet()) {
            try {
                Field privateField = actual.getClass().getDeclaredField(fieldName); // can throw NoSuchFieldException
                privateField.setAccessible(true);
                privateField.set(expected, originalValues.get(fieldName)); // can throw IllegalAccessException
            } catch (NoSuchFieldException exception) {
                // Field doesn't exist.  Shouldn't happen, since it didn't happen above.
            } catch (IllegalAccessException exception) {
                // Field was not accessible.  Shouldn't happen, since it didn't happen above.
            }
        }
    }
}
