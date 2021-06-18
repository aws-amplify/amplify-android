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

package com.amplifyframework.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.annotations.ModelField;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Utility that operates on the fields of a
 * {@link com.amplifyframework.core.model.Model}.
 */
public final class FieldFinder {

    /**
     * Dis-allows instantiation of this utility.
     */
    private FieldFinder() {}

    /**
     * Get a set of all the fields of a class that are
     * annotated with {@link ModelField} annotation.
     *
     * @param clazz the Class object.
     * @return set of fields
     */
    @NonNull
    public static List<Field> findModelFieldsIn(@NonNull Class<?> clazz) {
        final List<Field> fields = new ArrayList<>();
        Class<?> fieldContainerClazz = clazz;
        while (fieldContainerClazz != null) {
            for (Field field : fieldContainerClazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(ModelField.class)) {
                    fields.add(field);
                }
            }
            fieldContainerClazz = fieldContainerClazz.getSuperclass();
        }
        Collections.sort(fields, (o1, o2) -> o1.getName().compareTo(o2.getName()));
        return Immutable.of(fields);
    }

    /**
     * Helper for finding all fields in a class without limiting to {@link ModelField}.
     * @param clazz clazz the Class object.
     * @return set of fields
     */
    @NonNull
    public static List<Field> findNonTransientFieldsIn(@NonNull Class<?> clazz) {
        final List<Field> fields = new ArrayList<>();
        Class<?> fieldContainerClazz = clazz;
        while (fieldContainerClazz != null) {
            for (Field field : fieldContainerClazz.getDeclaredFields()) {
                /*
                 * In Android 21+, java.lang.Object has two transient fields, shadow$_klass_ and shadow$_monitor_.
                 * They are not actually present when running unit tests, but only when running on an Android device.
                 * We don't care about them, so we will filter them out by ignoring all transient fields.
                 */
                if (!Modifier.isTransient(field.getModifiers())) {
                    fields.add(field);
                }
            }
            fieldContainerClazz = fieldContainerClazz.getSuperclass();
        }
        Collections.sort(fields, Comparator.comparing(Field::getName));
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
        if (object instanceof SerializedModel) {
            SerializedModel serializedModel = (SerializedModel) object;
            Map<String, Object> serializedData = serializedModel.getSerializedData();
            return serializedData.get(fieldName);
        }
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
