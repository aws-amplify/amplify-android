package com.amplifyframework.analytics.pinpoint;

import com.amplifyframework.analytics.Properties;

import java.util.HashMap;
import java.util.Map;

public class PinpointProperties extends Properties {

    PinpointProperties(Builder builder) {
        // Populate the attributes
        for (Map.Entry<String, String> entry: builder.attributes.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }

        // Populate the metrics
        for(Map.Entry<String, Double> entry: builder.metrics.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }

    public void add(String name, String value) {
        add(name, new StringProperty(value));
    }

    public void add(String name, Double value) {
        add(name, new DoubleProperty(value));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        Map<String, String> attributes;
        Map<String, Double> metrics;

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