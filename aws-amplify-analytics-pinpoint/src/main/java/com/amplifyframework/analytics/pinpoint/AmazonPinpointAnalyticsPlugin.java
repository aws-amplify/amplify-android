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

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.pinpoint.PinpointConfiguration;
import com.amazonaws.mobileconnectors.pinpoint.PinpointManager;
import com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsClient;
import com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsEvent;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.pinpoint.model.ChannelType;
import com.amplifyframework.ConfigurationException;
import com.amplifyframework.analytics.AnalyticsException;
import com.amplifyframework.analytics.AnalyticsPlugin;
import com.amplifyframework.analytics.AnalyticsProfile;
import com.amplifyframework.analytics.GeneralAnalyticsEvent;
import com.amplifyframework.core.plugin.PluginException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * The plugin implementation for Amazon Pinpoint in Analytics category.
 */
public final class AmazonPinpointAnalyticsPlugin extends AnalyticsPlugin<Object> {

    private static final String TAG = AmazonPinpointAnalyticsPlugin.class.getSimpleName();
    private PinpointManager pinpointManager;
    private AmazonPinpointAnalyticsPluginConfiguration pinpointAnalyticsPluginConfiguration;
    private final AutoEventSubmitter autoEventSubmitter;
    protected AnalyticsClient analyticsClient;

    /**
     * Constructs a new AmazonPinpointAnalyticsPlugin.
     */
    public AmazonPinpointAnalyticsPlugin() {
        Log.d(TAG, "Amazon Pinpoint Analytics Plugin is initialized.");
        autoEventSubmitter = new AutoEventSubmitter();
    }

    private PinpointManager getPinpointManager(Context context) {
        if (this.pinpointManager == null) {
            PinpointManager pinpointManager;
            final AWSConfiguration awsConfiguration = new AWSConfiguration(context);

            CountDownLatch mobileClientlatch = new CountDownLatch(1);
            // Initialize the AWSMobileClient
            AWSMobileClient.getInstance().initialize(context, awsConfiguration,
                    new Callback<UserStateDetails>() {
                        @Override
                        public void onResult(UserStateDetails userStateDetails) {
                            Log.i(TAG, "Mobile client initialized");
                            mobileClientlatch.countDown();
                        }

                        @Override
                        public void onError(Exception exception) {
                            Log.e(TAG, "Error initializing AWS Mobile Client", exception);
                            mobileClientlatch.countDown();
                        }
                    });

            try {
                mobileClientlatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Construct configuration using information from the configure method
            PinpointConfiguration pinpointConfiguration = new PinpointConfiguration(
                    context,
                    pinpointAnalyticsPluginConfiguration.getAppId(),
                    Regions.fromName(pinpointAnalyticsPluginConfiguration.getRegion()),
                    ChannelType.GCM,
                    AWSMobileClient.getInstance()
            );

            pinpointManager = new PinpointManager(pinpointConfiguration);
            return pinpointManager;
        }
        return this.pinpointManager;
    }

    @Override
    public void enable() {
        autoEventSubmitter.start(pinpointManager.getAnalyticsClient(),
                pinpointAnalyticsPluginConfiguration.getAutoFlushEventsInterval());
    }

    @Override
    public void identifyUser(@NonNull String id, @NonNull AnalyticsProfile profile) {

    }

    @Override
    public void disable() {
        autoEventSubmitter.stop();
    }

    @Override
    public void recordEvent(@NonNull String eventName)
            throws AnalyticsException, ConfigurationException {

        final AnalyticsEvent pinpointEvent =
                pinpointManager.getAnalyticsClient().createEvent(eventName);
        pinpointManager.getAnalyticsClient().recordEvent(pinpointEvent);
    }

    @Override
    public void recordEvent(@NonNull GeneralAnalyticsEvent analyticsEvent)
            throws AnalyticsException, ConfigurationException {

        // TODO Distinguish between metrics and attributes
        final AnalyticsEvent pinpointEvent =
                pinpointManager.getAnalyticsClient().createEvent(analyticsEvent.getEventType());

        for(Map.Entry<String, String> property : analyticsEvent.getProperties().entrySet()) {
            pinpointEvent.addAttribute(property.getKey(), property.getValue());
        }
        pinpointManager.getAnalyticsClient().recordEvent(pinpointEvent);
    }

    @Override
    public void registerGlobalProperties(Map<String, Object> properties) {

    }

    @Override
    public void unregisterGlobalProperties(Collection<String> keys) {

    }

    @Override
    public void flushEvents() {
        pinpointManager.getAnalyticsClient().submitEvents();
    }

    @Override
    public String getPluginKey() {
        return "AmazonPinpointAnalyticsPlugin";
    }

    @Override
    public void configure(@NonNull JSONObject pluginConfiguration, Context context) throws PluginException {

        pinpointAnalyticsPluginConfiguration = new AmazonPinpointAnalyticsPluginConfiguration();
        // Read all the data from the configuration object to be used for record event
        try {
            pinpointAnalyticsPluginConfiguration
                    .setAppId(pluginConfiguration
                            .getString(PinpointConfigurationKeys.APP_ID.getConfigurationKey()));
            pinpointAnalyticsPluginConfiguration
                    .setRegion(pluginConfiguration
                            .getString(PinpointConfigurationKeys.REGION.getConfigurationKey()));

            if (pluginConfiguration.has(PinpointConfigurationKeys.AUTO_FLUSH_INTERVAL.getConfigurationKey())) {
                pinpointAnalyticsPluginConfiguration
                        .setAutoFlushEventsInterval(pluginConfiguration
                                .getLong(PinpointConfigurationKeys.AUTO_FLUSH_INTERVAL.getConfigurationKey()));
            }

            if (pluginConfiguration.has(PinpointConfigurationKeys.AUTO_SESSION_TRACKING_INTERVAL.getConfigurationKey())) {
                pinpointAnalyticsPluginConfiguration
                        .setAutoSessionTrackingInterval(pluginConfiguration
                                .getLong(PinpointConfigurationKeys.AUTO_SESSION_TRACKING_INTERVAL.getConfigurationKey()));
            }

            if (pluginConfiguration.has(PinpointConfigurationKeys.TRACK_APP_LIFECYCLE_EVENTS.getConfigurationKey())) {
                pinpointAnalyticsPluginConfiguration
                        .setTrackAppLifecycleEvents(pluginConfiguration
                                .getBoolean(PinpointConfigurationKeys.TRACK_APP_LIFECYCLE_EVENTS.getConfigurationKey()));
            }
        } catch (JSONException e) {
            throw new RuntimeException("Unable to read appId or region from the amplify configuration json");
        }
        pinpointManager = getPinpointManager(context);
        this.analyticsClient = pinpointManager.getAnalyticsClient();

        // Initiate the logic to automatically submit events periodically
        autoEventSubmitter.start(analyticsClient,
                pinpointAnalyticsPluginConfiguration.getAutoFlushEventsInterval());
    }

    @Override
    public Object getEscapeHatch() {
        return null;
    }

    /**
     * Pinpoint Analytics configuration in amplifyconfiguration.json contains following values
     */
    public enum PinpointConfigurationKeys {
        /**
         * The Pinpoint Application Id.
         */
        APP_ID("appId"),

        /**
         * the AWS {@link Regions} for the Pinpoint service.
         */
        REGION("region"),

        /**
         * Time interval after which the events are automatically submitted to pinpoint
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
        PinpointConfigurationKeys(final String configurationKey) {
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
