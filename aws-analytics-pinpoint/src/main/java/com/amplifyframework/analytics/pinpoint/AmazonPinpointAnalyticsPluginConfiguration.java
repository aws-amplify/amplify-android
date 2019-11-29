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

    private static final long DEFAULT_AUTO_FLUSH_INTERVAL = 30L;
    private static final long DEFAULT_AUTO_SESSION_TRACKING_INTERVAL = 30L;

    // Pinpoint plugin configuration options
    private String appId;
    private boolean trackAppLifecycleEvents = false;
    private String region;
    private long autoFlushEventsInterval = DEFAULT_AUTO_FLUSH_INTERVAL;
    private long autoSessionTrackingInterval = DEFAULT_AUTO_SESSION_TRACKING_INTERVAL;

    /**
     * AppId getter.
     *
     * @return appId
     */
    public String getAppId() {
        return appId;
    }

    /**
     *  AppId setter.
     *
     * @param appId pinpoint app id.
     */
    public void setAppId(String appId) {
        this.appId = appId;
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
     * Mutator for pinpoint region.
     *
     * @param region pinpoint region.
     */
    public void setRegion(String region) {
        this.region = region;
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
     * Mutator for auto event flush interval.
     *
     * @param autoFlushEventsInterval auto event flush interval.
     */
    public void setAutoFlushEventsInterval(long autoFlushEventsInterval) {
        this.autoFlushEventsInterval = autoFlushEventsInterval;
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
     * Mutator for auto session tracking interval.
     *
     * @param autoSessionTrackingInterval auto session tracking interval.
     */
    public void setAutoSessionTrackingInterval(long autoSessionTrackingInterval) {
        this.autoSessionTrackingInterval = autoSessionTrackingInterval;
    }

    /**
     * Is auto session tracking enabled.
     * @return Is auto session tracking enabled.
     */
    public boolean isTrackAppLifecycleEvents() {
        return trackAppLifecycleEvents;
    }

    /**
     * Set auto session tracking flag.
     * @param trackAppLifecycleEvents auto session tracking flag.
     */
    public void setTrackAppLifecycleEvents(boolean trackAppLifecycleEvents) {
        this.trackAppLifecycleEvents = trackAppLifecycleEvents;
    }
}
