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

package com.amplifyframework.devmenu;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.logging.LogLevel;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.Immutable;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * An implementation of {@link Logger} that stores logs.
 */
final class PersistentLogger implements Logger {
    // Maximum number of logs to store.
    private static final int MAX_NUM_LOGS = 500;
    private static boolean isEnabled = true;
    // Namespace for this logger.
    private final String namespace;
    // The logs stored by this logger.
    private final List<LogEntry> logs;

    PersistentLogger(@NonNull String namespace) {
        this.namespace = Objects.requireNonNull(namespace);
        this.logs = new LinkedList<>();
    }

    static void setIsEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    @NonNull
    @Override
    public LogLevel getThresholdLevel() {
        return LogLevel.VERBOSE;
    }

    @NonNull
    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public void error(@Nullable String message) {
        addToLogs(message, null, LogLevel.ERROR);
    }

    @Override
    public void error(@Nullable String message, @Nullable Throwable error) {
        addToLogs(message, error, LogLevel.ERROR);
    }

    @Override
    public void warn(@Nullable String message) {
        addToLogs(message, null, LogLevel.WARN);
    }

    @Override
    public void warn(@Nullable String message, @Nullable Throwable issue) {
        addToLogs(message, issue, LogLevel.WARN);
    }

    @SuppressLint("LogConditional") // We guard with our own LogLevel.
    @Override
    public void info(@Nullable String message) {
        addToLogs(message, null, LogLevel.INFO);
    }

    @SuppressLint("LogConditional") // We guard with our own LogLevel.
    @Override
    public void debug(@Nullable String message) {
        addToLogs(message, null, LogLevel.DEBUG);
    }

    @SuppressLint("LogConditional") // We guard with our own LogLevel.
    @Override
    public void verbose(@Nullable String message) {
        addToLogs(message, null, LogLevel.VERBOSE);
    }

    /**
     * Returns the logs stored by this logger.
     * @return the list of logs stored by this logger.
     */
    public List<LogEntry> getLogs() {
        return Immutable.of(logs);
    }

    /**
     * Stores a new log with the given information and removes the first log currently
     * stored if there would be more than MAX_NUM_LOGS stored.
     * @param message the message for the log
     * @param throwable the throwable (if any) associated with the log
     * @param logLevel the level the log was logged at
     */
    private void addToLogs(String message, Throwable throwable, LogLevel logLevel) {
        if (!isEnabled) {
            return;
        }
        if (logs.size() == MAX_NUM_LOGS) {
            logs.remove(0);
        }
        logs.add(new LogEntry(new Date(), namespace, message, throwable, logLevel));
    }
}
