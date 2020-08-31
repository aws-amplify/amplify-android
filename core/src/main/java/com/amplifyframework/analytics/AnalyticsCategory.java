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
     *
     * @return enum that represents Analytics category
     */
    @NonNull
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.ANALYTICS;
    }

    @Override
    public void identifyUser(@NonNull String userId, @Nullable UserProfile profile) {
        if (enabled) {
            getSelectedPlugin().identifyUser(userId, profile);
        }
    }

    @Override
    public void disable() {
        enabled = false;
        getSelectedPlugin().disable();
    }

    @Override
    public void enable() {
        enabled = true;
        getSelectedPlugin().enable();
    }

    @Override
    public void recordEvent(@NonNull String eventName) {
        if (enabled) {
            getSelectedPlugin().recordEvent(eventName);
        }
    }

    @Override
    public void recordEvent(@NonNull final AnalyticsEventBehavior analyticsEvent) {
        if (enabled) {
            getSelectedPlugin().recordEvent(analyticsEvent);
        }
    }

    @Override
    public void registerGlobalProperties(@NonNull AnalyticsProperties properties) {
        getSelectedPlugin().registerGlobalProperties(properties);
    }

    @Override
    public void unregisterGlobalProperties(@NonNull String... propertyNames) {
        getSelectedPlugin().unregisterGlobalProperties(propertyNames);
    }

    @Override
    public void flushEvents() {
        if (enabled) {
            getSelectedPlugin().flushEvents();
        }
    }

    @Override
    public void startSession() {
        getSelectedPlugin().startSession();
    }

    @Override
    public void stopSession() {
        getSelectedPlugin().stopSession();
    }
}
