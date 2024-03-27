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
import aws.sdk.kotlin.services.location.LocationClient
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.amplifyframework.AmplifyException
import com.amplifyframework.annotations.InternalApiWarning
import com.amplifyframework.auth.AuthCategory
import com.amplifyframework.auth.CognitoCredentialsProvider
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.geo.GeoCategoryPlugin
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.location.configuration.GeoConfiguration
import com.amplifyframework.geo.location.options.AmazonLocationSearchByCoordinatesOptions
import com.amplifyframework.geo.location.options.AmazonLocationSearchByTextOptions
import com.amplifyframework.geo.location.service.AmazonLocationService
import com.amplifyframework.geo.location.service.GeoService
import com.amplifyframework.geo.models.Coordinates
import com.amplifyframework.geo.models.MapStyle
import com.amplifyframework.geo.models.MapStyleDescriptor
import com.amplifyframework.geo.options.GeoSearchByCoordinatesOptions
import com.amplifyframework.geo.options.GeoSearchByTextOptions
import com.amplifyframework.geo.options.GetMapStyleDescriptorOptions
import com.amplifyframework.geo.result.GeoSearchResult
import java.util.concurrent.Executors
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

    private val executor = Executors.newCachedThreadPool()
    private val defaultMapName: String by lazy {
        configuration.maps!!.default.mapName
    }
    private val defaultSearchIndexName: String by lazy {
        configuration.searchIndices!!.default
    }

    @InternalApiWarning
    val credentialsProvider: CredentialsProvider by lazy {
        CognitoCredentialsProvider(authCategory)
    }

    override fun getPluginKey(): String {
        return GEO_PLUGIN_KEY
    }

    @Throws(AmplifyException::class)
    override fun configure(pluginConfiguration: JSONObject, context: Context) {
        try {
            this.configuration =
                userConfiguration ?: GeoConfiguration.fromJson(pluginConfiguration).build()
            this.geoService = AmazonLocationService(credentialsProvider, configuration.region)
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
}
