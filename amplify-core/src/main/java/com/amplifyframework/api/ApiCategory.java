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

package com.amplifyframework.api;

import android.support.annotation.NonNull;

import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.exception.ConfigurationException;
import com.amplifyframework.core.plugin.PluginException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ApiCategory implements Category<ApiPlugin>, RestApiCategoryBehavior, GraphQLApiCategoryBehavior {
    /**
     * Map of the { pluginKey => plugin } object
     */
    private Map<String, RestApiPlugin> restApiPlugins;
    private Map<String, GraphQLApiPlugin> gqlApiPlugins;

    /**
     * Flag to remember that API category is already configured by Amplify
     * and throw an error if configure method is called again
     */
    private boolean isConfigured;

    /**
     * Protect enabling and disabling of Analytics event
     * collection and sending.
     */
    private static final Object LOCK = new Object();

    public ApiCategory() {
        this.restApiPlugins = new ConcurrentHashMap<String, RestApiPlugin>();
        this.gqlApiPlugins = new ConcurrentHashMap<String, GraphQLApiPlugin>();
        this.isConfigured = false;
    }

    @Override
    public void query() {
        getSelectedGraphQLApiPlugin().query();
    }

    @Override
    public void mutate() {
        getSelectedGraphQLApiPlugin().mutate();
    }

    @Override
    public void subscribe() {
        getSelectedGraphQLApiPlugin().subscribe();
    }

    @Override
    public void unsubscribe() {
        getSelectedGraphQLApiPlugin().unsubscribe();
    }

    @Override
    public void get() {
        getSelectedRestApiPlugin().get();
    }

    @Override
    public void put() {
        getSelectedRestApiPlugin().put();
    }

    @Override
    public void post() {
        getSelectedRestApiPlugin().post();
    }

    @Override
    public void patch() {
        getSelectedRestApiPlugin().patch();
    }

    @Override
    public void delete() {
        getSelectedRestApiPlugin().delete();
    }

    /**
     * Configure API category based on AmplifyConfiguration object
     *
     * @param configuration AmplifyConfiguration object for configuration via code
     * @throws ConfigurationException thrown when already configured
     * @throws PluginException        thrown when there is no plugin found for a configuration
     */
    @Override
    public void configure(AmplifyConfiguration configuration) throws ConfigurationException, PluginException {
        if (isConfigured) {
            throw new ConfigurationException.AmplifyAlreadyConfiguredException();
        }

        for (ApiPlugin plugin : getPlugins()) {
            String pluginKey = plugin.getPluginKey();
            Object pluginConfig = configuration.api.pluginConfigs.get(pluginKey);
            if (pluginConfig != null) {
                plugin.configure(pluginConfig);
            } else {
                throw new PluginException.PluginConfigurationException();
            }
        }

        isConfigured = true;
    }

    @Override
    public void addPlugin(@NonNull ApiPlugin plugin) throws PluginException {
        try {
            switch (plugin.getApiType()) {
                case REST:
                    restApiPlugins.put(plugin.getPluginKey(), (RestApiPlugin) plugin);
                    break;
                case GRAPHQL:
                    gqlApiPlugins.put(plugin.getPluginKey(), (GraphQLApiPlugin) plugin);
                    break;
            }
        } catch (Exception ex) {
            throw new PluginException.NoSuchPluginException();
        }
    }

    @Override
    public void removePlugin(@NonNull ApiPlugin plugin) throws PluginException {
        ApiPlugin removed;
        switch (plugin.getApiType()) {
            case REST:
                removed = restApiPlugins.remove(plugin.getPluginKey());
                break;
            case GRAPHQL:
                removed = gqlApiPlugins.remove(plugin.getPluginKey());
                break;
            default:
                removed = null;
        }
        if (removed == null) {
            throw new PluginException.NoSuchPluginException();
        }
    }

    @Override
    public ApiPlugin getPlugin(@NonNull String pluginKey) throws PluginException {
        if (restApiPlugins.containsKey(pluginKey)) {
            return restApiPlugins.get(pluginKey);
        } else if (gqlApiPlugins.containsKey(pluginKey)) {
            return gqlApiPlugins.get(pluginKey);
        } else {
            throw new PluginException.NoSuchPluginException();
        }
    }

    /**
     * @return the set of plugins added to a Category.
     */
    @Override
    public Set<ApiPlugin> getPlugins() {
        Set<ApiPlugin> plugins = new HashSet<ApiPlugin>();
        plugins.addAll(restApiPlugins.values());
        plugins.addAll(gqlApiPlugins.values());
        return plugins;
    }

    @Override
    public final CategoryType getCategoryType() {
        return CategoryType.API;
    }

    /**
     * Obtain the registered plugin. Throw runtime exception if
     * no plugin is registered or multiple plugins are registered.
     *
     * @return the only registered plugin for this category
     */
    private RestApiPlugin getSelectedRestApiPlugin() {
        if (!isConfigured) {
            throw new ConfigurationException("API category is not yet configured.");
        }
        if (restApiPlugins.isEmpty()) {
            throw new PluginException.NoSuchPluginException();
        }
        if (restApiPlugins.size() > 1) {
            throw new PluginException.MultiplePluginsException();
        }

        return restApiPlugins.values().iterator().next();
    }

    /**
     * Obtain the registered plugin. Throw runtime exception if
     * no plugin is registered or multiple plugins are registered.
     *
     * @return the only registered plugin for this category
     */
    private GraphQLApiPlugin getSelectedGraphQLApiPlugin() {
        if (!isConfigured) {
            throw new ConfigurationException("API category is not yet configured.");
        }
        if (gqlApiPlugins.isEmpty()) {
            throw new PluginException.NoSuchPluginException();
        }
        if (gqlApiPlugins.size() > 1) {
            throw new PluginException.MultiplePluginsException();
        }

        return gqlApiPlugins.values().iterator().next();
    }

}
