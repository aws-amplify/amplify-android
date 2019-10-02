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

import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.plugin.PluginException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ApiCategory extends Category<ApiPlugin> implements RestApiCategoryBehavior, GraphQLApiCategoryBehavior {
    private Map<String, RestApiPlugin> restApiPlugins;
    private Map<String, GraphQLApiPlugin> gqlApiPlugins;

    public ApiCategory() {
        this.restApiPlugins = new ConcurrentHashMap<String, RestApiPlugin>();
        this.gqlApiPlugins = new ConcurrentHashMap<String, GraphQLApiPlugin>();
    }

    @Override
    public final CategoryType getCategoryType() {
        return CategoryType.API;
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

    @Override
    public void addPlugin(@NonNull ApiPlugin plugin) throws PluginException {
        super.addPlugin(plugin);
        switch (plugin.getApiType()) {
            case REST:
                restApiPlugins.put(plugin.getPluginKey(), (RestApiPlugin) plugin);
                break;
            case GRAPHQL:
                gqlApiPlugins.put(plugin.getPluginKey(), (GraphQLApiPlugin) plugin);
                break;
            default:
                throw new ApiException.UnsupportedAPITypeException();
        }
    }

    @Override
    public void removePlugin(@NonNull ApiPlugin plugin) throws PluginException {
        super.removePlugin(plugin);
        switch (plugin.getApiType()) {
            case REST:
                restApiPlugins.remove(plugin.getPluginKey());
                break;
            case GRAPHQL:
                gqlApiPlugins.remove(plugin.getPluginKey());
                break;
            default:
                throw new ApiException.UnsupportedAPITypeException();
        }
    }

    private RestApiPlugin getSelectedRestApiPlugin() {
        guardConfigured();
        if (restApiPlugins.isEmpty()) {
            throw new PluginException.NoSuchPluginException("Plugin for REST API was not registered in this category.")
                    .withRecoverySuggestion("Please register a plugin that implements RestApiPlugin using `Amplify.addPlugin`.");
        }
        if (restApiPlugins.size() > 1) {
            throw new PluginException.MultiplePluginsException();
        }

        return restApiPlugins.values().iterator().next();
    }

    private GraphQLApiPlugin getSelectedGraphQLApiPlugin() {
        guardConfigured();
        if (gqlApiPlugins.isEmpty()) {
            throw new PluginException.NoSuchPluginException("Plugin for GraphQL API was not registered in this category.")
                    .withRecoverySuggestion("Please register a plugin that implements GraphQLApiPlugin using `Amplify.addPlugin`.");
        }
        if (gqlApiPlugins.size() > 1) {
            throw new PluginException.MultiplePluginsException();
        }

        return gqlApiPlugins.values().iterator().next();
    }

}
