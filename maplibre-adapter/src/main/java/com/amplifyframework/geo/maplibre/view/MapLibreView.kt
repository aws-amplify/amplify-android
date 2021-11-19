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
import android.graphics.BitmapFactory
import android.util.AttributeSet
import androidx.annotation.UiThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.amplifyframework.core.Amplify
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.maplibre.AmplifyMapLibreAdapter
import com.amplifyframework.geo.maplibre.R
import com.amplifyframework.geo.models.MapStyle
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.ClusterOptions
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.style.layers.Property.ICON_ROTATION_ALIGNMENT_VIEWPORT
import com.mapbox.mapboxsdk.utils.BitmapUtils
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color

import android.graphics.drawable.VectorDrawable
import androidx.core.util.Pair
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage
import com.mapbox.mapboxsdk.style.layers.SymbolLayer


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

        val PLACE_ICON_NAME = "place"
    }

    private val adapter: AmplifyMapLibreAdapter by lazy {
        AmplifyMapLibreAdapter(context, geo)
    }

//    private var ready: Boolean = false
//        set(value) {
//            field = value
//            onMapReadyListener?.onReady(this.map)
//        }

    lateinit var style: Style

    lateinit var symbolManager: SymbolManager

//    var onMapReadyListener: OnMapReadyListener? = null

    var defaultPlaceIcon =
        AppCompatResources.getDrawable(context, R.drawable.place)!!

    var defaultPlaceIconColor = ContextCompat.getColor(context, R.color.search_placeIconColor)

    var clusterOptions: ClusterOptions = ClusterOptions()
//        .withCircleRadius(
//            Expre
//        )
        .withCircleRadius(Expression.literal(25))
        .withColorLevels(arrayOf(
            Pair(0, Color.GRAY),
            Pair(25, Color.BLUE),
            Pair(50, Color.YELLOW),
            Pair(75, Color.RED),
            Pair(100, Color.MAGENTA),
        ))

    init {
        setup(context, options)
    }

    @SuppressLint("MissingSuperCall")
    override fun initialize(context: Context, options: MapLibreOptions) {
        // defer the execution after the Adapter is initialized
        // see setup() where the superclass initialize is called
    }

//    fun onMapReady(listener: (MapboxMap, Style) -> Unit) {
//        this.onMapReadyListener = object : OnMapReadyListener {
//            override fun onReady(map: MapboxMap, style: Style) {
//                listener(map, style)
//            }
//        }
//    }

    fun setStyle(style: MapStyle? = null) {
        getMapAsync { map ->
            adapter.setStyle(map, style) {
                log.verbose("Amazon Location styles applied to MapView")
                this.style = it.apply {
                    addImage(
                        PLACE_ICON_NAME,
                        BitmapFactory.decodeResource(resources, R.drawable.place)
//                        BitmapUtils.getBitmapFromDrawable(defaultPlaceIcon)!!,
//                        ,true
                    )
                    addLayer(SymbolLayer("places", "places").apply {
                        setProperties(
                            iconImage(PLACE_ICON_NAME)
                        )
                    })
                    println("**************************")
                    println(layers.joinToString(", ") { layer -> layer.id })
                    println("**************************")
//                    addLayer(SymbolLayer())
                }
//                this.symbolManager = SymbolManager(this, map, it, null, clusterOptions).apply {
//                this.symbolManager = SymbolManager(this, map, it, "places", clusterOptions).apply {
//                this.symbolManager = SymbolManager(this, map, it, "places", clusterOptions).apply {
                this.symbolManager = SymbolManager(this, map, it).apply {
                    iconAllowOverlap = true
                    iconRotationAlignment = ICON_ROTATION_ALIGNMENT_VIEWPORT
                }
            }
        }
    }

    private fun setup(context: Context, options: MapLibreOptions) {
        if (context is LifecycleOwner) {
            context.lifecycle.addObserver(LifecycleHandler())
        }
        adapter.initialize()
        super.initialize(context, options)
    }

    internal fun loadDefaultStyle() {
        setStyle(null)
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

//    interface OnMapReadyListener {
//        fun onReady(map: MapboxMap, style: Style)
//    }

}
