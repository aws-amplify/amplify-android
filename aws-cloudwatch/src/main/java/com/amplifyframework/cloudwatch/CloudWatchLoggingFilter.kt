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
package com.amplifyframework.cloudwatch

import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.foundation.logging.LogLevel

/**
 * Decides whether a log message passes the current [LoggingConstraints]. Precedence, highest first:
 * per-user override (by exact user id), then per-namespace override (namespace keys matched
 * case-insensitively), then the default level. A threshold of [LogLevel.None] blocks everything.
 */
@ExperimentalAmplifyApi
internal class CloudWatchLoggingFilter(initialConstraints: LoggingConstraints) {

    @Volatile
    var loggingConstraints: LoggingConstraints = initialConstraints

    fun canLog(namespace: String, level: LogLevel, userIdentifier: String?): Boolean {
        if (level == LogLevel.None) return false
        val constraints = loggingConstraints

        val userLogLevel = userIdentifier?.let { constraints.userLogLevel[it] }
        if (userLogLevel != null) {
            val threshold = userLogLevel.namespaceLogLevel.matching(namespace) ?: userLogLevel.defaultLogLevel
            return threshold.allows(level)
        }

        constraints.namespaceLogLevel.matching(namespace)?.let { return it.allows(level) }

        return constraints.defaultLogLevel.allows(level)
    }

    private fun Map<String, LogLevel>.matching(namespace: String): LogLevel? =
        entries.firstOrNull { it.key.equals(namespace, ignoreCase = true) }?.value

    // A threshold allows a message when the message level is at or above the threshold. LogLevel is
    // ordered Verbose < Debug < Info < Warn < Error < None, so None as a threshold allows nothing.
    private fun LogLevel.allows(level: LogLevel): Boolean = this != LogLevel.None && this <= level
}
