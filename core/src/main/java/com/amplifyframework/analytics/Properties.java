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

package com.amplifyframework.analytics;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper for {@link Map} to restrict the types that may be passed in.
 */
public abstract class Properties {
    private Map<String, Property<?>> properties;

    /**
     * Properties constructor that initializes the underlying map.
     */
    public Properties() {
        properties = new HashMap<>();
    }

    /**
     * Allows adding {@link Property} of type {@link T}.
     * @param name property name
     * @param property property
     * @param <T> property type
     */
    public <T> void add(String name, Property<T> property) {
        properties.put(name, property);
    }

    /**
     * Returns the underlying property map.
     * @return underlying properties map
     */
    public final Map<String, Property<?>> get() {
        return properties;
    }
}
