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

import java.util.Objects;

/**
 * AnalyticsStringProperty wraps a String value to store in {@link AnalyticsProperties}.
 */
public final class AnalyticsStringProperty implements AnalyticsPropertyBehavior<String> {
    private final String value;

    private AnalyticsStringProperty(String value) {
        this.value = value;
    }

    /**
     * getValue returns the wrapped String value stored in the property.
     *
     * @return The wrapped String value
     */
    @Override
    public String getValue() {
        return value;
    }

    /**
     * Factory method to instantiate a {@link AnalyticsStringProperty} from a {@link String} value.
     *
     * @param value a string value
     * @return an instance of {@link AnalyticsStringProperty}
     */
    @NonNull
    public static AnalyticsStringProperty from(@NonNull String value) {
        return new AnalyticsStringProperty(Objects.requireNonNull(value));
    }
}
