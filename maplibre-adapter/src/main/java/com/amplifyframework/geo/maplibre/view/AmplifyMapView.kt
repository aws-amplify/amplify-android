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

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import androidx.annotation.UiThread
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams.MATCH_PARENT
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams.WRAP_CONTENT
import androidx.core.content.ContextCompat
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.location.models.AmazonLocationPlace
import com.amplifyframework.geo.maplibre.R
import com.amplifyframework.geo.maplibre.util.getPlaceData
import com.amplifyframework.geo.maplibre.util.parseCoordinates
import com.amplifyframework.geo.maplibre.util.toCoordinates
import com.amplifyframework.geo.maplibre.util.toJsonElement
import com.amplifyframework.geo.maplibre.util.toLatLng
import com.amplifyframework.geo.maplibre.view.support.MapControls
import com.amplifyframework.geo.maplibre.view.support.PlaceInfoPopupView
import com.amplifyframework.geo.maplibre.view.support.fadeIn
import com.amplifyframework.geo.maplibre.view.support.fadeOut
import com.amplifyframework.geo.models.SearchArea
import com.amplifyframework.geo.options.GeoSearchByTextOptions
import com.amplifyframework.geo.result.GeoSearchResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import kotlin.math.absoluteValue
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.plugins.annotation.Symbol
import org.maplibre.android.plugins.annotation.SymbolOptions

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
@JvmOverloads @UiThread
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val options: MapViewOptions = MapViewOptions.createFromAttributes(
        context,
        attrs,
        defStyleAttr
    ),
    private val geo: GeoCategory = Amplify.Geo
) : CoordinatorLayout(context, attrs, defStyleAttr) {

    companion object {
        private val log = Amplify.Logging.logger(CategoryType.GEO, "amplify:maplibre-adapter")
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

    /**
     * The reference to the search text field.
     */
    val searchField by lazy {
        SearchTextField(context)
    }

    private val searchResultView by lazy {
        SearchResultListView(context).apply {
            onItemClick(::handleOnItemClick)
        }
    }

    private val placeInfoPopupView by lazy {
        PlaceInfoPopupView(context).apply {
            alpha = 0f
            visibility = GONE
            onVisibilityChanged { isVisible ->
                if (!isVisible) {
                    deselectActiveSymbol()
                }
            }
        }
    }

    private val updateSearchButton by lazy {
        Button(context).apply {
            alpha = 0f
            elevation = context.resources.getDimension(R.dimen.map_controls_elevation)
            text = context.getString(R.string.map_search_updateSearchArea)
            visibility = GONE

            setTextColor(ContextCompat.getColor(context, R.color.map_controls_foreground))
            setBackgroundResource(R.drawable.map_search_update_background)

            val padding = context.resources.getDimensionPixelSize(
                R.dimen.map_search_updateButtonPadding
            )
            setPaddingRelative(padding, 0, padding, 0)
            setOnClickListener {
                this.fadeOut()
                search(lastQuery.orEmpty())
            }
        }
    }

    private val bottomSheet by lazy {
        BottomSheetBehavior.from(searchResultView)
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

    // Search query state
    private var lastQuery: String? = null
    private var lastQueryBounds: LatLngBounds? = null

    // Place markers state
    private var activeSymbol: Symbol? = null
    private var symbols: List<Symbol> = listOf()

    init {
        val defaultMargin = context.resources.getDimensionPixelSize(R.dimen.map_controls_margin)
        val largeMargin = defaultMargin * 3

        overlayLayout.addView(
            placeInfoPopupView,
            RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                marginStart = largeMargin
                marginEnd = largeMargin
            }
        )
        overlayLayout.addView(
            searchField,
            RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                marginStart = defaultMargin
                marginEnd = defaultMargin
                topMargin = defaultMargin
                addRule(RelativeLayout.ALIGN_PARENT_TOP)
            }
        )
        if (options.shouldRenderControls()) {
            overlayLayout.addView(
                controls,
                RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    topMargin = defaultMargin
                    marginEnd = defaultMargin
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                    addRule(RelativeLayout.BELOW, R.id.map_search_input)
                }
            )
        }
        overlayLayout.addView(
            updateSearchButton,
            RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                marginStart = defaultMargin
                marginEnd = defaultMargin
                topMargin = largeMargin
                addRule(RelativeLayout.BELOW, R.id.map_search_input)
                addRule(RelativeLayout.CENTER_HORIZONTAL)
            }
        )
        addView(mapView, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(overlayLayout, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(
            searchResultView,
            LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                behavior = BottomSheetBehavior<SearchResultListView>().apply {
                    topMargin = context.resources.getDimensionPixelSize(
                        R.dimen.map_search_visibleArea
                    )
                    addBottomSheetCallback(BottomSheetCallback())
                }
            }
        )
        adjustMapCenter()
        bindEvents()
    }

    /**
     * Functional listener for [onPlaceSelectListener].
     */
    fun onPlaceSelect(listener: (AmazonLocationPlace, Symbol) -> Unit) {
        onPlaceSelectListener = object : OnPlaceSelectListener {
            override fun onSelect(place: AmazonLocationPlace, symbol: Symbol) {
                listener(place, symbol)
            }
        }
    }

    private fun handleOnItemClick(place: AmazonLocationPlace) {
        symbols.find { it.getPlaceData().id == place.id }?.let { symbol ->
            searchField.searchMode = SearchTextField.SearchMode.MAP
            handlePlaceMarkerClick(symbol, toggle = false)
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
        mapView.getStyle { map, _ ->
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
            searchField.onSearchAction(::handleSearchAction)

            // bind map camera events

            mapView.addOnCameraIsChangingListener {
                updatePlaceInfoViewPosition()
            }
            map.addOnCameraMoveListener {
                val camera = map.cameraPosition
                controls.compassIndicatorButton.rotateIcon(camera.bearing, animate = false)
                updateZoomControls(camera)
                updateSearchBounds(map.projection.visibleRegion.latLngBounds)
                updatePlaceInfoViewPosition()
            }
            mapView.symbolOnClickListener = { symbol ->
                handlePlaceMarkerClick(symbol, true)
            }
            mapView.symbolManager.addClickListener(mapView.symbolOnClickListener)
        }
    }

    private fun handleSearchAction(query: String) {
        updateSearchButton.fadeOut()
        if (query.isNotBlank()) {
            search(query.trim())
        } else {
            clearSearch()
            updateSearchResults()
        }
    }

    private fun clearSearch() {
        places = listOf()
        activeSymbol = null
        lastQuery = null
        lastQueryBounds = null
    }

    private fun search(query: String) = withMap { map ->
        val coordinates = parseCoordinates(query)
        if (coordinates != null) {
            geo.searchByCoordinates(coordinates, ::onSearchResult, ::onSearchError)
        } else {
            val options = GeoSearchByTextOptions
                .builder()
                .searchArea(
                    SearchArea.near(map.cameraPosition.target!!.toCoordinates())
                )
                .build()
            geo.searchByText(query, options, ::onSearchResult, ::onSearchError)
        }
        this.lastQuery = query
        this.lastQueryBounds = map.projection.visibleRegion.latLngBounds
    }

    private fun handlePlaceMarkerClick(symbol: Symbol, toggle: Boolean = true): Boolean {
        if (activeSymbol == symbol && toggle) {
            deselectActiveSymbol()
        } else {
            activeSymbol?.let {
                it.iconImage = MapLibreView.PLACE_ICON_NAME
                it.symbolSortKey = symbols.indexOf(symbol).toFloat()
                mapView.symbolManager.update(it)
            }
            symbol.iconImage = MapLibreView.PLACE_ACTIVE_ICON_NAME
            symbol.symbolSortKey = symbols.size.toFloat()
            mapView.symbolManager.update(symbol)
            activeSymbol = symbol

            // update map UI based on the selected place
            val place = symbol.getPlaceData()
            withMap { map ->
                val zoom = map.cameraPosition.zoom.coerceAtLeast(12.0)
                if (zoom == map.cameraPosition.zoom) {
                    map.animateCamera(CameraUpdateFactory.newLatLng(symbol.latLng))
                } else {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(symbol.latLng, zoom))
                }

                val ref = map.projection.toScreenLocation(place.coordinates.toLatLng())
                placeInfoPopupView.show(place, ref)
            }

            onPlaceSelectListener?.onSelect(place, symbol)
        }
        return true
    }

    private fun deselectActiveSymbol() {
        activeSymbol?.let { symbol ->
            placeInfoPopupView.hide()
            symbol.iconImage = MapLibreView.PLACE_ICON_NAME
            symbol.symbolSortKey = symbols.indexOf(symbol).toFloat()
            mapView.symbolManager.update(symbol)
            activeSymbol = null
        }
    }

    private fun onSearchResult(result: GeoSearchResult) = post {
        places = result.places.filterIsInstance<AmazonLocationPlace>()
        updateSearchResults()
    }

    private fun onSearchError(exception: GeoException) = post {
        log.error(exception.message, exception)
        val defaultError = mapView.context.getString(R.string.map_search_defaultError, lastQuery)
        val error = exception.message ?: defaultError
        Snackbar
            .make(this@AmplifyMapView, error, 3000)
            .show()
    }

    private fun updateSearchResults() = withMap {
        placeInfoPopupView.hide()
        if (places.size == 1) {
            val singleResult = places.first()
            it.animateCamera(
                CameraUpdateFactory.newLatLng(
                    singleResult.coordinates.toLatLng()
                )
            )
        }
        activeSymbol = null
        mapView.symbolManager.deleteAll()
        symbols = mapView.symbolManager.create(
            places.mapIndexed { index, place ->
                SymbolOptions()
                    .withSymbolSortKey(index.toFloat())
                    .withData(place.toJsonElement())
                    .withLatLng(place.coordinates.toLatLng())
                    .withIconImage(MapLibreView.PLACE_ICON_NAME)
            }
        )
        searchResultView.places = places
    }

    private fun updateZoomControls(camera: CameraPosition) {
        controls.zoomOutButton.isEnabled = camera.zoom > options.minZoomLevel
        controls.zoomInButton.isEnabled = camera.zoom < options.maxZoomLevel
    }

    private fun updateSearchBounds(bounds: LatLngBounds) {
        val threshold = 0.05
        lastQueryBounds?.let {
            val boundariesUpdated =
                (it.latitudeNorth - bounds.latitudeNorth).absoluteValue > threshold ||
                    (it.longitudeWest - bounds.longitudeWest).absoluteValue > threshold ||
                    (it.span.latitudeSpan - bounds.span.latitudeSpan).absoluteValue > threshold ||
                    (it.span.longitudeSpan - bounds.span.longitudeSpan).absoluteValue > threshold
            if (boundariesUpdated) {
                updateSearchButton.fadeIn()
            }
        }
    }

    private fun updatePlaceInfoViewPosition() = withMap { map ->
        activeSymbol?.let { symbol ->
            val position = map.projection.toScreenLocation(symbol.latLng)
            placeInfoPopupView.update(position)
        }
    }

    private fun withMap(block: (MapLibreMap) -> Unit) {
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

    /**
     * Event listener interface that handles place selection changes.
     */
    interface OnPlaceSelectListener {
        fun onSelect(place: AmazonLocationPlace, symbol: Symbol)
    }
}
