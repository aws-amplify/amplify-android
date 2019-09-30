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

package com.amplifyframework.analytics;

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

/**
 * Defines the Client API consumed by the application.
 * Internally routes the calls to the Analytics CategoryType
 * plugins registered.
 */
public class AnalyticsCategory implements Category<AnalyticsPlugin>, AnalyticsCategoryBehavior {
    /**
     * Map of the { pluginKey => plugin } object
     */
    private Map<String, AnalyticsPlugin> plugins;

    /**
     * Flag to remember that Analytics category is already configured by Amplify
     * and throw an error if configure method is called again
     */
    private boolean isConfigured;

    /**
     * By default collection and sending of Analytics events
     * are enabled.
     */
    private boolean enabled;

    /**
     * Protect enabling and disabling of Analytics event
     * collection and sending.
     */
    private static final Object LOCK = new Object();

    public AnalyticsCategory() {
        this.plugins = new ConcurrentHashMap<String, AnalyticsPlugin>();
        this.enabled = true;
    }

    /**
     * Obtain the registered plugin. Throw runtime exception if
     * no plugin is registered or multiple plugins are registered.
     *
     * @return the only registered plugin for this category
     */
    private AnalyticsPlugin getSelectedPlugin() {
        if (!isConfigured) {
            throw new ConfigurationException("Analytics category is not yet configured.");
        }
        if (plugins.isEmpty()) {
            throw new PluginException.NoSuchPluginException();
        }
        if (plugins.size() > 1) {
            throw new PluginException.MultiplePluginsException();
        }

        return plugins.values().iterator().next();
    }

    @Override
    public void disable() {
        synchronized (LOCK) {
            enabled = false;
        }
    }

    @Override
    public void enable() {
        synchronized (LOCK) {
            enabled = true;
        }
    }

    @Override
    public void recordEvent(@NonNull String eventName) throws AnalyticsException {
        if (enabled) {
            getSelectedPlugin().recordEvent(eventName);
        }
    }

    @Override
    public void identifyUser(@NonNull String id, @NonNull AnalyticsUserProfile analyticsUserProfile) {

    }

    @Override
    public void recordEvent(@NonNull final AnalyticsEvent analyticsEvent) throws AnalyticsException {
        if (enabled) {
            getSelectedPlugin().recordEvent(analyticsEvent);
        }
    }

    public void updateProfile(@NonNull AnalyticsUserProfile analyticsUserProfile) throws AnalyticsException {
        if (enabled) {
        }
    }

    /**
     * Configure Analytics category based on AmplifyConfiguration object
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

        for (AnalyticsPlugin plugin : plugins.values()) {
            String pluginKey = plugin.getPluginKey();
            Object pluginConfig = configuration.analytics.pluginConfigs.get(pluginKey);

            if (pluginConfig != null) {
                plugin.configure(pluginConfig);
            } else {
                throw new PluginException.NoSuchPluginException();
            }
        }

        isConfigured = true;
    }

    /**
     * Register an Analytics plugin with Amplify
     *
     * @param plugin an implementation of AnalyticsPlugin
     * @throws PluginException when a plugin cannot be registered for Analytics category
     */
    @Override
    public void addPlugin(@NonNull AnalyticsPlugin plugin) throws PluginException {
        try {
            if (plugins.put(plugin.getPluginKey(), plugin) == null) {
                throw new PluginException.NoSuchPluginException();
            }
        } catch (Exception ex) {
            throw new PluginException.NoSuchPluginException();
        }
    }

    /**
     * Remove a registered Analytics plugin
     *
     * @param plugin an implementation of AnalyticsPlugin
     */
    @Override
    public void removePlugin(@NonNull AnalyticsPlugin plugin) throws PluginException {
        if (plugins.remove(plugin.getPluginKey()) == null) {
            throw new PluginException.NoSuchPluginException();
        }
    }

    /**
     * Retrieve a registered Analytics plugin
     *
     * @param pluginKey the key that identifies the plugin implementation
     * @return the plugin object
     */
    @Override
    public AnalyticsPlugin getPlugin(@NonNull String pluginKey) throws PluginException {
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
    public Set<AnalyticsPlugin> getPlugins() {
        return new HashSet<AnalyticsPlugin>(plugins.values());
    }

    /**
     * Retrieve the Analytics category type enum
     *
     * @return enum that represents Analytics category
     */
    @Override
    public final CategoryType getCategoryType() {
        return CategoryType.ANALYTICS;
    }
}
