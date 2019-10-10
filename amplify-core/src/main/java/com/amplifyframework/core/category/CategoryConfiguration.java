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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class that a given category will extend in order to define the
 * various data required for successful configuration.
 */
public abstract class CategoryConfiguration {
    private Map<String, Object> pluginConfigs;

    /**
     * Constructs a new CategoryConfiguration.
     */
    public CategoryConfiguration() {
        pluginConfigs = new ConcurrentHashMap<String, Object>();
    }

    /**
     * Looks up a configuration for a particular plugin, based on the
     * plugin key.
     * @param key A plugin key, used to uniquely identify a plugin
     * @return An object used by a plugin, for its configuration. This
     *         value is possibly null
     */
    public final Object getPluginConfig(final String key) {
        return pluginConfigs.get(key);
    }
}
