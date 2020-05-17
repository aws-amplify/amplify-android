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

import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This is a test double of the {@link AndroidLogger}. It works as an in-memory
 * buffer. Pass in a log, and it gets added to a list of logs. Later, you can
 * inspect the log lines via {@link FakeLogger#getLogs()}.
 * {@link FakeLogger.Log#assertEquals(LogLevel, String, Throwable)} simplifies the
 * work of verifying an actual log line against one that was captured by this logger
 * implementation.
 */
final class FakeLogger implements Logger {
    private final String namespace;
    private final LogLevel threshold;
    private final List<Log> logs;

    private FakeLogger(String namespace, LogLevel threshold) {
        this.namespace = namespace;
        this.threshold = threshold;
        this.logs = new ArrayList<>();
    }

    @SuppressWarnings("SameParameterValue")
    static FakeLogger instance(@NonNull String namespace, @NonNull LogLevel threshold) {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(threshold);
        return new FakeLogger(namespace, threshold);
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
        logs.add(Log.instance(LogLevel.ERROR, message));
    }

    @Override
    public void error(@Nullable String message, @Nullable Throwable error) {
        logs.add(Log.instance(LogLevel.ERROR, message, error));
    }

    @Override
    public void warn(@Nullable String message) {
        logs.add(Log.instance(LogLevel.WARN, message));
    }

    @Override
    public void warn(@Nullable String message, @Nullable Throwable issue) {
        logs.add(Log.instance(LogLevel.WARN, message, issue));
    }

    @Override
    public void info(@Nullable String message) {
        logs.add(Log.instance(LogLevel.INFO, message));
    }

    @Override
    public void debug(@Nullable String message) {
        logs.add(Log.instance(LogLevel.DEBUG, message));
    }

    @Override
    public void verbose(@Nullable String message) {
        logs.add(Log.instance(LogLevel.VERBOSE, message));
    }

    List<Log> getLogs() {
        return Immutable.of(logs);
    }

    static final class Log {
        private final LogLevel logLevel;
        private final String message;
        private final Throwable throwable;

        private Log(LogLevel logLevel, String message, Throwable throwable) {
            this.logLevel = logLevel;
            this.message = message;
            this.throwable = throwable;
        }

        @NonNull
        static Log instance(@NonNull LogLevel level, @Nullable String message) {
            Objects.requireNonNull(level);
            Objects.requireNonNull(message);
            return new Log(level, message, null);
        }

        @NonNull
        static Log instance(@NonNull LogLevel level, @Nullable String message, @Nullable Throwable throwable) {
            Objects.requireNonNull(level);
            Objects.requireNonNull(message);
            Objects.requireNonNull(throwable);
            return new Log(level, message, throwable);
        }

        void assertEquals(
                @Nullable LogLevel actualLevel,
                @Nullable String actualMessage,
                @Nullable Throwable actualThrowable) {
            org.junit.Assert.assertEquals(logLevel, actualLevel);
            org.junit.Assert.assertEquals(message, actualMessage);
            org.junit.Assert.assertEquals(throwable, actualThrowable);
        }
    }
}
