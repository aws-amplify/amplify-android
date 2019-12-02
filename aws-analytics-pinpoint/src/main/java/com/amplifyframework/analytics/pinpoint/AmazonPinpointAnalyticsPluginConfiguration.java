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

/**
 * Configuration options for Amplify Analytics Pinpoint plugin.
 */
public final class AmazonPinpointAnalyticsPluginConfiguration {

    private static final long DEFAULT_AUTO_FLUSH_INTERVAL = 30000L;
    private static final long DEFAULT_AUTO_SESSION_TRACKING_INTERVAL = 30000L;

    // Pinpoint plugin configuration options
    private final String appId;
    private final boolean trackAppLifecycleEvents;
    private final String region;
    private final long autoFlushEventsInterval;
    private final long autoSessionTrackingInterval;

    AmazonPinpointAnalyticsPluginConfiguration(Builder builder) {
        this.appId = builder.appId;
        this.region = builder.region;
        this.trackAppLifecycleEvents = builder.trackAppLifecycleEvents;
        this.autoFlushEventsInterval = builder.autoFlushEventsInterval;
        this.autoSessionTrackingInterval = builder.autoSessionTrackingInterval;
    }

    /**
     * AppId getter.
     *
     * @return appId
     */
    public String getAppId() {
        return appId;
    }

    /**
     * Accessor for pinpoint region.
     *
     * @return pinpoint region.
     */
    public String getRegion() {
        return region;
    }

    /**
     * Accessor for auto event flush interval.
     *
     * @return auto event flush interval.
     */
    public long getAutoFlushEventsInterval() {
        return autoFlushEventsInterval;
    }

    /**
     * Accessor for auto session tracking interval.
     *
     * @return auto session tracking interval.
     */
    public long getAutoSessionTrackingInterval() {
        return autoSessionTrackingInterval;
    }

    /**
     * Is auto session tracking enabled.
     * @return Is auto session tracking enabled.
     */
    public boolean isTrackAppLifecycleEvents() {
        return trackAppLifecycleEvents;
    }

    /**
     * Return a builder that can be used to construct a new instance of
     * {@link AmazonPinpointAnalyticsPluginConfiguration}.
     * @return An {@link PinpointProperties.Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Used for fluent construction of an immutable {@link AmazonPinpointAnalyticsPluginConfiguration} object.
     */
    public static final class Builder {
        private String appId;
        private boolean trackAppLifecycleEvents = false;
        private String region;
        private long autoFlushEventsInterval = DEFAULT_AUTO_FLUSH_INTERVAL;
        private long autoSessionTrackingInterval = DEFAULT_AUTO_SESSION_TRACKING_INTERVAL;

        Builder withAppId(final String appId) {
            this.appId = appId;
            return this;
        }

        Builder withRegion(final String region) {
            this.region = region;
            return this;
        }

        Builder withAutoFlushEventsInterval(final long autoFlushEventsInterval) {
            this.autoFlushEventsInterval = autoFlushEventsInterval;
            return this;
        }

        Builder withAutoSessionTrackingInterval(final long autoSessionTrackingInterval) {
            this.autoFlushEventsInterval = autoSessionTrackingInterval;
            return this;
        }

        Builder withTrackAppLifecycleEvents(final boolean trackAppLifecycleEvents) {
            this.trackAppLifecycleEvents = trackAppLifecycleEvents;
            return this;
        }

        AmazonPinpointAnalyticsPluginConfiguration build() {
            return new AmazonPinpointAnalyticsPluginConfiguration(this);
        }
    }
}
