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

package com.amplifyframework.geo.location

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import aws.sdk.kotlin.services.location.LocationClient
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthCategory
import com.amplifyframework.core.Action
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.geo.GeoCategoryPlugin
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.location.auth.CognitoCredentialsProvider
import com.amplifyframework.geo.location.configuration.GeoConfiguration
import com.amplifyframework.geo.location.database.worker.UploadWorker
import com.amplifyframework.geo.location.options.AmazonLocationSearchByCoordinatesOptions
import com.amplifyframework.geo.location.options.AmazonLocationSearchByTextOptions
import com.amplifyframework.geo.location.service.AmazonLocationService
import com.amplifyframework.geo.location.service.GeoService
import com.amplifyframework.geo.location.tracking.LocationTracker
import com.amplifyframework.geo.location.util.getId
import com.amplifyframework.geo.models.Coordinates
import com.amplifyframework.geo.models.GeoDevice
import com.amplifyframework.geo.models.GeoDeviceType
import com.amplifyframework.geo.models.GeoLocation
import com.amplifyframework.geo.models.GeoPosition
import com.amplifyframework.geo.models.MapStyle
import com.amplifyframework.geo.models.MapStyleDescriptor
import com.amplifyframework.geo.options.GeoDeleteLocationHistoryOptions
import com.amplifyframework.geo.options.GeoSearchByCoordinatesOptions
import com.amplifyframework.geo.options.GeoSearchByTextOptions
import com.amplifyframework.geo.options.GeoTrackingSessionOptions
import com.amplifyframework.geo.options.GeoUpdateLocationOptions
import com.amplifyframework.geo.options.GetMapStyleDescriptorOptions
import com.amplifyframework.geo.result.GeoSearchResult
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/**
 * A plugin for the Geo category to interact with Amazon Location Service.
 */
