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
import androidx.annotation.NonNull;

import com.amplifyframework.analytics.AnalyticsEvent;
import com.amplifyframework.analytics.AnalyticsException;
import com.amplifyframework.analytics.AnalyticsPlugin;
import com.amplifyframework.analytics.AnalyticsProfile;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.logging.Logger;

import org.json.JSONObject;

import java.util.Map;
import java.util.Set;

/**
 * The plugin implementation for Amazon Pinpoint in Analytics category.
 */
public final class AmazonPinpointAnalyticsPlugin extends AnalyticsPlugin<Object> {
    private static final Logger LOG = Amplify.Logging.forNamespace("ampilfy:aws-pinpoint-analytics");

    /**
     * Constructs a new AmazonPinpointAnalyticsPlugin.
     */
    public AmazonPinpointAnalyticsPlugin() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enable() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void identifyUser(@NonNull String userId, @NonNull AnalyticsProfile profile) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disable() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordEvent(@NonNull String eventName) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordEvent(@NonNull AnalyticsEvent analyticsEvent) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerGlobalProperties(Map<String, Object> properties) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterGlobalProperties(Set<String> keys) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flushEvents() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPluginKey() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(@NonNull JSONObject pluginConfiguration, Context context) throws AnalyticsException {
        LOG.info("Amazon Pinpoint Analytics Plugin is initialized.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getEscapeHatch() {
        return null;
    }
}
