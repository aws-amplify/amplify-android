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
 * Constraints that control which log messages are captured, by default and per namespace,
 * with optional per-user overrides.
 *
 * @param defaultLogLevel The default minimum [LogLevel] to capture
 * @param namespaceLogLevel Per-namespace overrides of the minimum [LogLevel]
 * @param userLogLevel Per-user overrides, keyed by user identifier
 */
@ExperimentalAmplifyApi
data class LoggingConstraints(
    val defaultLogLevel: LogLevel = LogLevel.Error,
    val namespaceLogLevel: Map<String, LogLevel> = emptyMap(),
    val userLogLevel: Map<String, UserLogLevel> = emptyMap()
)

/**
 * Per-user logging overrides.
 *
 * @param defaultLogLevel The default minimum [LogLevel] for the user
 * @param namespaceLogLevel Per-namespace overrides of the minimum [LogLevel] for the user
 */
@ExperimentalAmplifyApi
data class UserLogLevel(
    val defaultLogLevel: LogLevel = LogLevel.Error,
    val namespaceLogLevel: Map<String, LogLevel> = emptyMap()
)
