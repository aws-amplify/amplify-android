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

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link Logger} that emits logs to all loggers.
 */
public final class BroadcastLogger implements Logger {
    // List of loggers to emit logs to.
    private final List<Logger> delegates;

    /**
     * Creates a new BroadcastLogger.
     * @param delegates the list of loggers to emit logs to
     */
    public BroadcastLogger(@Nullable List<Logger> delegates) {
        this.delegates = new ArrayList<>();
        if (delegates != null) {
            this.delegates.addAll(delegates);
        }
    }

    @NonNull
    @Override
    public LogLevel getThresholdLevel() {
        throw new UnsupportedOperationException("Cannot get threshold level for BroadcastLogger.");
    }

    @NonNull
    @Override
    public String getNamespace() {
        if (delegates.isEmpty()) {
            return "";
        } else {
            return delegates.get(0).getNamespace();
        }
    }

    @Override
    public void error(@Nullable String message) {
        for (Logger delegate : delegates) {
            delegate.error(message);
        }
    }

    @Override
    public void error(@Nullable String message, @Nullable Throwable error) {
        for (Logger delegate : delegates) {
            delegate.error(message, error);
        }
    }

    @Override
    public void warn(@Nullable String message) {
        for (Logger delegate : delegates) {
            delegate.warn(message);
        }
    }

    @Override
    public void warn(@Nullable String message, @Nullable Throwable issue) {
        for (Logger delegate : delegates) {
            delegate.warn(message, issue);
        }
    }

    @SuppressLint("LogConditional") // We guard with our own LogLevel.
    @Override
    public void info(@Nullable String message) {
        for (Logger delegate : delegates) {
            delegate.info(message);
        }
    }

    @SuppressLint("LogConditional") // We guard with our own LogLevel.
    @Override
    public void debug(@Nullable String message) {
        for (Logger delegate : delegates) {
            delegate.debug(message);
        }
    }

    @SuppressLint("LogConditional") // We guard with our own LogLevel.
    @Override
    public void verbose(@Nullable String message) {
        for (Logger delegate : delegates) {
            delegate.verbose(message);
        }
    }
}
