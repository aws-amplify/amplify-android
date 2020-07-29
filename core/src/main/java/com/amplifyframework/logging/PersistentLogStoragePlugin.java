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

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.devmenu.LogEntry;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the {@link LoggingCategoryBehavior} that stores logs.
 */
public final class PersistentLogStoragePlugin extends LoggingPlugin<Void> {
    private static final String AMPLIFY_NAMESPACE = "amplify";
    // List of PersistentLoggers created.
    private final List<PersistentLogger> loggers;

    /**
     * Creates a new PersistentLogStoragePlugin.
     */
    public PersistentLogStoragePlugin() {
        loggers = new ArrayList<>();
    }

    @NonNull
    @Override
    public Logger forNamespace(@Nullable String namespace) {
        String usedNamespace = namespace == null ? AMPLIFY_NAMESPACE : namespace;
        PersistentLogger logger = new PersistentLogger(usedNamespace);
        loggers.add(logger);
        return logger;
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return "PersistentLogStoragePlugin";
    }

    @Override
    public void configure(
            JSONObject pluginConfiguration,
            @NonNull Context context) {
    }

    @Nullable
    @Override
    public Void getEscapeHatch() {
        return null;
    }

    /**
     * Returns the logs stored by all of the {@link PersistentLogger}s.
     * @return a list of LogEntry.
     */
    public List<LogEntry> getLogs() {
        List<LogEntry> logs = new ArrayList<>();
        for (PersistentLogger logger : loggers) {
            logs.addAll(logger.getLogs());
        }
        return logs;
    }
}
