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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.analytics.AnalyticsException;
import com.amplifyframework.analytics.BasicAnalyticsEvent;
import com.amplifyframework.analytics.UserProfile;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.testutils.Sleep;

import com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsClient;
import com.amazonaws.mobileconnectors.pinpoint.targeting.TargetingClient;
import com.amazonaws.mobileconnectors.pinpoint.targeting.endpointProfile.EndpointProfile;
import com.amazonaws.mobileconnectors.pinpoint.targeting.endpointProfile.EndpointProfileLocation;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Validates the functionality of the {@link AmazonPinpointAnalyticsPlugin}.
 */
public class AnalyticsPinpointInstrumentedTest {

    /**
     * Log tag for the test class.
     */
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-analytics");
    private static final int EVENT_FLUSH_TIMEOUT = 60;
    private static final int EVENT_FLUSH_WAIT = 2;

    private static AmazonPinpointAnalyticsPlugin plugin;
    private static AnalyticsClient analyticsClient;
    private static TargetingClient targetingClient;

    /**
     * Configure the Amplify framework.
     * @throws AmplifyException From Amplify configuration.
     */
    @BeforeClass
    public static void setUp() throws AmplifyException {
        Context context = getApplicationContext();
        plugin = new AmazonPinpointAnalyticsPlugin((Application) context);
        Amplify.addPlugin(plugin);
        Amplify.configure(context);
        analyticsClient = plugin.getAnalyticsClient();
        targetingClient = plugin.getTargetingClient();
    }

    /**
     * Flush events in local database before each test.
     */
    @Before
    public void flushEvents() {
        Amplify.Analytics.flushEvents();
        waitForAutoFlush(analyticsClient);
    }

    /**
     * Record a basic analytics event and verify that it has been recorded using Analytics
     * pinpoint client.
     * @throws AnalyticsException Caused by incorrect usage of the Analytics API.
     */
    @Test
    public void testRecordEvent() throws AnalyticsException {
        BasicAnalyticsEvent event = new BasicAnalyticsEvent("Amplify-event" + UUID.randomUUID().toString(),
                PinpointProperties.builder()
                .add("DemoProperty1", "DemoValue1")
                .add("DemoDoubleProperty2", 2.0)
                .build());

        Amplify.Analytics.recordEvent(event);

        assertEquals(1, analyticsClient.getAllEvents().size());
    }

    /**
     * Record a basic analytics event and test that events are flushed from local database periodically.
     * @throws AnalyticsException Caused by incorrect usage of the Analytics API.
     */
    @Test
    public void testAutoFlush() throws AnalyticsException {
        BasicAnalyticsEvent event = new BasicAnalyticsEvent("Amplify-event" + UUID.randomUUID().toString(),
                PinpointProperties.builder()
                        .add("DemoProperty1", "DemoValue1")
                        .add("DemoDoubleProperty2", 2.0)
                        .build());

        Amplify.Analytics.recordEvent(event);

        assertEquals(1, analyticsClient.getAllEvents().size());

        waitForAutoFlush(analyticsClient);

        LOG.debug("Events in database after calling submitEvents() after submitting: " +
                analyticsClient.getAllEvents().size());

        assertEquals(0, analyticsClient.getAllEvents().size());

        BasicAnalyticsEvent event2 = new BasicAnalyticsEvent("Amplify-event" + UUID.randomUUID().toString(),
                PinpointProperties.builder()
                        .add("DemoProperty1", "DemoValue1")
                        .add("DemoProperty2", 2.0)
                        .build());

        Amplify.Analytics.recordEvent(event2);

        assertEquals(1, analyticsClient.getAllEvents().size());

        waitForAutoFlush(analyticsClient);

        LOG.debug("Events in database after calling submitEvents() after submitting: " +
                analyticsClient.getAllEvents().size());

        assertEquals(0, analyticsClient.getAllEvents().size());
    }

