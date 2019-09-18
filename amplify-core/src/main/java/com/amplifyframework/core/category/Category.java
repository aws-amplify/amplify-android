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

import com.amplifyframework.core.exception.AmplifyAlreadyConfiguredException;
import com.amplifyframework.core.exception.MismatchedPluginException;
import com.amplifyframework.core.exception.NoSuchPluginException;
import com.amplifyframework.core.plugin.Plugin;

import java.util.Map;

public interface Category extends CategoryTypable {

    /**
     * Read the configuration from amplifyconfiguration.json file
     *
     * @param context Android context required to read the contents of file
     * @throws AmplifyAlreadyConfiguredException thrown when already configured
     * @throws NoSuchPluginException thrown when there is no plugin found for a configuration
     */
    void configure(@NonNull Context context) throws AmplifyAlreadyConfiguredException, NoSuchPluginException;

    /**
     * Read the configuration from amplifyconfiguration.json file
     *
     * @param context Android context required to read the contents of file
     * @param environment specifies the name of the environment being operated on.
     *                    For example, "Default", "Custom", etc.
     * @throws AmplifyAlreadyConfiguredException thrown when already configured
     * @throws NoSuchPluginException thrown when there is no plugin found for a configuration
     */
    void configure(@NonNull Context context, @NonNull String environment) throws AmplifyAlreadyConfiguredException, NoSuchPluginException;

    /**
     * Register a plugin with Amplify
     *
     * @param plugin an implementation of a CATEGORY_TYPE that
     *               conforms to the {@link Plugin} interface.
     * @param <P> any plugin that conforms to the {@link Plugin} interface
     * @throws MismatchedPluginException when a plugin cannot be registered for this CATEGORY_TYPE
     */
    <P extends Plugin> void addPlugin(@NonNull final P plugin) throws MismatchedPluginException;

    /**
     * Remove a registered plugin
     *
     * @param pluginKey an implementation of a CATEGORY_TYPE that
     *               conforms to the {@link Plugin} interface.
     * @param <P> any plugin that conforms to the {@link Plugin} interface
     */
    <P extends Plugin> void removePlugin(@NonNull final String pluginKey);

    /**
     * Reset Amplify to state where it is not configured.
     *
     * Remove all the plugins added.
     * Remove the configuration stored.
     */
    void reset();

    /**
     * Retrieve a plugin of CATEGORY_TYPE.
     *
     * @param pluginKey the key that identifies the plugin implementation
     * @param <P> any plugin that conforms to the {@link Plugin} interface
     * @return the plugin object
     */
    <P extends Plugin> Plugin getPlugin(@NonNull final String pluginKey);

    /**
     * Retrieve the map of plugins: {PluginName => PluginObject}}
     *     A category can have more than one plugins registered through
     *     the Amplify System. Each plugin is identified with a name.
     *
     * @return the map that represents the plugins.
     */
    Map<String, Plugin> getPlugins();
}
