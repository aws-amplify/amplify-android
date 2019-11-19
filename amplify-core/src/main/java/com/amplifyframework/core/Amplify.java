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
import android.text.TextUtils;
import androidx.annotation.NonNull;

import com.amplifyframework.ConfigurationException;
import com.amplifyframework.analytics.AnalyticsCategory;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.core.plugin.PluginException;
import com.amplifyframework.hub.HubCategory;
import com.amplifyframework.logging.LoggingCategory;
import com.amplifyframework.storage.StorageCategory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    private static final String TAG = Amplify.class.getSimpleName();

    // These static references provide an entry point to the different categories.
    // For example, you can call storage operations through Amplify.Storage.list(String path).
    @SuppressWarnings("all") public static final AnalyticsCategory Analytics;
    @SuppressWarnings("all") public static final ApiCategory API;
    @SuppressWarnings("all") public static final LoggingCategory Logging;
    @SuppressWarnings("all") public static final StorageCategory Storage;
    @SuppressWarnings("all") public static final HubCategory Hub;

    private static final Object LOCK;
    private static final Map<CategoryType, Category<? extends Plugin<?>>> CATEGORIES;

    private static AmplifyConfiguration amplifyConfiguration;
    private static boolean configured;

    static {
        LOCK = new Object();
        configured = false;

        Analytics = new AnalyticsCategory();
        API = new ApiCategory();
        Logging = new LoggingCategory();
        Storage = new StorageCategory();
        Hub = new HubCategory();

        final Map<CategoryType, Category<? extends Plugin<?>>> modifiableCategories = new HashMap<>();
        modifiableCategories.put(CategoryType.ANALYTICS, Analytics);
        modifiableCategories.put(CategoryType.API, API);
        modifiableCategories.put(CategoryType.LOGGING, Logging);
        modifiableCategories.put(CategoryType.STORAGE, Storage);
        modifiableCategories.put(CategoryType.HUB, Hub);
        CATEGORIES = Collections.unmodifiableMap(modifiableCategories);
    }

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
        AmplifyConfiguration config = new AmplifyConfiguration();
        config.populateFromConfigFile(context);
        configure(config, context);
    }

    /**
     * Configure Amplify with AmplifyConfiguration object.
     * @param configuration AmplifyConfiguration object for configuration via code
     * @param context An Android Context
     * @throws ConfigurationException thrown when already configured
     * @throws PluginException thrown when there is no configuration found for a plugin
     */
    public static void configure(final AmplifyConfiguration configuration, Context context)
            throws ConfigurationException, PluginException {

        synchronized (LOCK) {
            if (configured) {
                throw new ConfigurationException.AmplifyAlreadyConfiguredException();
            }
            amplifyConfiguration = configuration;

            for (Category<? extends Plugin<?>> category : CATEGORIES.values()) {
                if (category.getPlugins().size() > 0) {
                    CategoryConfiguration categoryConfiguration =
                        amplifyConfiguration.forCategoryType(category.getCategoryType());
                    category.configure(categoryConfiguration, context);
                }
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
        updatePluginRegistry(plugin, RegistryUpdateType.ADD);
    }

    /**
     * Removes a plugin form the Amplify framework.
     * @param plugin The plugin to remove from the Amplify framework
     * @param <P> The type of the plugin being removed
     * @throws PluginException On failure to remove a plugin
     */
    public static <P extends Plugin<?>> void removePlugin(@NonNull final P plugin) throws PluginException {
        updatePluginRegistry(plugin, RegistryUpdateType.REMOVE);
    }

    @SuppressWarnings("unchecked") // Wants Category<P> from CATEGORIES.get(...), but it has Category<?>
    private static <P extends Plugin<?>> void updatePluginRegistry(
            final P plugin, final RegistryUpdateType registryUpdateType) throws PluginException {

        synchronized (LOCK) {
            if (TextUtils.isEmpty(plugin.getPluginKey())) {
                throw new PluginException.EmptyKeyException();
            } else if (!CATEGORIES.containsKey(plugin.getCategoryType())) {
                throw new PluginException.NoSuchPluginException("Plugin category does not exist. " +
                    "Verify that the library version is correct and supports the plugin's category.");
            }

            Category<P> category;
            try {
                category = (Category<P>) CATEGORIES.get(plugin.getCategoryType());
            } catch (ClassCastException classCastException) {
                // will throw in a moment...
                category = null;
            }
            if (category == null) {
                throw new PluginException.MismatchedPluginException();
            }

            if (RegistryUpdateType.REMOVE.equals(registryUpdateType)) {
                category.removePlugin(plugin);
            } else {
                category.addPlugin(plugin);
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

    private enum RegistryUpdateType {
        ADD,
        REMOVE
    }
}

