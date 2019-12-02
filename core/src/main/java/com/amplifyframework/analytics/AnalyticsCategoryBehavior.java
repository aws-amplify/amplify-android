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

import java.util.Map;
import java.util.Set;

/**
 * Defines the client behavior (client API) consumed
 * by the app for collection and sending of Analytics
 * events.
 */
public interface AnalyticsCategoryBehavior {

    /**
     * Allows you to tie a user to their actions and record traits about them. It includes
     * an unique User ID and any optional traits you know about them like their email, name, etc.
     *
     * @param userId The unique identifier for the user
     * @param profile User specific data (e.g. plan, accountType, email, age, location, etc)
     */
    void identifyUser(@NonNull String userId, @NonNull AnalyticsProfile profile);

    /**
     * Disable collection and sending of Analytics Events.
     */
    void disable();

    /**
     * Enable collection and sending of Analytics Events.
     */
    void enable();

    /**
     * Record the event by storing in the local database.
     * @param eventName name of the event. An AnalyticsEvent is constructed
     *                  based on the name of the event.
     * @throws AnalyticsException when there is an error in
     *                            storing the event in the local database.
     * @throws AnalyticsException If the category is badly/not yet configured
     */
    void recordEvent(@NonNull String eventName) throws AnalyticsException;

    /**
     * Record the event by storing in the local database.
     * @param analyticsEvent object that encapsulates the details of an AnalyticsEvent
     * @throws AnalyticsException when there is an error in
     *                            storing the event in the local database.
     * @throws AnalyticsException If the category is badly/not yet configured
     */
    void recordEvent(@NonNull AnalyticsEvent analyticsEvent) throws AnalyticsException;

    /**
     * Register properties that will be recorded by all the subsequent calls to {@link #recordEvent(AnalyticsEvent)}.
     * Properties registered here can be overridden by the ones with the same
     * name when calling `recordEvent`.
     *
     * Examples of global properties would be `selectedPlan`, `campaignSource`
     *
     * @param properties Map of global properties
     */
    void registerGlobalProperties(Map<String, Object> properties);

    /**
     * Registered global properties can be unregistered though this method.
     *
     * **Note:** In case no keys are provided, *all* registered global properties will
     * be unregistered.
     *
     * @param keys a collection of property names to unregister
     */
    void unregisterGlobalProperties(Set<String> keys);

    /**
     * Attempts to submit the locally stored events to the underlying service.
     *
     * **Note:** Implementations do not guarantee that all the stored data will be sent in one
     * request. Some analytics services have hard limits on how much data you can send at once.
     * What is the behavior in this case? Naming ??
     */
    void flushEvents();
}
