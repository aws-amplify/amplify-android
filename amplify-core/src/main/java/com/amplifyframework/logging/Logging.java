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
import android.support.annotation.NonNull;

import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.exception.ConfigurationException;
import com.amplifyframework.core.plugin.PluginException;

import java.util.Set;

public class Logging implements Category<LoggingPlugin, LoggingPluginConfiguration>, LoggingCategoryBehavior {

    @Override
    public void configure(@NonNull Context context) throws ConfigurationException, PluginException {

    }

    @Override
    public void addPlugin(@NonNull LoggingPlugin plugin) throws PluginException {

    }

    @Override
    public void addPlugin(@NonNull LoggingPlugin plugin, @NonNull LoggingPluginConfiguration pluginConfiguration) throws PluginException {

    }

    @Override
    public void removePlugin(@NonNull LoggingPlugin plugin) throws PluginException {

    }

    @Override
    public void reset() {

    }

    @Override
    public LoggingPlugin getPlugin(@NonNull String pluginKey) throws PluginException {
        return null;
    }

    @Override
    public Set<LoggingPlugin> getPlugins() {
        return null;
    }

    @Override
    public CategoryType getCategoryType() {
        return null;
    }
}
