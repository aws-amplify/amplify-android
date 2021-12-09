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

package com.amplifyframework.geo.maplibre.view.support

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.amplifyframework.geo.maplibre.R
import kotlin.math.abs

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
            accessibilityLabel = R.string.map_control_compassIndicator,
        )
    }

    internal val zoomInButton by lazy {
        MapControl(
            context,
            iconResource = R.drawable.ic_baseline_add_24,
            accessibilityLabel = R.string.map_control_zoomIn,
        )
    }

    internal val zoomOutButton by lazy {
        MapControl(
            context,
            iconResource = R.drawable.ic_baseline_minus_24,
            accessibilityLabel = R.string.map_control_zoomOut,
        )
    }

    init {
        clipToOutline = true
        clipChildren = true
        background = ContextCompat.getDrawable(context, R.drawable.map_control_background)
        elevation = context.resources.getDimension(R.dimen.map_controls_elevation)
        orientation = VERTICAL

        // divider
        showDividers = SHOW_DIVIDER_MIDDLE
        dividerDrawable = ContextCompat.getDrawable(context, R.drawable.map_control_divider)

        val size = context.resources.getDimensionPixelSize(R.dimen.map_controls_size)
        val buttonSize = LayoutParams(size, size)
        if (showZoomControls) {
            addView(zoomInButton, buttonSize)
            addView(zoomOutButton, buttonSize)
        }
        if (showCompassIndicator) {
            addView(compassIndicatorButton, buttonSize)
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
                setSelectableBackground(context, R.color.map_controls_background)
            }
        }

        private val icon: ImageView by lazy {
            ImageView(context).apply {
                setColorFilter(ContextCompat.getColor(context, R.color.map_controls_foreground))
                setImageResource(iconResource)

                val padding = context.resources.getDimensionPixelSize(R.dimen.map_controls_padding)
                setPaddingRelative(padding, padding, padding, padding)
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
        fun rotateIcon(bearing: Double, animate: Boolean = true) {
            val target = bearing.unaryMinus().toFloat()
            if (animate) {
                val current = icon.rotation
                val diff = target - current
                val delta = abs(diff) % 360
                val direction = if (diff in 0.0..180.0 || diff in -360.0..-180.0) 1 else -1
                val angle = (if (delta > 180) 360 - delta else delta) * direction

                icon.animate()
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .rotationBy(angle)

            } else {
                icon.rotation = target
            }
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
}
