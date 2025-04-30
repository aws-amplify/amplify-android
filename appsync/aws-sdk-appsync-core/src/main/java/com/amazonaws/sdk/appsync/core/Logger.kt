/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amazonaws.sdk.appsync.core

import java.util.function.Supplier

fun interface LoggerProvider {
    fun getLogger(namespace: String): Logger
}

/**
 * A component which can emit logs.
 */
interface Logger {
    /**
     * Gets the minimum log-level, below which no logs are emitted.
     * This value is assigned when obtaining and instance of the logger.
     * @return The minimum permissible LogLevel for which logs will be emitted
     */
    val thresholdLevel: LogLevel

    /**
     * Logs a message at the [LogLevel.ERROR] level.
     * @param message An error message
     */
    fun error(message: String)

    /**
     * Logs a message at the [LogLevel.ERROR] level. The supplier is only invoked if the log level threshold
     * is at ERROR or below.
     * @param messageSupplier A function that returns an error message
     */
    fun error(messageSupplier: Supplier<String>) {
        if (!thresholdLevel.above(LogLevel.ERROR)) {
            error(messageSupplier.get())
        }
    }

    /**
     * Logs a message and thrown error at [LogLevel.ERROR] level.
     * @param message An error message
     * @param error A thrown error
     */
    fun error(message: String, error: Throwable?)

    /**
     * Logs a message and thrown error at [LogLevel.ERROR] level. The supplier is only invoked if the log level
     * threshold is at ERROR or below.
     * @param error A thrown error
     * @param messageSupplier A function that returns an error message
     */
    fun error(error: Throwable?, messageSupplier: Supplier<String>) {
        if (!thresholdLevel.above(LogLevel.ERROR)) {
            error(messageSupplier.get(), error)
        }
    }

    /**
     * Log a message at the [LogLevel.WARN] level.
     * @param message A warning message
     */
    fun warn(message: String)

    /**
     * Log a message at the [LogLevel.WARN] level. The supplier is only invoked if the log level threshold
     * is at WARN or below.
     * @param messageSupplier A function that returns a warning message
     */
    fun warn(messageSupplier: Supplier<String>) {
        if (!thresholdLevel.above(LogLevel.WARN)) {
            warn(messageSupplier.get())
        }
    }

    /**
     * Log a message and a throwable issue at the [LogLevel.WARN] level.
     * @param message A warning message
     * @param issue An issue that caused this warning
     */
    fun warn(message: String, issue: Throwable?)

    /**
     * Log a message and a throwable issue at the [LogLevel.WARN] level. The supplier is only invoked if the
     * log level threshold is at WARN or below.
     * @param issue An issue that caused this warning
     * @param messageSupplier A function that returns a warning message
     */
    fun warn(issue: Throwable?, messageSupplier: Supplier<String>) {
        if (!thresholdLevel.above(LogLevel.WARN)) {
            warn(messageSupplier.get(), issue)
        }
    }

    /**
     * Logs a message at [LogLevel.INFO] level.
     * @param message An informational message
     */
    fun info(message: String)

    /**
     * Logs a message at [LogLevel.INFO] level. The supplier is only invoked if the log level threshold
     * is at INFO or below.
     * @param messageSupplier A function that returns an info message
     */
    fun info(messageSupplier: Supplier<String>) {
        if (!thresholdLevel.above(LogLevel.INFO)) {
            info(messageSupplier.get())
        }
    }

    /**
     * Logs a message at the [LogLevel.DEBUG] level.
     * @param message A debugging message.
     */
    fun debug(message: String)

    /**
     * Logs a message at the [LogLevel.DEBUG] level. The supplier is only invoked if the log level threshold
     * is at DEBUG or below.
     * @param messageSupplier A function that returns a debugging message
     */
    fun debug(messageSupplier: Supplier<String>) {
        if (!thresholdLevel.above(LogLevel.DEBUG)) {
            debug(messageSupplier.get())
        }
    }

    /**
     * Logs a message at the [LogLevel.VERBOSE] level.
     * @param message A verbose message
     */
    fun verbose(message: String)

    /**
     * Logs a message at the [LogLevel.VERBOSE] level. The supplier is only invoked if the log level threshold
     * is at VERBOSE.
     * @param messageSupplier A function that returns a verbose message
     */
    fun verbose(messageSupplier: Supplier<String>) {
        if (!thresholdLevel.above(LogLevel.VERBOSE)) {
            verbose(messageSupplier.get())
        }
    }
}

/**
 * An enumeration of the different levels of logging.
 * The levels are progressive, with lower-value items being lower priority
 * than higher-value items. For example, INFO is lower priority than WARNING
 * or ERROR.
 */
enum class LogLevel {
    /**
     * Verbose logs are used to study the behavior of particular components/flows
     * within a system, by developers. Verbose logs are not suitable for emission
     * in production, as they may contain sensitive information, and/or be emitted
     * so frequently that performance is impacted.
     */
    VERBOSE,

    /**
     * Debug logs are useful during development to understand the behavior of the system.
     * These logs may contain information that is inappropriate for emission in a production
     * environment.
     */
    DEBUG,

    /**
     * Informational logs may be emitted in production code, and provide
     * terse information about the general operation and flow of a piece of software.
     */
    INFO,

    /**
     * Warning logs indicate potential issues while running a piece of software.
     * For example, a network connection might retry, before succeeding with success -
     * the system is functioning without error, but not optimally.
     */
    WARN,

    /**
     * Errors should be logged when the system is not operating as expected.
     * For example, there might be no internet connection available to the system.
     * The application probably shouldn't need to crash, but anything that needs the
     * Internet will error out. Errors may logically be recoverable or fatal, but are
     * not distinguished here, and are logged at this same error level.
     */
    ERROR,

    /**
     * A log level above all other log levels. This log level may be used as a threshold
     * value, to prevent any logs from being emitted.
     */
    NONE;

    /**
     * Checks if a log level is above the current level.
     * For example, NONE is above ERROR, but ERROR is not above ERROR,
     * and WARN is not above ERROR.
     * @param threshold A threshold level to consider for evaluation
     * @return true if the current level is above the threshold level
     * @throws NullPointerException if threshold is null
     */
    fun above(threshold: LogLevel): Boolean {
        return this.ordinal > threshold.ordinal
    }
}
