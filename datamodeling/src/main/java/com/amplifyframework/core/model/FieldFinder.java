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

package com.amplifyframework.core.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.util.Immutable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility that operates on the fields of a
 * {@link com.amplifyframework.core.model.Model}.
 */
public final class FieldFinder {

    /**
     * Dis-allows instantiation of this utility.
     */
    private FieldFinder() {
    }

    /**
     * Get a set of all the fields of a class that are
     * annotated with {@link com.amplifyframework.core.model.ModelField} annotation.
     *
     * @param clazz the Class object.
     * @return set of fields
     */
    @NonNull
    public static List<Field> findFieldsIn(@NonNull Class<?> clazz) {
        final List<Field> fields = new ArrayList<>();
        Class<?> c = clazz;
        while (c != null) {
            for (Field field : c.getDeclaredFields()) {
                if (field.isAnnotationPresent(ModelField.class)) {
                    fields.add(field);
                }
            }
            c = c.getSuperclass();
        }
        Collections.sort(fields, (o1, o2) -> o1.getName().compareTo(o2.getName()));
        return Immutable.of(fields);
    }

    /**
     * Extract the value of a field in an Object by field name.
     * @param object Object to obtain field value from
     * @param fieldName Name of the field being examined
     * @return Value of the field if the field exists
     * @throws NoSuchFieldException if object does not contain
     *         a field that matches fieldName
     */
    @Nullable
    public static Object extractFieldValue(@NonNull Object object,
                                       @NonNull String fieldName) throws NoSuchFieldException {
        try {
            Field objectField = object.getClass().getDeclaredField(fieldName);
            objectField.setAccessible(true);
            return objectField.get(object);
        } catch (NoSuchFieldException noSuchFieldException) {
            throw noSuchFieldException;
        } catch (Exception exception) {
            return null;
        }
    }
}
