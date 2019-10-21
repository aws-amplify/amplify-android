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
import android.util.Log;
import androidx.annotation.NonNull;

import com.amplifyframework.ConfigurationException;
import com.amplifyframework.analytics.AnalyticsEvent;
import com.amplifyframework.analytics.AnalyticsException;
import com.amplifyframework.analytics.AnalyticsPlugin;
import com.amplifyframework.analytics.AnalyticsProfile;
import com.amplifyframework.core.plugin.PluginException;

import org.json.JSONObject;

/**
 * The plugin implementation for Amazon Pinpoint in Analytics category.
 */
public final class AmazonPinpointAnalyticsPlugin extends AnalyticsPlugin<Object> {

    private static final String TAG = AmazonPinpointAnalyticsPlugin.class.getSimpleName();

    /**
     * Constructs a new AmazonPinpointAnalyticsPlugin.
     * @param context An Android Context
     */
    public AmazonPinpointAnalyticsPlugin(@NonNull Context context) {
        Log.d(TAG, "Amazon Pinpoint Analytics Plugin is initialized.");
    }

    @Override
    public void enable() {

    }

    @Override
    public void disable() {

    }

    @Override
    public void recordEvent(@NonNull String eventName)
            throws AnalyticsException, ConfigurationException {

    }

    @Override
    public void recordEvent(@NonNull AnalyticsEvent analyticsEvent)
            throws AnalyticsException, ConfigurationException {

    }

    @Override
    public void updateProfile(@NonNull AnalyticsProfile analyticsProfile)
            throws AnalyticsException, ConfigurationException {

    }

    @Override
    public String getPluginKey() {
        return null;
    }

    @Override
    public void configure(@NonNull JSONObject pluginConfiguration, Context context) throws PluginException {

    }

    @Override
    public Object getEscapeHatch() {
        return null;
    }
}
