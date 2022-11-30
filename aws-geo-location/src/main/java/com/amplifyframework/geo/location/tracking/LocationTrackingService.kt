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

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.amplifyframework.geo.location.database.GeoDatabase
import com.amplifyframework.geo.location.database.LocationDao
import com.amplifyframework.geo.location.database.LocationEntity
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class LocationTracker(private val context: Context) {
    fun start() {
        context.startService(Intent(context, LocationTrackingService::class.java))
    }

    fun stop() {
        context.stopService(Intent(context, LocationTrackingService::class.java))
    }
}

internal class LocationTrackingService() : Service() {
    private lateinit var locationDao: LocationDao
    private val coroutineScope = CoroutineScope(SupervisorJob())

    // Service binding is not supported
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Do not restart the service if the process is killed
        return START_NOT_STICKY
    }

    override fun onCreate() {
        locationDao = GeoDatabase(this).locationDao
        startTracking()
    }

    fun startTracking() {
        coroutineScope.launch {
            // For testing purposes let's add one location a second to the database for as long as the service is
            // running
            while (true) {
                val entity = LocationEntity(
                    deviceId = "someId",
                    tracker = "someTracker",
                    datetime = Instant.now(),
                    latitude = Random.nextDouble(),
                    longitude = Random.nextDouble()
                )
                locationDao.insert(entity)
                delay(1000)
            }
        }
    }

    override fun onDestroy() {
        coroutineScope.cancel()
    }
}
