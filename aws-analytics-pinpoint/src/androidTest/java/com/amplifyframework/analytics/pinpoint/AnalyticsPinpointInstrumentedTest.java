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
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.testutils.Sleep;

import com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsClient;
import org.junit.BeforeClass;
import org.junit.Test;

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
    private static final int EVENT_FLUSH_WAIT = 5;

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
    }

    /**
     * Record a basic analytics event and verify that it has been recorded using Analytics
     * pinpoint client.
     * @throws AnalyticsException Caused by incorrect usage of the Analytics API.
     */
    @Test
    public void testRecordEvent() throws AnalyticsException {
        AmazonPinpointAnalyticsPlugin plugin = (AmazonPinpointAnalyticsPlugin) Amplify
                .Analytics
                .getPlugin("amazonPinpointAnalyticsPlugin");
        AnalyticsClient analyticsClient = plugin.getAnalyticsClient();

        // Flush any events from previous tests.
        Amplify.Analytics.flushEvents();
        waitForAutoFlush(analyticsClient.getAllEvents().size());

        BasicAnalyticsEvent event = new BasicAnalyticsEvent("Amplify-event" + UUID.randomUUID().toString(),
                PinpointProperties.builder()
                .add("DemoProperty1", "DemoValue1")
                .add("DemoDoubleProperty2", 2.0)
                .build());

        Amplify.Analytics.recordEvent(event);

        assertEquals(1, analyticsClient.getAllEvents().size());
    }

    /**
     * Record a basic analytic event and test that events are flushed from local database periodically.
     * @throws AnalyticsException Caused by incorrect usage of the Analytics API.
     */
    @Test
    public void testAutoFlush() throws AnalyticsException {
        AmazonPinpointAnalyticsPlugin plugin = (AmazonPinpointAnalyticsPlugin) Amplify
                .Analytics
                .getPlugin("amazonPinpointAnalyticsPlugin");
        AnalyticsClient analyticsClient = plugin.getAnalyticsClient();

        // Flush any events from previous tests.
        Amplify.Analytics.flushEvents();
        waitForAutoFlush(analyticsClient.getAllEvents().size());

        BasicAnalyticsEvent event = new BasicAnalyticsEvent("Amplify-event" + UUID.randomUUID().toString(),
                PinpointProperties.builder()
                        .add("DemoProperty1", "DemoValue1")
                        .add("DemoDoubleProperty2", 2.0)
                        .build());

        Amplify.Analytics.recordEvent(event);

        assertEquals(1, analyticsClient.getAllEvents().size());

        waitForAutoFlush(analyticsClient.getAllEvents().size());

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

        waitForAutoFlush(analyticsClient.getAllEvents().size());

        LOG.debug("Events in database after calling submitEvents() after submitting: " +
                analyticsClient.getAllEvents().size());

        assertEquals(0, analyticsClient.getAllEvents().size());
    }

    private void waitForAutoFlush(int numOfEvents) {
        long timeSleptSoFar = 0;
        while (timeSleptSoFar < TimeUnit.SECONDS.toMillis(EVENT_FLUSH_TIMEOUT)) {
            Sleep.milliseconds(TimeUnit.SECONDS.toMillis(EVENT_FLUSH_WAIT));
            timeSleptSoFar += TimeUnit.SECONDS.toMillis(EVENT_FLUSH_WAIT);
            if (numOfEvents == 0) {
                break;
            }
        }
    }
}
