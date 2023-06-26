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
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * TODO: Acd documentation
 */
@Serializable
data class LoggingConstraint(
    val defaultLogLevel: LogLevel = LogLevel.ERROR,
    val categoryLogLevel: Map<CategoryType, LogLevel> = emptyMap(),
    val userLogLevel: Map<String, UserLogLevel> = emptyMap(),
) {
    companion object {
        fun fromString(jsonString: String): LoggingConstraint {
            val json = Json {
                encodeDefaults = true
                explicitNulls = false
                ignoreUnknownKeys = true
            }
            return json.decodeFromString<LoggingConstraint>(jsonString)
        }
    }
}

/**
 * TODO: Acd documentation
 */
@Serializable
data class UserLogLevel(
    val defaultLogLevel: LogLevel = LogLevel.ERROR,
    val categoryLogLevel: Map<CategoryType, LogLevel> = emptyMap()
)
