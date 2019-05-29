package com.amplifyframework.analytics;

import com.amplifyframework.core.plugin.Plugin;

public interface AnalyticsPlugin extends Plugin {
    void record(AnalyticsEvent event);

    void enable();

    void disable();
}
