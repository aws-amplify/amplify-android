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

/**
 * Enum that declares the various categories of APIs supported by Amplify
 * System + their config keys. We don't expect the config keys to be
 * very different from the value of enum's name(), but they are logically
 * different since they are mandated by the config spec, not Java.
 */
public enum CategoryType {

    /**
     * Analytics track your app's operational status and customer
     * engagement, recording to an AWS backend service.
     */
    ANALYTICS("Analytics"),

    /**
     * API simplifies interactions with a remove AWS backend via REST
     * And GraphQL operations.
     */
    API("API"),

    /**
     * Hub is an event bus style pub/sub system that is used to
     * communicate state inside and outside of the Amplify framework.
     * This category is expected to operate locally to the device,
     * without talking to AWS backend services, directly.
     */
    HUB("Hub"),

    /**
     * Logging for troubleshooting of component behaviors during
     * development, or when deployed in production.  This category is
     * expected to operate locally to the device, without talking to AWS
     * backend services, directly.
     */
    LOGGING("Logging"),

    /**
     * Storage is an interface to a remote repository to store and
     * retrieve instances of domain models. AWS provides several backend
     * systems that are suitable for storage of your data.
     */
    STORAGE("Storage");

    /**
     * The key this category is listed under in the config JSON.
     */
    private final String configurationKey;

    /**
     * Construct the enum with the config key.
     * @param configurationKey The key this category is listed under in the config JSON.
     */
    CategoryType(final String configurationKey) {
        this.configurationKey = configurationKey;
    }

    /**
     * Returns the key this category is listed under in the config JSON.
     * @return The key as a string
     */
    public String getConfigurationKey() {
        return configurationKey;
    }
}

