package com.amplifyframework.analytics;

import com.amplifyframework.core.provider.Provider;

public interface AnalyticsProvider extends Provider {
    void record(AnalyticsEvent event);

    void enable();

    void disable();
}
