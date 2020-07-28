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

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.util.Immutable;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * An implementation of {@link Logger} that can store logs and
 * uses a provided logger to emit logs.
 */
final class BroadcastLogger implements Logger {
    // Maximum number of logs to store.
    private static final int MAX_NUM_LOGS = 500;
    // Logger used to emit the logs.
    private final Logger logger;
    // Indicates whether to store the logs emitted by this logger.
    private final boolean shouldStoreLogs;
    // The logs stored by this logger.
    private final List<LogEntry> logs;

    BroadcastLogger(@NonNull Logger logger, boolean shouldStoreLogs) {
        this.logger = Objects.requireNonNull(logger);
        this.shouldStoreLogs = shouldStoreLogs;
        logs = new LinkedList<>();
    }

    @NonNull
    @Override
    public LogLevel getThresholdLevel() {
        return logger.getThresholdLevel();
    }

    @NonNull
    @Override
    public String getNamespace() {
        return logger.getNamespace();
    }

    @Override
    public void error(@Nullable String message) {
        if (getThresholdLevel().above(LogLevel.ERROR)) {
            return;
        }
        addToLogs(new LogEntry(LocalDateTime.now(), getNamespace(), message, LogLevel.ERROR));
        logger.error(message);
    }

    @Override
    public void error(@Nullable String message, @Nullable Throwable error) {
        if (getThresholdLevel().above(LogLevel.ERROR)) {
            return;
        }
        addToLogs(new LogEntry(LocalDateTime.now(), getNamespace(), message, error, LogLevel.ERROR));
        logger.error(message, error);
    }

    @Override
    public void warn(@Nullable String message) {
        if (getThresholdLevel().above(LogLevel.WARN)) {
            return;
        }
        addToLogs(new LogEntry(LocalDateTime.now(), getNamespace(), message, LogLevel.WARN));
        logger.warn(message);
    }

    @Override
    public void warn(@Nullable String message, @Nullable Throwable issue) {
        if (getThresholdLevel().above(LogLevel.WARN)) {
            return;
        }
        addToLogs(new LogEntry(LocalDateTime.now(), getNamespace(), message, issue, LogLevel.WARN));
        logger.warn(message, issue);
    }

    @SuppressLint("LogConditional") // We guard with our own LogLevel.
    @Override
    public void info(@Nullable String message) {
        if (getThresholdLevel().above(LogLevel.INFO)) {
            return;
        }
        addToLogs(new LogEntry(LocalDateTime.now(), getNamespace(), message, LogLevel.INFO));
        logger.info(message);
    }

    @SuppressLint("LogConditional") // We guard with our own LogLevel.
    @Override
    public void debug(@Nullable String message) {
        if (getThresholdLevel().above(LogLevel.DEBUG)) {
            return;
        }
        addToLogs(new LogEntry(LocalDateTime.now(), getNamespace(), message, LogLevel.DEBUG));
        logger.debug(message);
    }

    @SuppressLint("LogConditional") // We guard with our own LogLevel.
    @Override
    public void verbose(@Nullable String message) {
        if (getThresholdLevel().above(LogLevel.VERBOSE)) {
            return;
        }
        addToLogs(new LogEntry(LocalDateTime.now(), getNamespace(), message, LogLevel.VERBOSE));
        logger.verbose(message);
    }

    /**
     * Returns the logs stored by this logger.
     * @return the list of logs stored by this logger.
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
        if (shouldStoreLogs) {
            if (logs.size() == MAX_NUM_LOGS) {
                logs.remove(0);
            }
            logs.add(logEntry);
        }
    }
}
