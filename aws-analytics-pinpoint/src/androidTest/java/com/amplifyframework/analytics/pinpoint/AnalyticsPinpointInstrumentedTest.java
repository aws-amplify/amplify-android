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
import com.amplifyframework.analytics.AnalyticsCategory;
import com.amplifyframework.analytics.AnalyticsEvent;
import com.amplifyframework.analytics.AnalyticsProperties;
import com.amplifyframework.analytics.UserProfile;
import com.amplifyframework.analytics.pinpoint.models.AWSPinpointUserProfile;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.testutils.Sleep;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsClient;
import com.amazonaws.mobileconnectors.pinpoint.targeting.TargetingClient;
import com.amazonaws.mobileconnectors.pinpoint.targeting.endpointProfile.EndpointProfile;
import com.amazonaws.mobileconnectors.pinpoint.targeting.endpointProfile.EndpointProfileLocation;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Validates the functionality of the {@link AWSPinpointAnalyticsPlugin}.
 */
public class AnalyticsPinpointInstrumentedTest {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-analytics");

    private static final int EVENT_FLUSH_TIMEOUT = 60 /* seconds */;
    private static final int EVENT_FLUSH_WAIT = 2 /* seconds */;
    private static final long AUTH_TIMEOUT = 5 /* seconds */;

    private static AnalyticsClient analyticsClient;
    private static TargetingClient targetingClient;

    /**
     * Configure the Amplify framework.
     *
     * @throws AmplifyException From Amplify configuration.
     */
    @BeforeClass
    public static void setUp() throws AmplifyException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Exception> asyncException = new AtomicReference<>();
        Context context = getApplicationContext();

        AWSMobileClient.getInstance().initialize(
                context,
                new AWSConfiguration(context),
                new Callback<UserStateDetails>() {
                    @Override
                    public void onResult(UserStateDetails result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception exception) {
                        asyncException.set(exception);
                        latch.countDown();
                    }
                }
        );

        try {
            if (latch.await(AUTH_TIMEOUT, TimeUnit.SECONDS)) {
                if (asyncException.get() != null) {
                    fail("Failed to instantiate AWSMobileClient");
                }
            } else {
                fail("Failed to instantiate AWSMobileClient within " + AUTH_TIMEOUT + " seconds");
            }
        } catch (InterruptedException error) {
            fail("Failed to instantiate AWSMobileClient");
        }

        AWSPinpointAnalyticsPlugin plugin =
                new AWSPinpointAnalyticsPlugin((Application) context, AWSMobileClient.getInstance());
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
     *
     * @throws JSONException Caused by unexpected event structure.
     */
    @Test
    public void recordEventStoresPassedBasicAnalyticsEvent() throws JSONException {
        // Arrange: Create an event
        AnalyticsEvent event = AnalyticsEvent.builder()
                .name("Amplify-event" + UUID.randomUUID().toString())
                .addProperty("AnalyticsStringProperty", "Pancakes")
                .addProperty("AnalyticsBooleanProperty", true)
                .addProperty("AnalyticsDoubleProperty", 3.14)
                .addProperty("AnalyticsIntegerProperty", 42)
                .build();

        // Act: Record the event
        Amplify.Analytics.recordEvent(event);

        // Assert: Verify the event was recorded and its attributes are present
        List<JSONObject> events = analyticsClient.getAllEvents();
        assertEquals(1, events.size());
        JSONObject json = events.get(0);

        JSONObject attributes = json.getJSONObject("attributes");
        assertEquals("Pancakes", attributes.getString("AnalyticsStringProperty"));
        // Booleans will be translated into Strings as Pinpoint doesn't support booleans.
        assertEquals("true", attributes.getString("AnalyticsBooleanProperty"));

        JSONObject metrics = json.getJSONObject("metrics");
        assertEquals(3.14, metrics.getDouble("AnalyticsDoubleProperty"), 0);
        assertEquals(42, metrics.getInt("AnalyticsIntegerProperty"));
    }

    /**
     * Record a basic analytics event and test that events are flushed from local database periodically.
     *
     */
    @Test
    public void testAutoFlush() {
        AnalyticsEvent event1 = AnalyticsEvent.builder()
                .name("Amplify-event" + UUID.randomUUID().toString())
                .addProperty("DemoProperty1", "DemoValue1")
                .addProperty("DemoDoubleProperty2", 2.0)
                .build();

        Amplify.Analytics.recordEvent(event1);

        assertEquals(1, analyticsClient.getAllEvents().size());

        waitForAutoFlush(analyticsClient);

        LOG.debug("Events in database after calling submitEvents() after submitting: " +
                analyticsClient.getAllEvents().size());

        assertEquals(0, analyticsClient.getAllEvents().size());

        AnalyticsEvent event2 = AnalyticsEvent.builder()
                .name("Amplify-event" + UUID.randomUUID().toString())
                .addProperty("DemoProperty1", "DemoValue1")
                .addProperty("DemoProperty2", 2.0)
                .build();

        Amplify.Analytics.recordEvent(event2);

        assertEquals(1, analyticsClient.getAllEvents().size());

        waitForAutoFlush(analyticsClient);

        LOG.debug("Events in database after calling submitEvents() after submitting: " +
                analyticsClient.getAllEvents().size());

        assertEquals(0, analyticsClient.getAllEvents().size());
    }

