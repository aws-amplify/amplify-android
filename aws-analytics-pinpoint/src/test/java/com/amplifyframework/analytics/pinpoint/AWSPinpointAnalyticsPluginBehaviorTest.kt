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

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.analytics.AnalyticsEvent
import com.amplifyframework.analytics.pinpoint.models.AndroidAppDetails
import com.amplifyframework.analytics.pinpoint.models.AndroidDeviceDetails
import com.amplifyframework.analytics.pinpoint.models.PinpointEvent
import com.amplifyframework.analytics.pinpoint.models.PinpointSession
import com.amplifyframework.analytics.pinpoint.models.SDKInfo
import com.amplifyframework.analytics.pinpoint.targeting.TargetingClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AWSPinpointAnalyticsPluginBehaviorTest {

    private val analyticsClientMock = mockk<AnalyticsClient>(relaxed = true)
    private val targetingClientMock = mockk<TargetingClient>(relaxed = true)
    private val autoEventSubmitterMock = mockk<AutoEventSubmitter>(relaxed = true)
    private val autoSessionTrackerMock = mockk<AutoSessionTracker>(relaxed = true)
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var awsPinpointAnalyticsPluginBehavior: AWSPinpointAnalyticsPluginBehavior

    @Before
    fun setup() {
        awsPinpointAnalyticsPluginBehavior = AWSPinpointAnalyticsPluginBehavior(
            ApplicationProvider.getApplicationContext(),
            analyticsClientMock,
            targetingClientMock,
            autoEventSubmitterMock,
            autoSessionTrackerMock
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
        verify(exactly = 1) { autoEventSubmitterMock.start() }
        verify(exactly = 1) { autoSessionTrackerMock.startSessionTracking(context.applicationContext as Application) }
    }

    @Test
    fun `test disable()`() {
        awsPinpointAnalyticsPluginBehavior.disable()
        verify(exactly = 1) { autoEventSubmitterMock.stop() }
        verify(exactly = 1) { autoSessionTrackerMock.stopSessionTracking(context.applicationContext as Application) }
    }
}
