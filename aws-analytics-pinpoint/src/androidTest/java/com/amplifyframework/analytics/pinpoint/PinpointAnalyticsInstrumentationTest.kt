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
import android.content.SharedPreferences
import android.util.Log
import android.util.Pair
import androidx.annotation.RawRes
import androidx.test.core.app.ApplicationProvider
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import aws.sdk.kotlin.services.pinpoint.model.EndpointLocation
import aws.sdk.kotlin.services.pinpoint.model.EndpointResponse
import aws.sdk.kotlin.services.pinpoint.model.GetEndpointRequest
import com.amplifyframework.analytics.AnalyticsEvent
import com.amplifyframework.analytics.AnalyticsProperties
import com.amplifyframework.analytics.UserProfile
import com.amplifyframework.analytics.pinpoint.models.AWSPinpointUserProfile
import com.amplifyframework.auth.AuthPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import com.amplifyframework.logging.AndroidLoggingPlugin
import com.amplifyframework.logging.LogLevel
import com.amplifyframework.pinpoint.core.endpointProfile.EndpointProfile
import com.amplifyframework.testutils.HubAccumulator
import com.amplifyframework.testutils.Resources
import com.amplifyframework.testutils.Sleep
import com.amplifyframework.testutils.sync.SynchronousAuth
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.json.JSONException
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class PinpointAnalyticsInstrumentationTest {
    @Before
    fun flushEvents() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @RawRes val resourceId = Resources.getRawResourceId(context, CREDENTIALS_RESOURCE_NAME)
        val userAndPasswordPair = readCredentialsFromResource(context, resourceId)
        synchronousAuth.signOut()
        synchronousAuth.signIn(
            userAndPasswordPair!!.first,
            userAndPasswordPair.second
        )
        val hubAccumulator =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 1).start()
        Amplify.Analytics.flushEvents()
        val events = hubAccumulator.await(10, TimeUnit.SECONDS)
        pinpointClient = Amplify.Analytics.getPlugin("awsPinpointAnalyticsPlugin").escapeHatch as
            PinpointClient
        uniqueId = preferences.getString(UNIQUE_ID_KEY, "error-no-unique-id")!!
        Assert.assertNotEquals(uniqueId, "error-no-unique-id")
    }

    /**
     * Record a basic analytics event and verify that it has been recorded using Analytics
     * pinpoint client.
     */
    @Test
    fun recordEventStoresPassedBasicAnalyticsEvent() {
        val hubAccumulator =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 2).start()
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
        Sleep.milliseconds(DEFAULT_TIMEOUT)
        Amplify.Analytics.flushEvents()
        val hubEvents = hubAccumulator.await(10, TimeUnit.SECONDS)
        val submittedEvents = combineAndFilterEvents(hubEvents)
        Assert.assertEquals(1, submittedEvents.size.toLong())
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
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 3)
                .start()
        val eventName1 = "Amplify-event" + UUID.randomUUID().toString()
        val event1 = AnalyticsEvent.builder()
            .name(eventName1)
            .addProperty("DemoProperty1", "DemoValue1")
            .addProperty("DemoDoubleProperty2", 2.0)
            .build()
        Amplify.Analytics.recordEvent(event1)
        Sleep.milliseconds(DEFAULT_TIMEOUT)
        val eventName2 = "Amplify-event" + UUID.randomUUID().toString()
        val event2 = AnalyticsEvent.builder()
            .name(eventName2)
            .addProperty("DemoProperty1", "DemoValue1")
            .addProperty("DemoProperty2", 2.0)
            .build()
        Amplify.Analytics.recordEvent(event2)
        Sleep.milliseconds(DEFAULT_TIMEOUT)
        Amplify.Analytics.flushEvents()
        val hubEvents = analyticsHubEventAccumulator.await(10, TimeUnit.SECONDS)
        val hubEvent = analyticsHubEventAccumulator.awaitFirst()
        val submittedEvents = combineAndFilterEvents(hubEvents)
        Assert.assertEquals(2, submittedEvents.size.toLong())
        Assert.assertEquals(AnalyticsChannelEventName.FLUSH_EVENTS.eventName, hubEvent.name)
        Assert.assertEquals(eventName1, submittedEvents[0].name)
        Assert.assertEquals(eventName2, submittedEvents[1].name)
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
        // Autosubmitter runs every 10 secs, so waiting for 15sec to receive the hub event
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
        // Autosubmitter runs every 10 secs, so waiting for 15sec to receive the hub event
        val hubEvents2 = analyticsHubEventAccumulator1.await(EVENT_FLUSH_TIMEOUT_WAIT, TimeUnit.SECONDS)
        Assert.assertEquals(1, hubEvents2.size.toLong())
    }

    /**
     * Registers a global property and ensures that all recorded events have the global property.
     */
    @Test
    fun registerGlobalPropertiesAddsGivenPropertiesToRecordedEvents() {
        val analyticsHubEventAccumulator =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 3)
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
        Sleep.milliseconds(DEFAULT_TIMEOUT)
        Amplify.Analytics.recordEvent("amplify-test-event")
        Sleep.milliseconds(DEFAULT_TIMEOUT)
        Amplify.Analytics.flushEvents()
        val hubEvents = analyticsHubEventAccumulator.await(10, TimeUnit.SECONDS)
        val submittedEvents = combineAndFilterEvents(hubEvents)
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
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 3)
                .start()
        // Arrange: Register a global property
        Amplify.Analytics.registerGlobalProperties(
            AnalyticsProperties.builder()
                .add("GlobalProperty", "globalVal")
                .build()
        )

        // Act: Record an event, unregister the global property, and record another event
        Amplify.Analytics.recordEvent("amplify-test-event-with-property")
        Sleep.milliseconds(DEFAULT_TIMEOUT)
        Amplify.Analytics.unregisterGlobalProperties("GlobalProperty")
        Amplify.Analytics.recordEvent("amplify-test-event-without-property")
        Sleep.milliseconds(DEFAULT_TIMEOUT)
        Amplify.Analytics.flushEvents()
        val hubEvents = analyticsHubEventAccumulator.await(10, TimeUnit.SECONDS)
        val submittedEvents = combineAndFilterEvents(hubEvents)

        // Assert: Ensure there are two events, the first has attributes, and the second doesn't
        Assert.assertEquals(2, submittedEvents.size.toLong())
        Assert.assertEquals("globalVal", submittedEvents[0].properties["GlobalProperty"].value)
        Assert.assertEquals(0, submittedEvents[1].properties.size().toLong())
    }

    /**
     * The [AWSPinpointAnalyticsPlugin.identifyUser] method should set
     * an [EndpointProfile] on the [PinpointClient], containing
     * all provided Amplify attributes.
     */
    @Test
    fun testIdentifyUserWithDefaultProfile() {
        val location = testLocation
        val properties = endpointProperties
        val userProfile = AWSPinpointUserProfile.builder()
            .name("test-user")
            .email("user@test.com")
            .plan("test-plan")
            .location(location)
            .customProperties(properties)
            .build()
        Amplify.Analytics.identifyUser(UUID.randomUUID().toString(), userProfile)
        Sleep.milliseconds(PINPOINT_ROUNDTRIP_TIMEOUT)
        val endpointResponse = fetchEndpointResponse()
        assertCommonEndpointResponseProperties(endpointResponse)
        assert(null == endpointResponse.user!!.userAttributes)
    }

    /**
     * [AWSPinpointUserProfile] extends [UserProfile] to include
     * [AWSPinpointUserProfile.userAttributes] which is specific to Pinpoint. This test is very
     * similar to testIdentifyUserWithDefaultProfile, but it adds user attributes in addition
     * to the endpoint attributes.
     */
    @Test
    fun testIdentifyUserWithUserAttributes() {
        val location = testLocation
        val properties = endpointProperties
        val userAttributes = userAttributes
        val pinpointUserProfile = AWSPinpointUserProfile.builder()
            .name("test-user")
            .email("user@test.com")
            .plan("test-plan")
            .location(location)
            .customProperties(properties)
            .userAttributes(userAttributes)
            .build()
        val uuid = UUID.randomUUID().toString()
        Amplify.Analytics.identifyUser(uuid, pinpointUserProfile)
        Sleep.milliseconds(PINPOINT_ROUNDTRIP_TIMEOUT)
        var endpointResponse = fetchEndpointResponse()
        var retry_count = 0
        while (retry_count < MAX_RETRIES && endpointResponse.attributes.isNullOrEmpty()) {
            Sleep.milliseconds(DEFAULT_TIMEOUT)
            endpointResponse = fetchEndpointResponse()
            retry_count++
        }
        assertCommonEndpointResponseProperties(endpointResponse)
        Assert.assertEquals(
            "User attribute value",
            endpointResponse.user!!
                .userAttributes!!
            ["SomeUserAttribute"]!!
            [0]
        )
    }

    private fun fetchEndpointResponse(): EndpointResponse {
        var endpointResponse: EndpointResponse? = null
        runBlocking {
            endpointResponse = pinpointClient.getEndpoint(
                GetEndpointRequest.invoke {
                    this.applicationId = appId
                    this.endpointId = uniqueId
                }
            ).endpointResponse
        }
        assert(null != endpointResponse)
        return endpointResponse!!
    }

    private fun assertCommonEndpointResponseProperties(endpointResponse: EndpointResponse) {
        Log.d("PinpointAnalyticsInstrumentationTest", endpointResponse.toString())
        val attributes = endpointResponse.attributes!!
        Assert.assertEquals("user@test.com", attributes["email"]!![0])
        Assert.assertEquals("test-user", attributes["name"]!![0])
        Assert.assertEquals("test-plan", attributes["plan"]!![0])
        Assert.assertEquals("TestStringValue", attributes["TestStringProperty"]!![0])
        val endpointProfileLocation: EndpointLocation = endpointResponse.location!!
        Assert.assertEquals(47.6154086, endpointProfileLocation.latitude!!, 0.1)
        Assert.assertEquals((-122.3349685), endpointProfileLocation.longitude!!, 0.1)
        Assert.assertEquals("98122", endpointProfileLocation.postalCode)
        Assert.assertEquals("Seattle", endpointProfileLocation.city)
        Assert.assertEquals("WA", endpointProfileLocation.region)
        Assert.assertEquals("USA", endpointProfileLocation.country)
        Assert.assertEquals(1.0, endpointResponse.metrics!!["TestDoubleProperty"]!!, 0.1)
    }

    private val userAttributes: AnalyticsProperties
        get() = AnalyticsProperties.builder()
            .add("SomeUserAttribute", "User attribute value")
            .build()
    private val endpointProperties: AnalyticsProperties
        get() {
            return AnalyticsProperties.builder()
                .add("TestStringProperty", "TestStringValue")
                .add("TestDoubleProperty", 1.0)
                .build()
        }
    private val testLocation: UserProfile.Location
        get() {
            return UserProfile.Location.builder()
                .latitude(47.6154086)
                .longitude(-122.3349685)
                .postalCode("98122")
                .city("Seattle")
                .region("WA")
                .country("USA")
                .build()
        }

    private fun combineAndFilterEvents(hubEvents: List<HubEvent<*>>): MutableList<AnalyticsEvent> {
        val result = mutableListOf<AnalyticsEvent>()
        hubEvents.forEach {
            if ((it.data as List<*>).size != 0) {
                (it.data as ArrayList<*>).forEach { event ->
                    if (!(event as AnalyticsEvent).name.startsWith("_session")) {
                        result.add(event)
                    }
                }
            }
        }
        return result
    }

    companion object {
        private const val EVENT_FLUSH_TIMEOUT_WAIT = 15 /* seconds */
        private const val CREDENTIALS_RESOURCE_NAME = "credentials"
        private const val CONFIGURATION_NAME = "amplifyconfiguration"
        private const val COGNITO_CONFIGURATION_TIMEOUT = 10 * 1000L
        private const val PINPOINT_ROUNDTRIP_TIMEOUT = 10 * 1000L
        private const val DEFAULT_TIMEOUT = 5 * 1000L
        private const val MAX_RETRIES = 10
        private const val UNIQUE_ID_KEY = "UniqueId"
        private const val PREFERENCES_AND_FILE_MANAGER_SUFFIX = "515d6767-01b7-49e5-8273-c8d11b0f331d"
        private lateinit var synchronousAuth: SynchronousAuth
        private lateinit var preferences: SharedPreferences
        private lateinit var appId: String
        private lateinit var uniqueId: String
        private lateinit var pinpointClient: PinpointClient
        @BeforeClass
        @JvmStatic
        fun setupBefore() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            @RawRes val resourceId = Resources.getRawResourceId(context, CONFIGURATION_NAME)
            appId = readAppIdFromResource(context, resourceId)
            preferences = context.getSharedPreferences(
                "${appId}$PREFERENCES_AND_FILE_MANAGER_SUFFIX",
                Context.MODE_PRIVATE
            )
            setUniqueId()
            Amplify.Auth.addPlugin(AWSCognitoAuthPlugin() as AuthPlugin<*>)
            Amplify.addPlugin(AWSPinpointAnalyticsPlugin())
            Amplify.Logging.addPlugin(AndroidLoggingPlugin(LogLevel.DEBUG))
            Amplify.configure(context)
            Sleep.milliseconds(COGNITO_CONFIGURATION_TIMEOUT)
            synchronousAuth = SynchronousAuth.delegatingTo(Amplify.Auth)
        }

        private fun setUniqueId() {
            uniqueId = UUID.randomUUID().toString()
            preferences.edit().putString(UNIQUE_ID_KEY, uniqueId).commit()
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

        private fun readAppIdFromResource(context: Context, @RawRes resourceId: Int): String {
            val resource = Resources.readAsJson(context, resourceId)
            return try {
                val analyticsJson = resource.getJSONObject("analytics")
                val pluginsJson = analyticsJson.getJSONObject("plugins")
                val pluginJson = pluginsJson.getJSONObject("awsPinpointAnalyticsPlugin")
                val pinpointJson = pluginJson.getJSONObject("pinpointAnalytics")
                pinpointJson.getString("appId")
            } catch (jsonReadingFailure: JSONException) {
                throw RuntimeException(jsonReadingFailure)
            }
        }
    }
}
