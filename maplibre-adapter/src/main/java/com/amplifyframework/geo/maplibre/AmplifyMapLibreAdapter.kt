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
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.location.AWSLocationGeoPlugin
import com.amplifyframework.geo.maplibre.http.AWSRequestSignerInterceptor
import com.amplifyframework.geo.models.MapStyle
import com.amplifyframework.geo.options.GetMapStyleDescriptorOptions
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil

private const val LOCATION_GEO_PLUGIN_KEY = "awsLocationGeoPlugin"

class AmplifyMapLibreAdapter internal constructor(
    private val context: Context,
    private val geo: GeoCategory = Amplify.Geo
) {

    companion object {
        private val log = Amplify.Logging.logger(CategoryType.GEO, "amplify:maplibre-adapter")
    }

    private val plugin: AWSLocationGeoPlugin by lazy {
        geo.getPlugin(LOCATION_GEO_PLUGIN_KEY) as AWSLocationGeoPlugin
    }

    /**
     * Initialize the Mapbox instance and add the [AWSRequestSignerInterceptor]
     * to the default [OkHttpClient] used by the underlying Mapbox component.
     */
    fun initialize() {
        MapLibre.getInstance(context, null, WellKnownTileServer.MapLibre)
        HttpRequestUtil.setOkHttpClient(
            OkHttpClient.Builder()
                .addInterceptor(AWSRequestSignerInterceptor(plugin))
                .build()
        )
    }

    /**
     * Convenience method to use style from Amplify directly with MapLibre's [MapboxMap].
     *
     * @param map MapLibre's map instance to load style on
     * @param style Amplify map style to use
     * @param callback Callback to trigger upon successfully loading map style
     */
    fun setStyle(map: MapLibreMap, style: MapStyle? = null, callback: Style.OnStyleLoaded) {
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
