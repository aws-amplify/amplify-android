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

package com.amplifyframework.core.category;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class that a given category will extend in order to define the
 * various data required for successful configuration.
 */
public abstract class CategoryConfiguration implements CategoryTypeable {
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") // Configs will be populated by future work
    private static final String PLUGINS_KEY = "plugins";
    private final Map<String, JSONObject> pluginConfigs;

    /**
     * Constructs a new CategoryConfiguration.
     */
    public CategoryConfiguration() {
        pluginConfigs = new ConcurrentHashMap<>();
    }

    /**
     * Looks up a configuration for a particular plugin, based on the
     * plugin key.
     * @param key A plugin key, used to uniquely identify a plugin
     * @return An object used by a plugin, for its configuration. This
     *         value is possibly null
     */
    public final JSONObject getPluginConfig(final String key) {
        return pluginConfigs.get(key);
    }

    /**
     * Populates pluginConfigs map from JSON - each category should implement parsing any category specific properties
     * in their override of this method.
     * @param json Configuration data for this category
     * @throws JSONException if getJsonObject fails
     */
    public void populateFromJSON(JSONObject json) throws JSONException {
        if (json.has(PLUGINS_KEY)) {
            JSONObject plugins = json.getJSONObject(PLUGINS_KEY);
            Iterator<String> keys = plugins.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                pluginConfigs.put(key, plugins.getJSONObject(key));
            }
        }
    }
}
