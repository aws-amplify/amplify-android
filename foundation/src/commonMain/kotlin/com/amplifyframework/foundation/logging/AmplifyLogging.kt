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

package com.amplifyframework.foundation.logging

import com.amplifyframework.annotations.InternalAmplifyApi

/**
 * Entry point for debug logging
 */
object AmplifyLogging {
    // Use copy-on-write to avoid concurrent modification
    private var sinks = setOf<LogSink>()

    /**
     * Add a sink for emitting logs. Sinks must be added before loggers are created. This should generally be called
     * during your Application's initialization (for example, in Application.onCreate for Android).
     * @param sink The [LogSink] to add.
     */
    @JvmStatic
    fun addSink(sink: LogSink) {
        sinks += sink
    }

    // Internal API for testing, not safe for production use
    @JvmSynthetic
    internal fun removeSink(sink: LogSink) {
        sinks -= sink
    }

    @InternalAmplifyApi
    fun logger(name: String): Logger = LoggerImpl(name, sinks.toSet())

    @InternalAmplifyApi
    inline fun <reified T> logger(): Logger = logger(T::class.simpleName ?: "unknown")
}

private class LoggerImpl(
    private val name: String,
    private val sinks: Set<LogSink>
) : Logger {

    override fun log(level: LogLevel, message: String, cause: Throwable?) {
        val logMessage = LogMessage(level, name, message, cause)
        sinks.forEach {
            if (it.isEnabledFor(level)) {
                it.emit(logMessage)
            }
        }
    }

    override fun log(level: LogLevel, cause: Throwable?, message: () -> String) {
        // Only evaluate the message if at least one sink will emit it
        if (sinks.any { it.isEnabledFor(level) }) {
            log(level, message(), cause)
        }
    }
}
