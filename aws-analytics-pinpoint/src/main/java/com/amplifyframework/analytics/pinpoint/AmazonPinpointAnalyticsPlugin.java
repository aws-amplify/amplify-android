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

import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import com.amplifyframework.analytics.AnalyticsChannelEventName;
import com.amplifyframework.analytics.AnalyticsEvent;
import com.amplifyframework.analytics.AnalyticsException;
import com.amplifyframework.analytics.AnalyticsPlugin;
import com.amplifyframework.analytics.Properties;
import com.amplifyframework.analytics.Property;
import com.amplifyframework.analytics.UserProfile;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;

import com.amazonaws.mobileconnectors.pinpoint.PinpointManager;
import com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsClient;
import com.amazonaws.mobileconnectors.pinpoint.targeting.TargetingClient;
import com.amazonaws.mobileconnectors.pinpoint.targeting.endpointProfile.EndpointProfile;
import com.amazonaws.mobileconnectors.pinpoint.targeting.endpointProfile.EndpointProfileLocation;
import com.amazonaws.mobileconnectors.pinpoint.targeting.endpointProfile.EndpointProfileUser;
import com.amazonaws.regions.Regions;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The plugin implementation for Amazon Pinpoint in Analytics category.
 */
public final class AmazonPinpointAnalyticsPlugin extends AnalyticsPlugin<Object> {
    @SuppressWarnings("checkstyle:WhitespaceAround")
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
        USER_NAME,
        USER_EMAIL,
        USER_PLAN
    })
    private @interface PinpointUserProfileAttribute {}
    private static final String USER_NAME = "name";
    private static final String USER_EMAIL = "email";
    private static final String USER_PLAN = "plan";

    private final Application application;
    private AutoEventSubmitter autoEventSubmitter;
    private AmazonPinpointAnalyticsPluginConfiguration pinpointAnalyticsPluginConfiguration;
    private AnalyticsClient analyticsClient;
    private AutoSessionTracker autoSessionTracker;
    private TargetingClient targetingClient;

    /**
     * Constructs a new AmazonPinpointAnalyticsPlugin.
     *
     * @param application Global application context
     */
    public AmazonPinpointAnalyticsPlugin(final Application application) {
        this.application = application;
    }

    /**
     * Accessor method for pinpoint analytics client.
     * @return returns pinpoint analytics client.
     */
    AnalyticsClient getAnalyticsClient() {
        return analyticsClient;
    }

    /**
     * Accessor method for pinpoint targeting client.
     * @return returns pinpoint targeting client.
     */
    TargetingClient getTargetingClient() {
        return targetingClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enable() {
        autoEventSubmitter.start();
        // Start auto session tracking
        autoSessionTracker.startSessionTracking(application);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void identifyUser(@NonNull String userId, @Nullable UserProfile userProfile) {
        Objects.requireNonNull(userId);
        EndpointProfile endpointProfile = targetingClient.currentEndpoint();
        // Assign userId to the endpoint.
        EndpointProfileUser user = new EndpointProfileUser();
        user.setUserId(userId);
        endpointProfile.setUser(user);
        // Add user-specific data to the endpoint
        if (userProfile != null) {
            addUserProfileToEndpoint(endpointProfile, userProfile);
        }
        // update endpoint
        targetingClient.updateEndpointProfile();
    }

    /**
     * Add user specific data from {@link UserProfile} to the endpoint profile.
     * @param endpointProfile endpoint profile.
     * @param userProfile user specific data to be added to the endpoint.
     */
    private void addUserProfileToEndpoint(@NonNull EndpointProfile endpointProfile,
                                          @NonNull UserProfile userProfile) {
        addAttribute(endpointProfile, USER_NAME, userProfile.getName());
        addAttribute(endpointProfile, USER_EMAIL, userProfile.getEmail());
        addAttribute(endpointProfile, USER_PLAN, userProfile.getPlan());

        // Add location
        if (userProfile.getLocation() != null) {
            addLocation(endpointProfile.getLocation(), userProfile.getLocation());
        }

        // Add custom properties
        if (userProfile.getCustomProperties() != null) {
            addCustomProperties(endpointProfile, userProfile.getCustomProperties());
        }
    }

    /**
     * Add user profile attribute to the endpoint profile. If an attribute value is null. It is
     * removed from the set of endpoint attributes.
     * @param endpointProfile current endpoint profile.
     * @param pinpointUserProfileAttribute String def enumerating the allowed user attributes.
     * @param attributeValue user attribute value.
     */
    private void addAttribute(@NonNull EndpointProfile endpointProfile,
                              @PinpointUserProfileAttribute String pinpointUserProfileAttribute,
                              @Nullable final String attributeValue) {
        if (attributeValue != null) {
            endpointProfile.addAttribute(pinpointUserProfileAttribute,
                    Collections.singletonList(attributeValue));
        } else {
            // If attribute value is null, corresponding attribute is removed/unset from the endpoint profile.
            endpointProfile.addAttribute(pinpointUserProfileAttribute, null);
        }
    }

    /**
     * Add custom user properties to the endpoint profile.
     * @param endpointProfile endpoint profile.
     * @param customProperties custom user properties to be added to the endpoint profile.
     */
    private void addCustomProperties(@NonNull EndpointProfile endpointProfile,
                                     @NonNull Properties customProperties) {
        for (Map.Entry<String, Property<?>> entry : customProperties.get().entrySet()) {
            if (entry.getValue() instanceof StringProperty) {
                endpointProfile.addAttribute(entry.getKey(),
                        Collections.singletonList(((StringProperty) entry.getValue()).getValue()));
            } else if (entry.getValue() instanceof DoubleProperty) {
                endpointProfile.addMetric(entry.getKey(),
                        ((DoubleProperty) entry.getValue()).getValue());
            } else {
                Amplify.Hub.publish(HubChannel.ANALYTICS,
                        HubEvent.create(AnalyticsChannelEventName.INVALID_PROPERTY_TYPE,
                        new AnalyticsException("Invalid Property type detected.",
                                "AmazonPinpointAnalyticsPlugin supports only StringProperty or " +
                                        "DoubleProperty.")));
            }
        }
    }

    /**
     * Add location details to the endpoint profile location.
     * @param endpointProfileLocation endpoint location.
     * @param location location details.
     */
    private void addLocation(@NonNull EndpointProfileLocation endpointProfileLocation,
                             @NonNull UserProfile.Location location) {
        endpointProfileLocation.setLatitude(location.getLatitude());
        endpointProfileLocation.setLongitude(location.getLongitude());
        endpointProfileLocation.setPostalCode(location.getPostalCode());
        endpointProfileLocation.setCity(location.getCity());
        endpointProfileLocation.setRegion(location.getRegion());
        endpointProfileLocation.setCountry(location.getCountry());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disable() {
        autoEventSubmitter.stop();
        // Stop auto session tracking
        autoSessionTracker.stopSessionTracking(application);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordEvent(@NonNull String eventName) {

        final com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsEvent pinpointEvent =
                analyticsClient.createEvent(eventName);
        analyticsClient.recordEvent(pinpointEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordEvent(@NonNull AnalyticsEvent analyticsEvent)
            throws AnalyticsException {

        final com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsEvent pinpointEvent =
                analyticsClient.createEvent(analyticsEvent.getName());

        if (analyticsEvent.getProperties() != null) {
            for (Map.Entry<String, Property<?>> entry : analyticsEvent.getProperties().get().entrySet()) {
                if (entry.getValue() instanceof StringProperty) {
                    pinpointEvent.addAttribute(entry.getKey(), ((StringProperty) entry.getValue()).getValue());
                } else if (entry.getValue() instanceof DoubleProperty) {
                    pinpointEvent.addMetric(entry.getKey(), ((DoubleProperty) entry.getValue()).getValue());
                } else {
                    throw new AnalyticsException("Invalid property type detected.",
                            "AmazonPinpointAnalyticsPlugin supports only StringProperty or DoubleProperty. " +
                                    "Refer to the documentation for details.");
                }
            }
        }
        analyticsClient.recordEvent(pinpointEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerGlobalProperties(@NonNull Properties properties) throws AnalyticsException {
        for (Map.Entry<String, Property<?>> entry : properties.get().entrySet()) {
            if (entry.getValue() instanceof StringProperty) {
                analyticsClient.addGlobalAttribute(entry.getKey(), ((StringProperty) entry.getValue()).getValue());
            } else if (entry.getValue() instanceof DoubleProperty) {
                analyticsClient.addGlobalMetric(entry.getKey(), ((DoubleProperty) entry.getValue()).getValue());
            } else {
                Amplify.Hub.publish(HubChannel.ANALYTICS,
                        HubEvent.create(AnalyticsChannelEventName.INVALID_PROPERTY_TYPE,
                                new AnalyticsException("Invalid Property type detected.",
                                        "AmazonPinpointAnalyticsPlugin supports only StringProperty or " +
                                                "DoubleProperty.")));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterGlobalProperties(@NonNull Set<String> keys) {
        for (String key: keys) {
            analyticsClient.removeGlobalAttribute(key);
            analyticsClient.removeGlobalMetric(key);
        }
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
    @NonNull
    @Override
    public String getPluginKey() {
        return "awsPinpointAnalyticsPlugin";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(@NonNull JSONObject pluginConfiguration, @NonNull Context context) throws AnalyticsException {

        AmazonPinpointAnalyticsPluginConfiguration.Builder configurationBuilder =
                AmazonPinpointAnalyticsPluginConfiguration.builder();
        // Read all the data from the configuration object to be used for record event
        try {
            configurationBuilder
                    .withAppId(pluginConfiguration.getJSONObject("pinpointAnalytics")
                            .getString(PinpointConfigurationKey.APP_ID.getConfigurationKey()));
            configurationBuilder
                    .withRegion(pluginConfiguration.getJSONObject("pinpointAnalytics")
                            .getString(PinpointConfigurationKey.REGION.getConfigurationKey()));

            if (pluginConfiguration.has(PinpointConfigurationKey.AUTO_FLUSH_INTERVAL.getConfigurationKey())) {
                configurationBuilder
                        .withAutoFlushEventsInterval(pluginConfiguration
                                .getLong(PinpointConfigurationKey.AUTO_FLUSH_INTERVAL.getConfigurationKey()));
            }

            if (pluginConfiguration
                    .has(PinpointConfigurationKey.AUTO_SESSION_TRACKING_INTERVAL.getConfigurationKey())) {
                configurationBuilder
                        .withAutoSessionTrackingInterval(pluginConfiguration
                                .getLong(PinpointConfigurationKey.AUTO_SESSION_TRACKING_INTERVAL
                                        .getConfigurationKey()));
            }

            if (pluginConfiguration.has(PinpointConfigurationKey.TRACK_APP_LIFECYCLE_EVENTS
                    .getConfigurationKey())) {
                configurationBuilder
                        .withTrackAppLifecycleEvents(pluginConfiguration
                                .getBoolean(PinpointConfigurationKey.TRACK_APP_LIFECYCLE_EVENTS
                                        .getConfigurationKey()));
            }
        } catch (JSONException exception) {
            throw new AnalyticsException(
                "Unable to read appId or region from the amplify configuration json.",
                exception,
                "Make sure amplifyconfiguration.json is a valid json object in expected format. " +
                "Please take a look at the documentation for expected format of amplifyconfiguration.json."
            );
        }

        pinpointAnalyticsPluginConfiguration = configurationBuilder.build();
        PinpointManager pinpointManager = PinpointManagerFactory.create(context, pinpointAnalyticsPluginConfiguration);
        this.analyticsClient = pinpointManager.getAnalyticsClient();
        this.targetingClient = pinpointManager.getTargetingClient();

        // Initiate the logic to automatically submit events periodically
        autoEventSubmitter = new AutoEventSubmitter(analyticsClient,
                pinpointAnalyticsPluginConfiguration.getAutoFlushEventsInterval());
        autoEventSubmitter.start();

        // Instantiate the logic to automatically track app session
        autoSessionTracker = new AutoSessionTracker(this.analyticsClient, pinpointManager.getSessionClient());
        autoSessionTracker.startSessionTracking(application);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalyticsClient getEscapeHatch() {
        return analyticsClient;
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
