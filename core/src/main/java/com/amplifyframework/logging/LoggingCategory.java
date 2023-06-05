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
import com.amplifyframework.util.Environment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The LoggingCategory is a collection of zero or more plugin
 * implementations which implement the logging category behavior. The
 * LoggingCategoryBehavior itself is provided by this top-level
 * LoggingCategory class.
 */
public final class LoggingCategory extends Category<LoggingPlugin<?>> implements LoggingCategoryBehavior {
    private final LoggingPlugin<?> defaultPlugin;

    /**
     * Constructs a logging category.
     *
     * When running on an Android device, logs are handled by the AndroidLoggingPlugin, which uses.
     * {@link android.util.Log}.
     *
     * When running unit tests, the Android library is not available, so logs are handled by the JavaLoggingPlugin,
     * which outputs logs using {@code System.out.println()}.
     */
    public LoggingCategory() {
        this(Environment.isJUnitTest() ? new JavaLoggingPlugin() : new AndroidLoggingPlugin());
    }

    @VisibleForTesting
    LoggingCategory(LoggingPlugin<?> defaultPlugin) {
        super();
        this.defaultPlugin = defaultPlugin;
    }

    @NonNull
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.LOGGING;
    }

    @NonNull
    @Override
    @SuppressWarnings("deprecation")
    public Logger forNamespace(@Nullable String namespace) {
        Set<LoggingPlugin<?>> loggingPlugins = new HashSet<>(getPlugins());
        loggingPlugins.add(defaultPlugin);
        List<Logger> delegates = new ArrayList<>();
        for (LoggingPlugin<?> plugin : loggingPlugins) {
            delegates.add(plugin.forNamespace(namespace));
        }
        return new BroadcastLogger(delegates);
    }

    @NonNull
    @Override
    public Logger logger(@NonNull String namespace) {
        return null;
    }

    @NonNull
    @Override
    public Logger logger(@NonNull CategoryType categoryType, @NonNull String namespace) {
        return null;
    }

    @Override
    public void enable() {

    }

    @Override
    public void disable() {

    }

    @NonNull
    @Override
    protected LoggingPlugin<?> getSelectedPlugin() throws IllegalStateException {
        throw new UnsupportedOperationException("Getting the selected logging plugin is not supported.");
    }
}
