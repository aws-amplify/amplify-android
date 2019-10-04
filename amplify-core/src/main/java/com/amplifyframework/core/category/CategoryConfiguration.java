package com.amplifyframework.core.category;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CategoryConfiguration {
    /** Map of the { pluginKey => pluginConfiguration } object */
    public Map<String, Object> pluginConfigs;

    public CategoryConfiguration() {
        pluginConfigs = new ConcurrentHashMap<String, Object>();
    }
}
