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

package com.amplifyframework.api;

import android.support.annotation.NonNull;

import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.exception.ConfigurationException;
import com.amplifyframework.core.plugin.PluginException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ApiCategory implements Category<ApiPlugin>, ApiCategoryBehavior {
    /**
     * Map of the { pluginKey => plugin } object
     */
    private Map<String, ApiPlugin> plugins;

    /**
     * Flag to remember that API category is already configured by Amplify
     * and throw an error if configure method is called again
     */
    private boolean isConfigured;

    /**
     * Protect enabling and disabling of Analytics event
     * collection and sending.
     */
    private static final Object LOCK = new Object();

    public ApiCategory() {
        this.plugins = new ConcurrentHashMap<String, ApiPlugin>();
    }

    /**
     * Obtain the registered plugin. Throw runtime exception if
     * no plugin is registered or multiple plugins are registered.
     *
     * @return the only registered plugin for this category
     */
    private ApiPlugin getSelectedPlugin() {
        if (!isConfigured) {
            throw new ConfigurationException("API category is not yet configured.");
        }
        if (plugins.isEmpty()) {
            throw new PluginException.NoSuchPluginException();
        }
        if (plugins.size() > 1) {
            throw new PluginException.MultiplePluginsException();
        }

        return plugins.values().iterator().next();
    }
    /**
     * Configure API category based on AmplifyConfiguration object
     *
     * @param configuration AmplifyConfiguration object for configuration via code
     * @throws ConfigurationException thrown when already configured
     * @throws PluginException        thrown when there is no plugin found for a configuration
     */
    @Override
    public void configure(AmplifyConfiguration configuration) throws ConfigurationException, PluginException {
        if (isConfigured) {
            throw new ConfigurationException.AmplifyAlreadyConfiguredException();
        }

        if (!plugins.values().isEmpty()) {
            if (plugins.values().iterator().hasNext()) {
                ApiPlugin plugin = plugins.values().iterator().next();
                String pluginKey = plugin.getPluginKey();
                Object pluginConfig = configuration.api.pluginConfigs.get(pluginKey);
                if (pluginConfig != null) {
                    plugin.configure(pluginConfig);
                } else {
                    throw new PluginException.NoSuchPluginException();
                }
            }
        }

        isConfigured = true;
    }

    @Override
    public void addPlugin(@NonNull ApiPlugin plugin) throws PluginException {
        try {
            if (plugins.put(plugin.getPluginKey(), plugin) == null) {
                throw new PluginException.NoSuchPluginException();
            }
        } catch (Exception ex) {
            throw new PluginException.NoSuchPluginException();
        }
    }

    @Override
    public void removePlugin(@NonNull ApiPlugin plugin) throws PluginException {
        if (plugins.remove(plugin.getPluginKey()) == null) {
            throw new PluginException.NoSuchPluginException();
        }
    }

    @Override
    public ApiPlugin getPlugin(@NonNull String pluginKey) throws PluginException {
        if (plugins.containsKey(pluginKey)) {
            return plugins.get(pluginKey);
        } else {
            throw new PluginException.NoSuchPluginException();
        }
    }

    /**
     * @return the set of plugins added to a Category.
     */
    @Override
    public Set<ApiPlugin> getPlugins() {
        return new HashSet<ApiPlugin>(plugins.values());
    }

    @Override
    public final CategoryType getCategoryType() {
        return CategoryType.API;
    }
}
