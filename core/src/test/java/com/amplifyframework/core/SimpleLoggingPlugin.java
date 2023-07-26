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

package com.amplifyframework.core;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.logging.LoggingPlugin;

import org.json.JSONObject;

import java.util.UUID;

/**
 * Some plugin (doesn't really matter what kind) that we can use
 * to test the functionality of {@link Amplify#addPlugin(Plugin)}
 * and {@link Amplify#removePlugin(Plugin)}.
 */
@SuppressWarnings("ConstantConditions") // null returns on @NonNull; this is only a stub.
public final class SimpleLoggingPlugin extends LoggingPlugin<Void> {
    private final String uuid;

    private SimpleLoggingPlugin() {
        this.uuid = UUID.randomUUID().toString();
    }

    /**
     * Creates a new SimpleLoggingPlugin.
     * @return A simple logging plugin
     */
    public static SimpleLoggingPlugin instance() {
        return new SimpleLoggingPlugin();
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return uuid;
    }

    @Override
    public void configure(
            @NonNull final JSONObject pluginConfiguration,
            @NonNull final Context context) {
        // No configuration for this one. Cool, huh?
    }

    @Override
    public Void getEscapeHatch() {
        // No escape hatch, either. Sweet.
        return null;
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
        return null;
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
}
