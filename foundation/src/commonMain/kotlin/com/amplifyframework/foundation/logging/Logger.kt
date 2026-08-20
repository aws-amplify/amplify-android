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
 * A component which can emit logs.
 */
@InternalAmplifyApi
interface Logger {
    /**
     * Logs a message and thrown error at [LogLevel.Error] level.
     * @param message An error message
     * @param cause A thrown error
     */
    fun error(message: String, cause: Throwable? = null) = log(LogLevel.Error, message, cause)

    /**
     * Logs a message and thrown error at [LogLevel.Error] level. The supplier is only invoked if the log level
     * threshold is at ERROR or below.
     * @param cause A thrown error
     * @param message A function that returns an error message
     */
    fun error(cause: Throwable? = null, message: () -> String) = log(LogLevel.Error, cause, message)

    /**
     * Log a message and a throwable issue at the [LogLevel.Warn] level.
     * @param message A warning message
     * @param cause An issue that caused this warning
     */
    fun warn(message: String, cause: Throwable? = null) = log(LogLevel.Warn, message, cause)

    /**
     * Log a message and a throwable issue at the [LogLevel.Warn] level. The supplier is only invoked if the
     * log level threshold is at WARN or below.
     * @param cause An issue that caused this warning
     * @param message A function that returns a warning message
     */
    fun warn(cause: Throwable? = null, message: () -> String) = log(LogLevel.Warn, cause, message)

    /**
     * Logs a message at [LogLevel.Info] level.
     * @param message An informational message
     * @param cause An issue that caused this log
     */
    fun info(message: String, cause: Throwable? = null) = log(LogLevel.Info, message)

    /**
     * Logs a message at [LogLevel.Info] level. The supplier is only invoked if the log level threshold
     * is at INFO or below.
     * @param cause An issue that caused this log
     * @param message A function that returns an info message
     */
    fun info(cause: Throwable? = null, message: () -> String) = log(LogLevel.Info, cause, message)

    /**
     * Logs a message at the [LogLevel.Debug] level.
     * @param message A debugging message.
     * @param cause An issue that caused this log
     */
    fun debug(message: String, cause: Throwable? = null) = log(LogLevel.Debug, message)

    /**
     * Logs a message at the [LogLevel.Debug] level. The supplier is only invoked if the log level threshold
     * is at DEBUG or below.
     * @param cause An issue that caused this log
     * @param message A function that returns a debugging message
     */
    fun debug(cause: Throwable? = null, message: () -> String) = log(LogLevel.Debug, cause, message)

    /**
     * Logs a message at the [LogLevel.Verbose] level.
     * @param message A verbose message
     * @param cause An issue that caused this log
     */
    fun verbose(message: String, cause: Throwable? = null) = log(LogLevel.Verbose, message)

    /**
     * Logs a message at the [LogLevel.Verbose] level. The supplier is only invoked if the log level threshold
     * is at VERBOSE.
     * @param cause An issue that caused this log
     * @param message A function that returns a verbose message
     */
    fun verbose(cause: Throwable? = null, message: () -> String) = log(LogLevel.Verbose, cause, message)

    /**
     * Logs a message at the given [LogLevel].
     * @param level The level to log at
     * @param cause The issue that caused this log
     * @param message A function that returns the log message
     */
    fun log(level: LogLevel, cause: Throwable? = null, message: () -> String)

    /**
     * Logs a message at the given [LogLevel].
     * @param level The level to log at
     * @param message The log message
     * @param cause The issue that caused this log
     */
    fun log(level: LogLevel, message: String, cause: Throwable? = null)
}
