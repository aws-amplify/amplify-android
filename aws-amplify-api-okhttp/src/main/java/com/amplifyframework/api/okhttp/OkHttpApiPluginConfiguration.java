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

package com.amplifyframework.api.okhttp;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration bundle for the OkHttp API plugin.
 */
public final class OkHttpApiPluginConfiguration {

    /** Map of API name -> API details. */
    private final Map<String, ApiConfiguration> apiDetails;

    /**
     * Constructs custom configuration for OkHttp API Plugin.
     */
    public OkHttpApiPluginConfiguration() {
        apiDetails = new HashMap<>();
    }

    /**
     * Adds API configuration with a unique name.
     *
     * @param name API name
     * @param api API configuration to be associated with name
     */
    public void addApi(@NonNull String name, @NonNull ApiConfiguration api) {
        apiDetails.put(name, api);
    }

    /**
     * Gets API configuration associated with given name.
     *
     * @param name API name
     * @return API configuration associated with name
     */
    public ApiConfiguration getApi(@NonNull String name) {
        return apiDetails.get(name);
    }

    /**
     * Gets every API in a map.
     *
     * @return list of every API configurations
     */
    public Map<String, ApiConfiguration> getApis() {
        return apiDetails;
    }
}
