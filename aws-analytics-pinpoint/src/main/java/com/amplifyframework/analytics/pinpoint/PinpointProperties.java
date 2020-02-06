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
            this.properties.put(entry.getKey(), StringProperty.of(entry.getValue()));
        }

        // Populate the metrics
        for (Map.Entry<String, Double> entry: builder.metrics.entrySet()) {
            this.properties.put(entry.getKey(), DoubleProperty.of(entry.getValue()));
        }
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
        private final Map<String, String> attributes;
        private final Map<String, Double> metrics;

        Builder() {
            attributes = new HashMap<>();
            metrics = new HashMap<>();
        }

        public Builder add(String key, String value) {
            attributes.put(key, value);
            return this;
        }

        public Builder add(String key, Double value) {
            metrics.put(key, value);
            return this;
        }

        public PinpointProperties build() {
            return new PinpointProperties(this);
        }
    }
}
