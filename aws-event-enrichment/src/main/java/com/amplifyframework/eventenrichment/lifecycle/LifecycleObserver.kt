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

/**
 * Abstract interface for observing app lifecycle transitions.
 *
 * Implement this to bridge platform-specific lifecycle events to the session
 * manager. The default [AndroidLifecycleObserver] uses
 * [android.app.Application.ActivityLifecycleCallbacks].
 */
interface LifecycleObserver {
    /** Called when the app moves to the background. */
    fun onPause()

    /** Called when the app returns to the foreground. */
    fun onResume()

    /** Stops observing lifecycle events and releases resources. */
    fun dispose()
}
