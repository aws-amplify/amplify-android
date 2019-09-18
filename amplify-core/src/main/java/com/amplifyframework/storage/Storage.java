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

import android.content.Context;
import android.support.annotation.NonNull;

import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.exception.ConfigurationException;
import com.amplifyframework.core.plugin.PluginException;
import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.storage.exception.*;
import com.amplifyframework.storage.operation.*;
import com.amplifyframework.storage.option.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Storage implements Category, StorageCategoryBehavior {
    private Map<String, StoragePlugin> plugins = new HashMap<>();
    private StoragePlugin plugin;

    private boolean isConfigured = false;

    @Override
    public StorageGetOperation get(@NonNull String key, StorageGetOption option) throws StorageGetException {
        assert isConfigured;
        return plugin.get(key, option);
    }

    @Override
    public StoragePutOperation put(@NonNull String key, @NonNull File file, StoragePutOption option) throws StoragePutException {
        assert isConfigured;
        return plugin.put(key, file, option);
    }

    @Override
    public StoragePutOperation put(@NonNull String key, @NonNull String path, StoragePutOption option) throws StoragePutException {
        assert isConfigured;
        return plugin.put(key, path, option);
    }

    @Override
    public StorageListOperation list(StorageListOption option) throws StorageListException {
        assert isConfigured;
        return plugin.list(option);
    }

    @Override
    public StorageRemoveOperation remove(@NonNull String key, StorageRemoveOption option) throws StorageRemoveException {
        assert isConfigured;
        return plugin.remove(key, option);
    }

    @Override
    public void configure(@NonNull Context context) throws ConfigurationException, PluginException {
        configure(context, ""); //TODO: REPLACE WITH REAL DEFAULT PARAMETER
    }

    @Override
    public void configure(@NonNull Context context, @NonNull String environment) throws ConfigurationException, PluginException {
        if (isConfigured) {
            throw new ConfigurationException.AmplifyAlreadyConfiguredException();
        }
        if (plugins.size() == 1) {
            plugin = (StoragePlugin) plugins.values().toArray()[0];
        } else {
            //TODO: Set up a selector
        }
        isConfigured = true;
    }

    @Override
    public void addPlugin(@NonNull Plugin plugin) throws PluginException {
        if (plugin.getPluginKey() == null || plugin.getPluginKey().isEmpty()) {
            throw new PluginException.EmptyKeyException();
        }
        if (plugin instanceof StoragePlugin) {
            plugins.put(plugin.getPluginKey(), (StoragePlugin) plugin);
        } else {
            throw new PluginException.MismatchedPluginException();
        }
    }

    @Override
    public void removePlugin(@NonNull String pluginKey) {
        plugins.remove(pluginKey);
    }

    @Override
    public void reset() {
        //TODO: Implement
    }

    @Override
    public StoragePlugin getPlugin(@NonNull String pluginKey) throws PluginException {
        if (plugins.containsKey(pluginKey)) {
            return plugins.get(pluginKey);
        } else {
            throw new PluginException.NoSuchPluginException();
        }
    }

    @Override
    public Map<String, StoragePlugin> getPlugins() {
        return plugins;
    }

    @Override
    public CategoryType getCategoryType() {
        return CategoryType.STORAGE;
    }
}
