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

import android.support.annotation.NonNull;

import com.amplifyframework.core.category.CategoryTypable;

import org.json.JSONObject;

/**
 * Interface that defines the contract that every plugin
 * in Amplify System will adhere to.
 */
public interface Plugin extends CategoryTypable {
    /**
     * @return the identifier that identifies
     *         the plugin implementation
     */
    String getPluginKey();

    /**
     * Configure the Plugin with the configuration passed via
     * the JSONObject.
     *
     * @param jsonObject configuration passed via the JSONObject
     */
    void configure(@NonNull JSONObject jsonObject);

    /**
     * Configure the Plugin with the configuration passed via
     * the JSONObject.
     *
     * @param jsonObject configuration passed via the JSONObject
     * @param key the identifier that identifies the plugin implementation
     */
    void configure(@NonNull JSONObject jsonObject, @NonNull String key);

    /**
     * Reset the plugin to the state where it's not configured.
     */
    void reset();

    /**
     * Initialize the plugin with the configuration passed.
     * @param jsonObject configuration for the plugin
     * @return the plugin object configured
     */
    Plugin initWithConfiguration(@NonNull JSONObject jsonObject);

    /**
     * Initialize the plugin with the configuration passed.
     * @param jsonObject configuration for the plugin
     * @param key the identifier that identifies the plugin implementation
     * @return the plugin object configured
     */
    Plugin initWithConfiguration(@NonNull JSONObject jsonObject, @NonNull String key);
}
