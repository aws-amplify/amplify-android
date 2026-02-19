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
    Verbose,

    /**
     * Debug logs are useful during development to understand the behavior of the system.
     * These logs may contain information that is inappropriate for emission in a production
     * environment.
     */
    Debug,

    /**
     * Informational logs may be emitted in production code, and provide
     * terse information about the general operation and flow of a piece of software.
     */
    Info,

    /**
     * Warning logs indicate potential issues while running a piece of software.
     * For example, a network connection might retry, before succeeding with success -
     * the system is functioning without error, but not optimally.
     */
    Warn,

    /**
     * Errors should be logged when the system is not operating as expected.
     * For example, there might be no internet connection available to the system.
     * The application probably shouldn't need to crash, but anything that needs the
     * Internet will error out. Errors may logically be recoverable or fatal, but are
     * not distinguished here, and are logged at this same error level.
     */
    Error,

    /**
     * A log level above all other log levels. This log level may be used as a threshold
     * value, to prevent any logs from being emitted.
     */
    None
}

internal infix fun LogLevel.allows(level: LogLevel) = this <= level
