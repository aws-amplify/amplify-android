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
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.analytics.AnalyticsException;
import com.amplifyframework.analytics.BasicAnalyticsEvent;
import com.amplifyframework.analytics.pinpoint.test.R;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.testutils.Sleep;

import com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

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

    /**
     * Configure the Amplify framework.
     * @throws AmplifyException From Amplify configuration.
     */
    @BeforeClass
    public static void setUp() throws AmplifyException {
        Context context = ApplicationProvider.getApplicationContext();
        AmplifyConfiguration configuration = new AmplifyConfiguration();
        configuration.populateFromConfigFile(context, R.raw.amplifyconfiguration);
        Amplify.addPlugin(new AmazonPinpointAnalyticsPlugin());
        Amplify.configure(configuration, context);
        plugin = (AmazonPinpointAnalyticsPlugin) Amplify
                .Analytics
                .getPlugin("amazonPinpointAnalyticsPlugin");
        analyticsClient = plugin.getAnalyticsClient();
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
     * Unregisters a global property and ensures that it is respected in events recorded thereafter.
     * @throws AnalyticsException Caused by incorrect usage of the Analytics API.
     * @throws JSONException Caused by unexpected event structure.
     */
    @Test
    public void testGlobalProperties() throws AnalyticsException, JSONException {
        // Register global events
        Amplify.Analytics.registerGlobalProperties(PinpointProperties.builder()
                .add("GlobalProperty", "globalVal")
                .build());

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
        assertEquals(true, eventAttributes.has("Property"));
        assertEquals(true, eventAttributes.has("GlobalProperty"));
        assertEquals(false, event2Attributes.has("Property"));
        assertEquals(true, event2Attributes.has("GlobalProperty"));

        waitForAutoFlush(analyticsClient);

        // Unregister global property
        Set<String> globalPropertyKeys = new HashSet<>();
        globalPropertyKeys.add("GlobalProperty");
        Amplify.Analytics.unregisterGlobalProperties(globalPropertyKeys);

        Amplify.Analytics.recordEvent("amplify-test-event-without-property");

        assertEquals(1, analyticsClient.getAllEvents().size());
        assertEquals(false, analyticsClient.getAllEvents().get(0).has("attributes"));
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