class AWSLocationGeoPlugin(
    // for programmatically overriding amplifyconfiguration.json
    private val userConfiguration: GeoConfiguration? = null,
    private val authCategory: AuthCategory = Amplify.Auth
) : GeoCategoryPlugin<LocationClient?>() {
    companion object {
        private const val GEO_PLUGIN_KEY = "awsLocationGeoPlugin"
    }

    private lateinit var configuration: GeoConfiguration
    private lateinit var geoService: GeoService<LocationClient>
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var locationTracker: LocationTracker

    private val executor = Executors.newCachedThreadPool()
    private val defaultMapName: String by lazy {
        configuration.maps!!.default.mapName
    }
    private val defaultSearchIndexName: String by lazy {
        configuration.searchIndices!!.default
    }
    private val defaultTracker: String by lazy {
        configuration.trackers!!.default
    }

    val credentialsProvider: CredentialsProvider by lazy {
        CognitoCredentialsProvider(authCategory)
    }

    override fun getPluginKey(): String {
        return GEO_PLUGIN_KEY
    }

    @Throws(AmplifyException::class)
    override fun configure(pluginConfiguration: JSONObject, context: Context) {
        try {
            System.loadLibrary("sqlcipher")
            this.configuration =
                userConfiguration ?: GeoConfiguration.fromJson(pluginConfiguration).build()
            this.geoService = AmazonLocationService(credentialsProvider, configuration.region)
            this.sharedPreferences = context.getSharedPreferences(GEO_PLUGIN_KEY, MODE_PRIVATE)
            val uploadWorkRequest: PeriodicWorkRequest = PeriodicWorkRequestBuilder<UploadWorker>(
                15,
                TimeUnit.MINUTES
            ).build()
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(
                "Amplify upload device tracking",
                ExistingPeriodicWorkPolicy.KEEP,
                uploadWorkRequest
            )
            UploadWorker.geoService = geoService as AmazonLocationService
            locationTracker = LocationTracker(context)
        } catch (error: Exception) {
            throw GeoException(
                "Failed to configure AWSLocationGeoPlugin.",
                error,
                "Make sure your amplifyconfiguration.json is valid."
            )
        }
    }

    override fun getEscapeHatch(): LocationClient {
        return geoService.provider
    }

    override fun getVersion(): String {
        return BuildConfig.VERSION_NAME
    }

    override fun getAvailableMaps(
        onResult: Consumer<Collection<MapStyle>>,
        onError: Consumer<GeoException>
    ) {
        try {
            onResult.accept(configuration.maps!!.items)
        } catch (error: Exception) {
            onError.accept(Errors.mapsError(error))
        }
    }

    override fun getDefaultMap(
        onResult: Consumer<MapStyle>,
        onError: Consumer<GeoException>
    ) {
        try {
            onResult.accept(configuration.maps!!.default)
        } catch (error: Exception) {
            onError.accept(Errors.mapsError(error))
        }
    }

    override fun getMapStyleDescriptor(
        onResult: Consumer<MapStyleDescriptor>,
        onError: Consumer<GeoException>
    ) {
        val options = GetMapStyleDescriptorOptions.defaults()
        getMapStyleDescriptor(options, onResult, onError)
    }

    override fun getMapStyleDescriptor(
        options: GetMapStyleDescriptorOptions,
        onResult: Consumer<MapStyleDescriptor>,
        onError: Consumer<GeoException>
    ) {
        execute(
            {
                val mapName = options.mapName ?: defaultMapName
                val styleJson = geoService.getStyleJson(mapName)
                MapStyleDescriptor(styleJson)
            },
            Errors::mapsError,
            onResult,
            onError
        )
    }

    override fun searchByText(
        query: String,
        onResult: Consumer<GeoSearchResult>,
        onError: Consumer<GeoException>
    ) {
        val options = GeoSearchByTextOptions.defaults()
        searchByText(query, options, onResult, onError)
    }

    override fun searchByText(
        query: String,
        options: GeoSearchByTextOptions,
        onResult: Consumer<GeoSearchResult>,
        onError: Consumer<GeoException>
    ) {
        execute(
            {
                val searchIndex = if (options is AmazonLocationSearchByTextOptions) {
                    options.searchIndex ?: defaultSearchIndexName
                } else defaultSearchIndexName
                val places = geoService.geocode(
                    searchIndex,
                    query,
                    options.maxResults,
                    options.searchArea,
                    options.countries
                )
                GeoSearchResult.withPlaces(places)
            },
            Errors::searchError,
            onResult,
            onError
        )
    }

    override fun searchByCoordinates(
        position: Coordinates,
        onResult: Consumer<GeoSearchResult>,
        onError: Consumer<GeoException>
    ) {
        val options = GeoSearchByCoordinatesOptions.defaults()
        searchByCoordinates(position, options, onResult, onError)
    }

    override fun searchByCoordinates(
        position: Coordinates,
        options: GeoSearchByCoordinatesOptions,
        onResult: Consumer<GeoSearchResult>,
        onError: Consumer<GeoException>
    ) {
        execute(
            {
                val searchIndex = if (options is AmazonLocationSearchByCoordinatesOptions) {
                    options.searchIndex ?: defaultSearchIndexName
                } else defaultSearchIndexName
                val places = geoService.reverseGeocode(
                    searchIndex,
                    position,
                    options.maxResults
                )
                GeoSearchResult.withPlaces(places)
            },
            Errors::searchError,
            onResult,
            onError
        )
    }

    override fun updateLocation(
        device: GeoDevice,
        location: GeoLocation,
        onResult: Action,
        onError: Consumer<GeoException>
    ) {
        val options = GeoUpdateLocationOptions.defaults()
        updateLocation(device, location, options, onResult, onError)
    }

    override fun updateLocation(
        device: GeoDevice,
        location: GeoLocation,
        options: GeoUpdateLocationOptions,
        onResult: Action,
        onError: Consumer<GeoException>
    ) {
        execute(
            {
                if (options.tracker.isEmpty()) {
                    options.tracker = defaultTracker
                }
                val position = GeoPosition()
                position.location = location
                geoService.updateLocation(device.resolvedId(), position, options)
            },
            Errors::deviceTrackingError,
            onResult,
            onError
        )
    }

    override fun deleteLocationHistory(
        device: GeoDevice,
        onResult: Action,
        onError: Consumer<GeoException>
    ) {
        val options = GeoDeleteLocationHistoryOptions.defaults()
        deleteLocationHistory(device, options, onResult, onError)
    }

    override fun deleteLocationHistory(
        device: GeoDevice,
        options: GeoDeleteLocationHistoryOptions,
        onResult: Action,
        onError: Consumer<GeoException>
    ) {
        val tracker = options.tracker ?: defaultTracker
        execute(
            { geoService.deleteLocationHistory(device.resolvedId(), tracker) },
            Errors::deleteHistoryError,
            onResult,
            onError
        )
    }

    override fun startTracking(device: GeoDevice, onResult: Action, onError: Consumer<GeoException>) {
        startTracking(device, GeoTrackingSessionOptions.defaults(), onResult, onError)
    }

    override fun startTracking(
        device: GeoDevice,
        options: GeoTrackingSessionOptions,
        onResult: Action,
        onError: Consumer<GeoException>
    ) {
        if (options.tracker == null) {
            options.tracker = defaultTracker
        }
        execute(
            {
                locationTracker.start(device.resolvedId(), options.tracker, options)
                UploadWorker.options = options
                UploadWorker.deviceId = device.resolvedId()
            },
            Errors::deviceTrackingError,
            onResult,
            onError
        )
    }

    override fun stopTracking(onResult: Action, onError: Consumer<GeoException>) {
        locationTracker.stop()
    }

    // Helper method that launches given task on a new worker thread.
    private fun <T : Any> execute(
        runnableTask: suspend () -> T,
        errorTransformer: (Throwable) -> GeoException,
        onResult: Action,
        onError: Consumer<GeoException>
    ) {
        executor.execute {
            try {
                runBlocking {
                    runnableTask()
                    onResult.call()
                }
            } catch (error: Throwable) {
                val geoException = errorTransformer.invoke(error)
                onError.accept(geoException)
            }
        }
    }

    // Helper method that launches given task on a new worker thread.
    private fun <T : Any> execute(
        runnableTask: suspend () -> T,
        errorTransformer: (Throwable) -> GeoException,
        onResult: Consumer<T>,
        onError: Consumer<GeoException>
    ) {
        executor.execute {
            try {
                runBlocking {
                    val result = runnableTask()
                    onResult.accept(result)
                }
            } catch (error: Throwable) {
                val geoException = errorTransformer.invoke(error)
                onError.accept(geoException)
            }
        }
    }

    private suspend fun GeoDevice.resolvedId() = when (type) {
        GeoDeviceType.UNCHECKED -> id
        GeoDeviceType.USER_AND_DEVICE ->
            (credentialsProvider as CognitoCredentialsProvider).getIdentityId() + " - " +
                sharedPreferences.getId()
        GeoDeviceType.DEVICE -> sharedPreferences.getId()
        else -> // GeoDeviceType.USER
            (credentialsProvider as CognitoCredentialsProvider).getIdentityId() + " - " +
                sharedPreferences.getId()
    }
}
