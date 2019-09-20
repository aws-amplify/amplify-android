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

import android.content.Context;
import android.support.annotation.NonNull;

import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.exception.ConfigurationException;
import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.core.plugin.PluginException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines the Client API consumed by the application.
 * Internally routes the calls to the Analytics CategoryType
 * plugins registered.
 */
public class Analytics implements Category<AnalyticsPlugin, AnalyticsPluginConfiguration>, AnalyticsCategoryBehavior {

    static class PluginDetails {
        AnalyticsPlugin analyticsPlugin;
        AnalyticsPluginConfiguration analyticsPluginConfiguration;

        public AnalyticsPlugin getAnalyticsPlugin() {
            return analyticsPlugin;
        }

        public PluginDetails analyticsPlugin(AnalyticsPlugin analyticsPlugin) {
            this.analyticsPlugin = analyticsPlugin;
            return this;
        }

        public AnalyticsPluginConfiguration getAnalyticsPluginConfiguration() {
            return analyticsPluginConfiguration;
        }

        public PluginDetails analyticsPluginConfiguration(AnalyticsPluginConfiguration analyticsPluginConfiguration) {
            this.analyticsPluginConfiguration = analyticsPluginConfiguration;
            return this;
        }
    }

    /**
     * Map of the { pluginKey => plugin } object
     */
    private Map<String, PluginDetails> plugins;

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

    public Analytics() {
        this.plugins = new ConcurrentHashMap<String, PluginDetails>();
        this.enabled = true;
    }

    @Override
    public void disable() {
        assert isConfigured;
        synchronized (LOCK) {
            enabled = false;
        }
    }

    @Override
    public void enable() {
        assert isConfigured;
        synchronized (LOCK) {
            enabled = true;
        }
    }

    @Override
    public void recordEvent(@NonNull String eventName) throws AnalyticsException {
        assert isConfigured;
        if (enabled) {

        }
    }

    @Override
    public void recordEvent(@NonNull final AnalyticsEvent analyticsEvent) throws AnalyticsException {
        assert isConfigured;
        if (enabled) {

        }
    }

    @Override
    public void updateProfile(@NonNull AnalyticsProfile analyticsProfile) throws AnalyticsException {
        assert isConfigured;
        if (enabled) {

        }
    }

    /**
     * Read the configuration from amplifyconfiguration.json file
     *
     * @param context Android context required to read the contents of file
     * @throws ConfigurationException thrown when already configured
     * @throws PluginException        thrown when there is no plugin found for a configuration
     */
    @Override
    public void configure(@NonNull Context context) throws ConfigurationException, PluginException {
        configure(context, "Default");
    }

    /**
     * Read the configuration from amplifyconfiguration.json file
     *
     * @param context     Android context required to read the contents of file
     * @param environment specifies the name of the environment being operated on.
     *                    For example, "Default", "Custom", etc.
     * @throws ConfigurationException thrown when already configured
     * @throws PluginException        thrown when there is no plugin found for a configuration
     */
    @Override
    public void configure(@NonNull Context context, @NonNull String environment) throws ConfigurationException, PluginException {
        if (isConfigured) {
            throw new ConfigurationException.AmplifyAlreadyConfiguredException();
        }

        if (!plugins.values().isEmpty()) {
            if (plugins.values().iterator().hasNext()) {
                PluginDetails pluginDetails = plugins.values().iterator().next();
                if (pluginDetails.analyticsPluginConfiguration == null) {
                    pluginDetails.analyticsPlugin.configure(context);
                } else {
                    pluginDetails.analyticsPlugin.configure(pluginDetails.analyticsPluginConfiguration);
                }
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
        PluginDetails pluginDetails = new PluginDetails()
                .analyticsPlugin(plugin);

        try {
            if (plugins.put(plugin.getPluginKey(), pluginDetails) == null) {
                throw new PluginException.NoSuchPluginException();
            }
        } catch (Exception ex) {
            throw new PluginException.NoSuchPluginException();
        }
    }

    /**
     * Register an Analytics plugin with Amplify
     *
     * @param plugin              an implementation of AnalyticsPlugin
     * @param pluginConfiguration configuration information for the plugin.
     * @throws PluginException when a plugin cannot be registered for this category
     */
    @Override
    public void addPlugin(@NonNull AnalyticsPlugin plugin, @NonNull AnalyticsPluginConfiguration pluginConfiguration) throws PluginException {
        PluginDetails pluginDetails = new PluginDetails()
                .analyticsPlugin(plugin)
                .analyticsPluginConfiguration(pluginConfiguration);

        try {
            if (plugins.put(plugin.getPluginKey(), pluginDetails) == null) {
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
        if (plugins.containsKey(plugin.getPluginKey())) {
            plugins.remove(plugin.getPluginKey());
        } else {
            throw new PluginException.NoSuchPluginException();
        }
    }

    /**
     * Reset Analytics category to state where it is not configured.
     * <p>
     * Remove all the plugins added.
     * Remove the configuration stored.
     */
    @Override
    public void reset() {

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
            return plugins.get(pluginKey).analyticsPlugin;
        } else {
            throw new PluginException.NoSuchPluginException();
        }
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
