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
package com.amplifyframework.pinpoint.core.database

import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.pinpoint.core.data.AndroidAppDetails
import com.amplifyframework.pinpoint.core.data.AndroidDeviceDetails
import com.amplifyframework.pinpoint.core.models.PinpointEvent
import com.amplifyframework.pinpoint.core.models.PinpointSession
import com.amplifyframework.pinpoint.core.models.SDKInfo
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PinpointDatabaseTest {

    private lateinit var pinpointDatabase: PinpointDatabase
    private lateinit var coroutineDispatcher: CoroutineDispatcher

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    public fun setUp() = runTest {
        coroutineDispatcher = UnconfinedTestDispatcher(testScheduler)
        pinpointDatabase = PinpointDatabase(
            ApplicationProvider.getApplicationContext(),
            coroutineDispatcher = coroutineDispatcher
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun `test save event`() = runTest {
        val testEventType = "TestEvent"
        val pinpointEvent = getPinpointEvent(testEventType)
        pinpointDatabase.saveEvent(pinpointEvent)
        val cursor = pinpointDatabase.queryAllEvents()
        assertEquals(1, cursor.count)
        val resultPinpointJson = takeIf { cursor.moveToFirst() }.let {
            cursor.getString(EventTable.COLUMNINDEX.JSON.index)
        }
        assertEquals(pinpointEvent.toJsonString(), resultPinpointJson)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun `test queryAllEvents`() = runTest {
        val pinpointEventList = listOf<PinpointEvent>(
            getPinpointEvent("testEventType_1"),
            getPinpointEvent("testEventType_2"),
            getPinpointEvent("testEventType_3")
        )
        pinpointEventList.forEach {
            pinpointDatabase.saveEvent(it)
        }
        val cursor = pinpointDatabase.queryAllEvents()
        assertEquals(3, cursor.count)
        pinpointEventList.forEach {
            var dbEvent: String? = null
            if (cursor.moveToNext()) {
                dbEvent = cursor.getString(EventTable.COLUMNINDEX.JSON.index)
            }
            assertEquals(it.toJsonString(), dbEvent)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun `test delete event by ID`() = runTest {
        val pinpointEventList = listOf<PinpointEvent>(
            getPinpointEvent("testEventType_1"),
            getPinpointEvent("testEventType_2"),
            getPinpointEvent("testEventType_3")
        )
        pinpointEventList.forEach {
            pinpointDatabase.saveEvent(it)
        }
        val cursor = pinpointDatabase.queryAllEvents()
        assertEquals(3, cursor.count)
        while (cursor.moveToNext()) {
            val id = cursor.getInt(EventTable.COLUMNINDEX.ID.index)
            val rowsDeleted = pinpointDatabase.deleteEventById(id)
            assertEquals(1, rowsDeleted)
        }
        assertEquals(0, pinpointDatabase.queryAllEvents().count)
    }

    @After
    public fun tearDown() {
        pinpointDatabase.closeDB()
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
