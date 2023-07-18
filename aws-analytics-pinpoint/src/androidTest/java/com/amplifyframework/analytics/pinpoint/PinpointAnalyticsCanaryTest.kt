/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

class PinpointAnalyticsCanaryTest {
    companion object {
        private const val CREDENTIALS_RESOURCE_NAME = "credentials"
        private const val CONFIGURATION_NAME = "amplifyconfiguration"
        private const val COGNITO_CONFIGURATION_TIMEOUT = 5 * 1000L
        private const val PINPOINT_ROUNDTRIP_TIMEOUT = 10 * 1000L
        private const val TIMEOUT_S = 20
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
        hubAccumulator.await(10, TimeUnit.SECONDS)
        pinpointClient = Amplify.Analytics.getPlugin("awsPinpointAnalyticsPlugin").escapeHatch as
            PinpointClient
        uniqueId = preferences.getString(UNIQUE_ID_KEY, "error-no-unique-id")!!
        Assert.assertNotEquals(uniqueId, "error-no-unique-id")
    }

    @Test
    fun recordEvent_flushEvent() {
        val hubAccumulator =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 2).start()
        val eventName = "Amplify-event" + UUID.randomUUID().toString()
        val event = AnalyticsEvent.builder()
            .name(eventName)
            .addProperty("AnalyticsStringProperty", "Pancakes")
            .addProperty("AnalyticsBooleanProperty", true)
            .addProperty("AnalyticsDoubleProperty", 3.14)
            .addProperty("AnalyticsIntegerProperty", 42)
            .build()

        Amplify.Analytics.recordEvent(event)
        Amplify.Analytics.flushEvents()
        val hubEvents = hubAccumulator.await(TIMEOUT_S, TimeUnit.SECONDS)
        val submittedEvents = combineAndFilterEvents(hubEvents)
        Assert.assertEquals(1, submittedEvents.size.toLong())
    }

    @Test
    fun registerGlobalProperties() {
        val hubAccumulator =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 2).start()
        val eventName = "Amplify-event" + UUID.randomUUID().toString()
        val event = AnalyticsEvent.builder()
            .name(eventName)
            .addProperty("AnalyticsStringProperty", "Pancakes")
            .addProperty("AnalyticsBooleanProperty", true)
            .addProperty("AnalyticsDoubleProperty", 3.14)
            .addProperty("AnalyticsIntegerProperty", 42)
            .build()

        Amplify.Analytics.recordEvent(event)
        Amplify.Analytics.registerGlobalProperties(
            AnalyticsProperties.builder()
                .add("AppStyle", "DarkMode")
                .build()
        )
        Amplify.Analytics.flushEvents()
        val hubEvents = hubAccumulator.await(TIMEOUT_S, TimeUnit.SECONDS)
        val submittedEvents = combineAndFilterEvents(hubEvents)
        Assert.assertEquals(1, submittedEvents.size.toLong())
    }

    @Test
    fun unregisterGlobalProperties() {
        val hubAccumulator =
            HubAccumulator.create(HubChannel.ANALYTICS, AnalyticsChannelEventName.FLUSH_EVENTS, 2).start()
        val eventName = "Amplify-event" + UUID.randomUUID().toString()
        val event = AnalyticsEvent.builder()
            .name(eventName)
            .addProperty("AnalyticsStringProperty", "Pancakes")
            .addProperty("AnalyticsBooleanProperty", true)
            .addProperty("AnalyticsDoubleProperty", 3.14)
            .addProperty("AnalyticsIntegerProperty", 42)
            .build()

        Amplify.Analytics.recordEvent(event)
        Amplify.Analytics.unregisterGlobalProperties("AppStyle")
        Amplify.Analytics.flushEvents()
        val hubEvents = hubAccumulator.await(TIMEOUT_S, TimeUnit.SECONDS)
        val submittedEvents = combineAndFilterEvents(hubEvents)
        Assert.assertEquals(1, submittedEvents.size.toLong())
    }

    @Test
    fun identifyUser() {
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
            if ((it.data as List<*>).isNotEmpty()) {
                (it.data as ArrayList<*>).forEach { event ->
                    if (!(event as AnalyticsEvent).name.startsWith("_session")) {
                        result.add(event)
                    }
                }
            }
        }
        return result
    }

    private fun assertCommonEndpointResponseProperties(endpointResponse: EndpointResponse) {
        val attributes = endpointResponse.attributes!!
        Assert.assertEquals("user@test.com", attributes["email"]!![0])
        Assert.assertEquals("test-user", attributes["name"]!![0])
        Assert.assertEquals("test-plan", attributes["plan"]!![0])
        val endpointProfileLocation: EndpointLocation = endpointResponse.location!!
        Assert.assertEquals(47.6154086, endpointProfileLocation.latitude ?: 0.0, 0.1)
        Assert.assertEquals((-122.3349685), endpointProfileLocation.longitude ?: 0.0, 0.1)
        Assert.assertEquals("98122", endpointProfileLocation.postalCode)
        Assert.assertEquals("Seattle", endpointProfileLocation.city)
        Assert.assertEquals("WA", endpointProfileLocation.region)
        Assert.assertEquals("USA", endpointProfileLocation.country)
        Assert.assertEquals("TestStringValue", attributes["TestStringProperty"]!![0])
        Assert.assertEquals(1.0, endpointResponse.metrics!!["TestDoubleProperty"]!!, 0.1)
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
}
