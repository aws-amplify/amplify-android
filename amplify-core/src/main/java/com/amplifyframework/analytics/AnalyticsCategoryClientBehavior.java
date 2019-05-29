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
 * Defines the client behavior (client API) consumed
 * by the app for collection and sending of Analytics
 * events.
 */
public interface AnalyticsCategoryClientBehavior {
    /**
     * Record the event by storing in the local database.
     *
     * @param eventName name of the event. An AnalyticsEvent is constructed
     *                  based on the name of the event.
     * @throws AnalyticsException when there is an error in
     *                            storing the event in the local database.
     */
    void record(@NonNull String eventName) throws AnalyticsException;

    /**
     * Record the event by storing in the local database.
     *
     * @param eventName name of the event. An AnalyticsEvent is constructed
     *                  based on the name of the event.
     * @param pluginKey the identifier of the Analytics plugin implementation
     * @throws AnalyticsException when there is an error in
     *                            storing the event in the local database.
     */
    void record(@NonNull String eventName,
                @NonNull final String pluginKey) throws AnalyticsException;

    /**
     * Record the event by storing in the local database.
     *
     * @param analyticsEvent object that encapsulates the details of an AnalyticsEvent
     * @throws AnalyticsException when there is an error in
     *                            storing the event in the local database.
     */
    void record(@NonNull AnalyticsEvent analyticsEvent) throws AnalyticsException;

    /**
     * Record the event by storing in the local database.
     *
     * @param analyticsEvent object that encapsulates the details of an AnalyticsEvent
     * @param pluginKey the identifier of the Analytics plugin implementation
     * @throws AnalyticsException when there is an error in
     *                            storing the event in the local database.
     */
    void record(@NonNull final AnalyticsEvent analyticsEvent,
                @NonNull final String pluginKey) throws AnalyticsException;

    /**
     * Enable collection and sending of Analytics Events.
     */
    void enable();

    /**
     * Disable collection and sending of Analytics Events.
     */
    void disable();
}
