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
package com.amplifyframework.logging.cloudwatch

import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.logging.LogLevel

data class LoggingConstraint(
    val defaultLogLevel: LogLevel = LogLevel.ERROR,
    val categoryLogLevel: Map<LogLevel, CategoryType> = emptyMap(),
    val userLogLevel: Array<UserLogLevel> = emptyArray(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LoggingConstraint

        if (defaultLogLevel != other.defaultLogLevel) return false
        if (categoryLogLevel != other.categoryLogLevel) return false
        if (!userLogLevel.contentEquals(other.userLogLevel)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = defaultLogLevel.hashCode()
        result = 31 * result + categoryLogLevel.hashCode()
        result = 31 * result + userLogLevel.contentHashCode()
        return result
    }
}

data class CategoryLogLevel(
    val logLevel: LogLevel,
    val categories: Array<CategoryType>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CategoryLogLevel

        if (logLevel != other.logLevel) return false
        if (!categories.contentEquals(other.categories)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = logLevel.hashCode()
        result = 31 * result + categories.contentHashCode()
        return result
    }
}

data class UserLogLevel(
    val userIdentifiers: Array<String>,
    val defaultLogLevel: LogLevel = LogLevel.ERROR,
    val categoryLogLevel: Array<CategoryLogLevel>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserLogLevel

        if (!userIdentifiers.contentEquals(other.userIdentifiers)) return false
        if (defaultLogLevel != other.defaultLogLevel) return false
        if (!categoryLogLevel.contentEquals(other.categoryLogLevel)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userIdentifiers.contentHashCode()
        result = 31 * result + defaultLogLevel.hashCode()
        result = 31 * result + categoryLogLevel.contentHashCode()
        return result
    }
}
