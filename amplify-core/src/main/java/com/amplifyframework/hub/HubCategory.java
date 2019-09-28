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

import android.support.annotation.NonNull;

import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.async.Callback;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.exception.ConfigurationException;
import com.amplifyframework.core.plugin.PluginException;
import com.amplifyframework.core.task.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HubCategory implements Category<HubPlugin>, HubCategoryBehavior {

    private static Map<HubChannel, ArrayList<Callback<? extends Result>>> callbacks =
            new HashMap<HubChannel, ArrayList<Callback<? extends Result>>>();


    @Override
    public void configure(AmplifyConfiguration configuration) throws ConfigurationException, PluginException {

    }

    /**
     * Register a plugin with Amplify
     *
     * @param plugin an implementation of HubPlugin interface.
     * @throws PluginException when a plugin cannot be registered for this category
     */
    @Override
    public void addPlugin(@NonNull HubPlugin plugin) throws PluginException {

    }

    /**
     * Remove a registered plugin
     *
     * @param plugin an implementation of HubPlugin interface.
     * @throws PluginException when a plugin cannot be registered for this category
     */
    @Override
    public void removePlugin(@NonNull HubPlugin plugin) throws PluginException {

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
        return null;
    }

    @Override
    public void listen(HubChannel hubChannel, HubCallback callback) {

    }

    @Override
    public void dispatch(HubChannel hubChannel, HubPayload hubpayload) {

    }

    @Override
    public void remove(HubChannel hubChannel, HubCallback callback) {

    }
}
