package com.amplifyframework.analytics.pinpoint

import androidx.test.core.app.ApplicationProvider
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import com.amplifyframework.analytics.pinpoint.database.PinpointDatabase
import com.amplifyframework.analytics.pinpoint.models.AndroidAppDetails
import com.amplifyframework.analytics.pinpoint.models.AndroidDeviceDetails
import com.amplifyframework.analytics.pinpoint.models.PinpointEvent
import com.amplifyframework.analytics.pinpoint.models.PinpointSession
import com.amplifyframework.analytics.pinpoint.models.SDKInfo
import com.amplifyframework.analytics.pinpoint.targeting.TargetingClient
import io.mockk.coVerify
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
@RunWith(RobolectricTestRunner::class)
class EventRecorderTest {

    private val pinpointClient = mockk<PinpointClient>(relaxed = true)
    private val pinpointDatabaseMock = mockk<PinpointDatabase>(relaxed = true)
    private val targetingClient = mockk<TargetingClient>(relaxed = true)
    private lateinit var eventRecorder: EventRecorder
    private lateinit var coroutineDispatcher: CoroutineDispatcher

    @Before
    fun setup() = runTest {
        coroutineDispatcher = UnconfinedTestDispatcher(testScheduler)
        eventRecorder = EventRecorder(
            ApplicationProvider.getApplicationContext(),
            pinpointClient,
            pinpointDatabaseMock,
            targetingClient,
            coroutineDispatcher = coroutineDispatcher
        )
    }

    @Test
    fun `test record event`() = runTest {
        val testEventType = "TestEvent"
        val pinpointEvent = PinpointEvent(
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
        eventRecorder.recordEvent(pinpointEvent)
        coVerify(exactly = 1) { pinpointDatabaseMock.saveEvent(pinpointEvent) }
    }
}
