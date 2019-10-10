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
import androidx.annotation.NonNull;

import com.amplifyframework.ConfigurationException;
import com.amplifyframework.analytics.AnalyticsCategory;
import com.amplifyframework.analytics.AnalyticsPlugin;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.ApiPlugin;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.core.plugin.PluginException;
import com.amplifyframework.hub.HubCategory;
import com.amplifyframework.hub.HubPlugin;
import com.amplifyframework.logging.LoggingCategory;
import com.amplifyframework.logging.LoggingPlugin;
import com.amplifyframework.storage.StorageCategory;
import com.amplifyframework.storage.StoragePlugin;

/**
 * This is the top-level customer-facing interface to the Amplify
 * framework.
 *
 * The Amplify System has the following responsibilities:
 *
 * 1) Add, Get and Remove Category plugins with the Amplify System
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
public final class Amplify {

    @SuppressWarnings("all") public static final AnalyticsCategory Analytics;
    @SuppressWarnings("all") public static final ApiCategory API;
    @SuppressWarnings("all") public static final LoggingCategory Logging;
    @SuppressWarnings("all") public static final StorageCategory Storage;
    @SuppressWarnings("all") public static final HubCategory Hub;

    private static final String TAG = Amplify.class.getSimpleName();

    private static AmplifyConfiguration amplifyConfiguration;
    private static boolean configured = false;

    static {
        Analytics = new AnalyticsCategory();
        API = new ApiCategory();
        Logging = new LoggingCategory();
        Storage = new StorageCategory();
        Hub = new HubCategory();
    }

    private static final Object LOCK = new Object();

    /**
     * Dis-allows instantiation of this utility class.
     */
    private Amplify() {
        throw new UnsupportedOperationException("No instances allowed.");
    }

    /**
     * Read the configuration from amplifyconfiguration.json file.
     * @param context Android context required to read the contents of file
     * @throws ConfigurationException thrown when already configured
     * @throws PluginException thrown when there is no plugin found for a configuration
     */
    public static void configure(@NonNull Context context) throws ConfigurationException, PluginException {
        configure(new AmplifyConfiguration(context));
    }

    /**
     * Configure Amplify with AmplifyConfiguration object.
     * @param configuration AmplifyConfiguration object for configuration via code
     * @throws ConfigurationException thrown when already configured
     * @throws PluginException thrown when there is no plugin found for a configuration
     */
    public static void configure(final AmplifyConfiguration configuration)
            throws ConfigurationException, PluginException {

        synchronized (LOCK) {
            if (configured) {
                throw new ConfigurationException.AmplifyAlreadyConfiguredException();
            }
            amplifyConfiguration = configuration;

            if (Analytics.getPlugins().size() > 0) {
                Analytics.configure(amplifyConfiguration.forCategoryType(CategoryType.ANALYTICS));
            }

            if (API.getPlugins().size() > 0) {
                API.configure(amplifyConfiguration.forCategoryType(CategoryType.API));
            }

            if (Hub.getPlugins().size() > 0) {
                Hub.configure(amplifyConfiguration.forCategoryType(CategoryType.HUB));
            }

            if (Logging.getPlugins().size() > 0) {
                Logging.configure(amplifyConfiguration.forCategoryType(CategoryType.LOGGING));
            }

            if (Storage.getPlugins().size() > 0) {
                Storage.configure(amplifyConfiguration.forCategoryType(CategoryType.STORAGE));
            }

            configured = true;
        }
    }

    /**
     * Register a plugin with Amplify.
     * @param plugin an implementation of a CATEGORY_TYPE that
     *               conforms to the {@link Plugin} interface.
     * @param <P> any plugin that conforms to the {@link Plugin} interface
     * @throws PluginException when a plugin cannot be registered for the category type it belongs to
     *                         or when when the plugin's category type is not supported by Amplify.
     */
    public static <P extends Plugin<?>> void addPlugin(@NonNull final P plugin) throws PluginException {
        synchronized (LOCK) {
            if (plugin.getPluginKey() == null || plugin.getPluginKey().isEmpty()) {
                throw new PluginException.EmptyKeyException();
            }

            switch (plugin.getCategoryType()) {
                case API:
                    if (plugin instanceof ApiPlugin) {
                        API.addPlugin((ApiPlugin) plugin);
                    } else {
                        throw new PluginException.MismatchedPluginException();
                    }
                    break;
                case ANALYTICS:
                    if (plugin instanceof AnalyticsPlugin) {
                        Analytics.addPlugin((AnalyticsPlugin) plugin);
                    } else {
                        throw new PluginException.MismatchedPluginException();
                    }
                    break;
                case HUB:
                    if (plugin instanceof HubPlugin) {
                        Hub.addPlugin((HubPlugin) plugin);
                    } else {
                        throw new PluginException.MismatchedPluginException();
                    }
                    break;
                case LOGGING:
                    if (plugin instanceof LoggingPlugin) {
                        Logging.addPlugin((LoggingPlugin) plugin);
                    } else {
                        throw new PluginException.MismatchedPluginException();
                    }
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
     * Removes a plugin form the Amplify framework.
     * @param plugin The plugin to remove from the Amplify framework
     * @param <P> The type of the plugin being removed
     * @throws PluginException On failure to remove a plugin
     */
    public static <P extends Plugin<?>> void removePlugin(@NonNull final P plugin) throws PluginException {
        synchronized (LOCK) {
            switch (plugin.getCategoryType()) {
                case API:
                    if (plugin instanceof ApiPlugin) {
                        API.removePlugin((ApiPlugin) plugin);
                    } else {
                        throw new PluginException.MismatchedPluginException();
                    }
                    break;
                case ANALYTICS:
                    if (plugin instanceof AnalyticsPlugin) {
                        Analytics.removePlugin((AnalyticsPlugin) plugin);
                    } else {
                        throw new PluginException.MismatchedPluginException();
                    }
                    break;
                case HUB:
                    if (plugin instanceof HubPlugin) {
                        Hub.removePlugin((HubPlugin) plugin);
                    } else {
                        throw new PluginException.MismatchedPluginException();
                    }
                    break;
                case LOGGING:
                    if (plugin instanceof LoggingPlugin) {
                        Logging.removePlugin((LoggingPlugin) plugin);
                    } else {
                        throw new PluginException.MismatchedPluginException();
                    }
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
     * Gets the Amplify configuration. The amplify configuration
     * includes all details about the various categories/plugins that
     * are available for use by the framework.
     * @return The current Amplify configuration, possibly null if
     *         Amplify has not yet been configured
     */
    static AmplifyConfiguration getAmplifyConfiguration() {
        return amplifyConfiguration;
    }
}

