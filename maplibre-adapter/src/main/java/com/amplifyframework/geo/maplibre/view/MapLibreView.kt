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

package com.amplifyframework.geo.maplibre.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.annotation.UiThread
import androidx.lifecycle.*
import com.amplifyframework.core.Amplify
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.maplibre.AmplifyMapLibreAdapter

typealias MapBoxView = com.mapbox.mapboxsdk.maps.MapView
typealias MapLibreOptions = com.mapbox.mapboxsdk.maps.MapboxMapOptions

/**
 * The MapLibreView encapsulates the legacy MapBox map integration with Amplify.Geo.
 */
class MapLibreView
@JvmOverloads @UiThread constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    options: MapLibreOptions = MapLibreOptions.createFromAttributes(context, attrs),
    private val geo: GeoCategory = Amplify.Geo
) : MapBoxView(context, attrs, defStyleAttr) {

    companion object {
        private val log = Amplify.Logging.forNamespace("amplify:maplibre-adapter")
    }

    private val adapter: AmplifyMapLibreAdapter by lazy {
        AmplifyMapLibreAdapter(context, geo)
    }

    init {
        setup(context, options)
    }

    @SuppressLint("MissingSuperCall")
    override fun initialize(context: Context, options: MapLibreOptions) {
        // defer the execution after the Adapter is initialized
        // see setup() where the superclass initialize is called
    }

    private fun setup(context: Context, options: MapLibreOptions) {
        if (context is LifecycleOwner) {
            context.lifecycle.addObserver(LifecycleHandler())
        }
        adapter.initialize()
        super.initialize(context, options)
    }

    internal fun loadDefaultStyle() {
        getMapAsync { map ->
            adapter.setStyle(map) { log.verbose("Amazon Location styles applied to MapView") }
        }
    }

    inner class LifecycleHandler : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            this@MapLibreView.onCreate(null)
            this@MapLibreView.loadDefaultStyle()
        }

        override fun onStart(owner: LifecycleOwner) {
            this@MapLibreView.onStart()
        }

        override fun onStop(owner: LifecycleOwner) {
            this@MapLibreView.onStop()
        }

        override fun onPause(owner: LifecycleOwner) {
            this@MapLibreView.onPause()
        }

        override fun onResume(owner: LifecycleOwner) {
            this@MapLibreView.onResume()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            this@MapLibreView.onDestroy()
        }
    }

}
