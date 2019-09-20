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

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.amplifyframework.analytics.AnalyticsEvent;
import com.amplifyframework.analytics.AnalyticsException;
import com.amplifyframework.analytics.AnalyticsPlugin;
import com.amplifyframework.analytics.AnalyticsPluginConfiguration;
import com.amplifyframework.analytics.AnalyticsProfile;
import com.amplifyframework.core.category.CategoryType;

/**
 * The plugin implementation for Amazon Pinpoint in Analytics CategoryType.
 */
public class AmazonPinpointAnalyticsCategoryPlugin implements AnalyticsPlugin {

    private static final String TAG = AmazonPinpointAnalyticsCategoryPlugin.class.getSimpleName();

    private Context context;

    /**
     * Construct the Amazon Pinpoint plugin, initialize
     * and configure it.
     *
     * @param context Android application context
     */
    public AmazonPinpointAnalyticsCategoryPlugin(@NonNull Context context) {
        this.context = context;

        Log.d(TAG, "Amazon Pinpoint Analytics CategoryType Plugin is initialized.");
    }

    @Override
    public void enable() {

    }

    /**
     * Record the event by storing in the local database.
     *
     * @param eventName name of the event. An AnalyticsEvent is constructed
     *                  based on the name of the event.
     * @throws AnalyticsException when there is an error in
     *                            storing the event in the local database.
     */
    @Override
    public void recordEvent(@NonNull String eventName) throws AnalyticsException {

    }

    /**
     * Record the event by storing in the local database.
     *
     * @param analyticsEvent object that encapsulates the details of an AnalyticsEvent
     * @throws AnalyticsException when there is an error in
     *                            storing the event in the local database.
     */
    @Override
    public void recordEvent(@NonNull AnalyticsEvent analyticsEvent) throws AnalyticsException {

    }

    /**
     * Update the profile of the end-user/device for whom/which you are
     * collecting analytics.
     *
     * @param analyticsProfile the profile of the end-user/device for whom/which you are
     *                         * collecting analytics.
     * @throws AnalyticsException when there is an error updating the
     *                            profile with the registered/chosen {@link AnalyticsPlugin}.
     */
    @Override
    public void updateProfile(@NonNull AnalyticsProfile analyticsProfile) throws AnalyticsException {

    }

    @Override
    public void disable() {

    }

    @Override
    public String getPluginKey() {
        return null;
    }

    /**
     * Configure the Plugin with the configuration passed.
     *
     * @param pluginConfiguration configuration for the plugin
     */
    @Override
    public void configure(@NonNull AnalyticsPluginConfiguration pluginConfiguration) {

    }

    @Override
    public void reset() {

    }

    @Override
    public CategoryType getCategoryType() {
        return CategoryType.ANALYTICS;
    }
}
