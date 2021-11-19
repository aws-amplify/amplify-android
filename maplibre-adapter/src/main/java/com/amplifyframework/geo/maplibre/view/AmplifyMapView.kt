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

import LoadingView
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.annotation.UiThread
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams.MATCH_PARENT
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams.WRAP_CONTENT
import androidx.core.content.ContextCompat
import com.amplifyframework.core.Amplify
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.location.models.AmazonLocationPlace
import com.amplifyframework.geo.maplibre.R
import com.amplifyframework.geo.maplibre.util.*
import com.amplifyframework.geo.maplibre.view.support.MapControls
import com.amplifyframework.geo.models.BoundingBox
import com.amplifyframework.geo.models.SearchArea
import com.amplifyframework.geo.options.GeoSearchByTextOptions
import com.amplifyframework.geo.result.GeoSearchResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.Feature
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import kotlin.math.abs

/**
 * The AmplifyMapView encapsulates the MapLibre map integration with Amplify.Geo and introduces
 * a handful of built-in features to the plain MapView, such as zoom controls, compass, search field,
 * and map markers.
 *
 * **Implementation note:** This view inherits from [CoordinatorLayout], therefore any custom
 * component can be added as an overlay if needed. The underlying map component can be accessed
 * through the `mapView` property for map-related listeners and properties.
 */
