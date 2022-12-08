package com.amplifyframework.geo.location.tracking

import android.content.ComponentName
import android.content.Context
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.options.GeoTrackingSessionOptions
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for the [LocationTracker] class.
 */
internal class LocationTrackerTest {
    private val context = mockk<Context> {
        every { bindService(any(), any(), any()) } answers {
            val connection = secondArg<LocationTracker.LocationServiceConnection>()
            connection.onServiceConnected(ComponentName("", ""), binder)
            true
        }
        every { unbindService(any()) } answers {
            val connection = firstArg<LocationTracker.LocationServiceConnection>()
            connection.onServiceDisconnected(ComponentName("", ""))
        }
    }

    private val trackerService = mockk<LocationTrackingService>(relaxed = true)
    private val binder = mockk<LocationTrackingService.LocationServiceBinder> {
        every { service } returns trackerService
    }

    private val locationTracker = LocationTracker(context)

    @Test
    fun `starts the service with the given options`() = runTest {
        val deviceId = "device"
        val tracker = "tracker"
        val options = GeoTrackingSessionOptions.builder()
            .withPowerRequired(GeoTrackingSessionOptions.Power.MEDIUM)
            .build()

        locationTracker.start(deviceId, tracker, options)

        verify {
            trackerService.startTracking(TrackingData(deviceId, tracker, options))
        }
    }

    @Test(expected = GeoException::class)
    fun `start function throws a GeoException if session is already active`() = runTest {
        val deviceId = "device"
        val tracker = "tracker"
        val options = GeoTrackingSessionOptions.builder()
            .withPowerRequired(GeoTrackingSessionOptions.Power.MEDIUM)
            .build()

        locationTracker.start(deviceId, tracker, options)
        locationTracker.start(deviceId, tracker, options)
    }

    @Test(expected = GeoException::class)
    fun `exceptions from service are rethrown`() = runTest {
        val deviceId = "device"
        val tracker = "tracker"
        val options = GeoTrackingSessionOptions.builder()
            .withPowerRequired(GeoTrackingSessionOptions.Power.MEDIUM)
            .build()

        every { trackerService.startTracking(any()) } throws GeoException("bad", "news")

        locationTracker.start(deviceId, tracker, options)
    }

    @Test
    fun `stop does nothing if not started`() {
        locationTracker.stop()
        verify {
            trackerService wasNot Called
        }
    }

    @Test
    fun `stop can be called multiple times`() = runTest {
        locationTracker.start("first", "second", GeoTrackingSessionOptions.defaults())
        locationTracker.stop()
        locationTracker.stop()

        verify(exactly = 1) {
            trackerService.stopTracking()
        }
    }
}
