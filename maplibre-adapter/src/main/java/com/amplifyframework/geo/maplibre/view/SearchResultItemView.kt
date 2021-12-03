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
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.amplifyframework.geo.maplibre.R

@SuppressLint("ViewConstructor")
class SearchResultItemView(
    context: Context,
) : LinearLayout(context) {

    internal val label: TextView by lazy {
        TextView(context).apply {
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.map_search_inputTextSize)
            )
        }
    }

    internal val address: TextView by lazy {
        TextView(context).apply {
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.map_search_inputTextSize)
            )
        }
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.map_search_itemBackground))

        val padding = context.resources.getDimensionPixelSize(R.dimen.map_search_itemPadding)
        setPaddingRelative(padding, padding, padding, padding)

        addView(label)
        addView(address)
    }

}
