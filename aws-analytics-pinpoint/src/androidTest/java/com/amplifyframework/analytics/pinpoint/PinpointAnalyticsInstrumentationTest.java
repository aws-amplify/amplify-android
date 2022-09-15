/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.analytics.AnalyticsEvent;
import com.amplifyframework.analytics.AnalyticsProperties;
import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthPlugin;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.Sleep;
import com.amplifyframework.testutils.sync.SynchronousAuth;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.util.Pair;
import androidx.annotation.RawRes;
import androidx.test.core.app.ApplicationProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public final class PinpointAnalyticsInstrumentationTest {

    private static final int EVENT_FLUSH_TIMEOUT_WAIT = 15 /* seconds */;
    private static final String CREDENTIALS_RESOURCE_NAME = "credentials";
    private static final Long COGNITO_CONFIGURATION_TIMEOUT = 10 * 1000L;
    private static final Long RECORD_INSERTION_TIMEOUT = 3 * 1000L;
    private static SynchronousAuth synchronousAuth;

    @BeforeClass
    public static void setupBefore() throws AmplifyException, InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        Amplify.Auth.addPlugin((AuthPlugin<?>) new AWSCognitoAuthPlugin());
        Amplify.addPlugin(new AWSPinpointAnalyticsPlugin());
        Amplify.configure(context);
        Sleep.milliseconds(COGNITO_CONFIGURATION_TIMEOUT);
        synchronousAuth = SynchronousAuth.delegatingTo(Amplify.Auth);
    }

    private static Pair<String, String> readCredentialsFromResource(Context context, @RawRes int resourceId) {
        JSONObject resource = Resources.readAsJson(context, resourceId);
        Pair<String, String> userCredentials = null;
        try {
            JSONArray credentials = resource.getJSONArray("credentials");
            for (int index = 0; index < credentials.length(); index++) {
                JSONObject credential = credentials.getJSONObject(index);
                String username = credential.getString("username");
                String password = credential.getString("password");
                userCredentials = new Pair<>(username, password);
            }
            return userCredentials;
        } catch (JSONException jsonReadingFailure) {
            throw new RuntimeException(jsonReadingFailure);
        }
    }

    @Before
    public void flushEvents() throws AuthException {
        Context context = ApplicationProvider.getApplicationContext();
        @RawRes int resourceId = Resources.getRawResourceId(context, CREDENTIALS_RESOURCE_NAME);
        Pair<String, String> userAndPasswordPair = readCredentialsFromResource(context, resourceId);
        synchronousAuth.signIn(userAndPasswordPair.first, userAndPasswordPair.second);
        HubAccumulator hubAccumulator =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 1).start();
        Amplify.Analytics.flushEvents();
        List<HubEvent<?>> events = hubAccumulator.await(10, TimeUnit.SECONDS);
    }

    @After
    public void cleanUp() throws AuthException {
        synchronousAuth.signOut();
    }

    /**
     * Record a basic analytics event and verify that it has been recorded using Analytics
     * pinpoint client.
     *
     * @throws JSONException Caused by unexpected event structure.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void recordEventStoresPassedBasicAnalyticsEvent() throws JSONException, InterruptedException {
        HubAccumulator hubAccumulator =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 1).start();
        // Arrange: Create an event
        String eventName = "Amplify-event" + UUID.randomUUID().toString();
        AnalyticsEvent event = AnalyticsEvent.builder()
            .name(eventName)
            .addProperty("AnalyticsStringProperty", "Pancakes")
            .addProperty("AnalyticsBooleanProperty", true)
            .addProperty("AnalyticsDoubleProperty", 3.14)
            .addProperty("AnalyticsIntegerProperty", 42)
            .build();

        // Act: Record the event
        Amplify.Analytics.recordEvent(event);
        Sleep.milliseconds(RECORD_INSERTION_TIMEOUT);

        Amplify.Analytics.flushEvents();
        List<HubEvent<?>> hubEvents = hubAccumulator.await(10, TimeUnit.SECONDS);
        assertEquals(1, hubEvents.size());

        ArrayList<AnalyticsEvent> submittedEvents = (ArrayList<AnalyticsEvent>) hubEvents.get(0).getData();
        assert submittedEvents != null;
        AnalyticsEvent submittedEvent = submittedEvents.get(0);

        assertEquals(submittedEvent.getName(), eventName);
        assertEquals("Pancakes", submittedEvent.getProperties().get("AnalyticsStringProperty").getValue());
        assertEquals("true", submittedEvent.getProperties().get("AnalyticsBooleanProperty").getValue());
        assertEquals(3.14, submittedEvent.getProperties().get("AnalyticsDoubleProperty").getValue());
        assertEquals(42d, submittedEvent.getProperties().get("AnalyticsIntegerProperty").getValue());
    }

    /**
     * Record a basic analytics event and test that events are flushed from local database by calling submitEvents.
     */
    @Test
    public void testFlushEventHubEvent() throws InterruptedException {
        HubAccumulator analyticsHubEventAccumulator =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 1)
                .start();
        String eventName1 = "Amplify-event" + UUID.randomUUID().toString();
        AnalyticsEvent event1 = AnalyticsEvent.builder()
            .name(eventName1)
            .addProperty("DemoProperty1", "DemoValue1")
            .addProperty("DemoDoubleProperty2", 2.0)
            .build();

        Amplify.Analytics.recordEvent(event1);
        Sleep.milliseconds(RECORD_INSERTION_TIMEOUT);

        String eventName2 = "Amplify-event" + UUID.randomUUID().toString();

        AnalyticsEvent event2 = AnalyticsEvent.builder()
            .name(eventName2)
            .addProperty("DemoProperty1", "DemoValue1")
            .addProperty("DemoProperty2", 2.0)
            .build();

        Amplify.Analytics.recordEvent(event2);
        Sleep.milliseconds(RECORD_INSERTION_TIMEOUT);

        Amplify.Analytics.flushEvents();
        List<HubEvent<?>> hubEvents = analyticsHubEventAccumulator.await(10, TimeUnit.SECONDS);
        assertEquals(1, hubEvents.size());

        HubEvent<?> hubEvent = analyticsHubEventAccumulator.awaitFirst();
        List<?> hubEventData = (List<?>) hubEvent.getData();
        assertEquals(AnalyticsChannelEventName.FLUSH_EVENTS.getEventName(), hubEvent.getName());
        assert hubEventData != null;
        assertEquals(eventName1, ((AnalyticsEvent) hubEventData.get(0)).getName());
        assertEquals(eventName2, ((AnalyticsEvent) hubEventData.get(1)).getName());
    }

    /**
     * Record a basic analytics event and test that events are flushed from local database periodically.
     */
    @Test
    public void testAutoFlush() {
        HubAccumulator analyticsHubEventAccumulator1 =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 1)
                .start();
        AnalyticsEvent event1 = AnalyticsEvent.builder()
            .name("Amplify-event" + UUID.randomUUID().toString())
            .addProperty("DemoProperty1", "DemoValue1")
            .addProperty("DemoDoubleProperty2", 2.0)
            .build();

        Amplify.Analytics.recordEvent(event1);
        //Autosubmitter runs every 10 secs, so waiting for 15sec to receive the hub event
        List<HubEvent<?>> hubEvents1 = analyticsHubEventAccumulator1.await(EVENT_FLUSH_TIMEOUT_WAIT, TimeUnit.SECONDS);
        assertEquals(1, hubEvents1.size());

        HubAccumulator analyticsHubEventAccumulator2 =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 1)
                .start();
        AnalyticsEvent event2 = AnalyticsEvent.builder()
            .name("Amplify-event" + UUID.randomUUID().toString())
            .addProperty("DemoProperty1", "DemoValue1")
            .addProperty("DemoProperty2", 2.0)
            .build();

        Amplify.Analytics.recordEvent(event2);
        //Autosubmitter runs every 10 secs, so waiting for 15sec to receive the hub event
        List<HubEvent<?>> hubEvents2 = analyticsHubEventAccumulator1.await(EVENT_FLUSH_TIMEOUT_WAIT, TimeUnit.SECONDS);
        assertEquals(1, hubEvents2.size());
    }

    /**
     * Registers a global property and ensures that all recorded events have the global property.
     *
     * @throws JSONException Caused by unexpected event structure.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void registerGlobalPropertiesAddsGivenPropertiesToRecordedEvents() throws JSONException {
        HubAccumulator analyticsHubEventAccumulator =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 1)
                .start();
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
        Sleep.milliseconds(RECORD_INSERTION_TIMEOUT);
        Amplify.Analytics.flushEvents();
        List<HubEvent<?>> hubEvents = analyticsHubEventAccumulator.await(10, TimeUnit.SECONDS);
        ArrayList<AnalyticsEvent> submittedEvents = (ArrayList<AnalyticsEvent>) hubEvents.get(0).getData();
        // Assert: Verify two event were recorded and global attributes are present on both
        assert submittedEvents != null;
        assertEquals(2, submittedEvents.size());
        AnalyticsProperties event1Attributes = submittedEvents.get(0).getProperties();
        AnalyticsProperties event2Attributes = submittedEvents.get(1).getProperties();

        // Global properties are attached to all events
        assertEquals("GlobalValue", event1Attributes.get("GlobalProperty").getValue());
        assertEquals("GlobalValue", event2Attributes.get("GlobalProperty").getValue());

        // Local properties are only attached is passed explicitly as in event
        assertEquals("LocalValue", event1Attributes.get("LocalProperty").getValue());
    }

    /**
     * Registers a global property and then Unregisters the global property
     * and ensures that it is respected in events recorded thereafter.
     *
     */
    @SuppressWarnings("unchecked")
    @Test
    public void unregisterGlobalPropertiesRemovesGivenProperties() {
        HubAccumulator analyticsHubEventAccumulator =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 1)
                .start();
        // Arrange: Register a global property
        Amplify.Analytics.registerGlobalProperties(
            AnalyticsProperties.builder()
                .add("GlobalProperty", "globalVal")
                .build()
        );

        // Act: Record an event, unregister the global property, and record another event
        Amplify.Analytics.recordEvent("amplify-test-event-with-property");
        Sleep.milliseconds(RECORD_INSERTION_TIMEOUT);
        Amplify.Analytics.unregisterGlobalProperties("GlobalProperty");
        Amplify.Analytics.recordEvent("amplify-test-event-without-property");
        Sleep.milliseconds(RECORD_INSERTION_TIMEOUT);
        Amplify.Analytics.flushEvents();
        List<HubEvent<?>> hubEvents = analyticsHubEventAccumulator.await(10, TimeUnit.SECONDS);
        ArrayList<AnalyticsEvent> submittedEvents = (ArrayList<AnalyticsEvent>) hubEvents.get(0).getData();

        // Assert: Ensure there are two events, the first has attributes, and the second doesn't
        assertEquals("globalVal", submittedEvents.get(0).getProperties().get("GlobalProperty").getValue());
        assertEquals(0, submittedEvents.get(1).getProperties().size());
    }
}