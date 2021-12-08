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
import android.graphics.PointF
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.amplifyframework.geo.location.models.AmazonLocationPlace
import com.amplifyframework.geo.maplibre.R
import com.amplifyframework.geo.maplibre.util.AddressFormatter
import com.amplifyframework.geo.maplibre.util.DefaultAddressFormatter

class PlaceInfoPopupView(context: Context) : RelativeLayout(context) {

    var onVisibilityChangedListener: OnVisibilityChangedListener? = null

    private val label by lazy {
        TextView(context).apply {
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.map_search_infoTitleTextSize)
            )
        }
    }

    private val address by lazy {
        TextView(context).apply {
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.map_search_infoTextSize)
            )
        }
    }

    private val placeInfo by lazy {
        LinearLayout(context).apply {
            id = R.id.map_search_info_container
            orientation = LinearLayout.VERTICAL

            val padding = context.resources.getDimensionPixelSize(
                R.dimen.map_search_itemPadding
            )
            setPaddingRelative(padding, padding, padding, padding)
        }
    }

    private val closeButton by lazy {
        ImageButton(context).apply {
            id = R.id.map_search_info_close
            val padding = context.resources.getDimensionPixelSize(
                R.dimen.map_search_infoPopupClosePadding
            )
            setPaddingRelative(padding, padding, padding, padding)
            setImageResource(R.drawable.ic_baseline_clear_24)
            setSelectableBackground(context, R.color.map_search_itemBackground)
            setOnClickListener { hide() }
        }
    }

    private val popupMargin: Int by lazy {
        context.resources.getDimensionPixelSize(R.dimen.map_search_infoPopupMargin)
    }

    var addressFormatter: AddressFormatter = DefaultAddressFormatter

    init {
        clipToOutline = true
        elevation = context.resources.getDimension(R.dimen.map_controls_elevation)
        setBackgroundResource(R.drawable.map_control_background)

        placeInfo.addView(
            label,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                addRule(ALIGN_PARENT_TOP)
                addRule(ALIGN_PARENT_START)
                val margin = context.resources.getDimensionPixelSize(R.dimen.map_search_itemPadding)
                marginEnd = margin
                bottomMargin = margin / 2
            }
        )
        placeInfo.addView(
            address,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        )
        addView(placeInfo, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(closeButton,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                addRule(ALIGN_TOP)
                addRule(ALIGN_END)
                addRule(END_OF, R.id.map_search_info_container)
            }
        )
    }

    fun update(position: PointF) {
        x = position.x - (measuredWidth / 2)
        y = position.y - measuredHeight - popupMargin
    }

    fun show(place: AmazonLocationPlace, position: PointF) {
        label.text = addressFormatter.formatName(place)
        address.text = addressFormatter.formatAddress(place)
        update(position)
        fadeIn()
    }

    fun hide() = fadeOut()

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        onVisibilityChangedListener?.onChange(isVisible)
    }

    fun onVisibilityChanged(listener: (Boolean) -> Unit) {
        onVisibilityChangedListener = object : OnVisibilityChangedListener {
            override fun onChange(isVisible: Boolean) {
                listener(isVisible)
            }
        }
    }

    interface OnVisibilityChangedListener {
        fun onChange(isVisible: Boolean)
    }
}
