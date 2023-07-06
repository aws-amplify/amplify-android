/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Objects;

final class JavaLogger implements Logger {

    private static boolean isEnabled = true;
    private final LogLevel threshold;
    private final String namespace;

    JavaLogger(@NonNull String namespace, @NonNull LogLevel threshold) {
        this.threshold = Objects.requireNonNull(threshold);
        this.namespace = Objects.requireNonNull(namespace);
    }

    static void setIsEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    @NonNull
    @Override
    public LogLevel getThresholdLevel() {
        return threshold;
    }

    @NonNull
    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public void error(@Nullable String message) {
        if (shouldNotLogMessage(LogLevel.ERROR)) {
            return;
        }
        log(LogLevel.ERROR, message);
    }

    @Override
    public void error(@Nullable String message, @Nullable Throwable error) {
        if (shouldNotLogMessage(LogLevel.ERROR)) {
            return;
        }
        log(LogLevel.ERROR, message, error);
    }

    @Override
    public void warn(@Nullable String message) {
        if (shouldNotLogMessage(LogLevel.WARN)) {
            return;
        }
        log(LogLevel.WARN, message);
    }

    @Override
    public void warn(@Nullable String message, @Nullable Throwable issue) {
        if (shouldNotLogMessage(LogLevel.WARN)) {
            return;
        }
        log(LogLevel.WARN, message, issue);
    }

    @Override
    public void info(@Nullable String message) {
        if (shouldNotLogMessage(LogLevel.INFO)) {
            return;
        }
        log(LogLevel.INFO, message);
    }

    @Override
    public void debug(@Nullable String message) {
        if (shouldNotLogMessage(LogLevel.DEBUG)) {
            return;
        }
        log(LogLevel.DEBUG, message);
    }

    @Override
    public void verbose(@Nullable String message) {
        if (shouldNotLogMessage(LogLevel.VERBOSE)) {
            return;
        }
        log(LogLevel.VERBOSE, message);
    }

    private void log(@NonNull LogLevel level, @Nullable String message) {
        log(level, message, null);
    }

    private void log(@NonNull LogLevel level, @Nullable String message, @Nullable Throwable throwable) {
        StringBuilder lineBuilder = new StringBuilder()
                .append(level)
                .append("/")
                .append(namespace)
                .append(": ")
                .append(message);
        if (throwable != null) {
            lineBuilder.append("\n")
                    .append(throwable);
        }
        System.out.println(lineBuilder.toString());

    }

    private boolean shouldNotLogMessage(LogLevel logLevel) {
        return !isEnabled || threshold.above(logLevel);
    }

}
