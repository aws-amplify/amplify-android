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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A representation of a log.
 */
public final class LogEntry {
    // The format for the log's date and time.
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    // The date and time of the log.
    private LocalDateTime dateTime;
    // The namespace of the logger that emitted the log.
    private String namespace;
    // The message for the log.
    private String message;
    // The Throwable (if any) associated with the log.
    private Throwable throwable;
    // The level the log was logged at.
    private LogLevel logLevel;

    /**
     * Creates a new LogEntry representing a log with the given time, tag,
     * and message that was logged at the given level.
     * @param dateTime the date and time of the log.
     * @param namespace the namespace of the logger that emitted the log.
     * @param message the message for the log.
     * @param logLevel the level the log was logged at.
     */
    public LogEntry(LocalDateTime dateTime, String namespace, String message, LogLevel logLevel) {
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
    public LogEntry(LocalDateTime dateTime, String namespace, String message, Throwable throwable, LogLevel logLevel) {
        this.dateTime = dateTime;
        this.namespace = namespace;
        this.message = message;
        this.throwable = throwable;
        this.logLevel = logLevel;
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
