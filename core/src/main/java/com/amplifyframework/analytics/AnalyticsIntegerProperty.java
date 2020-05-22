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

package com.amplifyframework.analytics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import java.util.Objects;

/**
 * AnalyticsIntegerProperty wraps an Integer value to store in {@link AnalyticsProperties}.
 */
public final class AnalyticsIntegerProperty implements AnalyticsPropertyBehavior<Integer> {
    private final Integer value;

    private AnalyticsIntegerProperty(Integer value) {
        this.value = value;
    }

    /**
     * getValue returns the wrapped Integer value stored in the property.
     *
     * @return The wrapped Boolean value
     */
    @NonNull
    @Override
    public Integer getValue() {
        return value;
    }

    /**
     * Factory method to instantiate an {@link AnalyticsIntegerProperty} from a {@link Integer} value.
     *
     * @param value an integer value
     * @return an instance of {@link AnalyticsIntegerProperty}
     */
    @NonNull
    public static AnalyticsIntegerProperty from(@NonNull Integer value) {
        return new AnalyticsIntegerProperty(Objects.requireNonNull(value));
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }
        AnalyticsIntegerProperty that = (AnalyticsIntegerProperty) thatObject;
        return ObjectsCompat.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return getValue().hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "AnalyticsIntegerProperty{" +
            "value=" + value +
            '}';
    }
}