    /**
     * Registers a global property and ensures that all recorded events have the global property.
     *
     * @throws JSONException Caused by unexpected event structure.
     */
    @Test
    public void registerGlobalPropertiesAddsGivenPropertiesToRecordedEvents() throws JSONException {
        // Arrange: Register global properties and create an event
        Amplify.Analytics.registerGlobalProperties(
                AnalyticsProperties.builder()
                        .add("GlobalProperty", "GlobalValue")
                        .build()
        );
        AnalyticsEvent event = AnalyticsEvent.builder()
                .name("Amplify-event" + UUID.randomUUID().toString())
                .addProperty("LocalProperty", "LocalValue")
                .build();

        // Act: Record two events: the one created above and another just with a key
        Amplify.Analytics.recordEvent(event);
        Amplify.Analytics.recordEvent("amplify-test-event");

        // Assert: Verify two event were recorded and global attributes are present on both
        List<JSONObject> events = analyticsClient.getAllEvents();
        assertEquals(2, events.size());
        JSONObject event1Attributes = events.get(0).getJSONObject("attributes");
        JSONObject event2Attributes = events.get(1).getJSONObject("attributes");

        // Global properties are attached to all events
        assertEquals("GlobalValue", event1Attributes.getString("GlobalProperty"));
        assertEquals("GlobalValue", event2Attributes.getString("GlobalProperty"));

        // Local properties are only attached is passed explicitly as in event
        assertEquals("LocalValue", event1Attributes.getString("LocalProperty"));
        assertFalse(event2Attributes.has("LocalProperty"));
    }

    /**
     * Registers a global property and then Unregisters the global property
     * and ensures that it is respected in events recorded thereafter.
     *
     */
    @Test
    public void unregisterGlobalPropertiesRemovesGivenProperties() {
        // Arrange: Register a global property
        Amplify.Analytics.registerGlobalProperties(
                AnalyticsProperties.builder()
                        .add("GlobalProperty", "globalVal")
                        .build()
        );

        // Act: Record an event, unregister the global property, and record another event
        Amplify.Analytics.recordEvent("amplify-test-event-with-property");
        Amplify.Analytics.unregisterGlobalProperties("GlobalProperty");
        Amplify.Analytics.recordEvent("amplify-test-event-without-property");

        // Assert: Ensure there are two events, the first has attributes, and the second doesn't
        List<JSONObject> events = analyticsClient.getAllEvents();
        assertEquals(2, events.size());
        assertTrue(analyticsClient.getAllEvents().get(0).has("attributes"));
        assertFalse(analyticsClient.getAllEvents().get(1).has("attributes"));
    }

    /**
     * The {@link AnalyticsCategory#identifyUser(String, UserProfile)} method should set
     * an {@link EndpointProfile} on the Pinpoint {@link TargetingClient}, containing
     * all provided Amplify attributes.
     */
    @Test
    public void testIdentifyUserWithDefaultProfile() {
        UserProfile.Location location = getTestLocation();
        AnalyticsProperties properties = getEndpointProperties();
        UserProfile userProfile = UserProfile.builder()
                .name("test-user")
                .email("user@test.com")
                .plan("test-plan")
                .location(location)
                .customProperties(properties)
                .build();

        Amplify.Analytics.identifyUser("userId", userProfile);

        EndpointProfile endpointProfile = targetingClient.currentEndpoint();
        assertCommonEndpointProfileProperties(endpointProfile);
        assertNull(endpointProfile.getUser().getUserAttributes());
    }

    /**
     * {@link AWSPinpointUserProfile} extends {@link UserProfile} to include
     * {@link AWSPinpointUserProfile#userAttributes} which is specific to Pinpoint. This test is very
     * similar to testIdentifyUserWithDefaultProfile, but it adds user attributes in additional
     * to the endpoint attributes.
     */
    @Test
    public void testIdentifyUserWithUserAtrributes() {
        UserProfile.Location location = getTestLocation();
        AnalyticsProperties properties = getEndpointProperties();
        AnalyticsProperties userAttributes = getUserAttributes();
        AWSPinpointUserProfile pinpointUserProfile = AWSPinpointUserProfile.builder()
                .name("test-user")
                .email("user@test.com")
                .plan("test-plan")
                .location(location)
                .customProperties(properties)
                .userAttributes(userAttributes)
                .build();

        Amplify.Analytics.identifyUser("userId", pinpointUserProfile);

        EndpointProfile endpointProfile = targetingClient.currentEndpoint();
        assertCommonEndpointProfileProperties(endpointProfile);

        assertEquals("User attribute value", endpointProfile.getUser()
                .getUserAttributes()
                .get("SomeUserAttribute")
                .get(0));
    }

    private void assertCommonEndpointProfileProperties(EndpointProfile endpointProfile) {
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

    private AnalyticsProperties getUserAttributes() {
        return AnalyticsProperties.builder()
            .add("SomeUserAttribute", "User attribute value")
            .build();
    }

    private AnalyticsProperties getEndpointProperties() {
        return AnalyticsProperties.builder()
            .add("TestStringProperty", "TestStringValue")
            .add("TestDoubleProperty", 1.0)
            .build();
    }

    private UserProfile.Location getTestLocation() {
        return UserProfile.Location.builder()
                .latitude(47.6154086)
                .longitude(-122.3349685)
                .postalCode("98122")
                .city("Seattle")
                .region("WA")
                .country("USA")
                .build();
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
