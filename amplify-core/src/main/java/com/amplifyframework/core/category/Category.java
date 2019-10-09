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

package com.amplifyframework.core.category;

import androidx.annotation.NonNull;

import com.amplifyframework.core.exception.ConfigurationException;
import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.core.plugin.PluginException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Category<P extends Plugin<?>> implements CategoryTypeable {
    /**
     * Map of the { pluginKey => plugin } object
     */
    private Map<String, P> plugins;

    /**
     * Flag to remember that the category is already configured by Amplify
     * and throw an error if configure method is called again
     */
    private boolean isConfigured;

    public Category() {
        this.plugins = new ConcurrentHashMap<String, P>();
        this.isConfigured = false;
    }

    /**
     * Configure category with provided AmplifyConfiguration object
     *
     * @throws ConfigurationException thrown when already configured
     * @throws PluginException thrown when there is no plugin found for a configuration
     */
    public void configure(CategoryConfiguration configuration) throws ConfigurationException, PluginException {
        if (isConfigured) {
            throw new ConfigurationException.AmplifyAlreadyConfiguredException();
        }

        for (P plugin : getPlugins()) {
            String pluginKey = plugin.getPluginKey();
            Object pluginConfig = configuration.pluginConfigs.get(pluginKey);

            if (pluginConfig != null) {
                plugin.configure(pluginConfig);
            } else {
                throw new PluginException.NoSuchPluginException();
            }
        }

        isConfigured = true;
    }

    /**
     * Register a plugin with Amplify
     *
     * @param plugin an implementation of a category plugin that
     *               conforms to the {@link Plugin} interface.
     * @throws PluginException when a plugin cannot be registered for this category
     */
    public void addPlugin(@NonNull P plugin) throws PluginException {
        try {
            plugins.put(plugin.getPluginKey(), plugin);
        } catch (Exception ex) {
            throw new PluginException.EmptyKeyException();
        }
    }

    /**
     * Remove a registered plugin
     *
     * @param plugin an implementation of a Category that
     *               conforms to the {@link Plugin} interface
     * @throws PluginException when a plugin cannot be registered for this category
     */
    public void removePlugin(@NonNull P plugin) throws PluginException {
        if (plugins.remove(plugin.getPluginKey()) == null) {
            throw new PluginException.NoSuchPluginException();
        }
    }

    /**
     * Retrieve a plugin of category.
     *
     * @param pluginKey the key that identifies the plugin implementation
     * @return the plugin object
     */
    public P getPlugin(@NonNull final String pluginKey) throws PluginException {
        if (plugins.containsKey(pluginKey)) {
            return plugins.get(pluginKey);
        } else {
            throw new PluginException.NoSuchPluginException();
        }
    }

    /**
     * @return the set of plugins added to a Category.
     */
    public Set<P> getPlugins() {
        return new HashSet<P>(plugins.values());
    }

    /**
     * Obtain the registered plugin for this category. Throw
     * runtime exception if no plugin is registered or
     * multiple plugins are registered.
     *
     * @return the only registered plugin for this category
     */
    protected P getSelectedPlugin() {
        if (!isConfigured) {
            throw new ConfigurationException("This category is not yet configured.");
        }
        if (plugins.isEmpty()) {
            throw new PluginException.NoSuchPluginException();
        }
        if (plugins.size() > 1) {
            throw new PluginException.MultiplePluginsException();
        }

        return getPlugins().iterator().next();
    }
}
