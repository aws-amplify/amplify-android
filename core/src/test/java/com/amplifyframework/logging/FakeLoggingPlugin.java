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

import com.amplifyframework.core.category.CategoryType;

import org.json.JSONObject;

/**
 * This is a test-double of the {@link AndroidLoggingPlugin}. Its main purpose
 * is as a factory for {@link FakeLogger}s.
 *
 * This is useful for testing the plugin-management behaviour of the {@link LoggingCategory}
 * in isolation to the details of the {@link AndroidLoggingPlugin}.
 * @param <E> The type of the escape hatch used by the plugin.
 */
@SuppressWarnings("unused")
final class FakeLoggingPlugin<E> extends LoggingPlugin<E> {
    private final E escapeHatch;
    private final Logger defaultLogger;

    private JSONObject pluginConfiguration;
    private Context context;

    private FakeLoggingPlugin(E escapeHatch, Logger defaultLogger) {
        this.escapeHatch = escapeHatch;
        this.defaultLogger = defaultLogger;
    }

    static FakeLoggingPlugin<Void> instance(@NonNull Logger defaultLogger) {
        return new FakeLoggingPlugin<>(null, defaultLogger);
    }

    static FakeLoggingPlugin<Void> instance() {
        return new FakeLoggingPlugin<>(null, FakeLogger.instance());
    }

    static <E> FakeLoggingPlugin<E> instance(@NonNull E escapeHatch, @NonNull Logger defaultLogger) {
        return new FakeLoggingPlugin<>(escapeHatch, defaultLogger);
    }

    static <E> FakeLoggingPlugin<E> instance(@NonNull E escapeHatch) {
        return new FakeLoggingPlugin<>(escapeHatch, FakeLogger.instance());
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return FakeLoggingPlugin.class.getSimpleName();
    }

    @Override
    public void configure(@NonNull JSONObject pluginConfiguration, @NonNull Context context) {
        this.pluginConfiguration = pluginConfiguration;
        this.context = context;
    }

    @Nullable
    @Override
    public E getEscapeHatch() {
        return escapeHatch;
    }

    @NonNull
    @Override
    public Logger getDefaultLogger() {
        return defaultLogger;
    }

    @NonNull
    @Override
    public Logger forNamespaceAndThreshold(@Nullable String namespace, @Nullable LogLevel threshold) {
        if (namespace == null) {
            return threshold == null ? FakeLogger.instance() : FakeLogger.instance(threshold);
        }
        return threshold == null ? FakeLogger.instance(namespace) : FakeLogger.instance(namespace, threshold);
    }

    @NonNull
    @Override
    public Logger forNamespace(@Nullable String namespace) {
        if (namespace == null) {
            return FakeLogger.instance();
        } else {
            return FakeLogger.instance(namespace);
        }
    }

    @NonNull
    @Override
    public Logger forCategory(@NonNull CategoryType categoryType) {
        return forNamespace("amplify:" + categoryType.getConfigurationKey());
    }

    JSONObject getPluginConfiguration() {
        return pluginConfiguration;
    }

    Context getContext() {
        return context;
    }
}
