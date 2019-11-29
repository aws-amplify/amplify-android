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

package com.amplifyframework.analytics.pinpoint;

import com.amplifyframework.analytics.Properties;

import java.util.HashMap;
import java.util.Map;

/**
 * Set of properties with String or Double values to represent metrics and values in Amazon pinpoint.
 */
public final class PinpointProperties extends Properties {

    PinpointProperties(Builder builder) {
        // Populate the attributes
        for (Map.Entry<String, String> entry: builder.attributes.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }

        // Populate the metrics
        for (Map.Entry<String, Double> entry: builder.metrics.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds an attribute.
     *
     * @param name attribute name.
     * @param value attribute value.
     */
    public void add(String name, String value) {
        add(name, new StringProperty(value));
    }

    /**
     * Adds a metric.
     *
     * @param name metric name.
     * @param value metric value.
     */
    public void add(String name, Double value) {
        add(name, new DoubleProperty(value));
    }

    /**
     * Return a builder that can be used to construct a new instance of {@link PinpointProperties}.
     * @return An {@link PinpointProperties.Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Used for fluent construction of an immutable {@link PinpointProperties} object.
     */
    public static final class Builder {
        private Map<String, String> attributes;
        private Map<String, Double> metrics;

        Builder() {
            attributes = new HashMap<>();
            metrics = new HashMap<>();
        }

        Builder add(String key, String value) {
            attributes.put(key, value);
            return this;
        }

        Builder add(String key, Double value) {
            metrics.put(key, value);
            return this;
        }

        PinpointProperties build() {
            return new PinpointProperties(this);
        }
    }
}
