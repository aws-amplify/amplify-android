package com.amplifyframework.analytics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Properties {
    protected Map<String, Property<?>> properties;
    public Properties(){
        properties = new HashMap<>();
    }

    // A generic way to add extensions
    public <T> void add(String name, Property<T> property) {
        properties.put(name, property);
    }

    public Map<String, Property<?>> get() {
        return properties;
    }
}
