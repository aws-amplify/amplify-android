/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.eventenrichment.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.amplifyframework.eventenrichment.session.SessionManager

/**
 * [LifecycleObserver] that bridges Android app foreground/background
 * transitions to a [SessionManager].
 *
 * Uses [Application.ActivityLifecycleCallbacks] and ref-counts started
 * activities to detect when the whole app moves to the background (the last
 * activity stops) or returns to the foreground (the first activity starts).
 * This avoids a dependency on `androidx.lifecycle-process`.
 *
 * @param application Application to register lifecycle callbacks on.
 * @param sessionManager Session manager driven by the lifecycle transitions.
 */
class AndroidLifecycleObserver(
    private val application: Application,
    private val sessionManager: SessionManager
) : LifecycleObserver {
    private var startedActivities = 0

    private val callbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            if (startedActivities == 0) onResume()
            startedActivities++
        }

        override fun onActivityStopped(activity: Activity) {
            if (startedActivities > 0) startedActivities--
            if (startedActivities == 0) onPause()
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    init {
        application.registerActivityLifecycleCallbacks(callbacks)
    }

    override fun onPause() = sessionManager.handleAppPaused()

    override fun onResume() = sessionManager.handleAppResumed()

    override fun dispose() = application.unregisterActivityLifecycleCallbacks(callbacks)
}
