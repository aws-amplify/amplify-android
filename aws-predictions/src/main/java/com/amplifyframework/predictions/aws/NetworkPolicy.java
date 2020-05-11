/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.predictions.aws;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Determines the type of computing resource that will be used to
 * perform a Predictions operation.
 */
public enum NetworkPolicy {
    /**
     * Only use local resources to perform offline Predictions
     * operation.
     */
    OFFLINE("offline"),

    /**
     * Uses whichever resources are available to perform Predictions
     * operation.
     *
     * If both online and offline resources are available, then the
     * results from both will be combined for higher accuracy.
     */
    AUTO("auto");

    private final String configurationKey;

    NetworkPolicy(String configurationKey) {
        this.configurationKey = configurationKey;
    }

    /**
     * Attempts to find a NetworkPolicy which has the provided value as a configuration key.
     * @param configurationKey An arbitrary, possibly null string which might be a configuration
     *                         key for one of the enumerated NetworkPolicy -- or, maybe not
     * @return An enumerated NetworkPolicy, if there is a match
     * @throws IllegalArgumentException If there is not a match
     */
    @NonNull
    public static NetworkPolicy fromKey(@Nullable String configurationKey) {
        for (NetworkPolicy policy : values()) {
            if (policy.getConfigurationKey().equals(configurationKey)) {
                return policy;
            }
        }
        throw new IllegalArgumentException("There is no network " +
                "policy that matches the provided key.");
    }

    /**
     * Gets the JSON configuration key associated with the
     * Network Policy enum instance.
     * @return the JSON configuration key
     */
    @NonNull
    public String getConfigurationKey() {
        return configurationKey;
    }
}
