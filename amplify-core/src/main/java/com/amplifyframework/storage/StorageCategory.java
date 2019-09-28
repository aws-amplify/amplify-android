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

package com.amplifyframework.storage;

import android.support.annotation.NonNull;

import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.async.Callback;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.exception.ConfigurationException;
import com.amplifyframework.core.plugin.PluginException;
import com.amplifyframework.storage.exception.*;
import com.amplifyframework.storage.operation.*;
import com.amplifyframework.storage.options.*;
import com.amplifyframework.storage.result.StorageGetResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StoragePutResult;
import com.amplifyframework.storage.result.StorageRemoveResult;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines the Client API consumed by the application.
 * Internally routes the calls to the Storage Category
 * plugins registered.
 */

public class StorageCategory implements Category<StoragePlugin>, StorageCategoryBehavior {

    private Map<String, StoragePlugin> plugins;

    /**
     * Flag to remember that Storage category is already configured by Amplify
     * and throw an error if configure method is called again
     */
    private boolean isConfigured;

    public StorageCategory() {
        this.plugins = new ConcurrentHashMap<String, StoragePlugin>();
        this.isConfigured = false;
    }

    /**
     * Obtain the registered plugin. Throw runtime exception if
     * no plugin is registered or multiple plugins are registered.
     *
     * @return the only registered plugin for this category
     */
    private StoragePlugin getSelectedPlugin() {
        if (!isConfigured) {
            throw new ConfigurationException("Storage category is not yet configured.");
        }
        if (plugins.isEmpty()) {
            throw new PluginException.NoSuchPluginException();
        }
        if (plugins.size() > 1) {
            throw new PluginException.MultiplePluginsException();
        }

        return plugins.values().iterator().next();
    }

    @Override
    public StorageGetOperation get(@NonNull String key) throws StorageGetException {
        return get(key, new StorageGetOptions(), null);
    }

    @Override
    public StorageGetOperation get(@NonNull String key,
                                   @NonNull StorageGetOptions options) throws StorageGetException {
        return get(key, options, null);
    }

    @Override
    public StorageGetOperation get(@NonNull String key,
                                   @NonNull StorageGetOptions options,
                                   Callback<StorageGetResult> callback) throws StorageGetException {
        return getSelectedPlugin().get(key, options, callback);
    }

    /**
     * Upload local file on given path to storage
     *
     * @param key   the unique identifier of the object in storage
     * @param local the path to a local file
     * @return an operation object that provides notifications and
     * actions related to the execution of the work
     * @throws StoragePutException
     */
    @Override
    public StoragePutOperation put(@NonNull String key, @NonNull String local) throws StoragePutException {
        return put(key, local, new StoragePutOptions(), null);
    }

    @Override
    public StoragePutOperation put(@NonNull String key,
                                   @NonNull String local,
                                   @NonNull StoragePutOptions options) throws StoragePutException {
        return put(key, local, options, null);
    }

    @Override
    public StoragePutOperation put(@NonNull String key,
                                   @NonNull String local,
                                   @NonNull StoragePutOptions options,
                                   Callback<StoragePutResult> callback) throws StoragePutException {
        return getSelectedPlugin().put(key, local, options, callback);
    }

    @Override
    public StorageListOperation list() throws StorageListException {
        return list(new StorageListOptions());
    }

    @Override
    public StorageListOperation list(@NonNull StorageListOptions options) throws StorageListException {
        return list(options, null);
    }

    @Override
    public StorageListOperation list(@NonNull StorageListOptions options,
                                     Callback<StorageListResult> callback) throws StorageListException {
        return getSelectedPlugin().list(options, callback);
    }

    @Override
    public StorageRemoveOperation remove(@NonNull String key) throws StorageRemoveException {
        return remove(key, new StorageRemoveOptions());
    }

    @Override
    public StorageRemoveOperation remove(@NonNull String key,
                                         StorageRemoveOptions options) throws StorageRemoveException {
        return remove(key, options, null);
    }

    @Override
    public StorageRemoveOperation remove(@NonNull String key,
                                         @NonNull StorageRemoveOptions options,
                                         Callback<StorageRemoveResult> callback) throws StorageRemoveException {
        return getSelectedPlugin().remove(key, options, callback);
    }

    /**
     * Configure Storage category based on AmplifyConfiguration object
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

        if (!plugins.values().isEmpty()) {
            if (plugins.values().iterator().hasNext()) {
                StoragePlugin plugin = plugins.values().iterator().next();
                String pluginKey = plugin.getPluginKey();
                Object pluginConfig = configuration.storage.pluginConfigs.get(pluginKey);
                if (pluginConfig != null) {
                    plugin.configure(pluginConfig);
                } else {
                    throw new PluginException.NoSuchPluginException();
                }
            }
        }

        isConfigured = true;
    }

    /**
     * Register a Storage plugin with Amplify
     *
     * @param plugin an implementation of StoragePlugin
     * @throws PluginException when this plugin cannot be found
     */
    @Override
    public void addPlugin(@NonNull StoragePlugin plugin) throws PluginException {
        try {
            if (plugins.put(plugin.getPluginKey(), plugin) == null) {
                throw new PluginException.NoSuchPluginException();
            }
        } catch (Exception ex) {
            throw new PluginException.NoSuchPluginException();
        }
    }

    /**
     * Remove a registered Storage plugin
     *
     * @param plugin an implementation of StoragePlugin
     */
    @Override
    public void removePlugin(@NonNull StoragePlugin plugin) throws PluginException {
        if (plugins.remove(plugin.getPluginKey()) == null) {
            throw new PluginException.NoSuchPluginException();
        }
    }

    /**
     * Retrieve a registered Storage plugin.
     *
     * @param pluginKey the key that identifies the plugin implementation
     * @return the Storage plugin object
     */
    @Override
    public StoragePlugin getPlugin(@NonNull String pluginKey) throws PluginException {
        if (plugins.containsKey(pluginKey)) {
            return plugins.get(pluginKey);
        } else {
            throw new PluginException.NoSuchPluginException();
        }
    }

    /**
     * @return the set of plugins added to a Category.
     */
    @Override
    public Set<StoragePlugin> getPlugins() {
        return new HashSet<StoragePlugin>(plugins.values());
    }

    /**
     * Retrieve the Storage category type enum
     *
     * @return enum that represents Storage category
     */
    @Override
    public final CategoryType getCategoryType() {
        return CategoryType.STORAGE;
    }
}
