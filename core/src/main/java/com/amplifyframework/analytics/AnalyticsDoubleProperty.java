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

package com.amplifyframework.analytics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import java.util.Objects;

/**
 * AnalyticsDoubleProperty wraps a Double value to store in {@link AnalyticsProperties}.
 */
public final class AnalyticsDoubleProperty implements AnalyticsPropertyBehavior<Double> {
    private final Double value;

    private AnalyticsDoubleProperty(Double value) {
        this.value = value;
    }

    /**
     * getValue returns the wrapped Double value stored in the property.
     *
     * @return The wrapped Double value
     */
    @Override
    public Double getValue() {
        return value;
    }

    /**
     * Factory method to instantiate a {@link AnalyticsDoubleProperty} from a {@link Double} value.
     *
     * @param value a floating point number value
     * @return an instance of {@link AnalyticsDoubleProperty}
     */
    @NonNull
    public static AnalyticsDoubleProperty from(@NonNull Double value) {
        return new AnalyticsDoubleProperty(Objects.requireNonNull(value));
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        AnalyticsDoubleProperty that = (AnalyticsDoubleProperty) thatObject;
        return ObjectsCompat.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return getValue().hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "AnalyticsDoubleProperty{" +
            "value=" + value +
            '}';
    }
}
