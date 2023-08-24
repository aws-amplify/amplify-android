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
    private val awsCloudWatchLoggingPlugin: AWSCloudWatchLoggingPluginImplementation,
    private val logEventsQueue: Queue<CloudWatchLogEvent> = ConcurrentLinkedQueue(),
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : Logger {

    private val coroutineScope = CoroutineScope(dispatcher)

    override fun getThresholdLevel(): LogLevel {
        return loggingConstraintsResolver.resolveLogLevel(namespace, categoryType)
    }

    override fun getNamespace(): String {
        return namespace
    }

    override fun error(message: String?) = log(LogLevel.ERROR, message)

    override fun error(message: String?, error: Throwable?) = log(LogLevel.ERROR, message, error)

    override fun warn(message: String?) = log(LogLevel.WARN, message)

    override fun warn(message: String?, issue: Throwable?) = log(LogLevel.WARN, message, issue)
    override fun info(message: String?) = log(LogLevel.INFO, message)

    override fun debug(message: String?) = log(LogLevel.DEBUG, message)

    override fun verbose(message: String?) = log(LogLevel.VERBOSE, message)

    private fun log(level: LogLevel, message: String?, error: Throwable? = null) {
        if (shouldNotLogMessage(level)) {
            return
        }
        var logMessage = "${level.name.lowercase()}/$namespace: $message".plus(error?.let { ", error: $it" } ?: "")
        val event = CloudWatchLogEvent(System.currentTimeMillis(), logMessage)
        persistEvent(event)
    }

    private fun persistEvent(event: CloudWatchLogEvent) {
        coroutineScope.launch {
            awsCloudWatchLoggingPlugin.cloudWatchLogManager?.saveLogEvent(event)?.also {
                flushLogsQueue()
            } ?: logEventsQueue.add(event)
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

    private fun shouldNotLogMessage(logLevel: LogLevel): Boolean {
        return !awsCloudWatchLoggingPlugin.isPluginEnabled || thresholdLevel.above(logLevel)
    }
}
