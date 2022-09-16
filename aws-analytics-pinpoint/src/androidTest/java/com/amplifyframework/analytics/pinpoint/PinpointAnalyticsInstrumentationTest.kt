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
package com.amplifyframework.analytics.pinpoint

import android.content.Context
import android.util.Pair
import androidx.annotation.RawRes
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.analytics.AnalyticsEvent
import com.amplifyframework.analytics.AnalyticsProperties
import com.amplifyframework.auth.AuthPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.testutils.HubAccumulator
import com.amplifyframework.testutils.Resources
import com.amplifyframework.testutils.Sleep
import com.amplifyframework.testutils.sync.SynchronousAuth
import org.json.JSONException
import org.junit.After
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.TimeUnit

class PinpointAnalyticsInstrumentationTest {
    @Before
    fun flushEvents() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @RawRes val resourceId = Resources.getRawResourceId(context, CREDENTIALS_RESOURCE_NAME)
        val userAndPasswordPair = readCredentialsFromResource(context, resourceId)
        synchronousAuth.signIn(
            userAndPasswordPair!!.first, userAndPasswordPair.second
        )
        val hubAccumulator =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 1).start()
        Amplify.Analytics.flushEvents()
        val events = hubAccumulator.await(10, TimeUnit.SECONDS)
    }

    @After
    fun cleanUp() {
        synchronousAuth.signOut()
    }

    /**
     * Record a basic analytics event and verify that it has been recorded using Analytics
     * pinpoint client.
     */
    @Test
    fun recordEventStoresPassedBasicAnalyticsEvent() {
        val hubAccumulator =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 1).start()
        // Arrange: Create an event
        val eventName = "Amplify-event" + UUID.randomUUID().toString()
        val event = AnalyticsEvent.builder()
            .name(eventName)
            .addProperty("AnalyticsStringProperty", "Pancakes")
            .addProperty("AnalyticsBooleanProperty", true)
            .addProperty("AnalyticsDoubleProperty", 3.14)
            .addProperty("AnalyticsIntegerProperty", 42)
            .build()

        // Act: Record the event
        Amplify.Analytics.recordEvent(event)
        Sleep.milliseconds(RECORD_INSERTION_TIMEOUT)
        Amplify.Analytics.flushEvents()
        val hubEvents = hubAccumulator.await(10, TimeUnit.SECONDS)
        Assert.assertEquals(1, hubEvents.size.toLong())
        val submittedEvents = (hubEvents[0].data as ArrayList<AnalyticsEvent>?)!!
        val submittedEvent = submittedEvents[0]
        Assert.assertEquals(submittedEvent.name, eventName)
        Assert.assertEquals("Pancakes", submittedEvent.properties["AnalyticsStringProperty"].value)
        Assert.assertEquals("true", submittedEvent.properties["AnalyticsBooleanProperty"].value)
        Assert.assertEquals(3.14, submittedEvent.properties["AnalyticsDoubleProperty"].value)
        Assert.assertEquals(42.0, submittedEvent.properties["AnalyticsIntegerProperty"].value)
    }

    /**
     * Record a basic analytics event and test that events are flushed from local database by calling submitEvents.
     */
    @Test
    fun testFlushEventHubEvent() {
        val analyticsHubEventAccumulator =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 1)
                .start()
        val eventName1 = "Amplify-event" + UUID.randomUUID().toString()
        val event1 = AnalyticsEvent.builder()
            .name(eventName1)
            .addProperty("DemoProperty1", "DemoValue1")
            .addProperty("DemoDoubleProperty2", 2.0)
            .build()
        Amplify.Analytics.recordEvent(event1)
        Sleep.milliseconds(RECORD_INSERTION_TIMEOUT)
        val eventName2 = "Amplify-event" + UUID.randomUUID().toString()
        val event2 = AnalyticsEvent.builder()
            .name(eventName2)
            .addProperty("DemoProperty1", "DemoValue1")
            .addProperty("DemoProperty2", 2.0)
            .build()
        Amplify.Analytics.recordEvent(event2)
        Sleep.milliseconds(RECORD_INSERTION_TIMEOUT)
        Amplify.Analytics.flushEvents()
        val hubEvents = analyticsHubEventAccumulator.await(10, TimeUnit.SECONDS)
        Assert.assertEquals(1, hubEvents.size.toLong())
        val hubEvent = analyticsHubEventAccumulator.awaitFirst()
        val hubEventData = hubEvent.data as List<*>?
        Assert.assertEquals(AnalyticsChannelEventName.FLUSH_EVENTS.eventName, hubEvent.name)
        assert(hubEventData != null)
        Assert.assertEquals(eventName1, (hubEventData!![0] as AnalyticsEvent).name)
        Assert.assertEquals(eventName2, (hubEventData[1] as AnalyticsEvent).name)
    }

    /**
     * Record a basic analytics event and test that events are flushed from local database periodically.
     */
    @Test
    fun testAutoFlush() {
        val analyticsHubEventAccumulator1 =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 1)
                .start()
        val event1 = AnalyticsEvent.builder()
            .name("Amplify-event" + UUID.randomUUID().toString())
            .addProperty("DemoProperty1", "DemoValue1")
            .addProperty("DemoDoubleProperty2", 2.0)
            .build()
        Amplify.Analytics.recordEvent(event1)
        //Autosubmitter runs every 10 secs, so waiting for 15sec to receive the hub event
        val hubEvents1 = analyticsHubEventAccumulator1.await(EVENT_FLUSH_TIMEOUT_WAIT, TimeUnit.SECONDS)
        Assert.assertEquals(1, hubEvents1.size.toLong())
        val analyticsHubEventAccumulator2 =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 1)
                .start()
        val event2 = AnalyticsEvent.builder()
            .name("Amplify-event" + UUID.randomUUID().toString())
            .addProperty("DemoProperty1", "DemoValue1")
            .addProperty("DemoProperty2", 2.0)
            .build()
        Amplify.Analytics.recordEvent(event2)
        //Autosubmitter runs every 10 secs, so waiting for 15sec to receive the hub event
        val hubEvents2 = analyticsHubEventAccumulator1.await(EVENT_FLUSH_TIMEOUT_WAIT, TimeUnit.SECONDS)
        Assert.assertEquals(1, hubEvents2.size.toLong())
    }

    /**
     * Registers a global property and ensures that all recorded events have the global property.
     */
    @Test
    fun registerGlobalPropertiesAddsGivenPropertiesToRecordedEvents() {
        val analyticsHubEventAccumulator =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 1)
                .start()
        // Arrange: Register global properties and create an event
        Amplify.Analytics.registerGlobalProperties(
            AnalyticsProperties.builder()
                .add("GlobalProperty", "GlobalValue")
                .build()
        )
        val event = AnalyticsEvent.builder()
            .name("Amplify-event" + UUID.randomUUID().toString())
            .addProperty("LocalProperty", "LocalValue")
            .build()

        // Act: Record two events: the one created above and another just with a key
        Amplify.Analytics.recordEvent(event)
        Sleep.milliseconds(RECORD_INSERTION_TIMEOUT)
        Amplify.Analytics.recordEvent("amplify-test-event")
        Sleep.milliseconds(RECORD_INSERTION_TIMEOUT)
        Amplify.Analytics.flushEvents()
        val hubEvents = analyticsHubEventAccumulator.await(10, TimeUnit.SECONDS)
        val submittedEvents = (hubEvents[0].data as ArrayList<AnalyticsEvent>?)!!
        // TODO: still flaky here due to session starting mid-test
        Assert.assertEquals(2, submittedEvents.size.toLong())
        val event1Attributes = submittedEvents[0].properties
        val event2Attributes = submittedEvents[1].properties

        // Global properties are attached to all events
        Assert.assertEquals("GlobalValue", event1Attributes["GlobalProperty"].value)
        Assert.assertEquals("GlobalValue", event2Attributes["GlobalProperty"].value)

        // Local properties are only attached is passed explicitly as in event
        Assert.assertEquals("LocalValue", event1Attributes["LocalProperty"].value)
    }

    /**
     * Registers a global property and then Unregisters the global property
     * and ensures that it is respected in events recorded thereafter.
     *
     */
    @Test
    fun unregisterGlobalPropertiesRemovesGivenProperties() {
        val analyticsHubEventAccumulator =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 1)
                .start()
        // Arrange: Register a global property
        Amplify.Analytics.registerGlobalProperties(
            AnalyticsProperties.builder()
                .add("GlobalProperty", "globalVal")
                .build()
        )

        // Act: Record an event, unregister the global property, and record another event
        Amplify.Analytics.recordEvent("amplify-test-event-with-property")
        Sleep.milliseconds(RECORD_INSERTION_TIMEOUT)
        Amplify.Analytics.unregisterGlobalProperties("GlobalProperty")
        Amplify.Analytics.recordEvent("amplify-test-event-without-property")
        Sleep.milliseconds(RECORD_INSERTION_TIMEOUT)
        Amplify.Analytics.flushEvents()
        val hubEvents = analyticsHubEventAccumulator.await(10, TimeUnit.SECONDS)
        val submittedEvents = hubEvents[0].data as ArrayList<AnalyticsEvent>?

        // Assert: Ensure there are two events, the first has attributes, and the second doesn't
        Assert.assertEquals("globalVal", submittedEvents!![0].properties["GlobalProperty"].value)
        Assert.assertEquals(0, submittedEvents[1].properties.size().toLong())
    }

    companion object {
        private const val EVENT_FLUSH_TIMEOUT_WAIT = 15 /* seconds */
        private const val CREDENTIALS_RESOURCE_NAME = "credentials"
        private const val COGNITO_CONFIGURATION_TIMEOUT = 10 * 1000L
        private const val RECORD_INSERTION_TIMEOUT = 3 * 1000L
        private lateinit var synchronousAuth: SynchronousAuth
        @BeforeClass
        @JvmStatic
        fun setupBefore() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            Amplify.Auth.addPlugin(AWSCognitoAuthPlugin() as AuthPlugin<*>)
            Amplify.addPlugin(AWSPinpointAnalyticsPlugin())
            Amplify.configure(context)
            Sleep.milliseconds(COGNITO_CONFIGURATION_TIMEOUT)
            synchronousAuth = SynchronousAuth.delegatingTo(Amplify.Auth)
        }

        private fun readCredentialsFromResource(context: Context, @RawRes resourceId: Int): Pair<String, String>? {
            val resource = Resources.readAsJson(context, resourceId)
            var userCredentials: Pair<String, String>? = null
            return try {
                val credentials = resource.getJSONArray("credentials")
                for (index in 0 until credentials.length()) {
                    val credential = credentials.getJSONObject(index)
                    val username = credential.getString("username")
                    val password = credential.getString("password")
                    userCredentials = Pair(username, password)
                }
                userCredentials
            } catch (jsonReadingFailure: JSONException) {
                throw RuntimeException(jsonReadingFailure)
            }
        }
    }
}