    /**
     * Registers a global property and ensures that all recorded events have the global property.
     * @throws AnalyticsException Caused by incorrect usage of the Analytics API.
     * @throws JSONException Caused by unexpected event structure.
     */
    @Test
    public void testRegisterGlobalProperties() throws AnalyticsException, JSONException {
        // Register a global property
        registerGobalProperty();

        BasicAnalyticsEvent event = new BasicAnalyticsEvent("Amplify-event" + UUID.randomUUID().toString(),
                PinpointProperties.builder()
                        .add("Property", "PropertyValue")
                        .build());
        Amplify.Analytics.recordEvent(event);
        Amplify.Analytics.recordEvent("amplify-test-event");

        JSONObject eventAttributes =
                new JSONObject(analyticsClient.getAllEvents().get(0).get("attributes").toString());
        JSONObject event2Attributes =
                new JSONObject(analyticsClient.getAllEvents().get(1).get("attributes").toString());

        assertEquals(2, analyticsClient.getAllEvents().size());
        assertTrue(eventAttributes.has("Property"));
        assertTrue(eventAttributes.has("GlobalProperty"));
        assertFalse(event2Attributes.has("Property"));
        assertTrue(event2Attributes.has("GlobalProperty"));
    }

    /**
     * Registers a global property and then Unregisters the global property
     * and ensures that it is respected in events recorded thereafter.
     * @throws AnalyticsException Caused by incorrect usage of the Analytics API.
     */
    @Test
    public void testUnregisterGlobalProperties() throws AnalyticsException {
        // Register a global property
        registerGobalProperty();

        // Unregister global property
        Set<String> globalPropertyKeys = new HashSet<>();
        globalPropertyKeys.add("GlobalProperty");
        Amplify.Analytics.unregisterGlobalProperties(globalPropertyKeys);

        Amplify.Analytics.recordEvent("amplify-test-event-without-property");

        assertEquals(1, analyticsClient.getAllEvents().size());
        assertFalse(analyticsClient.getAllEvents().get(0).has("attributes"));
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void testIdentifyUser() {
        UserProfile.Location location = UserProfile.Location.builder()
                .latitude(47.6154086)
                .longitude(-122.3349685)
                .postalCode("98122")
                .city("Seattle")
                .region("WA")
                .country("USA")
                .build();
        PinpointProperties pinpointProperties = PinpointProperties.builder()
                .add("TestStringProperty", "TestStringValue")
                .add("TestDoubleProperty", 1.0)
                .build();
        UserProfile userProfile = UserProfile.builder()
                .name("test-user")
                .email("user@test.com")
                .plan("test-plan")
                .location(location)
                .customProperties(pinpointProperties)
                .build();

        Amplify.Analytics.identifyUser("userId", userProfile);

        EndpointProfile endpointProfile = targetingClient.currentEndpoint();
        EndpointProfileLocation endpointProfileLocation = endpointProfile.getLocation();
        assertEquals("user@test.com", endpointProfile.getAttribute("email").get(0));
        assertEquals("test-user", endpointProfile.getAttribute("name").get(0));
        assertEquals("test-plan", endpointProfile.getAttribute("plan").get(0));
        assertEquals((Double) 47.6154086, endpointProfileLocation.getLatitude());
        assertEquals((Double) (-122.3349685), endpointProfileLocation.getLongitude());
        assertEquals("98122", endpointProfileLocation.getPostalCode());
        assertEquals("Seattle", endpointProfileLocation.getCity());
        assertEquals("WA", endpointProfileLocation.getRegion());
        assertEquals("USA", endpointProfileLocation.getCountry());
        assertEquals("TestStringValue", endpointProfile.getAttribute("TestStringProperty").get(0));
        assertEquals((Double) 1.0, endpointProfile.getMetric("TestDoubleProperty"));
    }

    private void registerGobalProperty() throws AnalyticsException {
        // Register a global property
        Amplify.Analytics.registerGlobalProperties(PinpointProperties.builder()
                .add("GlobalProperty", "globalVal")
                .build());
    }

    private void waitForAutoFlush(AnalyticsClient analyticsClient) {
        long timeSleptSoFar = 0;
        while (timeSleptSoFar < TimeUnit.SECONDS.toMillis(EVENT_FLUSH_TIMEOUT)) {
            Sleep.milliseconds(TimeUnit.SECONDS.toMillis(EVENT_FLUSH_WAIT));
            timeSleptSoFar += TimeUnit.SECONDS.toMillis(EVENT_FLUSH_WAIT);
            if (analyticsClient.getAllEvents().size() == 0) {
                break;
            }
        }
    }
}
