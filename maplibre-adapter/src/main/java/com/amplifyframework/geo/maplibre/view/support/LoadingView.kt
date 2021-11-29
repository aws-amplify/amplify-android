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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import com.amplifyframework.geo.maplibre.R

internal class LoadingView(context: Context) : FrameLayout(context) {
    init {
        visibility = GONE
        setBackgroundColor(ContextCompat.getColor(context, R.color.map_loadingBackground))
        addView(
            ProgressBar(context),
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_VERTICAL.and(Gravity.CENTER_HORIZONTAL)
            }
        )
    }

    fun show() {
        if (visibility == VISIBLE) return
        alpha = 0f
        visibility = VISIBLE
        animate()
            .alpha(1f)
            .setListener(null)
    }

    fun hide() {
        if (visibility == GONE) return
        animate()
            .alpha(0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = GONE
                }
            })
    }
}
