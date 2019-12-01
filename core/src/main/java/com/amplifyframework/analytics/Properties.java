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

import com.amplifyframework.core.Immutable;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper for {@link Map} to restrict the types that may be passed in.
 */
public class Properties {

    /**
     * Map to hold analytics properties.
     */
    @SuppressWarnings("VisibilityModifier")
    protected final Map<String, Property<?>> properties;

    /**
     * Properties constructor that initializes the underlying map.
     */
    public Properties() {
        properties = new HashMap<>();
    }

    /**
     * Returns the underlying property map.
     * @return underlying properties map
     */
    public final Map<String, Property<?>> get() {
        return Immutable.of(properties);
    }
}
