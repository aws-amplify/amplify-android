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

import java.util.HashMap;
import java.util.HashSet;
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

    /**
     * Constructs a new, not-yet-configured, Category.
     */
    public Category() {
        this.plugins = new ConcurrentHashMap<>();
        this.state = new AtomicReference<>(State.NOT_CONFIGURED);
    }

    /**
     * Configure category with provided AmplifyConfiguration object.
     * @param configuration Configuration for all plugins in the category
     * @param context An Android Context
     * @throws AmplifyException if already configured
     */
    public final synchronized void configure(
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
                    String pluginKey = plugin.getPluginKey();
                    JSONObject pluginConfig = configuration.getPluginConfig(pluginKey);
                    plugin.configure(pluginConfig != null ? pluginConfig : new JSONObject(), context);
                }
                state.set(State.CONFIGURED);
            } catch (Throwable anyError) {
                state.set(State.FAILED);
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
    public final synchronized CategoryInitializationResult initialize(@NonNull Context context) {
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
        if (result.isFailure()) {
            state.set(State.FAILED);
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
     * Removes all plugins and resets state to {@link State#NOT_CONFIGURED}.
     */
    public final void removeAllPlugins() {
        plugins.clear();
        state.set(State.NOT_CONFIGURED);
    }

    /**
     * Retrieve a plugin by its key.
     * @param pluginKey A key that identifies a plugin implementation
     * @return The plugin object associated to pluginKey, if registered
     * @throws IllegalStateException If the requested plugin does not exist
     */
    @NonNull
    public final P getPlugin(@NonNull final String pluginKey) throws IllegalStateException {
        P plugin = plugins.get(pluginKey);
        if (plugin != null) {
            return plugin;
        } else {
            throw new IllegalStateException(
                "Tried to get a plugin but that plugin was not present. " +
                "Check if the plugin was added originally or perhaps was already removed."
            );
        }
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
        if (plugins.isEmpty()) {
            throw new IllegalStateException(
                "Tried to get a plugin but that plugin was not present. " +
                "Check if the plugin was added originally or perhaps was already removed."
            );
        } else if (plugins.size() > 1) {
            throw new IllegalStateException(
                "Tried to get a default plugin but there are more than one to choose from in this category. " +
                "Call getPlugin(pluginKey) to choose the specific plugin you want to use in this case."
            );
        }

        return getPlugins().iterator().next();
    }

    /**
     * Gets the current state of the category.
     * @return Current category state
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected synchronized boolean isInitialized() {
        return State.INITIALIZED.equals(state.get());
    }

    /**
     * The Category must be in exactly one of the below states. During ideal operation, the state
     * machine flows from {@link #NOT_CONFIGURED} to {@link #INITIALIZED}, in the order below.
     * But, if there is an error at any point, the state transition will terminate in a {@link #FAILED}
     * state.
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
         * Configuration or initialization failed. This is a terminal state.
         */
        FAILED
    }
}
