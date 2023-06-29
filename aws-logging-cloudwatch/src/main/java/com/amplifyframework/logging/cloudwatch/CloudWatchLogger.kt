/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.logging.cloudwatch

import android.util.Log
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.logging.LogLevel
import com.amplifyframework.logging.Logger
import com.amplifyframework.logging.cloudwatch.models.CloudWatchLogEvent
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Logger to send logs to AWS Cloudwatch
 */
class CloudWatchLogger internal constructor(
    private val namespace: String,
    private val categoryType: CategoryType?,
    private val loggingConstraintsResolver: LoggingConstraintsResolver,
    private val awsCloudWatchLoggingPlugin: AWSCloudWatchLoggingPluginBehavior,
    private val logEventsQueue: Queue<CloudWatchLogEvent> = ConcurrentLinkedQueue(),
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
        if (shouldLog(LogLevel.ERROR)) {
            return
        }
        val event = CloudWatchLogEvent(System.currentTimeMillis(), "error/$namespace: $message")
        persistEvent(event)
    }

    override fun error(message: String?, error: Throwable?) {
        if (shouldLog(LogLevel.ERROR)) {
            return
        }
        val event = CloudWatchLogEvent(
            System.currentTimeMillis(),
            "error/$namespace: $message, error: ${Log.getStackTraceString(error)}",
        )
        persistEvent(event)
    }

    override fun warn(message: String?) {
        if (shouldLog(LogLevel.WARN)) {
            return
        }
        val event = CloudWatchLogEvent(System.currentTimeMillis(), "warn/$namespace: $message")
        persistEvent(event)
    }

    override fun warn(message: String?, issue: Throwable?) {
        if (shouldLog(LogLevel.WARN)) {
            return
        }
        val event = CloudWatchLogEvent(
            System.currentTimeMillis(),
            "warn/$namespace: $message, issue: ${Log.getStackTraceString(issue)}",
        )
        persistEvent(event)
    }

    override fun info(message: String?) {
        if (shouldLog(LogLevel.INFO)) {
            return
        }
        val event = CloudWatchLogEvent(System.currentTimeMillis(), "info/$namespace: $message")
        persistEvent(event)
    }

    override fun debug(message: String?) {
        if (shouldLog(LogLevel.DEBUG)) {
            return
        }
        val event = CloudWatchLogEvent(System.currentTimeMillis(), "debug/$namespace: $message")
        persistEvent(event)
    }

    override fun verbose(message: String?) {
        if (shouldLog(LogLevel.VERBOSE)) {
            return
        }
        val event = CloudWatchLogEvent(System.currentTimeMillis(), "verbose/$namespace: $message")
        persistEvent(event)
    }

    private fun persistEvent(event: CloudWatchLogEvent) {
        coroutineScope.launch {
            awsCloudWatchLoggingPlugin.cloudWatchLogManager?.saveLogEvent(event)?.also {
                flushLogsQueue()
            } ?: kotlin.run {
                logEventsQueue.add(event)
            }
        }
    }

    private suspend fun flushLogsQueue() {
        awsCloudWatchLoggingPlugin.cloudWatchLogManager?.let { cloudWatchManager ->
            val iterator = logEventsQueue.iterator()
            while (iterator.hasNext()) {
                cloudWatchManager.saveLogEvent(iterator.next())
                iterator.remove()
            }
        }
    }

    private fun shouldLog(logLevel: LogLevel): Boolean {
        return awsCloudWatchLoggingPlugin.isPluginEnabled && thresholdLevel.above(logLevel)
    }
}
