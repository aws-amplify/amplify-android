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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.logging.LoggingPlugin;

import org.json.JSONObject;

/**
 * A test class which always throws whenever {@link BadInitLoggingPlugin#configure(JSONObject, Context)}
 * is invoked.
 */
@SuppressWarnings("ConstantConditions") // It isn't supposed to be at all usable, just to throw on configure.
public final class BadInitLoggingPlugin extends LoggingPlugin<Void> {
    private BadInitLoggingPlugin() {}

    /**
     * Creates an instance of the {@link BadInitLoggingPlugin}.
     * @return A BadInitLoggingPlugin
     */
    public static BadInitLoggingPlugin instance() {
        return new BadInitLoggingPlugin();
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return BadInitLoggingPlugin.class.getSimpleName();
    }

    @Override
    public void configure(@NonNull JSONObject pluginConfiguration, @NonNull Context context) {}

    @Override
    public void initialize(@NonNull Context context) throws AmplifyException {
        throw new AmplifyException(
            "The point of this test class is to throw on initialization.",
            "Sit back and enjoy a root beer, because you're seeing this right!"
        );
    }

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
