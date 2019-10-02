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

package com.amplifyframework.hub;

import android.content.Context;
import android.support.annotation.NonNull;

import com.amplifyframework.core.async.AmplifyOperation;
import com.amplifyframework.core.async.AsyncEvent;
import com.amplifyframework.core.async.EventListener;
import com.amplifyframework.core.async.Listener;
import com.amplifyframework.core.async.Result;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.exception.ConfigurationException;
import com.amplifyframework.core.plugin.PluginException;
import com.amplifyframework.core.plugin.PluginRuntimeException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Amplify has a local eventing system called Hub. It is a lightweight implementation of
 * Publisher-Subscriber pattern, and is used to share data between modules and components
 * in your app. Amplify uses Hub for different categories to communicate with one another
 * when specific events occur, such as authentication events like a user sign-in or
 * notification of a file download.
 */
public class HubCategory implements Category<HubPlugin,HubPluginConfiguration>, HubCategoryBehavior {

    static class PluginDetails {
        HubPlugin hubPlugin;
        HubPluginConfiguration hubPluginConfiguration;

        public HubPlugin getHubPlugin() {
            return hubPlugin;
        }

        public PluginDetails hubPlugin(HubPlugin hubPlugin) {
            this.hubPlugin = hubPlugin;
            return this;
        }

        public HubPluginConfiguration getHubPluginConfiguration() {
            return hubPluginConfiguration;
        }

        public PluginDetails hubPluginConfiguration(HubPluginConfiguration hubPluginConfiguration) {
            this.hubPluginConfiguration = hubPluginConfiguration;
            return this;
        }
    }

    /**
     * Map of the { pluginKey => plugin } object
     */
    private Map<String, PluginDetails> plugins;

    private Map<HubChannel, ArrayList<Listener<? extends Result>>> callbacks;


    private boolean isConfigured;

    public HubCategory() {
        this.plugins = new ConcurrentHashMap<String, PluginDetails>();
        this.callbacks = new HashMap<HubChannel, ArrayList<Listener<? extends Result>>>();;
    }

    /**
     * Read the configuration from amplifyconfiguration.json file
     *
     * @param context     Android context required to read the contents of file
     * @throws ConfigurationException thrown when already configured
     * @throws PluginException        thrown when there is no plugin found for a configuration
     */
    @Override
    public void configure(@NonNull Context context) throws ConfigurationException, PluginException {
        if (isConfigured) {
            throw new ConfigurationException.AmplifyAlreadyConfiguredException();
        }

        if (!plugins.values().isEmpty()) {
            if (plugins.values().iterator().hasNext()) {
                PluginDetails pluginDetails = plugins.values().iterator().next();
                if (pluginDetails.hubPluginConfiguration == null) {
                    pluginDetails.hubPlugin.configure(context);
                } else {
                    pluginDetails.hubPlugin.configure(pluginDetails.hubPluginConfiguration);
                }
            }
        }

        isConfigured = true;
    }

    /**
     * Register a plugin with Amplify
     *
     * @param plugin an implementation of a CATEGORY_TYPE that
     *               conforms to the {@link Plugin} interface.
     * @throws PluginException when a plugin cannot be registered for this category
     */
    @Override
    public void addPlugin(@NonNull HubPlugin plugin) throws PluginException {
        PluginDetails pluginDetails = new PluginDetails()
                .hubPlugin(plugin);

        try {
            if (plugins.put(plugin.getPluginKey(), pluginDetails) == null) {
                throw new PluginException.NoSuchPluginException();
            }
        } catch (Exception ex) {
            throw new PluginException.NoSuchPluginException();
        }
    }

    /**
     * Register a plugin with Amplify
     *
     * @param plugin              an implementation of a Category that
     *                            conforms to the {@link Plugin} interface.
     * @param pluginConfiguration configuration information for the plugin.
     * @throws PluginException when a plugin cannot be registered for this category
     */
    @Override
    public void addPlugin(@NonNull HubPlugin plugin, @NonNull HubPluginConfiguration pluginConfiguration) throws PluginException {

    }

    /**
     * Remove a registered plugin
     *
     * @param plugin an implementation of a Category that
     *               conforms to the {@link Plugin} interface
     * @throws PluginException when a plugin cannot be registered for this category
     */
    @Override
    public void removePlugin(@NonNull HubPlugin plugin) throws PluginException {
        if (plugins.remove(plugin.getPluginKey()) == null) {
            throw new PluginException.NoSuchPluginException();
        }
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
     * Retrieve a plugin of category.
     *
     * @param pluginKey the key that identifies the plugin implementation
     * @return the plugin object
     */
    @Override
    public HubPlugin getPlugin(@NonNull String pluginKey) throws PluginException {
        return null;
    }

    /**
     * @return the set of plugins added to a Category.
     */
    @Override
    public Set<HubPlugin> getPlugins() {
        return null;
    }

    @Override
    public CategoryType getCategoryType() {
        return CategoryType.HUB;
    }

    @Override
    public UnsubscribeToken listen(@NonNull HubChannel hubChannel, @NonNull HubListener listener) {
        return null;
    }

    @Override
    public UnsubscribeToken listen(@NonNull HubChannel hubChannel, @NonNull HubFilter hubFilter, @NonNull HubListener listener) {
        return null;
    }

    @Override
    public void dispatch(@NonNull HubChannel hubChannel, @NonNull HubPayload hubpayload) {

    }

    @Override
    public void removeListener(@NonNull final UnsubscribeToken unsubscribeToken) {

    }

    /**
     * Obtain the registered plugin. Throw runtime exception if
     * no plugin is registered or multiple plugins are registered.
     *
     * @return the only registered plugin for this category
     */
    private HubPlugin plugin() throws ConfigurationException {
        if (!isConfigured) {
            throw new ConfigurationException("Hub category is not yet configured.");
        }
        if (plugins.isEmpty()) {
            throw new PluginRuntimeException.NoPluginException();
        }
        if (plugins.size() > 1) {
            throw new PluginRuntimeException.MultiplePluginsException();
        }

        return plugins.values().iterator().next().hubPlugin;
    }

    /**
     * Convenience method to allow callers to listen to Hub events for a particular operation.
     * Internally, the listener transforms the HubPayload into the Operation's expected AsyncEvent
     * type, so callers may re-use their `listener`s.
     *
     * @param operation The operation to listen to events for
     * @param eventListener The Operation-specific listener callback to be invoked
     *                 when an AsyncEvent for that operation is received.
     */
    public UnsubscribeToken listen(@NonNull final AmplifyOperation operation,
                                   @NonNull final EventListener eventListener) {
        HubChannel channel = HubChannel.fromCategoryType(operation.getCategoryType());
        HubFilter filter = HubFilters.hubFilter(operation);
        HubListener transformingListener = new HubListener() {
            @Override
            public void onHubEvent(@NonNull HubPayload payload) {
                if (payload.data instanceof AsyncEvent) {
                    eventListener.onEvent((AsyncEvent) payload.data);
                }
            }
        };

        return listen(channel, filter, transformingListener);
    }
}
