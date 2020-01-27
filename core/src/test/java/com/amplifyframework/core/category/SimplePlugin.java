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

package com.amplifyframework.core.category;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.plugin.Plugin;

import org.json.JSONObject;

import java.util.Objects;

@SuppressWarnings("unused")
final class SimplePlugin<T> implements Plugin<T> {

    private final T escapeHatch;
    private final CategoryType categoryType;
    private JSONObject pluginConfiguration;
    private Context context;

    private SimplePlugin(@Nullable T escapeHatch, @NonNull CategoryType categoryType) {
        this.escapeHatch = escapeHatch;
        this.categoryType = categoryType;
    }

    @SuppressWarnings("SameParameterValue")
    static SimplePlugin<Void> type(@NonNull CategoryType categoryType) {
        Objects.requireNonNull(categoryType);
        return new SimplePlugin<>(null, categoryType);
    }

    static <T> SimplePlugin<T> instance(@NonNull T escapeHatch, @NonNull CategoryType categoryType) {
        Objects.requireNonNull(escapeHatch);
        Objects.requireNonNull(categoryType);
        return new SimplePlugin<>(escapeHatch, categoryType);
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return SimplePlugin.class.getSimpleName();
    }

    @Override
    public void configure(@NonNull JSONObject pluginConfiguration, @NonNull Context context) {
        this.pluginConfiguration = pluginConfiguration;
        this.context = context;
    }

    @Override
    public void initialize(@NonNull Context context) {
        this.context = context;
    }

    @Nullable
    @Override
    public T getEscapeHatch() {
        return escapeHatch;
    }

    @NonNull
    @Override
    public CategoryType getCategoryType() {
        return categoryType;
    }

    @Nullable
    Context getContext() {
        return context;
    }

    @Nullable
    JSONObject getPluginConfiguration() {
        return pluginConfiguration;
    }
}
