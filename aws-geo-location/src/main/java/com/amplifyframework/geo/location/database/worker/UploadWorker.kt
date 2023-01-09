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

package com.amplifyframework.geo.location.database.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import aws.smithy.kotlin.runtime.ClientException
import com.amplifyframework.core.Amplify
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.location.GeoChannelEventName
import com.amplifyframework.geo.location.database.LocationDao
import com.amplifyframework.geo.location.database.LocationEntity
import com.amplifyframework.geo.location.service.AmazonLocationService
import com.amplifyframework.geo.models.GeoLocation
import com.amplifyframework.geo.models.GeoPosition
import com.amplifyframework.geo.options.GeoTrackingSessionOptions
import com.amplifyframework.geo.options.GeoUpdateLocationOptions
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import kotlinx.coroutines.runBlocking
import java.util.Date

class UploadWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    /**
     * A suspending method to upload device tracking events to Amazon Location Service
     */
    override suspend fun doWork(): Result {
        if (locationDao == null) {
            return Result.failure()
        }
        if (geoService == null && options.proxyDelegate == null) {
            // nowhere to pass the event
            return Result.failure()
        }

        val positions: MutableList<GeoPosition> = mutableListOf()
        val toRemove: MutableList<LocationEntity> = mutableListOf()

        val submitEvents = suspend {
            if (options.proxyDelegate == null) {
                try {
                    geoService!!.updateLocations(
                        deviceId,
                        positions,
                        GeoUpdateLocationOptions.builder().withTracker(options.tracker).build()
                    )

                    Amplify.Hub.publish(
                        HubChannel.GEO,
                        HubEvent.create(GeoChannelEventName.FLUSH_TRACKING_EVENTS, positions)
                    )

                    locationDao!!.removeAll(toRemove)
                } catch (e: GeoException) {
                    // GeoException is thrown on internet connection issue
                    // don't remove anything, this is retryable
                } catch (e: ClientException) {
                    // ClientException is thrown on issue from the Amazon Location Services client
                    if (!e.sdkErrorMetadata.isRetryable) {
                        // can't retry the events, they can't be submitted
                        runBlocking {
                            locationDao!!.removeAll(toRemove)
                        }
                    } else {
                        // else block required for Kotlin syntax purposes
                        // request is retryable, don't remove
                    }
                }
            } else {
                options.proxyDelegate.updatePositions(positions)

                Amplify.Hub.publish(
                    HubChannel.GEO,
                    HubEvent.create(GeoChannelEventName.FLUSH_TRACKING_EVENTS, positions)
                )

                locationDao!!.removeAll(toRemove)
            }
        }


        val locations = locationDao!!.getAll()
        var firstPosition: GeoPosition? = null
        locations.forEach {
            val position = GeoPosition()
            position.location = GeoLocation(it.latitude, it.longitude)
            position.timeStamp = Date.from(it.datetime)
            position.tracker = it.tracker
            position.deviceID = it.deviceId
            if (firstPosition == null) {
                firstPosition = position
            } else {
                if (options.batchingOptions.thresholdReached(position, firstPosition)) {
                    submitEvents()
                    firstPosition = position
                    positions.clear()
                }
            }
            positions.add(position)
            toRemove.add(it)
        }
        submitEvents()

        return Result.success()
    }

    companion object {
        @JvmStatic
        internal var options: GeoTrackingSessionOptions = GeoTrackingSessionOptions.defaults()
        @JvmStatic
        internal var deviceId: String = ""
        @JvmStatic
        internal var locationDao: LocationDao? = null
        @JvmStatic
        internal var geoService: AmazonLocationService? = null
    }
}
