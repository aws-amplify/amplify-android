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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.VisibleForTesting
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.location.database.GeoDatabase
import com.amplifyframework.geo.location.tracking.LocationTrackingService.LocationServiceBinder
import com.amplifyframework.geo.options.GeoTrackingSessionOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Manages the lifetime of the [LocationTrackingService]
 */
internal class LocationTracker(private val context: Context) {
    private var serviceConnection: LocationServiceConnection? = null
    private val database by lazy { GeoDatabase(context) }

    /**
     * Start a new tracking session
     */
    suspend fun start(
        deviceId: String,
        tracker: String,
        options: GeoTrackingSessionOptions
    ) = suspendCancellableCoroutine { continuation ->
        val trackingData = TrackingData(deviceId, tracker, options)
        val existingConnection = serviceConnection
        if (existingConnection?.binder == null) {
            // Ensure we have unbound the service connection
            stop()

            val connection = LocationServiceConnection(
                trackingData,
                { continuation.resume(Unit) },
                { continuation.resumeWithException(it) }
            ).also { serviceConnection = it }

            val intent = Intent(context, LocationTrackingService::class.java)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } else {
            val error = GeoException(
                "Tracking session is already active",
                "Ensure you are using stopTracking to end the prior session"
            )
            continuation.resumeWithException(error)
        }
    }

    /**
     * Stop the current tracking session
     */
    fun stop() {
        serviceConnection?.binder?.service?.stopTracking()
        serviceConnection?.let { context.unbindService(it) }
        serviceConnection = null
    }

    suspend fun clearSavedLocations(deviceId: String, tracker: String) {
        database.locationDao.removeAll(deviceId, tracker)
    }

    @VisibleForTesting
    internal class LocationServiceConnection(
        private val trackingData: TrackingData,
        private val onStarted: () -> Unit,
        private val onError: (GeoException) -> Unit
    ) : ServiceConnection {
        var binder: LocationServiceBinder? = null

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            this.binder = binder as LocationServiceBinder
            val service = binder.service
            try {
                service.startTracking(trackingData)
                onStarted()
            } catch (e: GeoException) {
                onError(e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }
    }
}
