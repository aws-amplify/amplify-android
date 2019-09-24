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
import android.support.annotation.NonNull;

import com.amplifyframework.core.exception.ConfigurationException;
import com.amplifyframework.core.plugin.PluginException;
import com.amplifyframework.core.plugin.Plugin;

import java.util.Set;

public interface Category<P, C> extends CategoryTypeable {

    /**
     * Read the configuration from amplifyconfiguration.json file
     *
     * @param context Android context required to read the contents of file
     * @param environment specifies the name of the environment being operated on.
     *                    For example, "Default", "Custom", etc.
     * @throws ConfigurationException thrown when already configured
     * @throws PluginException thrown when there is no plugin found for a configuration
     */
    void configure(@NonNull Context context, @NonNull String environment) throws ConfigurationException, PluginException;

    /**
     * Register a plugin with Amplify
     *
     * @param plugin an implementation of a CATEGORY_TYPE that
     *               conforms to the {@link Plugin} interface.
     * @throws PluginException when a plugin cannot be registered for this category
     */
    void addPlugin(@NonNull final P plugin) throws PluginException;

    /**
     * Register a plugin with Amplify
     *
     * @param plugin an implementation of a Category that
     *               conforms to the {@link Plugin} interface.
     * @param pluginConfiguration configuration information for the plugin.
     * @throws PluginException when a plugin cannot be registered for this category
     */
    void addPlugin(@NonNull final P plugin, @NonNull final C pluginConfiguration) throws PluginException;

    /**
     * Remove a registered plugin
     *
     * @param plugin an implementation of a Category that
     *               conforms to the {@link Plugin} interface
     * @throws PluginException when a plugin cannot be registered for this category
     */
    void removePlugin(@NonNull final P plugin) throws PluginException;

    /**
     * Reset Amplify to state where it is not configured.
     *
     * Remove all the plugins added.
     * Remove the configuration stored.
     */
    void reset();

    /**
     * Retrieve a plugin of category.
     *
     * @param pluginKey the key that identifies the plugin implementation
     * @return the plugin object
     */
    P getPlugin(@NonNull final String pluginKey) throws PluginException;

    /**
     * @return the set of plugins added to a Category.
     */
    Set<P> getPlugins();
}
