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
import androidx.annotation.WorkerThread;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.plugin.Plugin;

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
     * and throw an error if configuration method is called again.
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
     * @throws AmplifyException if already configured
     */
    @WorkerThread
    public final void configure(
            @NonNull CategoryConfiguration configuration, @NonNull Context context)
            throws AmplifyException {
        if (isConfigured) {
            throw new AmplifyException("Amplify was already configured",
                    "Be sure to only call Amplify.configure once");
        }

        for (P plugin : getPlugins()) {
            String pluginKey = plugin.getPluginKey();
            JSONObject pluginConfig = configuration.getPluginConfig(pluginKey);

            plugin.configure(pluginConfig, context);
        }

        isConfigured = true;
    }

    /**
     * Releases resources used by the category.
     * @param context An Android Context
     * @throws AmplifyException On failure to release resources
     */
    @WorkerThread
    public final void release(@NonNull Context context) throws AmplifyException {
        if (!isConfigured) {
            throw new AmplifyException(
                "Amplify is not configured, nothing to release.",
                "Did you configure it yet?"
            );
        }

        for (P plugin : getPlugins()) {
            plugin.release(context);
        }

        isConfigured = false;
    }

    /**
     * Register a plugin into the Category.
     * @param plugin A plugin to add
     * @throws AmplifyException On failure to add the plugin
     */
    public final void addPlugin(@NonNull P plugin) throws AmplifyException {
        try {
            plugins.put(plugin.getPluginKey(), plugin);
        } catch (Exception exception) {
            throw new AmplifyException(
                "Plugin key was missing for + " + plugin.getClass().getSimpleName(),
                "This should never happen - contact the plugin developers to find out why this is."
            );
        }
    }

    /**
     * Remove a plugin from the category.
     * @param plugin A plugin to remove
     */
    public final void removePlugin(@NonNull P plugin) {
        if (plugins.remove(plugin.getPluginKey()) == null) {
            // TODO: Fail silently for now, matching iOS - potentially publish on Hub in the future.
        }
    }

    /**
     * Retrieve a plugin by its key.
     * @param pluginKey A key that identifies a plugin implementation
     * @return The plugin object associated to pluginKey, if registered
     */
    public final P getPlugin(@NonNull final String pluginKey) {
        if (plugins.containsKey(pluginKey)) {
            return plugins.get(pluginKey);
        } else {
            throw new IllegalStateException(
                    "Tried to get a plugin but that plugin was not present." +
                    "Check if the plugin was added originally or perhaps was already removed."
            );
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
     */
    protected final P getSelectedPlugin() {
        if (!isConfigured) {
            throw new IllegalStateException("This category is not yet configured." +
                    "Make sure you added it with Amplify.addPlugin and then called Amplify.config");
        }
        if (plugins.isEmpty()) {
            throw new IllegalStateException(
                    "Tried to get a plugin but that plugin was not present." +
                    "Check if the plugin was added originally or perhaps was already removed."
            );
        }
        if (plugins.size() > 1) {
            throw new IllegalStateException(
                    "Tried to get a default plugin but there are more than one to choose from in this category." +
                    "Call getPlugin(pluginKey) to choose the specific plugin you want to use in this case."
            );
        }

        return getPlugins().iterator().next();
    }
}
