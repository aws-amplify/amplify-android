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

package com.amplifyframework.geo.maplibre

import android.content.Context
import androidx.annotation.VisibleForTesting

import com.amazonaws.auth.AWSCredentialsProvider
import com.amplifyframework.auth.AuthCategory
import com.amplifyframework.core.Amplify
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.maplibre.http.AWS4SigningInterceptor
import com.amplifyframework.geo.models.MapStyle
import com.amplifyframework.geo.options.GetMapStyleDescriptorOptions

import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.WellKnownTileServer
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.module.http.HttpRequestUtil
import kotlinx.coroutines.*
import okhttp3.OkHttpClient

/**
 * Entry point to initialize MapLibre instance and configure it to be
 * authorized to make requests directly to Amazon Location Service.
 */
object AmplifyMapLibreAdapter {
    private const val COGNITO_AUTH_PLUGIN_KEY = "awsCognitoAuthPlugin"
    private const val GEO_SERVICE_NAME = "geo"

    private val log = Amplify.Logging.forNamespace("amplify:maplibre-adapter")

    @VisibleForTesting internal var geo: GeoCategory = Amplify.Geo
    @VisibleForTesting internal var auth: AuthCategory = Amplify.Auth

    /**
     * Instantiates MapLibre SDK and attaches AWS Signature V4 signer.
     *
     * @param context Android context which holds or is an application context
     * @return the single instance of Mapbox
     */
    @JvmStatic
    fun getInstance(context: Context): AmplifyMapLibreAdapter {
        return synchronized(this) {
            Mapbox.getInstance(context, null, WellKnownTileServer.Mapbox)
            val interceptor = AWS4SigningInterceptor(credentialsProvider(), GEO_SERVICE_NAME)
            val client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()
            HttpRequestUtil.setOkHttpClient(client)
            this
        }
    }

    private fun credentialsProvider(): AWSCredentialsProvider {
        val authPlugin = auth.getPlugin(COGNITO_AUTH_PLUGIN_KEY)
        return authPlugin.escapeHatch as AWSCredentialsProvider
    }

    /**
     * Convenience method to use style from Amplify directly with MapLibre's [MapboxMap].
     *
     * @param map MapLibre's map instance to load style on
     * @param style Amplify map style to use
     * @param callback Callback to trigger upon successfully loading map style
     */
    @JvmStatic
    @JvmOverloads
    fun setStyle(map: MapboxMap, style: MapStyle? = null, callback: Style.OnStyleLoaded) {
        val options = if (style == null) {
            // Use default map if no style is provided
            GetMapStyleDescriptorOptions.defaults()
        } else {
            GetMapStyleDescriptorOptions.builder()
                .mapName(style.mapName)
                .build()
        }

        // Network request must be on a worker thread
        geo.getMapStyleDescriptor(
            options,
            {
                // Map interactions must be on a UI thread
                MainScope().launch {
                    map.setStyle(Style.Builder().fromJson(it.json), callback)
                }
            },
            {
                log.error("Failed to get map style document.", it)

                // Force trigger OnDidFailLoadingMapListener with invalid style
                MainScope().launch {
                    map.setStyle(Style.Builder().fromJson(""))
                }
            }
        )
    }
}