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

import java.util.Objects;

/**
 * AnalyticsBooleanProperty wraps a Boolean value to store in {@link AnalyticsProperties}.
 */
public final class AnalyticsBooleanProperty implements AnalyticsPropertyBehavior<Boolean> {
    private final Boolean value;

    private AnalyticsBooleanProperty(Boolean value) {
        this.value = value;
    }

    /**
     * getValue returns the wrapped Boolean value stored in the property.
     *
     * @return The wrapped Boolean value
     */
    @Override
    public Boolean getValue() {
        return value;
    }

    /**
     * Factory method to instantiate a {@link AnalyticsBooleanProperty} from a {@link Boolean} value.
     *
     * @param value a boolean value
     * @return an instance of {@link AnalyticsBooleanProperty}
     */
    @NonNull
    public static AnalyticsBooleanProperty from(@NonNull Boolean value) {
        return new AnalyticsBooleanProperty(Objects.requireNonNull(value));
    }
}
