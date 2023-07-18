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

package com.amplifyframework.datastore.storage.sqlite;

import androidx.annotation.Nullable;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.QuerySortBy;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.logging.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/***
 * Model comparator.
 * @param <T> type of Model.
 */
public final class ModelComparator<T extends Model> implements Comparator<T> {

    private static final int NUM_LETTERS_FOR_GET = 3;
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");
    private final QuerySortBy sortBy;
    private final Class<T> itemClass;
    private final Consumer<DataStoreException> onObservationError;

    /***
     * Compares type T which extends models.
     * @param sortBy sort by.
     * @param itemClass item class.
     * @param onObservationError invoked on observation error.
     */
    public ModelComparator(QuerySortBy sortBy,
                           Class<T> itemClass,
                           Consumer<DataStoreException> onObservationError) {
        this.sortBy = sortBy;
        this.itemClass = itemClass;
        this.onObservationError = onObservationError;
    }

    private Object getValue(Method method, Model model) {
        try {
            return method.invoke(model);
        } catch (IllegalAccessException exception) {
            String message = "Could not invoke method during sorting because of access level"
                    + method.getName();
            LOG.warn(message);
            onObservationError.accept(new DataStoreException("ObserveQuery",
                    message + Objects.requireNonNull(exception.getMessage())));
        } catch (InvocationTargetException exception) {
            String message = "Could not invoke method during sorting " + method.getName();
            LOG.warn(message);
            onObservationError.accept(new DataStoreException("ObserveQuery",
                    message + Objects.requireNonNull(exception.getMessage())));
        }
        return null;
    }

    @Override
    public int compare(T modelLeft, T modelRight) {
        if (sortBy != null && sortBy.getModelName() != null) {
            Method method = getMethod();
            if (method != null) {
                Object valueLeft = getValue(method, modelLeft);
                Object valueRight = getValue(method, modelRight);
                if (valueLeft == null) {
                    return -1;
                } else if (valueRight == null) {
                    return 1;
                }
                if (method.getReturnType() == String.class) {
                    String valueModel1 = (String) valueLeft;
                    String valueModel2 = (String) valueRight;
                    return valueModel1.compareTo(valueModel2);
                }
                if (method.getReturnType() == int.class) {
                    int valueModel1 = (int) valueLeft;
                    int valueModel2 = (int) valueRight;
                    return Integer.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType() == Integer.class) {
                    Integer valueModel1 = (Integer) valueLeft;
                    Integer valueModel2 = (Integer) valueRight;
                    return Integer.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType() == boolean.class) {
                    boolean valueModel1 = (boolean) valueLeft;
                    boolean valueModel2 = (boolean) valueRight;
                    return Boolean.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType() == Boolean.class) {
                    Boolean valueModel1 = (Boolean) valueLeft;
                    Boolean valueModel2 = (Boolean) valueRight;
                    return Boolean.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType() == long.class) {
                    long valueModel1 = (long) valueLeft;
                    long valueModel2 = (long) valueRight;
                    return Long.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType() == double.class) {
                    double valueModel1 = (double) valueLeft;
                    double valueModel2 = (double) valueRight;
                    return Double.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType() == Double.class) {
                    Double valueModel1 = (Double) valueLeft;
                    Double valueModel2 = (Double) valueRight;
                    return Double.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType() == Date.class) {
                    Date valueModel1 = (Date) valueLeft;
                    Date valueModel2 = (Date) valueRight;
                    return valueModel1.compareTo(valueModel2);
                }
                if (method.getReturnType() == float.class) {
                    float valueModel1 = (float) valueLeft;
                    float valueModel2 = (float) valueRight;
                    return Float.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType() == Float.class) {
                    Float valueModel1 = (Float) valueLeft;
                    Float valueModel2 = (Float) valueRight;
                    return Float.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType() == char.class) {
                    char valueModel1 = (char) valueLeft;
                    char valueModel2 = (char) valueRight;
                    return Character.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType() == Character.class) {
                    Character valueModel1 = (Character) valueLeft;
                    Character valueModel2 = (Character) valueRight;
                    return Character.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType() == byte.class) {
                    byte valueModel1 = (byte) valueLeft;
                    byte valueModel2 = (byte) valueRight;
                    return Byte.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType() == Byte.class) {
                    Byte valueModel1 = (Byte) valueLeft;
                    Byte valueModel2 = (Byte) valueRight;
                    return Byte.compare(valueModel1, valueModel2);
                }
                if (method.getReturnType() == Temporal.DateTime.class) {
                    Temporal.DateTime valueModel1 = (Temporal.DateTime) valueLeft;
                    Temporal.DateTime valueModel2 = (Temporal.DateTime) valueRight;
                    return valueModel1.compareTo(valueModel2);
                }
                if (method.getReturnType() == Temporal.Date.class) {
                    Temporal.DateTime valueModel1 = (Temporal.DateTime) valueLeft;
                    Temporal.DateTime valueModel2 = (Temporal.DateTime) valueRight;
                    return valueModel1.compareTo(valueModel2);
                }
            }
        }
        return -1;
    }

    private Method getMethod() {
        Method method = null;
        try {
            Field field = itemClass.getDeclaredField(sortBy.getField());
            if (field.getName().equals(sortBy.getField())) {
                method = getMethod(itemClass, field);
            }
        } catch (NoSuchFieldException exception) {
            LOG.warn("Could not find the method " + sortBy.getField());
        }
        return method;
    }

    @Nullable
    private Method getMethod(Class<?> className, Field field) {
        for (Method method : className.getMethods()) {
            if ((method.getName().startsWith("get")) && (method.getName().length() ==
                    (field.getName().length() + NUM_LETTERS_FOR_GET))) {
                if (method.getName().toLowerCase(Locale.ROOT).endsWith(field.getName().toLowerCase(Locale.ROOT))) {
                    return method;
                }
            }
        }
        return null;
    }
}


