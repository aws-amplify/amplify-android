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

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * AWS' default implementation of the {@link LoggingCategoryBehavior},
 * which emits logs to Android's {@link Log} class.
 */
public final class AndroidLoggingPlugin extends LoggingPlugin<Void> {
    private static final String AMPLIFY_NAMESPACE = "amplify";
    private final LogLevel defaultLoggerThreshold;

    /**
     * Creates a logging plugin using {@link LogLevel#INFO} as the default
     * logging threshold.
     */
    @SuppressWarnings("WeakerAccess") // This is a a public API
    public AndroidLoggingPlugin() {
        this(LogLevel.INFO);
    }

    /**
     * Constructs a logging plugin that used the provided threshold by default,
     * when creating loggers.
     * @param defaultLoggerThreshold default threshold to use when creating loggers.
     */
    @SuppressWarnings("WeakerAccess") // This is a a public API
    public AndroidLoggingPlugin(@NonNull LogLevel defaultLoggerThreshold) {
        this.defaultLoggerThreshold = defaultLoggerThreshold;
    }

    @NonNull
    @Override
    public Logger forNamespace(@Nullable String namespace) {
        String usedNamespace = namespace == null ? AMPLIFY_NAMESPACE : namespace;
        return new AndroidLogger(usedNamespace, defaultLoggerThreshold);
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return "AndroidLoggingPlugin";
    }

    @Override
    public void configure(
            JSONObject pluginConfiguration,
            @NonNull Context context) {
        // In the future, accept a log level configuration from JSON?
    }

    @Nullable
    @Override
    public Void getEscapeHatch() {
        return null;
    }
}
