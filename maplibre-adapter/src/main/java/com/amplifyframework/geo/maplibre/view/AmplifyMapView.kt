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

typealias MapLibreView = com.mapbox.mapboxsdk.maps.MapView
typealias MapViewOptions = com.mapbox.mapboxsdk.maps.MapboxMapOptions

/**
 * The AmplifyMapView encapsulates the MapLibre map integration with Amplify.Geo.
 */
class AmplifyMapView
@JvmOverloads @UiThread constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    options: MapViewOptions = MapViewOptions.createFromAttributes(context, attrs),
    private val geo: GeoCategory = Amplify.Geo
) : MapLibreView(context, attrs, defStyleAttr) {

    companion object {
        private val log = Amplify.Logging.forNamespace("amplify:maplibre-adapter:view")
    }

    private val adapter: AmplifyMapLibreAdapter by lazy {
        AmplifyMapLibreAdapter(context, geo)
    }

    init {
        if (context is LifecycleOwner) {
            context.lifecycle.addObserver(LifecycleHandler())
        }
        adapter.initialize()
        super.initialize(context, options)
    }

    @SuppressLint("MissingSuperCall")
    override fun initialize(context: Context, options: MapViewOptions) {
        // defer the execution after the Adapter is initialized
        // see the init block call above where the superclass initialize is called
    }

    internal fun loadDefaultStyle() {
        getMapAsync { map ->
            adapter.setStyle(map) { log.verbose("Amazon Location styles applied to MapView") }
        }
    }

    inner class LifecycleHandler : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fun onCreate() {
            this@AmplifyMapView.onCreate(null)
            this@AmplifyMapView.loadDefaultStyle()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onStart() {
            this@AmplifyMapView.onStart()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun onStop() {
            this@AmplifyMapView.onStop()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun onPause() {
            this@AmplifyMapView.onPause()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onResume() {
            this@AmplifyMapView.onResume()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            this@AmplifyMapView.onDestroy()
        }
    }

}
