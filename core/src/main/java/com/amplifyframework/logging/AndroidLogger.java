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

import android.annotation.SuppressLint;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.util.Immutable;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

final class AndroidLogger implements Logger {
    // Maximum number of logs to store.
    private static final int MAX_NUM_LOGS = 500;
    private final LogLevel threshold;
    private final String namespace;
    // The logs stored by this logger.
    private List<LogEntry> logs;

    AndroidLogger(@NonNull String namespace, @NonNull LogLevel threshold, boolean storeLogs) {
        this.threshold = Objects.requireNonNull(threshold);
        this.namespace = Objects.requireNonNull(namespace);
        if (storeLogs) {
            logs = new LinkedList<>();
        }
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
        if (threshold.above(LogLevel.ERROR)) {
            return;
        }
        addToLogs(new LogEntry(LocalDateTime.now(), getNamespace(), message, LogLevel.ERROR));
        Log.e(namespace, String.valueOf(message));
    }

    @Override
    public void error(@Nullable String message, @Nullable Throwable error) {
        if (threshold.above(LogLevel.ERROR)) {
            return;
        }
        addToLogs(new LogEntry(LocalDateTime.now(), getNamespace(), message, error, LogLevel.ERROR));
        Log.e(namespace, message, error);
    }

    @Override
    public void warn(@Nullable String message) {
        if (threshold.above(LogLevel.WARN)) {
            return;
        }
        addToLogs(new LogEntry(LocalDateTime.now(), getNamespace(), message, LogLevel.WARN));
        Log.w(namespace, String.valueOf(message));
    }

    @Override
    public void warn(@Nullable String message, @Nullable Throwable issue) {
        if (threshold.above(LogLevel.WARN)) {
            return;
        }
        addToLogs(new LogEntry(LocalDateTime.now(), getNamespace(), message, issue, LogLevel.WARN));
        Log.w(namespace, message, issue);
    }

    @SuppressLint("LogConditional") // We guard with our own LogLevel.
    @Override
    public void info(@Nullable String message) {
        if (threshold.above(LogLevel.INFO)) {
            return;
        }
        addToLogs(new LogEntry(LocalDateTime.now(), getNamespace(), message, LogLevel.INFO));
        Log.i(namespace, String.valueOf(message));
    }

    @SuppressLint("LogConditional") // We guard with our own LogLevel.
    @Override
    public void debug(@Nullable String message) {
        if (threshold.above(LogLevel.DEBUG)) {
            return;
        }
        addToLogs(new LogEntry(LocalDateTime.now(), getNamespace(), message, LogLevel.DEBUG));
        Log.d(namespace, String.valueOf(message));
    }

    @SuppressLint("LogConditional") // We guard with our own LogLevel.
    @Override
    public void verbose(@Nullable String message) {
        if (threshold.above(LogLevel.VERBOSE)) {
            return;
        }
        addToLogs(new LogEntry(LocalDateTime.now(), getNamespace(), message, LogLevel.VERBOSE));
        Log.v(namespace, String.valueOf(message));
    }

    /**
     * Returns the logs stored by this logger, or null if no logs were stored.
     * @return the list of logs stored by this logger, or null if no logs were stored.
     */
    public List<LogEntry> getLogs() {
        return Immutable.of(logs);
    }

    /**
     * If the logs should be stored, then stores the given log and removes the
     * first log currently stored if there would be more than MAX_NUM_LOGS stored.
     * @param logEntry the log to be stored.
     */
    private void addToLogs(LogEntry logEntry) {
        if (logs != null) {
            if (logs.size() == MAX_NUM_LOGS) {
                logs.remove(0);
            }
            logs.add(logEntry);
        }
    }
}
