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

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.BuildConfig;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.logging.LoggingCategoryBehavior;
import com.amplifyframework.logging.LoggingPlugin;
import com.amplifyframework.util.Immutable;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the {@link LoggingCategoryBehavior} that stores logs.
 */
public final class PersistentLogStoragePlugin extends LoggingPlugin<Void> {
    private static final String AMPLIFY_NAMESPACE = "amplify";
    // Map from namespace to the PersistentLogger for that namespace.
    private final Map<String, PersistentLogger> loggers;

    /**
     * Creates a new PersistentLogStoragePlugin.
     */
    public PersistentLogStoragePlugin() {
        loggers = new HashMap<>();
    }

    @NonNull
    @Override
    @SuppressWarnings("deprecation")
    public Logger forNamespace(@Nullable String namespace) {
        String usedNamespace = namespace == null ? AMPLIFY_NAMESPACE : namespace;
        return logger(usedNamespace);
    }

    @NonNull
    @Override
    public Logger logger(@NonNull String namespace) {
        PersistentLogger preExistingLogger = loggers.get(namespace);
        if (preExistingLogger != null) {
            return preExistingLogger;
        } else {
            PersistentLogger newLogger = new PersistentLogger(namespace);
            loggers.put(namespace, newLogger);
            return newLogger;
        }
    }

    @NonNull
    @Override
    public Logger logger(@NonNull CategoryType categoryType, @NonNull String namespace) {
        return logger(namespace);
    }

    @Override
    public void enable() {
        PersistentLogger.setIsEnabled(true);
    }

    @Override
    public void disable() {
        PersistentLogger.setIsEnabled(false);
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return PersistentLogStoragePlugin.class.getSimpleName();
    }

    @Override
    public void configure(JSONObject pluginConfiguration, @NonNull Context context) {}

    @Nullable
    @Override
    public Void getEscapeHatch() {
        return null;
    }

    @NonNull
    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * Returns the logs stored by all of the {@link PersistentLogger}s
     * in order from oldest to newest in terms of timestamp.
     * @return a sorted list of LogEntry.
     */
    public List<LogEntry> getLogs() {
        List<LogEntry> logs = new ArrayList<>();
        for (PersistentLogger logger : loggers.values()) {
            logs.addAll(logger.getLogs());
        }
        Collections.sort(logs);
        return Immutable.of(logs);
    }
}
