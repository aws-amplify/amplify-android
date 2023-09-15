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

import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.analytics.AnalyticsEvent
import com.amplifyframework.analytics.AnalyticsProperties
import com.amplifyframework.analytics.pinpoint.models.AWSPinpointUserProfile
import com.amplifyframework.pinpoint.core.AnalyticsClient
import com.amplifyframework.pinpoint.core.TargetingClient
import com.amplifyframework.pinpoint.core.data.AndroidAppDetails
import com.amplifyframework.pinpoint.core.data.AndroidDeviceDetails
import com.amplifyframework.pinpoint.core.endpointProfile.EndpointProfile
import com.amplifyframework.pinpoint.core.models.PinpointEvent
import com.amplifyframework.pinpoint.core.models.PinpointSession
import com.amplifyframework.pinpoint.core.models.SDKInfo
import com.amplifyframework.pinpoint.core.util.getUniqueId
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AWSPinpointAnalyticsPluginBehaviorTest {

    private val analyticsClientMock = mockk<AnalyticsClient>(relaxed = true)
    private val targetingClientMock = mockk<TargetingClient>(relaxed = true)
    private lateinit var awsPinpointAnalyticsPluginBehavior: AWSPinpointAnalyticsPluginBehavior
    private val sharedPrefs = mockk<SharedPreferences>()
    private val androidAppDetails = AndroidAppDetails("com.test.app", "TestApp", "com.test.app", "1.0", "test")
    private val androidDeviceDetails = AndroidDeviceDetails("test")

    @Before
    fun setup() {
        mockkStatic("com.amplifyframework.pinpoint.core.util.SharedPreferencesUtilKt")
        every { sharedPrefs.getUniqueId() }.answers { "UNIQUE_ID" }

        awsPinpointAnalyticsPluginBehavior = AWSPinpointAnalyticsPluginBehavior(
            analyticsClientMock,
            targetingClientMock,
        )
    }

    @Test
    fun `record event using event name`() {
        val testEventType = "TestEvent"
        val expectedPinpointEvent = PinpointEvent(
            eventType = testEventType,
            attributes = emptyMap(),
            metrics = emptyMap(),
            sdkInfo = mockk<SDKInfo>(),
            pinpointSession = mockk<PinpointSession>(),
            eventTimestamp = System.currentTimeMillis(),
            uniqueId = UUID.randomUUID().toString(),
            androidAppDetails = mockk<AndroidAppDetails>(),
            androidDeviceDetails = mockk<AndroidDeviceDetails>()
        )

        every {
            analyticsClientMock.createEvent(
                testEventType,
                mutableMapOf(),
                any(),
                any(),
                any()
            )
        } answers { expectedPinpointEvent }

        awsPinpointAnalyticsPluginBehavior.recordEvent(testEventType)

        verify(exactly = 1) { analyticsClientMock.recordEvent(expectedPinpointEvent) }
    }

    @Test
    fun `test record event with metrics & attributes`() {
        val testEventType = "TestEvent"
        val testAnalyticsEventBehavior =
            AnalyticsEvent.builder()
                .name(testEventType)
                .addProperty("key1", "value1")
                .addProperty("key2", 2.0)
                .addProperty("key3", true)
                .addProperty("key4", 1)
                .build()
        val expectedAttributes = mutableMapOf<String, String>("key1" to "value1", "key3" to "true")
        val expectedMetrics = mutableMapOf<String, Double>("key2" to 2.0, "key4" to 1.toDouble())
        val expectedPinpointEvent = PinpointEvent(
            eventType = testEventType,
            attributes = expectedAttributes,
            metrics = expectedMetrics,
            sdkInfo = mockk<SDKInfo>(),
            pinpointSession = mockk<PinpointSession>(),
            eventTimestamp = System.currentTimeMillis(),
            uniqueId = UUID.randomUUID().toString(),
            androidAppDetails = mockk<AndroidAppDetails>(),
            androidDeviceDetails = mockk<AndroidDeviceDetails>()
        )

        every {
            analyticsClientMock.createEvent(
                testEventType,
                expectedAttributes,
                expectedMetrics,
                any(),
                any()
            )
        } answers { expectedPinpointEvent }

        awsPinpointAnalyticsPluginBehavior.recordEvent(testAnalyticsEventBehavior)

        verify(exactly = 1) { analyticsClientMock.recordEvent(expectedPinpointEvent) }
    }

    @Test
    fun `test enable()`() {
        awsPinpointAnalyticsPluginBehavior.enable()
        verify(exactly = 1) { analyticsClientMock.enableEventSubmitter() }
    }

    @Test
    fun `test disable()`() {
        awsPinpointAnalyticsPluginBehavior.disable()
        verify(exactly = 1) { analyticsClientMock.disableEventSubmitter() }
    }

    @Test
    fun `test identify user`() {
        every { targetingClientMock.currentEndpoint() }.answers {
            EndpointProfile(
                sharedPrefs.getUniqueId(),
                androidAppDetails,
                androidDeviceDetails,
                ApplicationProvider.getApplicationContext(),
                mockk()
            )
        }
        val actualEndpoint = slot<EndpointProfile>()
        every { targetingClientMock.identifyUser(any(), any()) } answers { callOriginal() }
        every { targetingClientMock.updateEndpointProfile(capture(actualEndpoint)) } returns Unit

        val properties =
            AnalyticsProperties.builder()
                .add("key1", "String1")
                .add("key2", 1.0)
                .add("key3", true)
                .add("key4", 1)
                .build()
        val userProfile = AWSPinpointUserProfile.builder()
            .email("test@test.com")
            .name("test")
            .userAttributes(properties)
            .build()
        awsPinpointAnalyticsPluginBehavior.identifyUser(
            "USER_ID",
            userProfile
        )
        val expectedUserAttributes =
            mapOf("key1" to listOf("String1"), "key2" to listOf("1.0"), "key3" to listOf("true"), "key4" to listOf("1"))
        val expectedEndpointAttributes =
            mapOf("email" to listOf("test@test.com"), "name" to listOf("test"), "plan" to listOf())

        verify(exactly = 1) { targetingClientMock.identifyUser("USER_ID", userProfile) }
        verify(exactly = 1) { targetingClientMock.updateEndpointProfile(any()) }

        assertEquals(actualEndpoint.captured.user.userAttributes, expectedUserAttributes)
        assertEquals(actualEndpoint.captured.allAttributes, expectedEndpointAttributes)
        assertEquals(actualEndpoint.captured.user.userId, "USER_ID")
    }

    @Test
    fun `test registerGlobalProperties`() {
        val properties = AnalyticsProperties.builder()
            .add("key1", "String")
            .add("key2", true)
            .add("key3", 1.0)
            .add("key4", 1)
            .build()
        awsPinpointAnalyticsPluginBehavior.registerGlobalProperties(properties)
        verify(exactly = 1) { analyticsClientMock.addGlobalAttribute("key1", "String") }
        verify(exactly = 1) { analyticsClientMock.addGlobalAttribute("key2", "true") }
        verify(exactly = 1) { analyticsClientMock.addGlobalMetric("key3", 1.0) }
        verify(exactly = 1) { analyticsClientMock.addGlobalMetric("key4", "1".toDouble()) }
    }

    @Test
    fun `test unregisterGlobalProperties`() {
        val propertyName = "key"
        awsPinpointAnalyticsPluginBehavior.unregisterGlobalProperties(propertyName)
        verify(exactly = 1) { analyticsClientMock.removeGlobalAttribute("key") }
        verify(exactly = 1) { analyticsClientMock.removeGlobalMetric("key") }
    }
}
