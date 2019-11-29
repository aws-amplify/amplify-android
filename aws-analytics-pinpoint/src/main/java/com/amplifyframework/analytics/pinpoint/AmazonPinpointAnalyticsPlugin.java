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
import com.amplifyframework.analytics.AnalyticsException;
import com.amplifyframework.analytics.AnalyticsPlugin;
import com.amplifyframework.analytics.AnalyticsProfile;
import com.amplifyframework.analytics.GeneralAnalyticsEvent;
import com.amplifyframework.analytics.Properties;
import com.amplifyframework.analytics.Property;
import com.amplifyframework.core.plugin.PluginException;

import com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsClient;
import com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsEvent;
import com.amazonaws.regions.Regions;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.Set;

/**
 * The plugin implementation for Amazon Pinpoint in Analytics category.
 */
public final class AmazonPinpointAnalyticsPlugin extends AnalyticsPlugin<Object> {

    private static final String TAG = AmazonPinpointAnalyticsPlugin.class.getSimpleName();
    private final AutoEventSubmitter autoEventSubmitter;
    private AmazonPinpointAnalyticsPluginConfiguration pinpointAnalyticsPluginConfiguration;
    private AnalyticsClient analyticsClient;

    /**
     * Constructs a new AmazonPinpointAnalyticsPlugin.
     */
    public AmazonPinpointAnalyticsPlugin() {
        Log.d(TAG, "Amazon Pinpoint Analytics Plugin is initialized.");
        autoEventSubmitter = new AutoEventSubmitter();
    }

    /**
     * Accessor method for pinpoint analytics client.
     * @return returns pinpoint analytics client.
     */
    protected AnalyticsClient getAnalyticsClient() {
        return analyticsClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enable() {
        autoEventSubmitter.start(analyticsClient,
                pinpointAnalyticsPluginConfiguration.getAutoFlushEventsInterval());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void identifyUser(@NonNull String userId, @NonNull AnalyticsProfile profile) {
        throw new UnsupportedOperationException("This operation has not been implemented yet.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disable() {
        autoEventSubmitter.stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordEvent(@NonNull String eventName)
            throws AnalyticsException, ConfigurationException {

        final AnalyticsEvent pinpointEvent =
                analyticsClient.createEvent(eventName);
        analyticsClient.recordEvent(pinpointEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordEvent(@NonNull GeneralAnalyticsEvent analyticsEvent)
            throws AnalyticsException, ConfigurationException {

        final AnalyticsEvent pinpointEvent =
                analyticsClient.createEvent(analyticsEvent.getEventType());

        for (Map.Entry<String, Property<?>> entry: analyticsEvent.getProperties().get().entrySet()) {
            if (entry.getValue() instanceof StringProperty) {
                pinpointEvent.addAttribute(entry.getKey(), ((StringProperty) entry.getValue()).getValue());
            } else if (entry.getValue() instanceof DoubleProperty) {
                pinpointEvent.addMetric(entry.getKey(), ((DoubleProperty) entry.getValue()).getValue());
            } else {
                throw new RuntimeException("Invalid property type detected.");
            }
        }
        analyticsClient.recordEvent(pinpointEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerGlobalProperties(Properties properties) {
        throw new UnsupportedOperationException("This operation has not been implemented yet.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterGlobalProperties(Set<String> keys) {
        throw new UnsupportedOperationException("This operation has not been implemented yet.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flushEvents() {
        analyticsClient.submitEvents();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPluginKey() {
        return "AmazonPinpointAnalyticsPlugin";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(@NonNull JSONObject pluginConfiguration, Context context) throws PluginException {

        pinpointAnalyticsPluginConfiguration = new AmazonPinpointAnalyticsPluginConfiguration();
        // Read all the data from the configuration object to be used for record event
        try {
            pinpointAnalyticsPluginConfiguration
                    .setAppId(pluginConfiguration
                            .getString(PinpointConfigurationKey.APP_ID.getConfigurationKey()));
            pinpointAnalyticsPluginConfiguration
                    .setRegion(pluginConfiguration
                            .getString(PinpointConfigurationKey.REGION.getConfigurationKey()));

            if (pluginConfiguration.has(PinpointConfigurationKey.AUTO_FLUSH_INTERVAL.getConfigurationKey())) {
                pinpointAnalyticsPluginConfiguration
                        .setAutoFlushEventsInterval(pluginConfiguration
                                .getLong(PinpointConfigurationKey.AUTO_FLUSH_INTERVAL.getConfigurationKey()));
            }

            if (pluginConfiguration
                    .has(PinpointConfigurationKey.AUTO_SESSION_TRACKING_INTERVAL.getConfigurationKey())) {
                pinpointAnalyticsPluginConfiguration
                        .setAutoSessionTrackingInterval(pluginConfiguration
                                .getLong(PinpointConfigurationKey.AUTO_SESSION_TRACKING_INTERVAL
                                        .getConfigurationKey()));
            }

            if (pluginConfiguration.has(PinpointConfigurationKey.TRACK_APP_LIFECYCLE_EVENTS
                    .getConfigurationKey())) {
                pinpointAnalyticsPluginConfiguration
                        .setTrackAppLifecycleEvents(pluginConfiguration
                                .getBoolean(PinpointConfigurationKey.TRACK_APP_LIFECYCLE_EVENTS
                                        .getConfigurationKey()));
            }
        } catch (JSONException exception) {
            throw new RuntimeException("Unable to read appId or region from the amplify configuration json");
        }

        this.analyticsClient = PinpointClientFactory.create(context, pinpointAnalyticsPluginConfiguration);

        // Initiate the logic to automatically submit events periodically
        autoEventSubmitter.start(analyticsClient,
                pinpointAnalyticsPluginConfiguration.getAutoFlushEventsInterval());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getEscapeHatch() {
        return null;
    }

    /**
     * Pinpoint Analytics configuration in amplifyconfiguration.json contains following values.
     */
    public enum PinpointConfigurationKey {
        /**
         * The Pinpoint Application Id.
         */
        APP_ID("appId"),

        /**
         * the AWS {@link Regions} for the Pinpoint service.
         */
        REGION("region"),

        /**
         * Time interval after which the events are automatically submitted to pinpoint.
         */
        AUTO_FLUSH_INTERVAL("autoFlushEventsInterval"),

        /**
         * Time interval after which to track lifecycle events.
         */
        AUTO_SESSION_TRACKING_INTERVAL("autoSessionTrackingInterval"),

        /**
         * Whether to track app lifecycle events automatically.
         */
        TRACK_APP_LIFECYCLE_EVENTS("trackAppLifecycleEvents");

        /**
         * The key this property is listed under in the config JSON.
         */
        private final String configurationKey;

        /**
         * Construct the enum with the config key.
         * @param configurationKey The key this property is listed under in the config JSON.
         */
        PinpointConfigurationKey(final String configurationKey) {
            this.configurationKey = configurationKey;
        }

        /**
         * Returns the key this property is listed under in the config JSON.
         * @return The key as a string
         */
        public String getConfigurationKey() {
            return configurationKey;
        }
    }
}
