/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.InitializationResult;
import com.amplifyframework.core.InitializationStatus;
import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.util.Immutable;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A category groups together zero or more plugins that share the same
 * category type.
 * @param <P> A base class type for plugins that this category will
 *            support, e.g. StoragePlugin, AnalyticsPlugin, etc.
 */
public abstract class Category<P extends Plugin<?>> implements CategoryTypeable {
    /**
     * Map of the { pluginKey => plugin } object.
     */
    private final Map<String, P> plugins;

    /**
     * Records the initialization state. See {@link State} for possible values.
     */
    private final AtomicReference<State> state;

    private final AtomicReference<CategoryInitializationResult> categoryInitializationResult;

    /**
     * Constructs a new, not-yet-configured, Category.
     */
    public Category() {
        this.plugins = new ConcurrentHashMap<>();
        this.state = new AtomicReference<>(State.NOT_CONFIGURED);
        this.categoryInitializationResult = new AtomicReference<>(null);
    }

    /**
     * Configure category with provided AmplifyConfiguration object.
     * @param configuration Configuration for all plugins in the category
     * @param context An Android Context
     * @throws AmplifyException if already configured
     */
    public synchronized void configure(
            @NonNull CategoryConfiguration configuration, @NonNull Context context)
            throws AmplifyException {
        synchronized (state) {
            if (!State.NOT_CONFIGURED.equals(state.get())) {
                throw new AmplifyException(
                    "Category " + getCategoryType() + " has already been configured or is currently configuring.",
                    "Ensure that you are only attempting configuration once."
                );
            }
            state.set(State.CONFIGURING);
            try {
                for (P plugin : getPlugins()) {
                    if (configureFromDefaultConfigFile()) {
                        String pluginKey = plugin.getPluginKey();
                        JSONObject pluginConfig = configuration.getPluginConfig(pluginKey);
                        plugin.configure(pluginConfig != null ? pluginConfig : new JSONObject(), context);
                    }
                }
                state.set(State.CONFIGURED);
            } catch (Throwable anyError) {
                state.set(State.CONFIGURATION_FAILED);
                throw anyError;
            }
        }
    }

    /**
     * Initialize the category. This asynchronous call is made only after
     * the category has been successfully configured. Whereas configuration is a short-lived
     * synchronous phase of setup, initialization may require disk/network resources, etc.
     * @param context An Android Context
     * @return A category initialization result
     */
    @NonNull
    @WorkerThread
    public synchronized CategoryInitializationResult initialize(@NonNull Context context) {
        final Map<String, InitializationResult> pluginInitializationResults = new HashMap<>();
        if (!State.CONFIGURED.equals(state.get())) {
            for (P plugin : getPlugins()) {
                InitializationResult result = InitializationResult.failure(new AmplifyException(
                    "Tried to init before category was not configured.",
                    "Call configure() on category, first."
                ));
                pluginInitializationResults.put(plugin.getPluginKey(), result);
            }
        } else {
            state.set(State.CONFIGURING);
            for (P plugin : getPlugins()) {
                InitializationResult result;
                try {
                    plugin.initialize(context);
                    result = InitializationResult.success();
                } catch (AmplifyException pluginInitializationFailure) {
                    result = InitializationResult.failure(pluginInitializationFailure);
                }
                pluginInitializationResults.put(plugin.getPluginKey(), result);
            }
        }

        final CategoryInitializationResult result =
            CategoryInitializationResult.with(pluginInitializationResults);
        categoryInitializationResult.set(result);
        if (result.isFailure()) {
            state.set(State.INITIALIZATION_FAILED);
        } else {
            state.set(State.INITIALIZED);
        }
        HubChannel hubChannel = HubChannel.forCategoryType(getCategoryType());
        InitializationStatus status = result.isFailure() ?
            InitializationStatus.FAILED : InitializationStatus.SUCCEEDED;
        Amplify.Hub.publish(hubChannel, HubEvent.create(status, result));

        return result;
    }

    /**
     * Register a plugin into the Category.
     * @param plugin A plugin to add
     * @throws AmplifyException If Amplify is already configured
     */
    public final void addPlugin(@NonNull P plugin) throws AmplifyException {
        if (!State.NOT_CONFIGURED.equals(state.get())) {
            throw new AmplifyException(
                "Category " + getCategoryType() + " has already been configured or is configuring.",
                "Make sure that you have added all plugins before attempting configuration."
            );
        }
        String pluginKey = plugin.getPluginKey();
        plugins.put(pluginKey, plugin);
    }

