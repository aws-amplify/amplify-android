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
import com.amplifyframework.geo.location.database.LocationDao
import com.amplifyframework.geo.location.database.LocationEntity
import com.amplifyframework.geo.location.database.worker.UploadWorker
import com.amplifyframework.geo.location.service.AmazonLocationService
import com.amplifyframework.geo.models.GeoLocation
import com.amplifyframework.geo.models.GeoPosition
import com.amplifyframework.geo.options.BatchingOptions
import com.amplifyframework.geo.options.GeoTrackingSessionOptions
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.util.Date

/**
 * Unit tests for the [UploadWorker] class.
 */
@RunWith(RobolectricTestRunner::class)
internal class UploadWorkerTest {
    private lateinit var uploadWorker: UploadWorker
    private lateinit var context: Context
    private lateinit var mockGeoService: AmazonLocationService
    private lateinit var mockLocationDao: LocationDao
    private val id = "ID"
    private val tracker = "TRACKER"
    private val lat = 47.6154086
    private val long = -122.3349685
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockGeoService = mockk()
        coEvery {mockGeoService.updateLocations(any(), any(), any())}.returns(Unit)
        UploadWorker.geoService = mockGeoService
        UploadWorker.deviceId = id
        mockLocationDao = mockk()
        coEvery {mockLocationDao.removeAll(any())}.returns(0)
        UploadWorker.locationDao = mockLocationDao
        UploadWorker.options = GeoTrackingSessionOptions.defaults()
        uploadWorker = TestListenableWorkerBuilder<UploadWorker>(context)
            .build()
    }

    @Test
    fun `uploads locations`() {
        val now = Instant.now()
        coEvery {mockLocationDao.getAll()}.returns(
            listOf(LocationEntity(1, id, tracker, now, lat, long))
        )

        runBlocking {
            val result = uploadWorker.doWork()
            assertTrue(result.outputData.size() == 0) // success!
            coVerify { mockLocationDao.getAll() }
            coVerify { mockGeoService.updateLocations(id, withArg {
                assertTrue(it.size == 1)
                val position = it[0]
                assertTrue(position.deviceID == id)
                assertTrue(position.tracker == tracker)
                assertTrue(position.location.latitude == lat)
                assertTrue(position.location.longitude == long)
            }, any()) }
            coVerify { mockLocationDao.removeAll(withArg {
                assertTrue(it.size == 1)
                val position = (it as List<LocationEntity>)[0]
                assertTrue(position.deviceId == id)
                assertTrue(position.tracker == tracker)
                assertTrue(position.latitude == lat)
                assertTrue(position.longitude == long)
            }) }
        }
        confirmVerified(mockGeoService, mockLocationDao)
    }

    @Test
    fun `batches locations by distance`() {
        // setup: 5 locations, with distance-based batching.  the third location barely does not exceed the distance,
        // so we start a new batch after processing location 3.  locations 4 and 5 are submitted in their own batch.
        val now = Instant.now()
        val options = GeoTrackingSessionOptions.builder()
            .withBatchingOptions(BatchingOptions.metersTravelled(11133))
            .build()
        UploadWorker.options = options
        val locations = (0 until 5).map {
            LocationEntity(it.toLong(), id, tracker, now, lat + it * .05, long)
        }
        coEvery {mockLocationDao.getAll()}.returns(locations)

        runBlocking {
            val result = uploadWorker.doWork()
            assertTrue(result.outputData.size() == 0) // success!
            coVerify { mockLocationDao.getAll() }
            coVerify { mockGeoService.updateLocations(id, withArg {
                when(it.size) {
                    3 -> {
                        // batch 1
                        assertEq(it[0], position(lat + 0 * .05, long))
                        assertEq(it[1], position(lat + 1 * .05, long))
                        assertEq(it[2], position(lat + 2 * .05, long))
                    }
                    2 -> {
                        // batch 2
                        assertEq(it[0], position(lat + 3 * .05, long))
                        assertEq(it[1], position(lat + 4 * .05, long))
                    }
                    else -> assert(false)
                }
            }, any()) }
            coVerify { mockLocationDao.removeAll(withArg {
                assertTrue(it.size == 5)
                for (i in 0 until 5) {
                    val position = (it as List<LocationEntity>)[i]
                    assertTrue(position.deviceId == id)
                    assertTrue(position.tracker == tracker)
                    assertEquals(position.latitude, lat + i * .05, 0.001)
                    assertTrue(position.longitude == long)
                }
            }) }
        }
        confirmVerified(mockGeoService, mockLocationDao)
    }

    @Test
    fun `batches locations by time`() {
        // setup: 5 locations, with time-based batching.  the third location barely does not exceed the time threshold,
        // so we start a new batch after processing location 3.  locations 4 and 5 are submitted in their own batch.
        val now = Instant.now()
        val options = GeoTrackingSessionOptions.builder()
            .withBatchingOptions(BatchingOptions.secondsElapsed(7))
            .build()
        UploadWorker.options = options
        val locations = (0 until 5).map {
            LocationEntity(it.toLong(), id, tracker, now.plusMillis(it * 3000L), lat + it * .05, long)
        }
        coEvery {mockLocationDao.getAll()}.returns(locations)

        runBlocking {
            val result = uploadWorker.doWork()
            assertTrue(result.outputData.size() == 0) // success!
            coVerify { mockLocationDao.getAll() }
            coVerify { mockGeoService.updateLocations(id, withArg {
                when(it.size) {
                    3 -> {
                        // batch 1
                        assertEq(it[0], position(lat + 0 * .05, long))
                        assertEq(it[1], position(lat + 1 * .05, long))
                        assertEq(it[2], position(lat + 2 * .05, long))
                    }
                    2 -> {
                        // batch 2
                        assertEq(it[0], position(lat + 3 * .05, long))
                        assertEq(it[1], position(lat + 4 * .05, long))
                    }
                    else -> assert(false)
                }
            }, any()) }
            coVerify { mockLocationDao.removeAll(withArg {
                assertTrue(it.size == 5)
                for (i in 0 until 5) {
                    val position = (it as List<LocationEntity>)[i]
                    assertTrue(position.deviceId == id)
                    assertTrue(position.tracker == tracker)
                    assertEquals(position.latitude, lat + i * .05, 0.001)
                    assertTrue(position.longitude == long)
                }
            }) }
        }
        confirmVerified(mockGeoService, mockLocationDao)
    }

    @Test
    fun `supports proxy delegate`() {
        val mockProxyDelegate: GeoTrackingSessionOptions.LocationProxyDelegate = mockk()
        coEvery {mockProxyDelegate.updatePositions(any())}.just(Runs)
        UploadWorker.options = GeoTrackingSessionOptions.builder().withProxyDelegate(
            mockProxyDelegate
        ).build()
        val now = Instant.now()
        val locations = (0 until 5).map {
            LocationEntity(it.toLong(), id, tracker, now.plusMillis(it * 3000L), lat + it * .05, long)
        }
        coEvery {mockLocationDao.getAll()}.returns(locations)

        runBlocking {
            val result = uploadWorker.doWork()
            assertTrue(result.outputData.size() == 0) // success!
            coVerify { mockLocationDao.getAll() }

            coVerify { mockProxyDelegate.updatePositions(withArg {
                assertTrue(it.size == 5)
                for (i in 0 until 5) {
                    val position = it[i]
                    assertEq(position, position(lat + i * .05, long))
                }
            }) }

            coVerify { mockLocationDao.removeAll(withArg {
                assertTrue(it.size == 5)
                for (i in 0 until 5) {
                    val position = (it as List<LocationEntity>)[i]
                    assertTrue(position.deviceId == id)
                    assertTrue(position.tracker == tracker)
                    assertEquals(position.latitude, lat + i * .05, 0.001)
                    assertTrue(position.longitude == long)
                }
            }) }
        }
    }

    private fun position(lat: Double = 45.0, long: Double = 45.0): GeoPosition {
        val pos = GeoPosition()
        pos.location = GeoLocation(lat, long)
        pos.deviceID = id
        pos.timeStamp = Date.from(Instant.now())
        pos.tracker = tracker
        return pos
    }

    private fun assertEq(p1: GeoPosition, p2: GeoPosition) {
        assertEquals(p1.location.latitude, p2.location.latitude, .001)
        assertEquals(p1.location.longitude, p2.location.longitude, .001)
        assert(p1.deviceID == p2.deviceID)
        assert(p1.tracker == p2.tracker)
    }
}