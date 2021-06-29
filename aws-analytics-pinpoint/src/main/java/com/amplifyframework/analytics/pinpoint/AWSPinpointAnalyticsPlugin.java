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
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.analytics.AnalyticsBooleanProperty;
import com.amplifyframework.analytics.AnalyticsDoubleProperty;
import com.amplifyframework.analytics.AnalyticsEventBehavior;
import com.amplifyframework.analytics.AnalyticsException;
import com.amplifyframework.analytics.AnalyticsIntegerProperty;
import com.amplifyframework.analytics.AnalyticsPlugin;
import com.amplifyframework.analytics.AnalyticsProperties;
import com.amplifyframework.analytics.AnalyticsPropertyBehavior;
import com.amplifyframework.analytics.AnalyticsStringProperty;
import com.amplifyframework.analytics.UserProfile;
import com.amplifyframework.core.Amplify;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
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

/**
 * The plugin implementation for Amazon Pinpoint in Analytics category.
 */
public final class AWSPinpointAnalyticsPlugin extends AnalyticsPlugin<Object> {
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            USER_NAME,
            USER_EMAIL,
            USER_PLAN
    })
    @SuppressWarnings("checkstyle:WhitespaceAround")
    private @interface PinpointUserProfileAttribute {}

    private static final String USER_NAME = "name";
    private static final String USER_EMAIL = "email";
    private static final String USER_PLAN = "plan";
    private static final String AUTH_DEPENDENCY_PLUGIN_KEY = "awsCognitoAuthPlugin";

    private final Application application;
    private AutoEventSubmitter autoEventSubmitter;
    private AnalyticsClient analyticsClient;
    private AutoSessionTracker autoSessionTracker;
    private TargetingClient targetingClient;
    private AWSCredentialsProvider credentialsProviderOverride; // Currently used for integration testing purposes

    /**
     * Constructs a new {@link AWSPinpointAnalyticsPlugin}.
     *
     * @param application Global application context
     */
    public AWSPinpointAnalyticsPlugin(final Application application) {
        this.application = application;
    }

    @VisibleForTesting
    AWSPinpointAnalyticsPlugin(final Application application, AWSCredentialsProvider credentialsProviderOverride) {
        this(application);
        this.credentialsProviderOverride = credentialsProviderOverride;
    }

    /**
     * Accessor method for pinpoint analytics client.
     *
     * @return returns pinpoint analytics client.
     */
    AnalyticsClient getAnalyticsClient() {
        return analyticsClient;
    }

    /**
     * Accessor method for pinpoint targeting client.
     *
     * @return returns pinpoint targeting client.
     */
    TargetingClient getTargetingClient() {
        return targetingClient;
    }

    @Override
    public void enable() {
        autoEventSubmitter.start();
        // Start auto session tracking
        autoSessionTracker.startSessionTracking(application);
    }

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
        targetingClient.updateEndpointProfile(endpointProfile);
    }

    /**
     * Add user specific data from {@link UserProfile} to the endpoint profile.
     *
     * @param endpointProfile endpoint profile.
     * @param userProfile     user specific data to be added to the endpoint.
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
     *
     * @param endpointProfile              current endpoint profile.
     * @param pinpointUserProfileAttribute String def enumerating the allowed user attributes.
     * @param attributeValue               user attribute value.
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
     *
     * @param endpointProfile  endpoint profile.
     * @param properties custom user properties to be added to the endpoint profile.
     */
    private void addCustomProperties(@NonNull EndpointProfile endpointProfile,
                                     @NonNull AnalyticsProperties properties) {
        for (Map.Entry<String, AnalyticsPropertyBehavior<?>> entry : properties) {
            String key = entry.getKey();
            boolean isUserAttribute = key.startsWith("user:");
            AnalyticsPropertyBehavior<?> property = entry.getValue();
            if (property instanceof AnalyticsStringProperty) {
                String value = ((AnalyticsStringProperty) property).getValue();
                if (isUserAttribute) {
                    endpointProfile.getUser().addUserAttribute(key.replace("user:", ""),
                                                                Collections.singletonList(value));
                } else {
                    endpointProfile.addAttribute(key, Collections.singletonList(value));
                }
            } else if (property instanceof AnalyticsBooleanProperty) {
                String value = ((AnalyticsBooleanProperty) property).getValue().toString();
                if (isUserAttribute) {
                    endpointProfile.getUser().addUserAttribute(key.replace("user:", ""),
                                                                Collections.singletonList(value));
                } else {
                    endpointProfile.addAttribute(key, Collections.singletonList(value));
                }
            } else if (property instanceof AnalyticsDoubleProperty) {
                Double value = ((AnalyticsDoubleProperty) property).getValue();
                endpointProfile.addMetric(entry.getKey(), value);
            } else if (property instanceof AnalyticsIntegerProperty) {
                Double value = ((AnalyticsIntegerProperty) property).getValue().doubleValue();
                endpointProfile.addMetric(entry.getKey(), value);
            }
        }
    }

    private void addUserAttributes(@NonNull EndpointProfileUser user,
                                   @NonNull AnalyticsProperties userAttributes) {
        for (Map.Entry<String, AnalyticsPropertyBehavior<?>> entry : userAttributes) {
            String key = entry.getKey();
            AnalyticsPropertyBehavior<?> property = entry.getValue();

            if (property instanceof AnalyticsStringProperty) {
                String value = ((AnalyticsStringProperty) property).getValue();
                user.addUserAttribute(key, Collections.singletonList(value));
            } else if (property instanceof AnalyticsBooleanProperty) {
                String value = ((AnalyticsBooleanProperty) property).getValue().toString();
                user.addUserAttribute(key, Collections.singletonList(value));
            } else if (property instanceof AnalyticsDoubleProperty) {
                Double value = ((AnalyticsDoubleProperty) property).getValue();
                user.addUserAttribute(entry.getKey(), Collections.singletonList(value.toString()));
            } else if (property instanceof AnalyticsIntegerProperty) {
                Double value = ((AnalyticsIntegerProperty) property).getValue().doubleValue();
                user.addUserAttribute(entry.getKey(), Collections.singletonList(value.toString()));
            }
        }
    }

    /**
     * Add location details to the endpoint profile location.
     *
     * @param endpointProfileLocation endpoint location.
     * @param location                location details.
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

    @Override
    public void disable() {
        autoEventSubmitter.stop();
        // Stop auto session tracking
        autoSessionTracker.stopSessionTracking(application);
    }

    @Override
    public void recordEvent(@NonNull String eventName) {
        final com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsEvent pinpointEvent =
                analyticsClient.createEvent(eventName);
        analyticsClient.recordEvent(pinpointEvent);
    }

    @Override
    public void recordEvent(@NonNull AnalyticsEventBehavior analyticsEvent) {
        final com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsEvent pinpointEvent =
                analyticsClient.createEvent(analyticsEvent.getName());

        if (analyticsEvent.getProperties() != null) {
            for (Map.Entry<String, AnalyticsPropertyBehavior<?>> entry : analyticsEvent.getProperties()) {
                String key = entry.getKey();
                AnalyticsPropertyBehavior<?> property = entry.getValue();

                if (property instanceof AnalyticsStringProperty) {
                    pinpointEvent.addAttribute(key, ((AnalyticsStringProperty) property).getValue());
                } else if (property instanceof AnalyticsBooleanProperty) {
                    String value = ((AnalyticsBooleanProperty) property).getValue().toString();
                    pinpointEvent.addAttribute(key, value);
                } else if (property instanceof AnalyticsDoubleProperty) {
                    pinpointEvent.addMetric(key, ((AnalyticsDoubleProperty) property).getValue());
                } else if (property instanceof AnalyticsIntegerProperty) {
                    Double value = ((AnalyticsIntegerProperty) property).getValue().doubleValue();
                    pinpointEvent.addMetric(key, value);
                }
            }

            analyticsClient.recordEvent(pinpointEvent);
        }
    }

    @Override
    public void registerGlobalProperties(@NonNull AnalyticsProperties properties) {
        for (Map.Entry<String, AnalyticsPropertyBehavior<?>> entry : properties) {
            String key = entry.getKey();
            AnalyticsPropertyBehavior<?> property = entry.getValue();

            if (property instanceof AnalyticsStringProperty) {
                analyticsClient.addGlobalAttribute(key, ((AnalyticsStringProperty) property).getValue());
            } else if (property instanceof AnalyticsBooleanProperty) {
                String value = ((AnalyticsBooleanProperty) property).getValue().toString();
                analyticsClient.addGlobalAttribute(key, value);
            } else if (property instanceof AnalyticsDoubleProperty) {
                analyticsClient.addGlobalMetric(key, ((AnalyticsDoubleProperty) property).getValue());
            } else if (property instanceof AnalyticsIntegerProperty) {
                Double value = ((AnalyticsIntegerProperty) property).getValue().doubleValue();
                analyticsClient.addGlobalMetric(key, value);
            }
        }
    }

    @Override
    public void unregisterGlobalProperties(@NonNull String... propertyNames) {
        for (String propertyName : propertyNames) {
            analyticsClient.removeGlobalAttribute(propertyName);
            analyticsClient.removeGlobalMetric(propertyName);
        }
    }

    @Override
    public void flushEvents() {
        analyticsClient.submitEvents();
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return "awsPinpointAnalyticsPlugin";
    }

    @Override
    public void configure(
            JSONObject pluginConfiguration,
            @NonNull Context context
    ) throws AnalyticsException {
        if (pluginConfiguration == null) {
            throw new AnalyticsException(
                    "Missing configuration for " + getPluginKey(),
                    "Check amplifyconfiguration.json to make sure that there is a section for " +
                            getPluginKey() + " under the analytics category."
            );
        }

        AWSPinpointAnalyticsPluginConfiguration.Builder configurationBuilder =
                AWSPinpointAnalyticsPluginConfiguration.builder();

        AWSCredentialsProvider credentialsProvider;

        if (credentialsProviderOverride != null) {
            credentialsProvider = credentialsProviderOverride;
        } else {
            try {
                credentialsProvider =
                        (AWSMobileClient) Amplify.Auth.getPlugin(AUTH_DEPENDENCY_PLUGIN_KEY).getEscapeHatch();
            } catch (IllegalStateException exception) {
                throw new AnalyticsException(
                        "AWSPinpointAnalyticsPlugin depends on AWSCognitoAuthPlugin but it is currently missing",
                        exception,
                        "Before configuring Amplify, be sure to add AWSCognitoAuthPlugin same as you added " +
                                "AWSPinpointAnalyticsPlugin."
                );
            }
        }

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

        AWSPinpointAnalyticsPluginConfiguration pinpointAnalyticsPluginConfiguration = configurationBuilder.build();
        PinpointManager pinpointManager = PinpointManagerFactory.create(
                context,
                pinpointAnalyticsPluginConfiguration,
                credentialsProvider
        );
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

    @Override
    public AnalyticsClient getEscapeHatch() {
        return analyticsClient;
    }

    @NonNull
    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
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
         * Whether to track app lifecycle events automatically.
         */
        TRACK_APP_LIFECYCLE_EVENTS("trackAppLifecycleEvents");

        /**
         * The key this property is listed under in the config JSON.
         */
        private final String configurationKey;

        /**
         * Construct the enum with the config key.
         *
         * @param configurationKey The key this property is listed under in the config JSON.
         */
        PinpointConfigurationKey(final String configurationKey) {
            this.configurationKey = configurationKey;
        }

        /**
         * Returns the key this property is listed under in the config JSON.
         *
         * @return The key as a string
         */
        public String getConfigurationKey() {
            return configurationKey;
        }
    }
}
