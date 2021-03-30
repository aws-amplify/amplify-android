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

package com.amplifyframework.api.aws;

/**
 * Represents that different auth strategies supported by the client that
 * interfaces with the AppSync backend.
 */
public enum RequestAuthorizationStrategyType {
    /**
     * Uses the default authorization type from the API configuration
     * unless the incoming request specifies one.
     */
    DEFAULT("default"),

    /**
     * Leverages schema metadata to create a list of potential authorization types
     * that could be used for a given request. The underlying client then
     * iterates through that list until one of the modes succeeds or all of them fail.
     * This setting also respects authorization types set in the request.
     */
    MULTIAUTH("multiauth");

    private final String strategyName;

    RequestAuthorizationStrategyType(String strategyName) {
        this.strategyName = strategyName;
    }

    /**
     * Retrieve the strategy enum based on a name.
     * Comparison is case insensitive.
     * @param value The string value to try to match.
     * @return One of the enum items or an exception if one is not found.
     */
    public RequestAuthorizationStrategyType from(String value) {
        for (RequestAuthorizationStrategyType strategy : RequestAuthorizationStrategyType.values()) {
            if (strategy.name().equalsIgnoreCase(value) || strategy.strategyName.equalsIgnoreCase(value)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("Cannot find an authorization strategy for " + value);
    }
}
