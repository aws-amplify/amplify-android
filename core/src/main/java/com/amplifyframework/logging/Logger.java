/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.logging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A component which can emit logs.
 */
public interface Logger {

    /**
     * Gets the minimum log-level, below which no logs are emitted.
     * This value is assigned when obtaining and instance of the logger.
     * @return The minimum permissible LogLevel for which logs will be emitted
     */
    @NonNull
    LogLevel getThresholdLevel();

    /**
     * Gets the namespace of the logger.
     * @return namespace for logger
     */
    @NonNull
    String getNamespace();

    /**
     * Logs a message at the {@link LogLevel#ERROR} level.
     * @param message An error message
     */
    void error(@Nullable String message);

    /**
     * Logs a message and thrown error at {@link LogLevel#ERROR} level.
     * @param message An error message
     * @param error A thrown error
     */
    void error(@Nullable String message, @Nullable Throwable error);

    /**
     * Log a message at the {@link LogLevel#WARN} level.
     * @param message A warning message
     */
    void warn(@Nullable String message);

    /**
     * Log a message and a throwable issue at the {@link LogLevel#WARN} level.
     * @param message A warning message
     * @param issue An issue that caused this warning
     */
    void warn(@Nullable String message, @Nullable Throwable issue);

    /**
     * Logs a message at {@link LogLevel#INFO} level.
     * @param message An informational message
     */
    void info(@Nullable String message);

    /**
     * Logs a message at the {@link LogLevel#DEBUG} level.
     * @param message A debugging message.
     */
    void debug(@Nullable String message);

    /**
     * Logs a message at the {@link LogLevel#VERBOSE} level.
     * @param message A verbose message
     */
    void verbose(@Nullable String message);
}
