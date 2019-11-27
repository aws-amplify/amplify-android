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

public class AmazonPinpointAnalyticsPluginConfiguration {

    // Pinpoint plugin configuration options
    private String appId;
    private boolean trackAppLifecycleEvents;
    private String region;
    private long autoFlushEventsInterval = 30L;
    private long autoSessionTrackingInterval = 30L;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public long getAutoFlushEventsInterval() {
        return autoFlushEventsInterval;
    }

    public void setAutoFlushEventsInterval(long autoFlushEventsInterval) {
        this.autoFlushEventsInterval = autoFlushEventsInterval;
    }

    public long getAutoSessionTrackingInterval() {
        return autoSessionTrackingInterval;
    }

    public void setAutoSessionTrackingInterval(long autoSessionTrackingInterval) {
        this.autoSessionTrackingInterval = autoSessionTrackingInterval;
    }

    public boolean isTrackAppLifecycleEvents() {
        return trackAppLifecycleEvents;
    }

    public void setTrackAppLifecycleEvents(boolean trackAppLifecycleEvents) {
        this.trackAppLifecycleEvents = trackAppLifecycleEvents;
    }
}
