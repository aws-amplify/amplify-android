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

package com.amplifyframework.datastore.util;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Immutable;
import com.amplifyframework.datastore.annotations.ModelField;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility that operates on the fields of a
 * {@link com.amplifyframework.datastore.model.Model}.
 */
public final class FieldFinder {

    /**
     * Dis-allows instantiation of this utility.
     */
    private FieldFinder() {
    }

    /**
     * Get a set of all the fields of a class that are
     * annotated with {@link ModelField} annotation.
     *
     * @param clazz the Class object.
     * @return set of fields
     */
    public static Set<Field> findFieldsIn(@NonNull Class<?> clazz) {
        final Set<Field> modifiableSet = new HashSet<>();
        Class<?> c = clazz;
        while (c != null) {
            for (Field field : c.getDeclaredFields()) {
                if (field.isAnnotationPresent(ModelField.class)) {
                    modifiableSet.add(field);
                }
            }
            c = c.getSuperclass();
        }
        return Immutable.of(modifiableSet);
    }
}
