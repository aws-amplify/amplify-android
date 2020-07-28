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
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The LoggingCategory is a collection of zero or more plugin
 * implementations which implement the logging category behavior. The
 * LoggingCategoryBehavior itself is provided by this top-level
 * LoggingCategory class.
 */
public final class LoggingCategory extends Category<LoggingPlugin<?>> implements LoggingCategoryBehavior {
    private final LoggingPlugin<?> defaultPlugin;
    // List of BroadcastLoggers created.
    private final List<BroadcastLogger> loggers;
    // Indicates whether the logs from all loggers should be stored.
    private boolean storeLogs;

    /**
     * Constructs a logging category.
     */
    public LoggingCategory() {
        this(new AndroidLoggingPlugin());
    }

    @VisibleForTesting
    LoggingCategory(LoggingPlugin<?> defaultPlugin) {
        super();
        this.defaultPlugin = defaultPlugin;
        loggers = new ArrayList<>();
    }

    @NonNull
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.LOGGING;
    }

    @NonNull
    @Override
    public Logger forNamespace(@Nullable String namespace) {
        Logger logger = getLoggingPlugin().forNamespace(namespace);
        if (!(logger instanceof BroadcastLogger)) {
            return createBroadcastLogger(logger);
        } else {
            return logger;
        }
    }

    /**
     * Creates a new BroadcastLogger that uses the given logger to emit logs.
     * @param logger the Logger used to emit logs.
     * @return a new BroadcastLogger.
     */
    protected Logger createBroadcastLogger(@NonNull Logger logger) {
        Objects.requireNonNull(logger);
        BroadcastLogger broadcastLogger = new BroadcastLogger(logger, storeLogs);
        loggers.add(broadcastLogger);
        return broadcastLogger;
    }

    /**
     * Set whether the logs from all loggers should be stored.
     * @param storeLogs boolean indicating whether to store the logs.
     */
    public void shouldStoreLogs(boolean storeLogs) {
        this.storeLogs = storeLogs;
    }

    /**
     * Returns the logs stored by all of the {@link BroadcastLogger}s.
     * @return a list of LogEntry.
     */
    public List<LogEntry> getLogs() {
        List<LogEntry> logs = new ArrayList<>();
        if (storeLogs) {
            for (BroadcastLogger logger : loggers) {
                logs.addAll(logger.getLogs());
            }
        }
        return logs;
    }

    @NonNull
    private LoggingPlugin<?> getLoggingPlugin() {
        if (!super.isInitialized() || super.getPlugins().isEmpty()) {
            return defaultPlugin;
        } else {
            return super.getSelectedPlugin();
        }
    }
}
