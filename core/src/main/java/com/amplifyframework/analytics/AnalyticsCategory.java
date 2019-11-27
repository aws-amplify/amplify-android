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

import com.amplifyframework.ConfigurationException;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;

/**
 * Defines the Client API consumed by the application.
 * Internally routes the calls to the Analytics CategoryType
 * plugins registered.
 */
public final class AnalyticsCategory extends Category<AnalyticsPlugin<?>>
        implements AnalyticsCategoryBehavior {

    /**
     * Protect enabling and disabling of Analytics event
     * collection and sending.
     */
    private static final Object LOCK = new Object();

    /**
     * By default collection and sending of Analytics events
     * are enabled.
     */
    private boolean enabled;

    /**
     * Constructs a new AnalyticsCategory instance.
     */
    public AnalyticsCategory() {
        super();
        this.enabled = true;
    }

    /**
     * Retrieve the Analytics category type enum.
     * @return enum that represents Analytics category
     */
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.ANALYTICS;
    }

    @Override
    public void disable() {
        synchronized (LOCK) {
            enabled = false;
        }
    }

    @Override
    public void enable() {
        synchronized (LOCK) {
            enabled = true;
        }
    }

    @Override
    public void recordEvent(@NonNull String eventName)
            throws AnalyticsException, ConfigurationException {
        if (enabled) {
            getSelectedPlugin().recordEvent(eventName);
        }
    }

    @Override
    public void recordEvent(@NonNull final AnalyticsEvent analyticsEvent)
            throws AnalyticsException, ConfigurationException {
        if (enabled) {
            getSelectedPlugin().recordEvent(analyticsEvent);
        }
    }

    @Override
    public void updateProfile(@NonNull AnalyticsProfile analyticsProfile)
            throws AnalyticsException, ConfigurationException {
        if (enabled) {
            getSelectedPlugin().updateProfile(analyticsProfile);
        }
    }
}
