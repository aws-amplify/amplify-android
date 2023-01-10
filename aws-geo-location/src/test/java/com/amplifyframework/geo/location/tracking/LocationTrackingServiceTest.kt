package com.amplifyframework.geo.location.tracking

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.location.database.LocationDao
import com.amplifyframework.geo.options.GeoTrackingSessionOptions
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the [LocationTrackingService]
 */
internal class LocationTrackingServiceTest {
    private val dao = mockk<LocationDao>(relaxed = true)
    private val manager = mockk<LocationManager>(relaxed = true)
    private val service = LocationTrackingService().apply {
        locationDao = dao
        locationManager = manager
    }
    private val spy = spyk(service) {
        every { checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) } returns PERMISSION_GRANTED
        every { checkSelfPermission(ACCESS_COARSE_LOCATION) } returns PERMISSION_GRANTED
    }

    private val data = TrackingData("deviceId", "tracker", GeoTrackingSessionOptions.defaults())

    @Test(expected = GeoException::class)
    fun `throws if permissions are not present`() {
        every { spy.checkSelfPermission(ACCESS_FINE_LOCATION) } returns PERMISSION_DENIED
        every { spy.checkSelfPermission(ACCESS_COARSE_LOCATION) } returns PERMISSION_DENIED

        spy.startTracking(data)
    }

    @Test
    fun `registers for location updates`() {
        spy.startTracking(data)

        verify {
            manager.requestLocationUpdates(
                data.options.minUpdatesInterval,
                data.options.minUpdateDistanceMeters,
                any(),
                any(),
                any<Looper>()
            )
        }
    }

    @Test
    fun `removes location updates when stopping tracking`() {
        val slot = slot<LocationListener>()
        every { manager.requestLocationUpdates(any(), any(), any<Criteria>(), capture(slot), any()) } just Runs

        spy.startTracking(data)
        spy.stopTracking()

        verify {
            manager.removeUpdates(slot.captured)
        }
    }

    @Test
    fun `returns binder`() {
        val binder = spy.onBind(mockk())
        assertEquals(spy, binder.service)
    }

    @Test
    fun `stops tracking on unbind`() {
        val slot = slot<LocationListener>()
        every { manager.requestLocationUpdates(any(), any(), any<Criteria>(), capture(slot), any()) } just Runs

        spy.startTracking(data)
        spy.onUnbind(mockk())

        verify {
            manager.removeUpdates(slot.captured)
        }
    }

    @Test
    fun `stops tracking on destroy`() {
        val slot = slot<LocationListener>()
        every { manager.requestLocationUpdates(any(), any(), any<Criteria>(), capture(slot), any()) } just Runs

        spy.startTracking(data)
        spy.onDestroy()

        verify {
            manager.removeUpdates(slot.captured)
        }
    }

    @Test
    fun `writes locations to database`() {
        val location = mockLocation(65.0, 75.0)

        spy.startTracking(data)
        invokeLocationUpdate(location)
        invokeLocationUpdate(location)

        coVerify(exactly = 2) {
            dao.insert(
                withArg { entity ->
                    assertEquals(65.0, entity.latitude, 0.01)
                    assertEquals(75.0, entity.longitude, 0.01)
                    assertEquals(data.deviceId, entity.deviceId)
                    assertEquals(data.tracker, entity.tracker)
                }
            )
        }
    }

    @Test
    fun `stops tracking when max updates reached`() {
        val options = GeoTrackingSessionOptions.builder().withMaxUpdates(2).build()
        val trackingData = TrackingData("deviceId", "tracker", options)

        spy.startTracking(trackingData)
        invokeLocationUpdate()
        invokeLocationUpdate()
        invokeLocationUpdate()

        verify {
            manager.removeUpdates(any<LocationListener>())
        }
        coVerify(exactly = 2) {
            dao.insert(any())
        }
    }

    private fun invokeLocationUpdate(location: Location = mockLocation()) {
        val slot = slot<LocationListener>()
        verify {
            manager.requestLocationUpdates(any(), any(), any<Criteria>(), capture(slot), any())
        }
        slot.captured.onLocationChanged(location)
    }

    private fun mockLocation(lat: Double = 45.0, long: Double = 45.0) = mockk<Location> {
        every { latitude } returns lat
        every { longitude } returns long
    }
}
