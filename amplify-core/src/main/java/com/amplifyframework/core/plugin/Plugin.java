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

package com.amplifyframework.core.plugin;

import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.core.category.CategoryTypeable;

import org.json.JSONObject;

/**
 * Interface that defines the contract that every plugin
 * in Amplify System will adhere to.
 * @param <E> The class type of the escape hatch used to circumvent the generic plugin APIs
 */
public interface Plugin<E> extends CategoryTypeable {

    /**
     * Gets a key which uniquely identifies the plugin instance.
     * @return the identifier that identifies the plugin implementation
     */
    String getPluginKey();

    /**
     * Configure the plugin with customized configuration object.
     * @param pluginConfiguration plugin-specific configuration data
     * @param context An Android Context
     * @throws PluginException when configuration for a plugin was not found
     */
    void configure(@NonNull JSONObject pluginConfiguration, Context context) throws PluginException;

    /**
     * Returns escape hatch for plugin to enable lower-level client use-cases.
     * @return the client used by category plugin
     */
    E getEscapeHatch();
}
