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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.plugin.Plugin;

import org.json.JSONObject;

@SuppressWarnings("unused")
final class FakePlugin<T> implements Plugin<T> {
    private final String pluginKey;
    private final T escapeHatch;
    private final CategoryType categoryType;
    private JSONObject pluginConfiguration;
    private Context context;

    private FakePlugin(String pluginKey, T escapeHatch, CategoryType categoryType) {
        this.pluginKey = pluginKey;
        this.escapeHatch = escapeHatch;
        this.categoryType = categoryType;
    }

    static <T> Builder<T> builder() {
        return new Builder<>();
    }

    @Override
    public String getPluginKey() {
        return pluginKey;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public void configure(JSONObject pluginConfiguration, Context context) throws AmplifyException {
        this.pluginConfiguration = pluginConfiguration;
        this.context = context;
    }

    @Override
    public T getEscapeHatch() {
        return escapeHatch;
    }

    @Override
    public CategoryType getCategoryType() {
        return categoryType;
    }

    JSONObject getPluginConfiguration() {
        return pluginConfiguration;
    }

    Context getContext() {
        return context;
    }

    static final class Builder<T> {
        private String pluginKey;
        private T escapeHatch;
        private CategoryType categoryType;

        Builder<T> pluginKey(String pluginKey) {
            this.pluginKey = pluginKey;
            return this;
        }

        Builder<T> escapeHatch(T escapeHatch) {
            this.escapeHatch = escapeHatch;
            return this;
        }

        Builder<T> categoryType(CategoryType categoryType) {
            this.categoryType = categoryType;
            return this;
        }

        FakePlugin<T> build() {
            return new FakePlugin<>(pluginKey, escapeHatch, categoryType);
        }
    }
}
