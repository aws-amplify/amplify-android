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
import android.view.View
import android.widget.FrameLayout

/**
 * Utility to show the view using an alpha animation that animates from current alpha to 1f.
 * @see fadeOut
 */
fun View.fadeIn() {
    visibility = FrameLayout.VISIBLE
    animate()
        .alpha(1f)
        .setListener(null)
}

/**
 * Utility to hide the view using an alpha animation that animates from current alpha to 0f.
 *
 * **Note:** if the view initial state is hidden, make sure you also set the alpha to 0f.
 * @see fadeIn
 */
fun View.fadeOut() {
    animate()
        .alpha(0f)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                visibility = FrameLayout.GONE
            }
        })
}
