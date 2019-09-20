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

package com.amplifyframework.core;

import android.content.Context;
import android.support.annotation.NonNull;

import com.amplifyframework.analytics.Analytics;
import com.amplifyframework.analytics.AnalyticsPlugin;
import com.amplifyframework.analytics.AnalyticsPluginConfiguration;
import com.amplifyframework.api.APICategory;
import com.amplifyframework.auth.AuthCategory;
import com.amplifyframework.core.exception.ConfigurationException;
import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.core.plugin.PluginConfiguration;
import com.amplifyframework.core.plugin.PluginException;
import com.amplifyframework.logging.LoggingCategory;
import com.amplifyframework.storage.Storage;
import com.amplifyframework.storage.StoragePlugin;
import com.amplifyframework.storage.StoragePluginConfiguration;

/**
 * The Amplify System has the following responsibilities:
 *
 * 1) Add, Get and Remove CATEGORY_TYPE plugins with the Amplify System
 * 2) Configure and reset the Amplify System with the information
 * from the amplifyconfiguration.json.
 *
 * Configure using amplifyconfiguration.json
 * <pre>
 *     {@code
 *      Amplify.configure(getApplicationContext());
 *     }
 * </pre>
 */
public class Amplify {

    private static final String TAG = Amplify.class.getSimpleName();

    public static final Analytics Analytics;
    public static final APICategory API;
    public static final AuthCategory Auth;
    public static final LoggingCategory Logging;
    public static final Storage Storage;

    private static boolean CONFIGURED = false;

    private static AmplifyConfiguration amplifyConfiguration;

    static {
        Analytics = null;
        API = null;
        Auth = null;
        Logging = null;
        Storage = null;
    }

    private static final Object LOCK = new Object();

    /**
     * Read the configuration from amplifyconfiguration.json file
     *
     * @param context Android context required to read the contents of file
     * @throws ConfigurationException thrown when already configured
     * @throws PluginException thrown when there is no plugin found for a configuration
     */
    public static void configure(@NonNull Context context) throws ConfigurationException, PluginException {
        synchronized (LOCK) {
            configure(context, AmplifyConfiguration.DEFAULT_ENVIRONMENT_NAME);
        }
    }

    /**
     * Read the configuration from amplifyconfiguration.json file
     *
     * @param context Android context required to read the contents of file
     * @param environment specifies the name of the environment being operated on.
     *                    For example, "Default", "Custom", etc.
     * @throws ConfigurationException thrown when already configured
     * @throws PluginException thrown when there is no plugin found for a configuration
     */
    public static void configure(@NonNull Context context, @NonNull String environment) throws ConfigurationException, PluginException {
        synchronized (LOCK) {
            if (CONFIGURED) {
                throw new ConfigurationException.AmplifyAlreadyConfiguredException();
            }
            amplifyConfiguration = new AmplifyConfiguration(context);
            amplifyConfiguration.setEnvironment(environment);
            CONFIGURED = true;
        }
    }

    /**
     * Register a plugin with Amplify
     *
     * @param plugin an implementation of a CATEGORY_TYPE that
     *               conforms to the {@link Plugin} interface.
     * @param <P> any plugin that conforms to the {@link Plugin} interface
     * @throws PluginException when a plugin cannot be registered for the category type it belongs to
     *                         or when when the plugin's category type is not supported by Amplify.
     */
    public static <P extends Plugin> void addPlugin(@NonNull final P plugin) throws PluginException {
        synchronized (LOCK) {
            if (plugin.getPluginKey() == null || plugin.getPluginKey().isEmpty()) {
                throw new PluginException.EmptyKeyException();
            }

            switch (plugin.getCategoryType()) {
                case API:
                    break;
                case ANALYTICS:
                    if (plugin instanceof AnalyticsPlugin) {
                        Analytics.addPlugin((AnalyticsPlugin) plugin);
                    } else {
                        throw new PluginException.MismatchedPluginException();
                    }
                    break;
                case HUB:
                    break;
                case LOGGING:
                    break;
                case STORAGE:
                    if (plugin instanceof StoragePlugin) {
                        Storage.addPlugin((StoragePlugin) plugin);
                    } else {
                        throw new PluginException.MismatchedPluginException();
                    }
                    break;
                default:
                    throw new PluginException.NoSuchPluginException("Plugin category does not exist. " +
                            "Verify that the library version is correct and supports the plugin's category.");
            }
        }
    }

    /**
     * Register a plugin with Amplify
     *
     * @param plugin an implementation of a CATEGORY_TYPE that
     *               conforms to the {@link Plugin} interface.
     * @param <P> any plugin that conforms to the {@link Plugin} interface
     * @throws PluginException when a plugin cannot be registered for the category type it belongs to
     *                         or when when the plugin's category type is not supported by Amplify.
     */
    public static <P extends Plugin, C extends PluginConfiguration> void addPlugin(@NonNull final P plugin, @NonNull final C pluginConfiguration) throws PluginException {
        synchronized (LOCK) {
            if (plugin.getPluginKey() == null || plugin.getPluginKey().isEmpty()) {
                throw new PluginException.EmptyKeyException();
            }

            switch (plugin.getCategoryType()) {
                case API:
                    break;
                case ANALYTICS:
                    if (plugin instanceof AnalyticsPlugin && pluginConfiguration instanceof AnalyticsPluginConfiguration) {
                        Analytics.addPlugin((AnalyticsPlugin) plugin, (AnalyticsPluginConfiguration) pluginConfiguration);
                    } else {
                        throw new PluginException.MismatchedPluginException();
                    }
                    break;
                case HUB:
                    break;
                case LOGGING:
                    break;
                case STORAGE:
                    if (plugin instanceof StoragePlugin && pluginConfiguration instanceof StoragePluginConfiguration) {
                        Storage.addPlugin((StoragePlugin) plugin, (StoragePluginConfiguration) pluginConfiguration);
                    } else {
                        throw new PluginException.MismatchedPluginException();
                    }
                    break;
                default:
                    throw new PluginException.NoSuchPluginException("Plugin category does not exist. " +
                            "Verify that the library version is correct and supports the plugin's category.");
            }
        }
    }

    public static <P extends Plugin> void removePlugin(@NonNull final P plugin) throws PluginException {
        synchronized (LOCK) {
            switch (plugin.getCategoryType()) {
                case API:
                    break;
                case ANALYTICS:
                    if (plugin instanceof AnalyticsPlugin) {
                        Analytics.removePlugin((AnalyticsPlugin) plugin);
                    } else {
                        throw new PluginException.MismatchedPluginException();
                    }
                    break;
                case HUB:
                    break;
                case LOGGING:
                    break;
                case STORAGE:
                    if (plugin instanceof StoragePlugin) {
                        Storage.removePlugin((StoragePlugin) plugin);
                    } else {
                        throw new PluginException.MismatchedPluginException();
                    }
                    break;
                default:
                    throw new PluginException.NoSuchPluginException("Plugin category does not exist. " +
                            "Verify that the library version is correct and supports the plugin's category.");
            }
        }
    }

    /**
     * Reset Amplify to state where it is not configured.
     *
     * Remove all the plugins added.
     * Remove the configuration stored.
     */
    public static void reset() {
        synchronized (LOCK) {
            Amplify.amplifyConfiguration = null;
            CONFIGURED = false;
        }
    }
}
