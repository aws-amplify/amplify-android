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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * AnalyticsEvent wraps the information that is part of an event
 * being recorded and sent by an {@link AnalyticsPlugin}.
 */
public final class AnalyticsEvent {

    private final String eventName;
    private final Map<String, String> attributes;
    private final String eventType;
    private final Map<String, Double> metrics;
    private final Map<String, String> data;

    AnalyticsEvent(Builder builder) {
        this.eventName = builder.getEventName();
        this.attributes = builder.getAttributes();
        this.eventType = builder.getEventType();
        this.metrics = builder.getMetrics();
        this.data = builder.getData();
    }

    /**
     * Gets the event's attributes.
     * @return map representing the event attributes
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * Gets the event's type.
     * @return type of the event
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Gets the event's metrics properties.
     * @return metrics map that represents the numeric quantities
     */
    public Map<String, Double> getMetrics() {
        return metrics;
    }

    /**
     * Gets the event's data.
     * @return the event data
     */
    public Map<String, String> getData() {
        return data;
    }

    /**
     * Gets the name of the event.
     * @return The event name
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Return a builder that can be used to construct a new instance of {@link AnalyticsEvent}.
     * @return An {@link AnalyticsEvent.Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Used for fluent construction of an immutable {@link AnalyticsEvent} object.
     */
    public static final class Builder {
        private String eventName;
        private final Map<String, String> attributes;
        private String eventType;
        private final Map<String, Double> metrics;
        private final Map<String, String> data;

        Builder() {
            this.attributes = new HashMap<>();
            this.metrics = new HashMap<>();
            this.data = new HashMap<>();
        }

        /**
         * Configure an event name for the AnalyticsEvent under construction.
         * @param eventName Event name that will be used for final AnalyticsEvent
         * @return current Builder instance for fluent chaining
         */
        public Builder eventName(String eventName) {
            this.eventName = eventName;
            return this;
        }

        /**
         * Set the attributes for the AnalyticsEvent that is being constructed.
         * @param attributes Attributes to populate into AnalyticsEvent
         * @return current Builder instance for fluent chaining
         */
        public Builder attributes(Map<String, String> attributes) {
            this.attributes.clear();
            if (attributes != null) {
                this.attributes.putAll(attributes);
            }
            return this;
        }

        /**
         * Sets the event type for the AnalyticsEvent under construction.
         * @param eventType Event type to put in AnalyticsEvent
         * @return current Builder instance for fluent chaining
         */
        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        /**
         * Configures the metrics that will be put into the constructed AnalyticsEvent.
         * @param metrics Metrics for AnalyticsEvent
         * @return current Builder instance, for fluent chaining
         */
        public Builder metrics(Map<String, Double> metrics) {
            this.metrics.clear();
            if (metrics != null) {
                this.metrics.putAll(metrics);
            }
            return this;
        }

        /**
         * Configures the data that will be used to construct the AnalyticsEvent.
         * @param data to be put into the AnalyticsEvent
         * @return current Builder instance, for fluent chaining
         */
        public Builder data(Map<String, String> data) {
            this.data.clear();
            if (data != null) {
                this.data.putAll(data);
            }
            return this;
        }

        String getEventName() {
            return eventName;
        }

        Map<String, String> getAttributes() {
            return Collections.unmodifiableMap(attributes);
        }

        String getEventType() {
            return eventType;
        }

        Map<String, Double> getMetrics() {
            return Collections.unmodifiableMap(metrics);
        }

        Map<String, String> getData() {
            return Collections.unmodifiableMap(data);
        }

        /**
         * Builds an immutable AnalyticsEvent, given the values provided
         * in previous invocations on the builder instance.
         * @return An immutable AnalyticsEvent instance
         */
        public AnalyticsEvent build() {
            return new AnalyticsEvent(this);
        }
    }
}

