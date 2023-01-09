/*
 *
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
 *
 *
 */

package com.amplifyframework.geo.location.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.TestListenableWorkerBuilder
import aws.sdk.kotlin.services.location.LocationClient
import aws.sdk.kotlin.services.location.model.BatchUpdateDevicePositionResponse
import aws.sdk.kotlin.services.location.model.DevicePositionUpdate
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.amplifyframework.geo.location.database.LocationDao
import com.amplifyframework.geo.location.database.worker.UploadWorker
import com.amplifyframework.geo.location.service.AmazonLocationService
import com.amplifyframework.geo.models.GeoLocation
import com.amplifyframework.geo.models.GeoPosition
import com.amplifyframework.geo.options.GeoTrackingSessionOptions
import com.amplifyframework.geo.options.GeoUpdateLocationOptions
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant.now
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class BatchLocationUpdateTest {
    private lateinit var uploadWorker: UploadWorker
    private lateinit var context: Context
    private lateinit var geoService: AmazonLocationService
    private lateinit var mockLocationDao: LocationDao
    private val id = "ID"
    private val tracker = "TRACKER"
    private val mockAlsClient = mockk<LocationClient>(relaxed = true)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val mockCredentialsProvider: CredentialsProvider = mockk()

        mockkObject(LocationClient.Companion)
        every {LocationClient.invoke(any<LocationClient.Config>())}.returns(mockAlsClient)
        every {LocationClient.invoke(any<LocationClient.Config.Builder.() -> Unit>())}.returns(mockAlsClient)
        coEvery {mockAlsClient.batchUpdateDevicePosition(any())}.returns(
            BatchUpdateDevicePositionResponse.invoke {  })

        geoService = AmazonLocationService(mockCredentialsProvider, "placeholder region")
        UploadWorker.geoService = geoService
        UploadWorker.deviceId = id
        mockLocationDao = mockk()
        coEvery {mockLocationDao.removeAll(any())}.returns(0)
        UploadWorker.locationDao = mockLocationDao
        UploadWorker.options = GeoTrackingSessionOptions.defaults()
        uploadWorker = TestListenableWorkerBuilder<UploadWorker>(context)
            .build()
    }

    @Test
    fun `writes batch of locations to database`() {
        runBlocking {
            val positionList = mutableListOf(
                position(65.0, 75.0),
                position(66.0, 75.0),
            )

            geoService.updateLocations(id, positionList, GeoUpdateLocationOptions.defaults())

            positionList.clear()
            positionList.add(position(67.0, 75.0))
            positionList.add(position(68.0, 75.0))
            positionList.add(position(69.0, 75.0))

            geoService.updateLocations(id, positionList, GeoUpdateLocationOptions.defaults())
        }


        coVerify {mockAlsClient.batchUpdateDevicePosition(withArg { request ->
            when (request.updates!!.size) {
                2 -> {
                    // batch 1
                    assertEq(request.updates!![0], position(65.0, 75.0))
                    assertEq(request.updates!![1], position(66.0, 75.0))
                }
                3 -> {
                    // batch 2
                    assertEq(request.updates!![0], position(67.0, 75.0))
                    assertEq(request.updates!![1], position(68.0, 75.0))
                    assertEq(request.updates!![2], position(69.0, 75.0))
                }
                else -> assert(false)
            }
        })}

        confirmVerified(mockAlsClient)
    }

    private fun position(lat: Double = 45.0, long: Double = 45.0): GeoPosition {
        val pos = GeoPosition()
        pos.location = GeoLocation(lat, long)
        pos.deviceID = id
        pos.timeStamp = Date.from(now())
        pos.tracker = tracker
        return pos
    }

    private fun assertEq(p1: DevicePositionUpdate, p2: GeoPosition) {
        assertEquals(p1.position!![1], p2.location.latitude, .001)
        assertEquals(p1.position!![0], p2.location.longitude, .001)
        assert(p1.deviceId == p2.deviceID)
    }
}