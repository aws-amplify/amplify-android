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
import com.amplifyframework.core.category.CategoryTypeable;
import com.amplifyframework.core.exception.AmplifyAlreadyConfiguredException;
import com.amplifyframework.core.exception.MismatchedPluginException;
import com.amplifyframework.core.exception.NoSuchPluginException;
import com.amplifyframework.core.plugin.Plugin;

import java.util.Map;

/**
 * Defines the Client API consumed by the application.
 * Internally routes the calls to the Analytics CategoryType
 * plugins registered.
 */
public class Analytics implements AnalyticsCategoryClientBehavior, Category, CategoryTypeable {

    /**
     * By default collection and sending of Analytics events
     * are enabled.
     */
    private static boolean enabled = true;

    /**
     * Protect enabling and disabling of Analytics event
     * collection and sending.
     */
    private static final Object LOCK = new Object();

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

        }
    }

    @Override
    public void recordEvent(@NonNull final AnalyticsEvent analyticsEvent) throws AnalyticsException {
        if (enabled) {

        }
    }

    @Override
    public void updateProfile(@NonNull AnalyticsProfile analyticsProfile) throws AnalyticsException {
        if (enabled) {

        }
    }

    /**
     * Read the configuration from amplifyconfiguration.json file
     *
     * @param context Android context required to read the contents of file
     * @throws AmplifyAlreadyConfiguredException thrown when already configured
     * @throws NoSuchPluginException             thrown when there is no plugin found for a configuration
     */
    @Override
    public void configure(@NonNull Context context) throws AmplifyAlreadyConfiguredException, NoSuchPluginException {

    }

    /**
     * Read the configuration from amplifyconfiguration.json file
     *
     * @param context     Android context required to read the contents of file
     * @param environment specifies the name of the environment being operated on.
     *                    For example, "Default", "Custom", etc.
     * @throws AmplifyAlreadyConfiguredException thrown when already configured
     * @throws NoSuchPluginException             thrown when there is no plugin found for a configuration
     */
    @Override
    public void configure(@NonNull Context context, @NonNull String environment) throws AmplifyAlreadyConfiguredException, NoSuchPluginException {

    }

    /**
     * Register a plugin with Amplify
     *
     * @param plugin an implementation of a CATEGORY_TYPE that
     *               conforms to the {@link Plugin} interface.
     * @throws MismatchedPluginException when a plugin cannot be registered for this CATEGORY_TYPE
     */
    @Override
    public <P extends Plugin> void addPlugin(@NonNull P plugin) throws MismatchedPluginException {

    }

    /**
     * Remove a registered plugin
     *
     * @param pluginKey an implementation of a CATEGORY_TYPE that
     *                  conforms to the {@link Plugin} interface.
     */
    @Override
    public <P extends Plugin> void removePlugin(@NonNull String pluginKey) {

    }

    /**
     * Reset Amplify to state where it is not configured.
     * <p>
     * Remove all the plugins added.
     * Remove the configuration stored.
     */
    @Override
    public void reset() {

    }

    /**
     * Retrieve a plugin of CATEGORY_TYPE.
     *
     * @param pluginKey the key that identifies the plugin implementation
     * @return the plugin object
     */
    @Override
    public <P extends Plugin> Plugin getPlugin(@NonNull String pluginKey) {
        return null;
    }

    /**
     * Retrieve the map of plugins: {PluginName => PluginObject}}
     * A category can have more than one plugins registered through
     * the Amplify System. Each plugin is identified with a name.
     *
     * @return the map that represents the plugins.
     */
    @Override
    public Map<String, Plugin> getPlugins() {
        return null;
    }

    @Override
    public CategoryType getCategoryType() {
        return CategoryType.ANALYTICS;
    }
}