class AmplifyMapView
@JvmOverloads @UiThread constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val options: MapViewOptions = MapViewOptions.createFromAttributes(
        context,
        attrs,
        defStyleAttr
    ),
    private val geo: GeoCategory = Amplify.Geo
) : CoordinatorLayout(context) {

    companion object {
        private val log = Amplify.Logging.forNamespace("amplify:maplibre-adapter")
    }

    /**
     * The reference to the underlying map view.
     */
    val mapView by lazy {
        MapLibreView(
            context = context,
            options = options.toMapLibreOptions(context),
            geo = geo
        )
    }

    /**
     * The reference to the container used to overlay components on top of the [mapView]
     */
    val overlayLayout by lazy { RelativeLayout(context) }

    val searchField by lazy {
        SearchTextField(context)
    }

    val searchResultView by lazy {
        SearchResultListView(context)
    }

    private val bottomSheet by lazy {
        BottomSheetBehavior.from(searchResultView)
    }

    private val loadingView by lazy {
        LoadingView(context)
    }

    private val controls by lazy {
        MapControls(
            context,
            showCompassIndicator = options.showCompassIndicator,
            showZoomControls = options.showZoomControls
        )
    }

    var places: List<AmazonLocationPlace> = listOf()

    var onPlaceSelectListener: OnPlaceSelectListener? = null

    private var lastQuery: String? = null

    private var lastQueryBounds: LatLngBounds? = null

    private var symbols: List<Symbol> = listOf()

    init {
        val defaultMargin = context.resources.getDimensionPixelSize(R.dimen.controls_margin)

        overlayLayout.addView(
            searchField,
            RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                marginStart = defaultMargin
                marginEnd = defaultMargin
                topMargin = defaultMargin
                addRule(RelativeLayout.ALIGN_PARENT_TOP)
            }
        )
        overlayLayout.addView(loadingView, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        if (options.shouldRenderControls()) {
            overlayLayout.addView(
                controls,
                RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    topMargin = defaultMargin
                    marginEnd = defaultMargin
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                    addRule(RelativeLayout.BELOW, R.id.amplify_map_search_input)
                }
            )
        }
        addView(mapView, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(overlayLayout, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(searchResultView, LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            behavior = BottomSheetBehavior<SearchResultListView>().apply {
                topMargin = context.resources.getDimensionPixelSize(R.dimen.search_visibleArea)
                addBottomSheetCallback(BottomSheetCallback())
            }
        })
        adjustMapCenter()
        bindEvents()
    }


    fun onPlaceSelect(listener: (AmazonLocationPlace, Symbol) -> Unit) {
        onPlaceSelectListener = object : OnPlaceSelectListener {
            override fun onSelect(place: AmazonLocationPlace, symbol: Symbol) {
                listener(place, symbol)
            }
        }
    }

    private fun adjustMapCenter() = withMap {
        it.animateCamera(CameraUpdateFactory.paddingTo(0.0, 80.0, 0.0, 0.0))
    }

    private fun bindEvents() {
        searchField.onSearchModeChange {
            bottomSheet.state = when (it) {
                SearchTextField.SearchMode.LIST -> BottomSheetBehavior.STATE_EXPANDED
                SearchTextField.SearchMode.MAP -> BottomSheetBehavior.STATE_COLLAPSED
            }
        }
        searchField.onSearchAction {
            if (it.isNotBlank()) {
                search(it.trim())
            } else {
                places = listOf()
                updateSearchResults()
            }
        }
        mapView.getMapAsync { map ->
            updateZoomControls(map.cameraPosition)
            controls.compassIndicatorButton.onClick {
                map.animateCamera(CameraUpdateFactory.bearingTo(0.0))
            }
            controls.zoomOutButton.onClick {
                map.animateCamera(CameraUpdateFactory.zoomOut())
            }
            controls.zoomInButton.onClick {
                map.animateCamera(CameraUpdateFactory.zoomIn())
            }
            map.addOnCameraMoveListener {
                val camera = map.cameraPosition
                controls.compassIndicatorButton.rotateIcon(camera.bearing)
                updateZoomControls(camera)
//                updateSearchBounds(map.projection.visibleRegion.latLngBounds)
            }
//            mapView.symbolManager.addClickListener { selected ->
//                symbols.forEach { symbol ->
//                    symbol.apply {
//                        iconOpacity = 0.9f
//                        iconSize = 1f
//                    }
//                    mapView.symbolManager.update(symbol)
//                }
//                val place = selected.getPlaceData()
//                selected.apply {
//                    iconOpacity = 1f
//                    iconSize = 1.2f
//                }
//                return@addClickListener false
//            }
        }
    }

    private fun search(query: String) = withMap { map ->
        val coordinates = parseCoordinates(query)
        if (coordinates != null) {
            geo.searchByCoordinates(coordinates, ::onSearchResult, ::onSearchError)
        } else {
            val options = GeoSearchByTextOptions
                .builder()
                .searchArea(
                    SearchArea.within(
                        BoundingBox(
                            map.projection.visibleRegion.nearLeft.toCoordinates(),
                            map.projection.visibleRegion.farRight.toCoordinates()
                        )
                    )
//                    SearchArea.near(
//                        map.cameraPosition.target.toCoordinates()
//                    )
                )
                .build()
            geo.searchByText(query, options, ::onSearchResult, ::onSearchError)
        }
        this.lastQuery = query
        this.lastQueryBounds = map.projection.visibleRegion.latLngBounds
    }

    private fun onSearchResult(result: GeoSearchResult) = post {
        loadingView.hide()
        places = result.places.filterIsInstance<AmazonLocationPlace>()
        updateSearchResults()
    }

    private fun onSearchError(exception: GeoException) = post {
        loadingView.hide()
        log.error(exception.message, exception)
        val defaultError = mapView.context.getString(R.string.search_defaultError, lastQuery)
        val error = exception.message ?: defaultError
        Snackbar
            .make(this@AmplifyMapView, error, Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun updateSearchResults() = withMap {
        println("-----------------------------------")
        println(places)
        println("-----------------------------------")
        if (places.size == 1) {
            val singleResult = places.first()
            it.animateCamera(
                CameraUpdateFactory.newLatLng(
                    singleResult.coordinates.toLatLng()
                )
            )
        }
        mapView.symbolManager.deleteAll()
        symbols = mapView.symbolManager.create(places.map { place ->
            SymbolOptions()
                .withData(place.toJsonElement())
                .withLatLng(place.coordinates.toLatLng())
                .withIconImage(MapLibreView.PLACE_ICON_NAME)
//                .withIconOpacity(0.8f)
//                .withIconColor(String.format("#%06X", (0xFFFFFF and mapView.defaultPlaceIconColor)))
        })
        println("-----------")
        println(symbols)
        searchResultView.places = places
    }

    private fun updateZoomControls(camera: CameraPosition) {
        controls.zoomOutButton.isEnabled = camera.zoom > options.minZoomLevel
        controls.zoomInButton.isEnabled = camera.zoom < options.maxZoomLevel
    }

    private fun withMap(block: (MapboxMap) -> Unit) {
        mapView.getMapAsync(block)
    }

    internal inner class BottomSheetCallback : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                searchField.searchMode = SearchTextField.SearchMode.MAP
            } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                searchField.searchMode = SearchTextField.SearchMode.LIST
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit

    }

    interface OnPlaceSelectListener {
        fun onSelect(place: AmazonLocationPlace, symbol: Symbol)
    }

}
