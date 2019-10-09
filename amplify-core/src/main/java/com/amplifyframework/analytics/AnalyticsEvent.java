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
        this.eventName = builder.eventName();
        this.attributes = builder.attributes();
        this.eventType = builder.eventType();
        this.metrics = builder.metrics();
        this.data = builder.data();
    }

    /**
     * @return map representing the event attributes.
     */
    public Map<String, String> attributes() {
        return attributes;
    }

    /**
     * @return type of the event.
     */
    public String eventType() {
        return eventType;
    }

    /**
     * @return metrics map that
     *         represents the numeric quantities
     */
    public Map<String, Double> metrics() {
        return metrics;
    }

    /**
     * @return the event payload
     */
    public Map<String, String> data() {
        return data;
    }

    public String eventName() {
        return eventName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String eventName;
        private Map<String, String> attributes;
        private String eventType;
        private Map<String, Double> metrics;
        private Map<String, String> data;

        Builder() {
        }

        public Builder eventName(String eventName) {
            this.eventName = eventName;
            return this;
        }

        public Builder attributes(Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder metrics(Map<String, Double> metrics) {
            this.metrics = metrics;
            return this;
        }

        public Builder data(Map<String, String> data) {
            this.data = data;
            return this;
        }

        String eventName() {
            return eventName;
        }

        Map<String, String> attributes() {
            return attributes;
        }

        String eventType() {
            return eventType;
        }

        Map<String, Double> metrics() {
            return metrics;
        }

        Map<String, String> data() {
            return data;
        }

        public AnalyticsEvent build() {
            return new AnalyticsEvent(this);
        }
    }
}

