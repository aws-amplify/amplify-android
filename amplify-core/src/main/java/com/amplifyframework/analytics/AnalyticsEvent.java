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

import java.util.Map;

import androidx.annotation.NonNull;

/**
 * AnalyticsEvent wraps the information that is part of an event
 * being recorded and sent by an {@link AnalyticsPlugin}.
 */
public class AnalyticsEvent {

    private String eventName;
    private Map<String, String> attributes;
    private String eventType;
    private Map<String, Double> metrics;
    private Map<String, String> data;

    /**
     * Construct an AnalyticEvent based on the eventName.
     *
     * @param eventName name of the event.
     */
    public AnalyticsEvent(@NonNull String eventName) {
        this.eventName = eventName;
    }

    /**
     * @return map representing the event attributes.
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * Set the event attributes.
     *
     * @param attributes map of key-value pairs representing
     *                   the attributes
     */
    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    /**
     * @return type of the event.
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * @param eventType type of the event
     */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    /**
     * @return metrics map that
     *         represents the numeric quantities
     */
    public Map<String, Double> getMetrics() {
        return metrics;
    }

    /**
     * @param metrics map that
     *                represents the numeric quantities
     */
    public void setMetrics(Map<String, Double> metrics) {
        this.metrics = metrics;
    }

    /**
     * @return the event payload
     */
    public Map<String, String> getData() {
        return data;
    }

    /**
     * @param data the event payload
     */
    public void setData(Map<String, String> data) {
        this.data = data;
    }
}

