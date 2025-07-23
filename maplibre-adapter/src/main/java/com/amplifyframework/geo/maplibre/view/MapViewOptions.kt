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
import android.content.res.TypedArray
import android.os.Parcelable
import android.util.AttributeSet
import androidx.annotation.StyleableRes
import com.amplifyframework.geo.maplibre.Coordinate2D
import com.amplifyframework.geo.maplibre.R
import kotlinx.parcelize.Parcelize
import org.maplibre.android.camera.CameraPosition

const val MAX_ZOOM_BOUNDARY = 22.0
const val MIN_ZOOM_BOUNDARY = 0.0

const val DEFAULT_MIN_ZOOM = 3.0
const val DEFAULT_MAX_ZOOM = 18.0
const val DEFAULT_ZOOM = 14.0

const val DEFAULT_SHOW_COMPASS_INDICATOR = true
const val DEFAULT_SHOW_USER_LOCATION = false
const val DEFAULT_SHOW_ZOOM_CONTROLS = false

@SuppressLint("ParcelCreator")
@Parcelize
class MapViewOptions(
    val attribution: String? = null,
    val center: Coordinate2D = Coordinate2D(),
    val minZoomLevel: Double = DEFAULT_MIN_ZOOM,
    val maxZoomLevel: Double = DEFAULT_MAX_ZOOM,
    val showCompassIndicator: Boolean = DEFAULT_SHOW_COMPASS_INDICATOR,
    val showUserLocation: Boolean = DEFAULT_SHOW_USER_LOCATION,
    val showZoomControls: Boolean = DEFAULT_SHOW_ZOOM_CONTROLS,
    val zoomLevel: Double = DEFAULT_ZOOM
) : Parcelable {

    companion object {

        fun createFromAttributes(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0): MapViewOptions =
            attrs?.let {
                val typedArray = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.map_AmplifyMapView,
                    defStyleAttr,
                    0
                )
                val attribution =
                    typedArray.getString(R.styleable.map_AmplifyMapView_map_attribution)
                val minZoomLevel = typedArray.getDouble(
                    R.styleable.map_AmplifyMapView_map_minZoomLevel,
                    DEFAULT_MIN_ZOOM
                )
                val maxZoomLevel = typedArray.getDouble(
                    R.styleable.map_AmplifyMapView_map_maxZoomLevel,
                    DEFAULT_MAX_ZOOM
                )
                val center = Coordinate2D(
                    latitude = typedArray.getDouble(
                        R.styleable.map_AmplifyMapView_map_centerLatitude
                    ),
                    longitude = typedArray.getDouble(
                        R.styleable.map_AmplifyMapView_map_centerLongitude
                    )
                )
                val showCompassIndicator = typedArray.getBoolean(
                    R.styleable.map_AmplifyMapView_map_showCompassIndicator,
                    DEFAULT_SHOW_COMPASS_INDICATOR
                )
                val showUserLocation = typedArray.getBoolean(
                    R.styleable.map_AmplifyMapView_map_showUserLocation,
                    DEFAULT_SHOW_USER_LOCATION
                )
                val showZoomControls = typedArray.getBoolean(
                    R.styleable.map_AmplifyMapView_map_showZoomControls,
                    DEFAULT_SHOW_ZOOM_CONTROLS
                )
                val zoomLevel = typedArray.getDouble(
                    R.styleable.map_AmplifyMapView_map_zoomLevel,
                    DEFAULT_ZOOM
                )
                typedArray.recycle()
                MapViewOptions(
                    attribution = attribution,
                    center = center,
                    minZoomLevel = minZoomLevel,
                    maxZoomLevel = maxZoomLevel,
                    showCompassIndicator = showCompassIndicator,
                    showUserLocation = showUserLocation,
                    showZoomControls = showZoomControls,
                    zoomLevel = zoomLevel
                )
            } ?: MapViewOptions()
    }

    fun toMapLibreOptions(context: Context): MapLibreOptions = MapLibreOptions.createFromAttributes(context)
        .attributionEnabled(!attribution.isNullOrBlank())
        .camera(
            CameraPosition.Builder()
                .target(center.latlng)
                .zoom(zoomLevel)
                .build()
        )
        .compassEnabled(false)
        .logoEnabled(false)
        .minZoomPreference(minZoomLevel.coerceAtLeast(MIN_ZOOM_BOUNDARY))
        .maxZoomPreference(maxZoomLevel.coerceAtMost(MAX_ZOOM_BOUNDARY))

    fun shouldRenderControls() = showCompassIndicator || showZoomControls
}

private fun TypedArray.getDouble(@StyleableRes res: Int, defValue: Double = 0.0): Double =
    this.getFloat(res, defValue.toFloat()).toDouble()
