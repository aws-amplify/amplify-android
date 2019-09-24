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
import android.support.annotation.NonNull;

import com.amplifyframework.core.category.CategoryTypeable;

/**
 * Interface that defines the contract that every plugin
 * in Amplify System will adhere to.
 */
public interface Plugin<C> extends CategoryTypeable {
    /**
     * @return the identifier that identifies
     *         the plugin implementation
     */
    String getPluginKey();

    /**
     * Configure the Plugin with the configuration passed.
     *
     * @param pluginConfiguration configuration for the plugin
     * @throws PluginException when configuration for a plugin was not found
     */
    void configure(@NonNull C pluginConfiguration) throws PluginException;

    /**
     * Configure the Plugin using the details in amplifyconfiguration.json
     *
     * @param context Android context required to read the contents of file
     * @throws PluginException when configuration for a plugin was not found
     */
    void configure(@NonNull Context context, @NonNull String environment) throws PluginException;

    /**
     * Reset the plugin to the state where it's not configured.
     */
    void reset();
}
