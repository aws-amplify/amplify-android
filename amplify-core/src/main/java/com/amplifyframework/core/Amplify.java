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
import android.util.Log;

import com.amplifyframework.analytics.AnalyticsCategory;
import com.amplifyframework.api.APICategory;
import com.amplifyframework.auth.AuthCategory;
import com.amplifyframework.core.exception.AmplifyAlreadyConfiguredException;
import com.amplifyframework.core.exception.MismatchedPluginException;
import com.amplifyframework.core.exception.NoSuchPluginException;
import com.amplifyframework.core.plugin.Category;
import com.amplifyframework.core.plugin.CategoryPlugin;
import com.amplifyframework.logging.LoggingCategory;
import com.amplifyframework.storage.StorageCategory;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Amplify System has the following responsibilities:
 *
 * 1) Add, Get and Remove category plugins with the Amplify System
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

    public static final AnalyticsCategory Analytics;
    public static final APICategory API;
    public static final AuthCategory Auth;
    public static final LoggingCategory Logging;
    public static final StorageCategory Storage;

    private static boolean CONFIGURED = false;

    private static Context context;
    private static AmplifyConfiguration amplifyConfiguration;

    static {
        Analytics = null;
        API = null;
        Auth = null;
        Logging = null;
        Storage = null;
    }

    /**
     * Map of {Category, {pluginClass, pluginObject}}.
     *
     * {
     *     "AUTH" => {
     *         "keyForAuth" => "AmazonCognitoAuthPlugin@object"
     *     },
     *     "STORAGE" => {
     *         "keyForStorage" => "AmazonS3StoragePlugin@object"
     *     },
     *     "ANALYTICS" => {
     *         "keyForAmazonPinpoint" => "AmazonPinpointAnalyticsPlugin@object",
     *         "keyForAmazonKinesis" => "AmazonKinesisAnalyticsPlugin@object"
     *     },
     *     "API" => {
     *         "keyForAWSAPIGatewayPlugin" => "AWSRESTAPIGatewayPlugin@object"
     *     }
     * }
     */
    private static ConcurrentHashMap<Category, ConcurrentHashMap<String, CategoryPlugin>> plugins =
            new ConcurrentHashMap<Category, ConcurrentHashMap<String, CategoryPlugin>>();

    private static final Object LOCK = new Object();

    /**
     * Read the configuration from amplifyconfiguration.json file
     *
     * @param context Android context required to read the contents of file
     * @throws AmplifyAlreadyConfiguredException thrown when already configured
     * @throws NoSuchPluginException thrown when there is no plugin found for a configuration
     */
    public static void configure(@NonNull Context context) throws AmplifyAlreadyConfiguredException, NoSuchPluginException {
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
     * @throws AmplifyAlreadyConfiguredException thrown when already configured
     * @throws NoSuchPluginException thrown when there is no plugin found for a configuration
     */
    public static void configure(@NonNull Context context, @NonNull String environment) throws AmplifyAlreadyConfiguredException, NoSuchPluginException {
        synchronized (LOCK) {
            amplifyConfiguration = new AmplifyConfiguration(context);
            amplifyConfiguration.setEnvironment(environment);
            CONFIGURED = true;
        }
    }

    /**
     * Read the configuration from amplifyconfiguration.json file
     *
     * @param context Android context required to read the contents of file
     * @param amplifyConfiguration Pass the object via code that contains the configuration
     * @throws AmplifyAlreadyConfiguredException thrown when already configured
     * @throws NoSuchPluginException thrown when there is no plugin found for a configuration
     */
    public static void configure(@NonNull Context context, @NonNull AmplifyConfiguration amplifyConfiguration) throws AmplifyAlreadyConfiguredException, NoSuchPluginException {
        synchronized (LOCK) {
            configure(context, amplifyConfiguration, AmplifyConfiguration.DEFAULT_ENVIRONMENT_NAME);
        }
    }

    /**
     * Read the configuration from amplifyconfiguration.json file
     *
     * @param context Android context required to read the contents of file
     * @param amplifyConfiguration Pass the object via code that contains the configuration
     * @param environment specifies the name of the environment being operated on.
     *                    For example, "Default", "Custom", etc.
     * @throws AmplifyAlreadyConfiguredException thrown when already configured
     * @throws NoSuchPluginException thrown when there is no plugin found for a configuration
     */
    public static void configure(@NonNull Context context, @NonNull AmplifyConfiguration amplifyConfiguration, @NonNull String environment) throws AmplifyAlreadyConfiguredException, NoSuchPluginException {
        synchronized (LOCK) {
            Amplify.context = context;
            Amplify.amplifyConfiguration = amplifyConfiguration;
            amplifyConfiguration.setEnvironment(environment);
            CONFIGURED = true;
        }
    }

    /**
     * Register a plugin with Amplify
     *
     * @param plugin an implementation of a category that
     *               conforms to the {@link CategoryPlugin} interface.
     * @param <P> any plugin that conforms to the {@link CategoryPlugin} interface
     * @throws MismatchedPluginException when a plugin cannot be registered for this category
     */
    public static <P extends CategoryPlugin> void addPlugin(P plugin) throws MismatchedPluginException {
        synchronized (LOCK) {
            ConcurrentHashMap<String, CategoryPlugin> pluginsOfCategory = plugins.get(plugin.getCategory());
            if (pluginsOfCategory == null) {
                pluginsOfCategory = new ConcurrentHashMap<String, CategoryPlugin>();
            }
            pluginsOfCategory.put(plugin.getPluginKey(), plugin);
        }
    }

    /**
     * Remove a registered plugin
     *
     * @param plugin an implementation of a category that
     *               conforms to the {@link CategoryPlugin} interface.
     * @param <P> any plugin that conforms to the {@link CategoryPlugin} interface
     */
    public static <P extends CategoryPlugin> void removePlugin(P plugin) {
        synchronized (LOCK) {
            plugins.get(plugin.getCategory()).remove(plugin.getPluginKey());
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

    /**
     * Retrieve a plugin of category.
     *
     * @param pluginKey the key that identifies the plugin implementation
     * @param <P> any plugin that conforms to the {@link CategoryPlugin} interface
     * @return the plugin object
     */
    public static <P extends CategoryPlugin> CategoryPlugin getPlugin(@NonNull final String pluginKey) {
        synchronized (LOCK) {
            for (final ConcurrentHashMap<String, CategoryPlugin> pluginsOfCategory: plugins.values()) {
                if (pluginsOfCategory.get(pluginKey) != null) {
                    return pluginsOfCategory.get(pluginKey);
                }
            }
            return null;
        }
    }

    /**
     * Retrieve the map of category plugins.
     *     {Category => {PluginName => PluginObject}}
     *     A category can have more than one plugins registered through
     *     the Amplify System. Each plugin is identified with a name.
     *
     * @return the map that represents the category plugins.
     */
    public static ConcurrentHashMap<Category, ConcurrentHashMap<String, CategoryPlugin>> getPlugins() {
        synchronized (LOCK) {
            return plugins;
        }
    }

    /**
     * Retrieve the plugin for a particular category.
     * Returns the plugin registered for a category if one.
     *         the plugin returned by the selector if there are more than one plugin.
     *
     * @param category Name of the category
     * @return the plugin registered and chosen for the catgeory passed in.
     */
    public static CategoryPlugin getPluginForCategory(Category category) {
        synchronized (LOCK) {
            try {
                return new ArrayList<CategoryPlugin>(plugins.get(category).values()).get(0);
            } catch (Exception ex) {
                Log.e(TAG,"Error in retrieving the plugins of a category." + ex);
                return null;
            }
        }
    }
}

