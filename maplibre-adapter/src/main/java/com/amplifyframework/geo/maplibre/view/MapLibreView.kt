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
import android.view.Gravity
import androidx.annotation.UiThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.maplibre.AmplifyMapLibreAdapter
import com.amplifyframework.geo.maplibre.R
import com.amplifyframework.geo.maplibre.view.support.AttributionInfoView
import com.amplifyframework.geo.models.MapStyle
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.Symbol
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource

typealias MapLibreOptions = MapLibreMapOptions

/**
 * The MapLibreView encapsulates the MapBox map integration with `Amplify.Geo` and
 * serves as the foundation of map components.
 *
 * The requests of map tiles and features are routed to the Amazon Location Services,
 * check the documentation at [https://docs.amplify.aws/lib/geo/getting-started/q/platform/android]
 */
class MapLibreView
@JvmOverloads @UiThread
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    options: MapLibreOptions = MapLibreOptions
        .createFromAttributes(context, attrs)
        .logoEnabled(false)
        .attributionEnabled(false),
    private val geo: GeoCategory = Amplify.Geo
) : MapView(context, attrs, defStyleAttr) {

    companion object {
        private val log = Amplify.Logging.logger(CategoryType.GEO, "amplify:maplibre-adapter")

        // Marked as internal for testing purposes
        internal const val CLUSTER_CIRCLE_LAYER_ID = "cluster-circles"
        internal const val CLUSTER_NUMBER_LAYER_ID = "cluster-numbers"

        const val PLACE_ICON_NAME = "place"
        const val PLACE_ACTIVE_ICON_NAME = "place-active"
    }

    private val adapter: AmplifyMapLibreAdapter by lazy {
        AmplifyMapLibreAdapter(context, geo)
    }

    private val attributionInfoView by lazy {
        AttributionInfoView(context)
    }

    lateinit var symbolManager: SymbolManager
    internal lateinit var symbolOnClickListener: (Symbol) -> Boolean

    var defaultPlaceIcon = R.drawable.place
    var defaultPlaceActiveIcon = R.drawable.place_active

    private var shouldCluster = true
    private var clusteringOptions = ClusteringOptions.defaults()
    private var mapStyle: MapStyle? = null

    init {
        setup(context, options)
        addView(
            attributionInfoView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.START

                val margin = context.resources.getDimensionPixelSize(R.dimen.map_defaultMargin)
                marginEnd = margin
                marginStart = margin
                bottomMargin = margin
            }
        )
    }

    @SuppressLint("MissingSuperCall")
    override fun initialize(context: Context, options: MapLibreOptions) {
        // defer the execution after the Adapter is initialized
        // see setup() where the superclass initialize is called
    }

    /**
     * Get the both the map and its style asynchronously.
     *
     * @param callback the event listener
     */
    fun getStyle(callback: OnStyleLoaded) {
        getMapAsync { map ->
            map.getStyle { style ->
                callback.onLoad(map, style)
            }
        }
    }

    /**
     * Get both the map and its style asynchronously.
     *
     * **Implementation notes:** This is a shortcut to the existing nested callback solution:
     *
     * ```
     * getMapAsync { map ->
     *   map.getStyle { style ->
     *     // use APIs that depend on both map and its style
     *   }
     * }
     * ```
     *
     * @param callback the onLoad lambda
     */
    fun getStyle(callback: (MapLibreMap, Style) -> Unit) {
        getStyle(
            object : OnStyleLoaded {
                override fun onLoad(map: MapLibreMap, style: Style) {
                    callback(map, style)
                }
            }
        )
    }

    /**
     * Update the map using the passed style. If no style is set, the default
     * configured using the Amplify CLI is used.
     *
     * @param style the map style object
     * @param callback the function called when the style is done loaded
     */
    fun setStyle(style: MapStyle? = null, callback: Style.OnStyleLoaded) {
        getMapAsync { map ->
            adapter.setStyle(map, style) {
                mapStyle = style
                // setup the symbol manager
                it.apply {
                    addImage(
                        PLACE_ICON_NAME,
                        BitmapFactory.decodeResource(resources, defaultPlaceIcon)
                    )
                    addImage(
                        PLACE_ACTIVE_ICON_NAME,
                        BitmapFactory.decodeResource(resources, defaultPlaceActiveIcon)
                    )
                }

                // Clear the current symbols from the map since a new SymbolManager will be created
                if (this::symbolManager.isInitialized) {
                    this.symbolManager.deleteAll()
                }

                removeClusterLayers(it)
                if (shouldCluster) {
                    enableClustering(map, it)
                } else {
                    this.symbolManager = SymbolManager(this, map, it, null, null).apply {
                        iconAllowOverlap = true
                        iconIgnorePlacement = true
                    }
                }

                if (this::symbolOnClickListener.isInitialized) {
                    this.symbolManager.addClickListener(symbolOnClickListener)
                }

                callback.onStyleLoaded(it)
            }
        }
    }

    /**
     * Set whether the map features should cluster and the style for those clusters. Clustering is
     * enabled by default with the default ClusteringOptions.
     * @param shouldCluster true if clustering should be enabled, false if clustering should be disabled.
     * @param options the ClusteringOptions. If set to null and shouldCluster is true, uses
     *      the default ClusteringOptions.
     * @param callback the callback invoked after clustering has been enabled or disabled.
     */
    fun setClusterBehavior(shouldCluster: Boolean, options: ClusteringOptions?, callback: () -> Unit) {
        this.shouldCluster = shouldCluster
        this.clusteringOptions = options ?: ClusteringOptions.defaults()
        setStyle(mapStyle) {
            callback()
        }
    }

    private fun removeClusterLayers(style: Style) {
        style.apply {
            removeLayer(CLUSTER_CIRCLE_LAYER_ID)
            removeLayer(CLUSTER_NUMBER_LAYER_ID)
        }
    }

    private fun enableClustering(map: MapLibreMap, style: Style) {
        val geoJsonClusterOptions = GeoJsonOptions().withCluster(true)
            .withClusterMaxZoom(clusteringOptions.maxClusterZoomLevel)
            .withClusterRadius(clusteringOptions.clusterRadius)
        this.symbolManager = SymbolManager(this, map, style, null, null, geoJsonClusterOptions).apply {
            iconAllowOverlap = true
            iconIgnorePlacement = true
        }

        val geoJsonSources = style.sources.filterIsInstance<GeoJsonSource>()
        val geoJsonSourceId = geoJsonSources[0].id

        // Create a circle layer for cluster circles
        val clusterCircleLayer = CircleLayer(CLUSTER_CIRCLE_LAYER_ID, geoJsonSourceId)
        val circleColorProperty = if (clusteringOptions.clusterColorSteps.isEmpty()) {
            PropertyFactory.circleColor(clusteringOptions.clusterColor)
        } else {
            val circleColorStops =
                clusteringOptions.clusterColorSteps.toSortedMap().flatMap { (pointCount, clusterColor) ->
                    mutableListOf(Expression.stop(pointCount, Expression.color(clusterColor)))
                }.toTypedArray()
            PropertyFactory.circleColor(
                Expression.step(
                    Expression.get("point_count"),
                    Expression.color(clusteringOptions.clusterColor),
                    *circleColorStops
                )
            )
        }

        // Change the circle radius based on zoom level
        val circleRadiusExpression = Expression.interpolate(
            Expression.exponential(1.75),
            Expression.zoom(),
            Expression.stop(map.minZoomLevel, 60),
            Expression.stop(clusteringOptions.maxClusterZoomLevel, 20)
        )

        clusterCircleLayer.setProperties(
            circleColorProperty,
            PropertyFactory.circleRadius(circleRadiusExpression)
        )
        clusterCircleLayer.setFilter(Expression.has("point_count"))

        // Create a symbol layer for cluster numbers (point count)
        val clusterNumberLayer = SymbolLayer(CLUSTER_NUMBER_LAYER_ID, geoJsonSourceId)
        clusterNumberLayer.setProperties(
            PropertyFactory.textField(Expression.toString(Expression.get("point_count"))),
            PropertyFactory.textFont(arrayOf("Arial Bold")),
            PropertyFactory.textColor(clusteringOptions.clusterNumberColor),
            PropertyFactory.textIgnorePlacement(true),
            PropertyFactory.textAllowOverlap(true)
        )

        style.apply {
            addLayer(clusterCircleLayer)
            addLayer(clusterNumberLayer)
        }

        // Set the behavior when a cluster is clicked
        map.addOnMapClickListener { latLngPoint ->
            val pointClicked = map.projection.toScreenLocation(latLngPoint)
            val features = map.queryRenderedFeatures(pointClicked, "cluster-circles")
            if (features.isEmpty()) {
                false
            } else {
                clusteringOptions.onClusterClicked(this, features[0])
                true
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
        setStyle { log.verbose("Amazon Location default styles loaded") }
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

    /**
     * Callback interface that is invoked when both the map and its style are fully loaded.
     */
    interface OnStyleLoaded {
        fun onLoad(map: MapLibreMap, style: Style)
    }
}
