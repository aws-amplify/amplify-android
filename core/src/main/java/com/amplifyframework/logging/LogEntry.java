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

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * A representation of a log.
 */
public final class LogEntry {
    // The format for the log's date and time.
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private final LocalDateTime dateTime;
    private final String namespace;
    private final String message;
    private final Throwable throwable;
    private final LogLevel logLevel;

    /**
     * Creates a new LogEntry representing a log with the given time, tag,
     * and message that was logged at the given level.
     * @param dateTime the date and time of the log.
     * @param namespace the namespace of the logger that emitted the log.
     * @param message the message for the log.
     * @param logLevel the level the log was logged at.
     */
    public LogEntry(@NonNull LocalDateTime dateTime, @Nullable String namespace, @Nullable String message,
                    @NonNull LogLevel logLevel) {
        this(dateTime, namespace, message, null, logLevel);
    }

    /**
     * Creates a new LogEntry representing a log with the given time, tag, message,
     * and throwable that was logged at the given level.
     * @param dateTime the date and time of the log.
     * @param namespace the namespace of the logger that emitted the log.
     * @param message the message for the log.
     * @param throwable the Throwable associated with the log.
     * @param logLevel the level the log was logged at.
     */
    public LogEntry(@NonNull LocalDateTime dateTime, @Nullable String namespace, @Nullable String message,
                    @Nullable Throwable throwable, @NonNull LogLevel logLevel) {
        this.dateTime = Objects.requireNonNull(dateTime);
        this.logLevel = Objects.requireNonNull(logLevel);
        this.namespace = namespace;
        this.message = message;
        this.throwable = throwable;
    }

    /**
     * Gets the date and time of the log.
     * @return the date and time of the log.
     */
    public LocalDateTime getDateTime() {
        return dateTime;
    }

    /**
     * Gets the namespace of the logger that emitted the log.
     * @return the namespace of the logger that emitted the log.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Gets the message for the log.
     * @return the message for the log.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the throwable for the log.
     * @return the throwable for the log, or null if there is no throwable for the log.
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Gets the level the log was logged at.
     * @return the level the log was logged at.
     */
    public LogLevel getLogLevel() {
        return logLevel;
    }

    /**
     * Returns a String representation of this log.
     * @return a String representing this log.
     */
    public String toString() {
        return String.format("[%s] %s %s: %s \n%s", logLevel.name(),
                dateTime.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)), namespace, message,
                throwable == null ? "" : Log.getStackTraceString(throwable));
    }
}
