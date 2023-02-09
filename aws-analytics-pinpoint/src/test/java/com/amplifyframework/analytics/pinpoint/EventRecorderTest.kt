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

import android.database.MatrixCursor
import androidx.test.core.app.ApplicationProvider
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import aws.sdk.kotlin.services.pinpoint.model.EventItemResponse
import aws.sdk.kotlin.services.pinpoint.model.EventsResponse
import aws.sdk.kotlin.services.pinpoint.model.ItemResponse
import aws.sdk.kotlin.services.pinpoint.model.PutEventsRequest
import aws.sdk.kotlin.services.pinpoint.model.PutEventsResponse
import com.amplifyframework.analytics.pinpoint.database.EventTable
import com.amplifyframework.analytics.pinpoint.database.PinpointDatabase
import com.amplifyframework.analytics.pinpoint.models.AndroidAppDetails
import com.amplifyframework.analytics.pinpoint.models.AndroidDeviceDetails
import com.amplifyframework.analytics.pinpoint.models.PinpointEvent
import com.amplifyframework.analytics.pinpoint.models.PinpointSession
import com.amplifyframework.analytics.pinpoint.models.SDKInfo
import com.amplifyframework.analytics.pinpoint.targeting.TargetingClient
import com.amplifyframework.analytics.pinpoint.targeting.endpointProfile.EndpointProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EventRecorderTest {

    private val pinpointClient = mockk<PinpointClient>(relaxed = true)
    private val pinpointDatabaseMock = mockk<PinpointDatabase>(relaxed = true)
    private val targetingClient = mockk<TargetingClient>(relaxed = true)
    private val endpointProfile = mockk<EndpointProfile>(relaxed = true)
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

    @Test
    fun `test submit events`() = runTest {
        val pinpointEvent1 = getPinpointEvent("testEvent1")
        val pinpointEvent2 = getPinpointEvent("testEvent2")

        // setup database
        val pinpointEvents = listOf(
            arrayOf(1, pinpointEvent1.toJsonString().length, pinpointEvent1.toJsonString()),
            arrayOf(2, pinpointEvent2.toJsonString().length, pinpointEvent2.toJsonString())
        )
        val matrixCursor = MatrixCursor(arrayOf(EventTable.COLUMN_ID, EventTable.COLUMN_SIZE, EventTable.COLUMN_JSON))
        pinpointEvents.forEach {
            matrixCursor.addRow(it)
        }
        coEvery { pinpointDatabaseMock.queryAllEvents() }.answers { matrixCursor }

        // setup pinpoint client
        val endpointId = UUID.randomUUID().toString()
        coEvery { endpointProfile.endpointId }.answers { endpointId }
        coEvery { targetingClient.currentEndpoint() }.answers { endpointProfile }
        val itemResponse = ItemResponse {
            eventsItemResponse = mapOf(
                pinpointEvent1.eventId to EventItemResponse {
                    message = "Accepted"
                    statusCode = 202
                },
                pinpointEvent2.eventId to EventItemResponse {
                    message = "Accepted"
                    statusCode = 202
                }
            )
        }
        val putEventResponse = PutEventsResponse {
            eventsResponse = EventsResponse {
                results = mapOf(endpointId to itemResponse)
            }
        }
        coEvery { pinpointClient.putEvents(any<PutEventsRequest>()) }.answers { putEventResponse }

        eventRecorder.submitEvents()
        coVerifyOrder {
            pinpointDatabaseMock.deleteEventById(1)
            pinpointDatabaseMock.deleteEventById(2)
        }
    }

    @Test
    fun `test retryable errors`() = runTest {
        val pinpointEvent1 = getPinpointEvent("testEvent1")
        val pinpointEvent2 = getPinpointEvent("testEvent2")

        // setup database
        val pinpointEvents = listOf(
            arrayOf(1, pinpointEvent1.toJsonString().length, pinpointEvent1.toJsonString()),
            arrayOf(2, pinpointEvent2.toJsonString().length, pinpointEvent2.toJsonString())
        )
        val matrixCursor = MatrixCursor(arrayOf(EventTable.COLUMN_ID, EventTable.COLUMN_SIZE, EventTable.COLUMN_JSON))
        pinpointEvents.forEach {
            matrixCursor.addRow(it)
        }
        coEvery { pinpointDatabaseMock.queryAllEvents() }.answers { matrixCursor }

        // setup pinpoint client
        val endpointId = UUID.randomUUID().toString()
        coEvery { endpointProfile.endpointId }.answers { endpointId }
        coEvery { targetingClient.currentEndpoint() }.answers { endpointProfile }
        val itemResponse = ItemResponse {
            eventsItemResponse = mapOf(
                pinpointEvent1.eventId to EventItemResponse {
                    message = "Internal Server Error"
                    statusCode = 500
                },
                pinpointEvent2.eventId to EventItemResponse {
                    message = "Not real, but 599 should be retryable as well"
                    statusCode = 599
                }
            )
        }
        val putEventResponse = PutEventsResponse {
            eventsResponse = EventsResponse {
                results = mapOf(endpointId to itemResponse)
            }
        }
        coEvery { pinpointClient.putEvents(any<PutEventsRequest>()) }.answers { putEventResponse }

        eventRecorder.submitEvents()

        coVerify(exactly = 0) {
            pinpointDatabaseMock.deleteEventById(any())
        }
    }

    private fun getPinpointEvent(eventType: String): PinpointEvent {
        return PinpointEvent(
            eventType = eventType,
            attributes = emptyMap(),
            metrics = emptyMap(),
            sdkInfo = SDKInfo("Test", "1.0"),
            pinpointSession = PinpointSession("", 1L, 1L),
            eventTimestamp = System.currentTimeMillis(),
            uniqueId = UUID.randomUUID().toString(),
            androidAppDetails = AndroidAppDetails("com.test.app", "TestApp", "com.test.app", "1.0", "test"),
            androidDeviceDetails = AndroidDeviceDetails("test")
        )
    }
}
