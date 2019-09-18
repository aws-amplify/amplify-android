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

import com.amplifyframework.analytics.AnalyticsCategory;
import com.amplifyframework.api.APICategory;
import com.amplifyframework.auth.AuthCategory;
import com.amplifyframework.core.exception.AmplifyAlreadyConfiguredException;
import com.amplifyframework.core.exception.MismatchedPluginException;
import com.amplifyframework.core.exception.NoSuchPluginException;
import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.logging.LoggingCategory;
import com.amplifyframework.storage.Storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    public static final AnalyticsCategory Analytics;
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

    /**
     * Map of {CategoryType, {pluginClass, pluginObject}}.
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
    private static Map<String, Plugin> plugins =
            new ConcurrentHashMap<String, Plugin>();

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
     * Register a plugin with Amplify
     *
     * @param plugin an implementation of a CATEGORY_TYPE that
     *               conforms to the {@link Plugin} interface.
     * @param <P> any plugin that conforms to the {@link Plugin} interface
     * @throws MismatchedPluginException when a plugin cannot be registered for this CATEGORY_TYPE
     */
    public static <P extends Plugin> void addPlugin(@NonNull final P plugin) throws MismatchedPluginException {
        synchronized (LOCK) {
            plugins.put(plugin.getPluginKey(), plugin);
        }
    }

    /**
     * Remove a registered plugin
     *
     * @param pluginKey key that identifies the plugin
     * @param <P> any plugin that conforms to the {@link Plugin} interface
     */
    public static <P extends Plugin> void removePlugin(@NonNull final String pluginKey) {
        synchronized (LOCK) {
            plugins.remove(pluginKey);
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
     * Retrieve a plugin of CATEGORY_TYPE.
     *
     * @param pluginKey the key that identifies the plugin implementation
     * @param <P> any plugin that conforms to the {@link Plugin} interface
     * @return the plugin object
     */
    public static <P extends Plugin> Plugin getPlugin(@NonNull final String pluginKey) {
        synchronized (LOCK) {
            return plugins.get(pluginKey);
        }
    }

    /**
     * Retrieve the map of CATEGORY_TYPE plugins.
     *     {CategoryType => {PluginName => PluginObject}}
     *     A CATEGORY_TYPE can have more than one plugins registered through
     *     the Amplify System. Each plugin is identified with a name.
     *
     * @return the map that represents the CATEGORY_TYPE plugins.
     */
    public static Map<String, Plugin> getPlugins() {
        synchronized (LOCK) {
            return plugins;
        }
    }
}