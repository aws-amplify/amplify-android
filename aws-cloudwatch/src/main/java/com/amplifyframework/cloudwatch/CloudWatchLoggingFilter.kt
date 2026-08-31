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
@file:OptIn(ExperimentalAmplifyApi::class, InternalAmplifyApi::class)

package com.amplifyframework.cloudwatch

import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.foundation.logging.LogLevel
import com.amplifyframework.foundation.logging.allows

/**
 * Decides whether a log message passes the current [LoggingConstraints]. Precedence, highest first:
 * per-user override (by exact user id), then per-namespace override (namespace keys matched
 * case-insensitively), then the default level. A threshold of [LogLevel.None] blocks everything.
 */
internal class CloudWatchLoggingFilter(initialConstraints: LoggingConstraints) {

    @Volatile
    var loggingConstraints: LoggingConstraints = initialConstraints

    fun canLog(namespace: String, level: LogLevel, userIdentifier: String?): Boolean {
        val constraints = loggingConstraints

        val userLogLevel = userIdentifier?.let { constraints.userLogLevel[it] }
        if (userLogLevel != null) {
            val threshold = userLogLevel.namespaceLogLevel.matching(namespace) ?: userLogLevel.defaultLogLevel
            return threshold allows level
        }

        constraints.namespaceLogLevel.matching(namespace)?.let { return it allows level }

        return constraints.defaultLogLevel allows level
    }

    /**
     * Returns true if [level] could pass under *any* configured threshold (default, per-namespace, or
     * per-user). Namespace-agnostic and deliberately permissive — [canLog] stays authoritative per
     * message. Used by [AmplifyCloudWatchClient.isEnabledFor] so callers can skip materializing lazy
     * log messages that no threshold would ever emit.
     */
    fun couldLog(level: LogLevel): Boolean {
        val constraints = loggingConstraints
        val thresholds = buildList {
            add(constraints.defaultLogLevel)
            addAll(constraints.namespaceLogLevel.values)
            constraints.userLogLevel.values.forEach { userLevel ->
                add(userLevel.defaultLogLevel)
                addAll(userLevel.namespaceLogLevel.values)
            }
        }
        return thresholds.any { it allows level }
    }

    private fun Map<String, LogLevel>.matching(namespace: String): LogLevel? =
        entries.firstOrNull { it.key.equals(namespace, ignoreCase = true) }?.value
}
