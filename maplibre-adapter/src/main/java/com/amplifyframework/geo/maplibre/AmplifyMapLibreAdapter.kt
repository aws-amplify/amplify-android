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
import com.amplifyframework.core.Amplify
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.location.AWSLocationGeoPlugin
import com.amplifyframework.geo.maplibre.http.AWSRequestSignerInterceptor
import com.amplifyframework.geo.models.MapStyle
import com.amplifyframework.geo.options.GetMapStyleDescriptorOptions
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.WellKnownTileServer
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.module.http.HttpRequestUtil
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

private const val LOCATION_GEO_PLUGIN_KEY = "awsLocationGeoPlugin"

class AmplifyMapLibreAdapter(
    private val context: Context,
    private val geo: GeoCategory = Amplify.Geo
) {

    companion object {
        private val log = Amplify.Logging.forNamespace("amplify:maplibre-adapter")
    }

    private val plugin: AWSLocationGeoPlugin by lazy {
        geo.getPlugin(LOCATION_GEO_PLUGIN_KEY) as AWSLocationGeoPlugin
    }

    fun initialize() {
        Mapbox.getInstance(context, null, WellKnownTileServer.MapLibre)
        HttpRequestUtil.setOkHttpClient(
            OkHttpClient.Builder()
                .addInterceptor(AWSRequestSignerInterceptor(plugin))
                .build()
        )
    }

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
