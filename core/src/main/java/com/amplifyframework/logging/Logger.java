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

import java.util.function.Supplier;

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
     * Logs a message at the {@link LogLevel#ERROR} level. The supplier is only invoked if the log level threshold
     * is at ERROR or below.
     * @param messageSupplier A function that returns an error message
     */
    default void error(@NonNull Supplier<String> messageSupplier) {
        if (!getThresholdLevel().above(LogLevel.ERROR)) {
            error(messageSupplier.get());
        }
    }

    /**
     * Logs a message and thrown error at {@link LogLevel#ERROR} level.
     * @param message An error message
     * @param error A thrown error
     */
    void error(@Nullable String message, @Nullable Throwable error);

    /**
     * Logs a message and thrown error at {@link LogLevel#ERROR} level. The supplier is only invoked if the log level
     * threshold is at ERROR or below.
     * @param error A thrown error
     * @param messageSupplier A function that returns an error message
     */
    default void error(@Nullable Throwable error, @NonNull Supplier<String> messageSupplier) {
        if (!getThresholdLevel().above(LogLevel.ERROR)) {
            error(messageSupplier.get(), error);
        }
    }

    /**
     * Log a message at the {@link LogLevel#WARN} level.
     * @param message A warning message
     */
    void warn(@Nullable String message);

    /**
     * Log a message at the {@link LogLevel#WARN} level. The supplier is only invoked if the log level threshold
     * is at WARN or below.
     * @param messageSupplier A function that returns a warning message
     */
    default void warn(@NonNull Supplier<String> messageSupplier) {
        if (!getThresholdLevel().above(LogLevel.WARN)) {
            warn(messageSupplier.get());
        }
    }

    /**
     * Log a message and a throwable issue at the {@link LogLevel#WARN} level.
     * @param message A warning message
     * @param issue An issue that caused this warning
     */
    void warn(@Nullable String message, @Nullable Throwable issue);

    /**
     * Log a message and a throwable issue at the {@link LogLevel#WARN} level. The supplier is only invoked if the
     * log level threshold is at WARN or below.
     * @param issue An issue that caused this warning
     * @param messageSupplier A function that returns a warning message
     */
    default void warn(@Nullable Throwable issue, @NonNull Supplier<String> messageSupplier) {
        if (!getThresholdLevel().above(LogLevel.WARN)) {
            warn(messageSupplier.get(), issue);
        }
    }

    /**
     * Logs a message at {@link LogLevel#INFO} level.
     * @param message An informational message
     */
    void info(@Nullable String message);

    /**
     * Logs a message at {@link LogLevel#INFO} level. The supplier is only invoked if the log level threshold
     * is at INFO or below.
     * @param messageSupplier A function that returns an info message
     */
    default void info(@NonNull Supplier<String> messageSupplier) {
        if (!getThresholdLevel().above(LogLevel.INFO)) {
            info(messageSupplier.get());
        }
    }

    /**
     * Logs a message at the {@link LogLevel#DEBUG} level.
     * @param message A debugging message.
     */
    void debug(@Nullable String message);

    /**
     * Logs a message at the {@link LogLevel#DEBUG} level. The supplier is only invoked if the log level threshold
     * is at DEBUG or below.
     * @param messageSupplier A function that returns a debugging message
     */
    default void debug(@NonNull Supplier<String> messageSupplier) {
        if (!getThresholdLevel().above(LogLevel.DEBUG)) {
            debug(messageSupplier.get());
        }
    }

    /**
     * Logs a message at the {@link LogLevel#VERBOSE} level.
     * @param message A verbose message
     */
    void verbose(@Nullable String message);

    /**
     * Logs a message at the {@link LogLevel#VERBOSE} level. The supplier is only invoked if the log level threshold
     * is at VERBOSE.
     * @param messageSupplier A function that returns a verbose message
     */
    default void verbose(@NonNull Supplier<String> messageSupplier) {
        if (!getThresholdLevel().above(LogLevel.VERBOSE)) {
            verbose(messageSupplier.get());
        }
    }
}