    /**
     * Remove a plugin from the category.
     * @param plugin A plugin to remove
     */
    public final void removePlugin(@NonNull P plugin) {
        //noinspection StatementWithEmptyBody
        if (plugins.remove(plugin.getPluginKey()) == null) {
            // TODO: Fail silently for now, matching iOS - potentially publish on Hub in the future.
        }
    }

    /**
     * Retrieve a plugin by its key.
     * @param pluginKey A key that identifies a plugin implementation
     * @return The plugin object associated to pluginKey, if registered
     * @throws IllegalStateException If the requested plugin does not exist, or has not been configured.
     */
    @NonNull
    public final P getPlugin(@NonNull final String pluginKey) throws IllegalStateException {
        P plugin = plugins.get(pluginKey);
        return getPluginIfConfiguredOrThrow(plugin);
    }

    /**
     * Gets the set of plugins associated with the Category.
     * @return The set of plugins associated to the Category
     */
    @NonNull
    public final Set<P> getPlugins() {
        return Immutable.of(new HashSet<>(plugins.values()));
    }

    /**
     * Obtain the registered plugin for this category.
     * @return The only registered plugin for this category
     * @throws IllegalStateException
     *         If the category is not configured, or if there are no
     *         plugins associated to the category, or if Amplify has not
     *         been configured
     */
    @NonNull
    protected P getSelectedPlugin() throws IllegalStateException {
        if (plugins.size() > 1) {
            throw new IllegalStateException(
                "Tried to get a default plugin but there are more than one to choose from in this category. " +
                "Call getPlugin(pluginKey) to choose the specific plugin you want to use in this case."
            );
        }
        Iterator<P> pluginsIterator = getPlugins().iterator();
        return getPluginIfConfiguredOrThrow(pluginsIterator.hasNext() ? pluginsIterator.next() : null);
    }

    private P getPluginIfConfiguredOrThrow(P plugin) throws IllegalStateException {
        if (plugin == null) {
            throw new IllegalStateException(
                "Tried to get a plugin but that plugin was not present. " +
                "Check if the plugin was added originally or perhaps was already removed."
            );
        } else if (State.CONFIGURATION_FAILED.equals(state.get())) {
            throw new IllegalStateException(
                "Failed to get plugin because configuration previously failed.  Check for failures by logging any " +
                "exceptions thrown by Amplify.configure()."
            );
        } else if (State.INITIALIZATION_FAILED.equals(state.get())) {
            Throwable cause = null;
            final CategoryInitializationResult result = categoryInitializationResult.get();
            if (result != null) {
                cause = result.getPluginInitializationFailures().get(plugin.getPluginKey());
            }
            throw new IllegalStateException(
                "Failed to get plugin because initialization previously failed.  See attached exception for details.",
                cause);
        } else if (!isConfigured()) {
            throw new IllegalStateException(
                "Tried to get a plugin before it was configured.  Make sure you call Amplify.configure() first.");
        } else {
            return plugin;
        }
    }

    /**
     * Returns whether the category has been configured.
     * @return whether the category has been configured.
     */
    protected synchronized boolean isConfigured() {
        return Arrays.asList(State.CONFIGURED, State.INITIALIZING, State.INITIALIZED).contains(state.get());
    }

    /**
     * Returns whether the category has been initialized.
     * @return whether the category has been initialized.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected synchronized boolean isInitialized() {
        return State.INITIALIZED.equals(state.get());
    }

    /**
     * Return whether to configure the plugins using amplifyconfiguration.json.
     * override this method for categories not configured using the default amplifyconfiguration.json
     * For e.g., the Logging category
     * @return whether to configure the plugins using amplifyconfiguration.json
     */
    protected boolean configureFromDefaultConfigFile() {
        return true;
    }

    /**
     * The Category must be in exactly one of the below states. During ideal operation, the state
     * machine flows from {@link #NOT_CONFIGURED} to {@link #INITIALIZED}, in the order below.
     * But, if there is an error at any point, the state transition will terminate in a {@link #CONFIGURATION_FAILED}
     * or {@link #INITIALIZATION_FAILED} state.
     */
    private enum State {
        /**
         * The category has not began configuration, yet. This is the starting state.
         */
        NOT_CONFIGURED,

        /**
         * Configuration is under way.
         */
        CONFIGURING,

        /**
         * Configuration has succeeded.
         */
        CONFIGURED,

        /**
         * Initialization has began.
         */
        INITIALIZING,

        /**
         * Initialization has completed successfully. As a corollary, configuration
         * had also succeed. This is a terminal state.
         */
        INITIALIZED,

        /**
         * Configuration failed. This is a terminal state.
         */
        CONFIGURATION_FAILED,

        /**
         * Initialization failed. This is a terminal state.
         */
        INITIALIZATION_FAILED
    }
}
