/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.geo.AmazonLocationClient
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthCategory
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.geo.GeoCategoryPlugin
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.location.configuration.GeoConfiguration
import com.amplifyframework.geo.location.service.AmazonLocationService
import com.amplifyframework.geo.location.service.GeoService
import com.amplifyframework.geo.models.Coordinates
import com.amplifyframework.geo.models.MapStyle
import com.amplifyframework.geo.models.MapStyleDescriptor
import com.amplifyframework.geo.options.GeoSearchByCoordinatesOptions
import com.amplifyframework.geo.options.GeoSearchByTextOptions
import com.amplifyframework.geo.options.GetMapStyleDescriptorOptions
import com.amplifyframework.geo.result.GeoSearchResult

import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * A plugin for the Geo category to interact with Amazon Location Service.
 */
class AWSLocationGeoPlugin(
    private val userConfiguration: GeoConfiguration? = null, // for programmatically overriding amplifyconfiguration.json
    private val authProvider: AuthCategory = Amplify.Auth
): GeoCategoryPlugin<AmazonLocationClient?>() {
    companion object {
        private const val GEO_PLUGIN_KEY = "awsLocationGeoPlugin"
        private const val AUTH_PLUGIN_KEY = "awsCognitoAuthPlugin"
    }

    private lateinit var configuration: GeoConfiguration
    private lateinit var geoService: GeoService<AmazonLocationClient>

    private val executor = Executors.newCachedThreadPool()
    private val defaultMapName: String by lazy {
        configuration.maps!!.default.mapName
    }
    private val defaultSearchIndexName: String by lazy {
        configuration.searchIndices!!.default
    }

    override fun getPluginKey(): String {
        return GEO_PLUGIN_KEY
    }

    @Throws(AmplifyException::class)
    override fun configure(pluginConfiguration: JSONObject, context: Context) {
        try {
            this.configuration = userConfiguration ?: GeoConfiguration.fromJson(pluginConfiguration).build()
            this.geoService = AmazonLocationService(credentialsProvider(), configuration.region)
        } catch (error: Exception) {
            throw GeoException("Failed to configure AWSLocationGeoPlugin.", error,
                "Make sure your amplifyconfiguration.json is valid.")
        }
    }

    override fun getEscapeHatch(): AmazonLocationClient {
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
                val places = geoService.geocode(
                    options.searchIndex ?: defaultSearchIndexName,
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
                val places = geoService.reverseGeocode(
                    options.searchIndex ?: defaultSearchIndexName,
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

    private fun credentialsProvider(): AWSCredentialsProvider {
        val authPlugin = authProvider.getPlugin(AUTH_PLUGIN_KEY)
        return authPlugin.escapeHatch as AWSCredentialsProvider
    }

    // Helper method that launches given task on a new worker thread.
    private fun <T : Any> execute(
        runnableTask: () -> T,
        errorTransformer: (Throwable) -> GeoException,
        onResult: Consumer<T>,
        onError: Consumer<GeoException>
    ) {
        executor.execute {
            try {
                val result = runnableTask.invoke()
                onResult.accept(result)
            } catch (error: Throwable) {
                val geoException = errorTransformer.invoke(error)
                onError.accept(geoException)
            }
        }
    }
}