/*
 *  Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.geo.location.tracking

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Looper
import androidx.annotation.VisibleForTesting
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.location.database.GeoDatabase
import com.amplifyframework.geo.location.database.LocationDao
import com.amplifyframework.geo.location.database.LocationEntity
import com.amplifyframework.geo.location.database.worker.UploadWorker
import com.amplifyframework.geo.options.GeoTrackingSessionOptions
import com.amplifyframework.geo.options.GeoTrackingSessionOptions.Accuracy
import com.amplifyframework.geo.options.GeoTrackingSessionOptions.Power
import java.time.Instant
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class LocationTrackingService : Service() {
    @VisibleForTesting
    internal lateinit var locationDao: LocationDao

    @VisibleForTesting
    internal lateinit var locationManager: LocationManager

    private val coroutineScope = CoroutineScope(SupervisorJob())
    private var listener: Listener? = null
    private var trackingData: TrackingData? = null
    private var numUpdates = 0

    override fun onBind(intent: Intent?) = LocationServiceBinder(this)
    override fun onUnbind(intent: Intent?): Boolean {
        stopTracking()
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        locationDao = GeoDatabase(this).locationDao
        locationManager = getSystemService(LocationManager::class.java)
        UploadWorker.locationDao = locationDao
    }

    fun startTracking(trackingData: TrackingData) {
        val isPermissionGranted =
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!isPermissionGranted) {
            throw GeoException(
                "Missing Permissions",
                "Ensure that the user has granted ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION prior to starting " +
                    "device location tracking"
            )
        }

        val locationListener = Listener().also { listener = it }
        this.trackingData = trackingData

        locationManager.requestLocationUpdates(
            trackingData.options.minUpdatesInterval,
            trackingData.options.minUpdateDistanceMeters,
            trackingData.options.criteria,
            locationListener,
            Looper.myLooper()
        )
    }

    fun stopTracking() {
        coroutineScope.cancel()
        listener?.let(locationManager::removeUpdates)
        listener = null
        trackingData = null
        numUpdates = 0
    }

    override fun onDestroy() {
        stopTracking()
    }

    private fun handleNewLocation(location: Location) {
        val data = trackingData ?: return

        val trackUntil = data.options.trackUntil
        val abortTracking = ++numUpdates > data.options.maxUpdates ||
            (trackUntil != null && Date().after(trackUntil))

        if (abortTracking) {
            stopTracking()
        } else {
            uploadOrSaveLocation(location)
        }
    }

    private fun uploadOrSaveLocation(location: Location) {
        val data = trackingData ?: return

        // todo: attempt to upload first

        if (!data.options.disregardLocationUpdatesWhenOffline) {
            val entity = LocationEntity(
                deviceId = data.deviceId,
                tracker = data.tracker,
                datetime = Instant.now(),
                longitude = location.longitude,
                latitude = location.latitude
            )
            coroutineScope.launch {
                locationDao.insert(entity)
            }
        }
    }

    private val GeoTrackingSessionOptions.criteria: Criteria
        get() = Criteria().apply {
            powerRequirement = when (powerRequired) {
                Power.MEDIUM -> Criteria.POWER_MEDIUM
                Power.HIGH -> Criteria.POWER_HIGH
                else -> Criteria.POWER_LOW
            }
            accuracy = when (desiredAccuracy) {
                Accuracy.FINE -> Criteria.ACCURACY_FINE
                else -> Criteria.ACCURACY_COARSE
            }
        }

    inner class Listener : LocationListener {
        override fun onLocationChanged(location: Location) {
            handleNewLocation(location)
        }
    }

    class LocationServiceBinder(val service: LocationTrackingService) : Binder()
}
