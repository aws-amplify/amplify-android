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

import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.ConfigurationException;
import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.core.plugin.PluginException;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A category groups together zero or more plugins that share the same
 * category type.
 * @param <P> A base class type for plugins that this category will
 *            support, e.g. StoragePlugin, AnalyticsPlugin, etc.
 */
public abstract class Category<P extends Plugin<?>> implements CategoryTypeable {

    /**
     * Map of the { pluginKey => plugin } object.
     */
    private final Map<String, P> plugins;

    /**
     * Flag to remember that the category is already configured by Amplify
     * and throw an error if configure method is called again.
     */
    private boolean isConfigured;

    /**
     * Constructs a new, not-yet-configured, Category.
     */
    public Category() {
        this.plugins = new ConcurrentHashMap<>();
        this.isConfigured = false;
    }

    /**
     * Configure category with provided AmplifyConfiguration object.
     * @param configuration Configuration for all plugins in the category
     * @param context An Android Context
     * @throws ConfigurationException thrown when already configured
     * @throws PluginException thrown when there is no configuration found for a plugin
     */
    public final void configure(CategoryConfiguration configuration, Context context)
            throws ConfigurationException, PluginException {
        if (isConfigured) {
            throw new ConfigurationException.AmplifyAlreadyConfiguredException();
        }

        for (P plugin : getPlugins()) {
            String pluginKey = plugin.getPluginKey();
            JSONObject pluginConfig = configuration.getPluginConfig(pluginKey);

            if (pluginConfig != null) {
                plugin.configure(pluginConfig, context);
            } else {
                throw new PluginException("No configuration data was provided for " + pluginKey +
                        ". Check the amplifyconfiguration.json file or, if you are configuring manually, " +
                        "the config object you provided for the " + plugin.getCategoryType() + " category");
            }
        }

        isConfigured = true;
    }

    /**
     * Register a plugin into the Category.
     * @param plugin A plugin to add
     * @throws PluginException On failure to add the plugin
     */
    public final void addPlugin(@NonNull P plugin) throws PluginException {
        try {
            plugins.put(plugin.getPluginKey(), plugin);
        } catch (Exception exception) {
            throw new PluginException.EmptyKeyException();
        }
    }

    /**
     * Remove a plugin from the category.
     * @param plugin A plugin to remove 
     * @throws PluginException
     *         If the provided plugin was not associated to the
     *         category, perhaps because it never was, or because it was
     *         already removed
     */
    public final void removePlugin(@NonNull P plugin) throws PluginException {
        if (plugins.remove(plugin.getPluginKey()) == null) {
            throw new PluginException.NoSuchPluginException();
        }
    }

    /**
     * Retrieve a plugin by its key.
     * @param pluginKey A key that identifies a plugin implementation
     * @return The plugin object assocaited to pluginKey, if registered
     * @throws PluginException
     *         If there is no plugin associated to the requested key
     */
    public final P getPlugin(@NonNull final String pluginKey) throws PluginException {
        if (plugins.containsKey(pluginKey)) {
            return plugins.get(pluginKey);
        } else {
            throw new PluginException.NoSuchPluginException();
        }
    }

    /**
     * Gets the set of plugins associated with the Category.
     * @return The set of plugins associated to the Category
     */
    public final Set<P> getPlugins() {
        return new HashSet<>(plugins.values());
    }

    /**
     * Obtain the registered plugin for this category.
     * @return The only registered plugin for this category
     * @throws ConfigurationException
     *         If the category has not yet been configured, or if
     *         category configuration had been attempted previously but
     *         did not succeed
     */
    protected final P getSelectedPlugin() throws ConfigurationException {
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

