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

import com.amplifyframework.core.BuildConfig;
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
final class FakeLoggingPlugin<E> extends LoggingPlugin<E> {
    private final E escapeHatch;
    private final Logger logger;

    private FakeLoggingPlugin(E escapeHatch, Logger logger) {
        this.escapeHatch = escapeHatch;
        this.logger = logger;
    }

    static FakeLoggingPlugin<Void> instance(Logger defaultLogger) {
        return new FakeLoggingPlugin<>(null, defaultLogger);
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return FakeLoggingPlugin.class.getSimpleName();
    }

    @Override
    public void configure(@NonNull JSONObject pluginConfiguration, @NonNull Context context) {}

    @Nullable
    @Override
    public E getEscapeHatch() {
        return escapeHatch;
    }

    @NonNull
    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @NonNull
    @Override
    @SuppressWarnings("deprecation")
    public Logger forNamespace(@Nullable String namespace) {
        return logger;
    }

    @NonNull
    @Override
    public Logger logger(@NonNull String namespace) {
        return logger;
    }

    @NonNull
    @Override
    public Logger logger(@NonNull CategoryType categoryType, @NonNull String namespace) {
        return logger;
    }

    @Override
    public void enable() {
    }

    @Override
    public void disable() {
    }
}
