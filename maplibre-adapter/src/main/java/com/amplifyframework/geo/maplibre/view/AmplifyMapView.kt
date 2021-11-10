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
import android.content.res.ColorStateList
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import android.widget.FrameLayout.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import com.amplifyframework.core.Amplify
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.maplibre.R
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import kotlin.math.abs

private val FILL_CONTENT = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)

/**
 * The AmplifyMapView encapsulates the MapLibre map integration with Amplify.Geo and introduces
 * a handful of built-in features to the plain MapView, such as zoom controls, compass, search field,
 * and map markers.
 *
 * **Implementation note:** This view inherits from [FrameLayout], therefore any custom
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
    geo: GeoCategory = Amplify.Geo
) : FrameLayout(context) {

    companion object {
        private val log = Amplify.Logging.forNamespace("amplify:maplibre-adapter:view")
    }

    private val overlayLayout: RelativeLayout = RelativeLayout(context)

    val mapView by lazy {
        MapLibreView(
            context = context,
            options = options.toMapLibreOptions(context),
            geo = geo
        )
    }

    private val controls by lazy {
        MapControls(
            context,
            showCompassIndicator = options.showCompassIndicator,
            showZoomControls = options.showZoomControls
        )
    }

    init {
        addView(mapView, FILL_CONTENT)
        if (options.shouldRenderControls()) {
            overlayLayout.addView(
                controls,
                RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    topMargin = context.resources.getDimensionPixelSize(R.dimen.controls_margin)
                    marginEnd = context.resources.getDimensionPixelSize(R.dimen.controls_margin)
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                }
            )
        }
        addView(overlayLayout, FILL_CONTENT)
        bindViewModel()
    }

    private fun bindViewModel() {
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
            }
        }
    }

    private fun updateZoomControls(camera: CameraPosition) {
        controls.zoomOutButton.isEnabled = camera.zoom > options.minZoomLevel
        controls.zoomInButton.isEnabled = camera.zoom < options.maxZoomLevel
    }

}

@SuppressLint("ViewConstructor")
internal class MapControls(
    context: Context,
    val showCompassIndicator: Boolean = false,
    val showZoomControls: Boolean = false
) : LinearLayout(context) {

    internal val compassIndicatorButton by lazy {
        MapControl(
            context,
            iconResource = R.drawable.ic_baseline_navigation_24,
            accessibilityLabel = R.string.label_compassIndicator,
        )
    }

    internal val zoomInButton by lazy {
        MapControl(
            context,
            iconResource = R.drawable.ic_baseline_add_24,
            accessibilityLabel = R.string.label_zoomIn,
        )
    }

    internal val zoomOutButton by lazy {
        MapControl(
            context,
            iconResource = R.drawable.ic_baseline_minus_24,
            accessibilityLabel = R.string.label_zoomOut,
        )
    }

    init {
        clipToOutline = true
        clipChildren = true
        background = ContextCompat.getDrawable(context, R.drawable.bg_control)
        elevation = context.resources.getDimension(R.dimen.controls_elevation)
        orientation = VERTICAL

        val size = context.resources.getDimensionPixelSize(R.dimen.controls_size)
        val buttonSize = LayoutParams(size, size)
        if (showZoomControls) {
            addView(zoomInButton, buttonSize)
            addSeparator(context)
            addView(zoomOutButton, buttonSize)
        }
        if (showCompassIndicator) {
            if (showZoomControls) addSeparator(context)
            addView(compassIndicatorButton, buttonSize)
        }
    }

    private fun addSeparator(context: Context) {
        val borderSize = context.resources.getDimensionPixelSize(R.dimen.controls_borderSize)
        addView(
            View(context).apply {
                setBackgroundColor(ContextCompat.getColor(context, R.color.controls_border))
            },
            LayoutParams(LayoutParams.MATCH_PARENT, borderSize)
        )
    }

}

/**
 * Map button component used to control features like zoom in/out, compass. It is composed
 * by a Button and an ImageView (icon) inside a FrameLayout so the icon can be independently
 * animated, such as the compass icon.
 */
@SuppressLint("ViewConstructor")
internal class MapControl(
    context: Context,
    iconResource: Int,
    accessibilityLabel: Int
) : FrameLayout(context) {

    private val button: Button by lazy {
        Button(context).apply {
            contentDescription = context.getText(accessibilityLabel)
            val backgroundEffect = TypedValue()
            context.theme.resolveAttribute(
                android.R.attr.selectableItemBackground,
                backgroundEffect,
                true
            )
            setBackgroundResource(backgroundEffect.resourceId)
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.controls_background)
            )
        }
    }

    private val icon: ImageView by lazy {
        ImageView(context).apply {
            setColorFilter(ContextCompat.getColor(context, R.color.controls_foreground))
            setImageResource(iconResource)

            val padding = context.resources.getDimensionPixelSize(R.dimen.controls_padding)
            setPadding(padding, padding, padding, padding)
        }
    }

    init {
        addView(button)
        addView(icon)
    }

    /**
     * The default rotation logic provided by the Animation framework doesn't rotate
     * to the direction of the shortest distance to the target angle, resulting in a
     * unexpected animation (e.g. from 350 to 20 degrees it goes the whole way counter clockwise).
     *
     * This method calculates the shortest path from the current angle to the target and uses
     * the `rotationBy` API to achieve the expected effect in a compass rotation.
     */
    fun rotateIcon(target: Double) {
        val current = icon.rotation
        val diff = target - current
        val delta = abs(diff) % 360
        val direction = if (diff in 0.0..180.0 || diff in -360.0..-180.0) 1 else -1
        val angle = (if (delta > 180) 360 - delta else delta) * direction

        icon.animate()
            .setInterpolator(AccelerateDecelerateInterpolator())
            .rotationBy(angle.toFloat())
    }

    fun onClick(listener: OnClickListener) = button.setOnClickListener(listener)

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        button.isEnabled = enabled
        icon.isEnabled = enabled

        val alpha = if (enabled) 1.0f else 0.3f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            icon.transitionAlpha = alpha
        } else {
            icon.alpha = alpha
        }
    }
}
