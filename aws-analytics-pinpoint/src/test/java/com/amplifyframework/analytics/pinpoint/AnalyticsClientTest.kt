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

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import com.amplifyframework.analytics.pinpoint.database.PinpointDatabase
import com.amplifyframework.analytics.pinpoint.internal.core.idresolver.SharedPrefsUniqueIdService
import com.amplifyframework.analytics.pinpoint.models.AndroidAppDetails
import com.amplifyframework.analytics.pinpoint.models.AndroidDeviceDetails
import com.amplifyframework.analytics.pinpoint.models.PinpointEvent
import com.amplifyframework.analytics.pinpoint.models.PinpointSession
import com.amplifyframework.analytics.pinpoint.models.SDKInfo
import com.amplifyframework.analytics.pinpoint.targeting.TargetingClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AnalyticsClientTest {

    private val pinpointClient = mockk<PinpointClient>()
    private val sessionClient = mockk<SessionClient>()
    private val sharedPrefsUniqueIdService = mockk<SharedPrefsUniqueIdService>()
    private val androidAppDetails = AndroidAppDetails("com.test.app", "TestApp", "com.test.app", "1.0", "test")
    private val androidDeviceDetails = AndroidDeviceDetails("test")
    private val sdkInfo = SDKInfo("test", "1.0")
    private val pinpointDatabase = mockk<PinpointDatabase>()
    private val eventRecorder = mockk<EventRecorder>()
    private val targetingClient = mockk<TargetingClient>()
    private lateinit var analyticsClient: AnalyticsClient

    @Before
    fun setup() = runTest {
        analyticsClient = AnalyticsClient(
            ApplicationProvider.getApplicationContext(),
            pinpointClient,
            sessionClient,
            targetingClient,
            pinpointDatabase,
            sharedPrefsUniqueIdService,
            androidAppDetails,
            androidDeviceDetails,
            sdkInfo,
            UnconfinedTestDispatcher(testScheduler),
            eventRecorder
        )
        val sessionId = UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()
        every { sessionClient.session } answers { Session(sessionId, startTime, startTime) }
        every { sharedPrefsUniqueIdService.getUniqueId() } answers { "UNIQUE_ID" }
        coEvery { eventRecorder.recordEvent(any()) } answers { Uri.EMPTY }
    }

    @Test
    fun `create event with just eventType`() {
        val testEventId = UUID.randomUUID().toString()
        val testEventType = "TestEvent"
        val eventTimestamp = System.currentTimeMillis()
        val expectedPinpointEvent = PinpointEvent(
            eventId = testEventId,
            eventType = testEventType,
            attributes = mutableMapOf(),
            metrics = mutableMapOf(),
            sdkInfo = sdkInfo,
            pinpointSession = PinpointSession(
                sessionClient.session!!.sessionId,
                sessionClient.session!!.startTime,
                sessionClient.session!!.stopTime,
                0L
            ),
            eventTimestamp = eventTimestamp,
            uniqueId = "UNIQUE_ID",
            androidDeviceDetails = androidDeviceDetails,
            androidAppDetails = androidAppDetails
        )
        val actualPinpointEvent =
            analyticsClient.createEvent(testEventType, eventTimestamp = eventTimestamp, eventId = testEventId)

        assertEquals(expectedPinpointEvent.toJsonString(), actualPinpointEvent.toJsonString())
    }

    @Test
    fun `test record events`() = runTest {
        val testEventId = UUID.randomUUID().toString()
        val testEventType = "TestEvent"
        val eventTimestamp = System.currentTimeMillis()
        val pinpointEvent = PinpointEvent(
            eventId = testEventId,
            eventType = testEventType,
            attributes = mutableMapOf(),
            metrics = mutableMapOf(),
            sdkInfo = sdkInfo,
            pinpointSession = PinpointSession(
                sessionClient.session!!.sessionId,
                sessionClient.session!!.startTime,
                sessionClient.session!!.stopTime,
                0L
            ),
            eventTimestamp = eventTimestamp,
            uniqueId = "",
            androidDeviceDetails = androidDeviceDetails,
            androidAppDetails = androidAppDetails
        )
        analyticsClient.recordEvent(pinpointEvent)
        coVerify(exactly = 1) { eventRecorder.recordEvent(pinpointEvent) }
    }

    @Test
    fun `test submitEvents`() = runTest {
        coEvery { eventRecorder.submitEvents() } returns listOf()
        analyticsClient.flushEvents()
        coVerify(exactly = 1) { eventRecorder.submitEvents() }
    }

    @Test
    fun `test addGlobalAttributes`() {
        val attributeName = "attributeName"
        val attributeValue = "attributeValue"
        analyticsClient.addGlobalAttribute(attributeName, attributeValue)
        assertEquals(analyticsClient.getGlobalAttributes()[attributeName], "attributeValue")
    }

    @Test
    fun `test addGlobalMetrics`() {
        val metricName = "attributeName"
        val metricValue = 1.0
        analyticsClient.addGlobalMetric(metricName, metricValue)
        assertEquals(metricValue, analyticsClient.getGlobalMetrics()[metricName])
    }
}
