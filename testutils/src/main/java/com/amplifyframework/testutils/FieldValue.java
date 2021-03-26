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

import java.lang.reflect.Field;

/**
 * Helper class to manipulate model field data for testing purposes.
 */
public final class FieldValue {
    private FieldValue() {}

    /**
     * Sets the field value of a model to new value.
     * @param instance model instance
     * @param fieldName name of the field being written to
     * @param value new value to override field value with
     * @param <T> type of model
     * @throws NoSuchFieldException if fieldName is not a field of given instance
     * @throws IllegalAccessException if object's field is not accessible
     */
    public static <T extends Model> void set(T instance, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field privateField = instance.getClass().getDeclaredField(fieldName);
        privateField.setAccessible(true);
        privateField.set(instance, value);
    }
}
