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

import android.content.Context;
import android.support.annotation.NonNull;

import com.amplifyframework.api.graphql.GraphQLQuery;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.exception.ConfigurationException;
import com.amplifyframework.core.plugin.PluginException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Api implements Category<ApiPlugin, ApiPluginConfiguration>, ApiCategoryBehavior {


    private boolean enabled;

    @Override
    public GraphQLQuery query(@NonNull String query) {
        if (enabled){
            plugin().query(query);
        }
    }

    static class PluginDetails {
        ApiPlugin apiPlugin;
        ApiPluginConfiguration apiPluginConfiguration;

        public ApiPlugin getApiPlugin() {
            return apiPlugin;
        }

        public PluginDetails apiPlugin(ApiPlugin apiPlugin) {
            this.apiPlugin = apiPlugin;
            return this;
        }

        public ApiPluginConfiguration getApiPluginConfiguration() {
            return apiPluginConfiguration;
        }

        public PluginDetails apiPluginConfiguration(ApiPluginConfiguration apiPluginConfiguration) {
            this.apiPluginConfiguration = apiPluginConfiguration;
            return this;
        }
    }

    /**
     * Map of the { pluginKey => plugin } object
     */
    private Map<String, PluginDetails> plugins;

    /**
     * Flag to remember that Analytics category is already configured by Amplify
     * and throw an error if configure method is called again
     */
    private boolean isConfigured;

    /**
     * Protect enabling and disabling of Analytics event
     * collection and sending.
     */
    private static final Object LOCK = new Object();

    public Api() {
        this.plugins = new ConcurrentHashMap<String, PluginDetails>();
    }


    @Override
    public void configure(@NonNull Context context) throws ConfigurationException, PluginException {
        if (isConfigured) {
            throw new ConfigurationException.AmplifyAlreadyConfiguredException();
        }

        if (!plugins.values().isEmpty()) {
            if (plugins.values().iterator().hasNext()) {
                PluginDetails pluginDetails = plugins.values().iterator().next();
                if (pluginDetails.apiPluginConfiguration == null) {
                    pluginDetails.apiPlugin.configure(context);
                } else {
                    pluginDetails.apiPlugin.configure(pluginDetails.apiPluginConfiguration);
                }
            }
        }

        isConfigured = true;
    }

    @Override
    public void addPlugin(@NonNull ApiPlugin plugin) throws PluginException {
        PluginDetails pluginDetails = new PluginDetails()
                .apiPlugin(plugin);

        try {
            if (plugins.put(plugin.getPluginKey(), pluginDetails) == null) {
                throw new PluginException.NoSuchPluginException();
            }
        } catch (Exception ex) {
            throw new PluginException.NoSuchPluginException();
        }
    }

    @Override
    public void addPlugin(@NonNull ApiPlugin plugin, @NonNull ApiPluginConfiguration pluginConfiguration) throws PluginException {
        PluginDetails pluginDetails = new PluginDetails()
                .apiPlugin(plugin)
                .apiPluginConfiguration(pluginConfiguration);

        try {
            if (plugins.put(plugin.getPluginKey(), pluginDetails) == null) {
                throw new PluginException.NoSuchPluginException();
            }
        } catch (Exception ex) {
            throw new PluginException.NoSuchPluginException();
        }
    }

    @Override
    public void removePlugin(@NonNull ApiPlugin plugin) throws PluginException {
        if (plugins.containsKey(plugin.getPluginKey())) {
            plugins.remove(plugin.getPluginKey());
        } else {
            throw new PluginException.NoSuchPluginException();
        }
    }

    @Override
    public void reset() {

    }

    @Override
    public ApiPlugin getPlugin(@NonNull String pluginKey) throws PluginException {
        if (plugins.containsKey(pluginKey)) {
            return plugins.get(pluginKey).apiPlugin;
        } else {
            throw new PluginException.NoSuchPluginException();
        }
    }

    /**
     * @return the set of plugins added to a Category.
     */
    @Override
    public Set<ApiPlugin> getPlugins() {
        Set<ApiPlugin> analyticsPlugins = new HashSet<ApiPlugin>();
        if (!plugins.isEmpty()) {
            for (PluginDetails pluginDetails : plugins.values()) {
                analyticsPlugins.add(pluginDetails.apiPlugin);
            }
        }
        return analyticsPlugins;
    }

    @Override
    public CategoryType getCategoryType() {
        return CategoryType.API;
    }

    private ApiPlugin getSelectedPlugin() {
        if (!plugins.values().isEmpty()) {
            return (ApiPlugin) plugins.values().toArray()[0];
        } else {
            return null;
        }
    }
}
