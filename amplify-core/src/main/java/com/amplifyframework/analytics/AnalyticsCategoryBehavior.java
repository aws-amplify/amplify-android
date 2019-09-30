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

import android.support.annotation.NonNull;

/**
 * API contract for `Analytics` category that plugins should implement to provide
 * the Analytics functionality.
 */
public interface AnalyticsCategoryBehavior {
    /**
     * Disable analytics data collection. Useful to implement flows that require users
     * to *opt-in*.
     */
    void disable();

    /**
     * Enable analytics data collection. Useful to implement flows that require users
     * to *opt-in*.
     */
    void enable();

    /**
     * Record the actions your users perform. Every action triggers what we call an “event”,
     * which can also have associated properties.
     *
     * @param analyticsEvent object that encapsulates the details of an AnalyticsEvent
     */
    void recordEvent(@NonNull AnalyticsEvent analyticsEvent);


    /**
     * Convenience method to record events. It constructs an instance of AnalyticsEvent and calls
     * `recordEvent(@NonNull AnalyticsEvent analyticsEvent)` method.
     *
     * @param eventName name of the event. An AnalyticsEvent is constructed
     *                  based on the name of the event.
     */
    void recordEvent(@NonNull String eventName);

    /**
     * Allows you to tie a user to their actions and record traits about them. It includes
     * an unique User ID and any optional traits you know about them like their email, name, etc.
     *
     * @param id The unique identifier for the user
     * @param analyticsUserProfile User specific data (e.g. plan, accountType, email, age, location, etc)
     */
    void identifyUser(@NonNull String id, @NonNull AnalyticsUserProfile analyticsUserProfile);
}