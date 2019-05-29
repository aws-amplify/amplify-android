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

import com.amplifyframework.analytics.AnalyticsCategoryPlugin;
import com.amplifyframework.analytics.AnalyticsEvent;
import com.amplifyframework.analytics.AnalyticsException;
import com.amplifyframework.core.plugin.Category;
import com.amplifyframework.core.plugin.Plugin;

import org.json.JSONObject;

/**
 * The plugin implementation for Amazon Pinpoint in Analytics Category.
 */
public class AmazonPinpointAnalyticsCategoryPlugin implements AnalyticsCategoryPlugin {

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

        Log.d(TAG, "Amazon Pinpoint Analytics Category Plugin is initialized.");
    }

    @Override
    public void record(@NonNull String eventName) throws AnalyticsException {

    }

    @Override
    public void record(@NonNull String eventName, @NonNull String pluginKey) throws AnalyticsException {

    }

    @Override
    public void record(@NonNull AnalyticsEvent event) throws AnalyticsException {

    }

    @Override
    public void record(@NonNull AnalyticsEvent analyticsEvent, @NonNull String pluginKey) throws AnalyticsException {

    }

    @Override
    public void enable() {

    }

    @Override
    public void disable() {

    }

    @Override
    public String getPluginKey() {
        return null;
    }

    @Override
    public void configure(@NonNull JSONObject jsonObject) {

    }

    @Override
    public void configure(@NonNull JSONObject jsonObject, @NonNull String key) {

    }

    @Override
    public void reset() {

    }

    @Override
    public Plugin initWithConfiguration(@NonNull JSONObject jsonObject) {
        return null;
    }

    @Override
    public Plugin initWithConfiguration(@NonNull JSONObject jsonObject, @NonNull String key) {
        return null;
    }

    @Override
    public Category getCategory() {
        return null;
    }
}
