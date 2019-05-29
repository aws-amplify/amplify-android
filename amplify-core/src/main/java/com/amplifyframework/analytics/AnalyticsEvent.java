package com.amplifyframework.analytics;

import android.support.annotation.NonNull;

import java.util.Map;

public class AnalyticsEvent {

    public AnalyticsEvent(@NonNull String eventName) {
        this.eventName = eventName;
    }

    private String eventName;
    private Map<String, String> attributes;
    private String eventType;
    private Map<String, Double> metrics;
    private Map<String, String> data;

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Map<String, Double> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Double> metrics) {
        this.metrics = metrics;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
