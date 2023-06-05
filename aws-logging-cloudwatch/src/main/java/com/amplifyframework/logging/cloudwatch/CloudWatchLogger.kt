package com.amplifyframework.logging.cloudwatch

import android.util.Log
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.logging.LogLevel
import com.amplifyframework.logging.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
class CloudWatchLogger internal constructor(
    private val namespace: String,
    private val categoryType: CategoryType?,
    private val logToLogcat: Boolean,
    private val cloudwatchLogEventRecorder: CloudwatchLogEventRecorder?,
    private val loggingConstraintsResolver: LoggingConstraintsResolver,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Logger {

    private val coroutineScope = CoroutineScope(dispatcher)

    override fun getThresholdLevel(): LogLevel {
        return loggingConstraintsResolver.resolveLogLevel(namespace, categoryType)
    }

    override fun getNamespace(): String {
        return namespace
    }

    override fun error(message: String?) {
        if (thresholdLevel.above(LogLevel.ERROR)) {
            return
        }
        val event = CloudWatchLogEvent(System.currentTimeMillis(), "error/$namespace: $message")
        if (logToLogcat) {
            Log.e(namespace, message.toString())
        }
        coroutineScope.launch {
            cloudwatchLogEventRecorder?.saveLogEvent(event)
        }
    }

    override fun error(message: String?, error: Throwable?) {
        if (thresholdLevel.above(LogLevel.ERROR)) {
            return
        }
        val event = CloudWatchLogEvent(
            System.currentTimeMillis(),
            "error/$namespace: $message, error: ${Log.getStackTraceString(error)}",
        )
        if (logToLogcat) {
            Log.e(namespace, message.toString(), error)
        }
        coroutineScope.launch {
            cloudwatchLogEventRecorder?.saveLogEvent(event)
        }
    }

    override fun warn(message: String?) {
        if (thresholdLevel.above(LogLevel.WARN)) {
            return
        }
        val event = CloudWatchLogEvent(System.currentTimeMillis(), "warn/$namespace: $message")
        if (logToLogcat) {
            Log.w(namespace, message.toString())
        }
        coroutineScope.launch {
            cloudwatchLogEventRecorder?.saveLogEvent(event)
        }
    }

    override fun warn(message: String?, issue: Throwable?) {
        if (thresholdLevel.above(LogLevel.WARN)) {
            return
        }
        val event = CloudWatchLogEvent(
            System.currentTimeMillis(),
            "warn/$namespace: $message, issue: ${Log.getStackTraceString(issue)}",
        )
        if (logToLogcat) {
            Log.w(namespace, message.toString(), issue)
        }
        coroutineScope.launch {
            cloudwatchLogEventRecorder?.saveLogEvent(event)
        }
    }

    override fun info(message: String?) {
        if (thresholdLevel.above(LogLevel.INFO)) {
            return
        }
        val event = CloudWatchLogEvent(System.currentTimeMillis(), "info/$namespace: $message")
        if (logToLogcat) {
            Log.i(namespace, message.toString())
        }
        coroutineScope.launch {
            cloudwatchLogEventRecorder?.saveLogEvent(event)
        }
    }

    override fun debug(message: String?) {
        if (thresholdLevel.above(LogLevel.DEBUG)) {
            return
        }
        val event = CloudWatchLogEvent(System.currentTimeMillis(), "debug/$namespace: $message")
        if (logToLogcat) {
            Log.d(namespace, message.toString())
        }
        coroutineScope.launch {
            cloudwatchLogEventRecorder?.saveLogEvent(event)
        }
    }

    override fun verbose(message: String?) {
        if (thresholdLevel.above(LogLevel.VERBOSE)) {
            return
        }
        val event = CloudWatchLogEvent(System.currentTimeMillis(), "verbose/$namespace: $message")
        if (logToLogcat) {
            Log.v(namespace, message.toString())
        }
        coroutineScope.launch {
            cloudwatchLogEventRecorder?.saveLogEvent(event)
        }
    }
}
