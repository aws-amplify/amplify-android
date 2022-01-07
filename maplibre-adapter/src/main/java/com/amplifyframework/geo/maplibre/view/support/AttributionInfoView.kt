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

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import com.amplifyframework.geo.maplibre.R

/**
 * Support view that displays a clickable info icon that shows the map content attribution text.
 */
internal class AttributionInfoView(context: Context) : FrameLayout(context) {

    private val attributionInfoIcon by lazy {
        ImageButton(context).apply {
            contentDescription = context.getString(R.string.map_attributionIconDescription)
            setImageResource(R.drawable.ic_twotone_info_24)
            setBackgroundColor(Color.TRANSPARENT)

            val padding = context.resources
                .getDimensionPixelSize(R.dimen.map_attribution_iconPadding)
            setPaddingRelative(padding, padding, padding, padding)

            setOnClickListener { show() }
        }
    }

    private val attributionText by lazy {
        TextView(context).apply {
            text = context.getString(R.string.map_attributionText)
            visibility = GONE

            setBackgroundResource(R.drawable.map_attribution_text_background)

            val padding = context.resources
                .getDimensionPixelSize(R.dimen.map_attribution_textPadding)
            setPaddingRelative(padding, padding, padding, padding)

            val size = context.resources
                .getDimensionPixelSize(R.dimen.map_attribution_textSize)
                .toFloat()
            setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
            setOnClickListener { hide() }
        }
    }

    init {
        addView(
            attributionInfoIcon,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.START
            }
        )
        addView(
            attributionText,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        )
    }

    private fun show() {
        attributionInfoIcon.fadeOut()
        attributionText.fadeIn()
    }

    private fun hide() {
        attributionText.fadeOut()
        attributionInfoIcon.fadeIn()
    }
}
