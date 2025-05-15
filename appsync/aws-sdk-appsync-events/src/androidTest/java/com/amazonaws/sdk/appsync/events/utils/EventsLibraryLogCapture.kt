/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.sdk.appsync.events.utils

import android.util.Log
import com.amazonaws.sdk.appsync.core.LogLevel
import com.amazonaws.sdk.appsync.core.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class EventsLibraryLogCapture : Logger {

    companion object {
        const val TAG = "EventsLibraryLogCapture"
    }

    private val _messages = MutableSharedFlow<String>(replay = 100, extraBufferCapacity = Int.MAX_VALUE)
    val messages = _messages.asSharedFlow() // publicly exposed as read-only shared flow

    override val thresholdLevel = LogLevel.DEBUG

    override fun error(message: String) {
        Log.e(TAG, message, null)
        _messages.tryEmit(message)
    }

    override fun error(message: String, error: Throwable?) {
        Log.e(TAG, message, error)
        _messages.tryEmit(message)
    }

    override fun warn(message: String) {
        Log.w(TAG, message, null)
        _messages.tryEmit(message)
    }

    override fun warn(message: String, issue: Throwable?) {
        Log.w(TAG, message, issue)
        _messages.tryEmit(message)
    }

    override fun info(message: String) {
        Log.i(TAG, message)
        _messages.tryEmit(message)
    }

    override fun debug(message: String) {
        Log.d(TAG, message)
        _messages.tryEmit(message)
    }

    override fun verbose(message: String) {
        Log.v(TAG, message)
        _messages.tryEmit(message)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun resetCache() {
        _messages.resetReplayCache()
    }
}
