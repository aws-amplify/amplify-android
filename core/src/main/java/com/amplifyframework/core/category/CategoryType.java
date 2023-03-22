/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import androidx.annotation.NonNull;

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
    ANALYTICS("analytics"),

    /**
     * API simplifies interactions with a remote backend via REST
     * and GraphQL operations.
     */
    API("api"),

    /**
     * Provides user authentication and authorization functionality.
     */
    AUTH("auth"),

    /**
     * DataStore simplifies local storage of your application data on the
     * device for offline access and automatically synchronizes data with
     * the cloud.
     */
    DATASTORE("dataStore"),

    /**
     * Hub is an event bus style pub/sub system that is used to
     * communicate state inside and outside of the Amplify framework.
     * This category is expected to operate locally to the device,
     * without talking to the cloud backend services, directly.
     */
    HUB("hub"),

    /**
     * Logging for troubleshooting of component behaviors during
     * development, or when deployed in production. This category is
     * expected to operate locally to the device, without talking to the
     * cloud backend services, directly.
     */
    LOGGING("logging"),

    /**
     * Notifications holds sub-categories to push remote/local
     * messages that can be displayed inside/outside of app's UI.
     */
    NOTIFICATIONS("notifications"),

    /**
     * Predictions use machine learning to convert text and/or identify
     * images using both online and offline trained models.
     */
    PREDICTIONS("predictions"),

    /**
     * Storage is an interface to a remote repository to store and
     * retrieve instances of domain models.
     */
    STORAGE("storage"),

    /**
     * Geo category provides an interface for maps and other location-aware
     * capabilities such as location search, routing and asset tracking.
     */
    GEO("geo");

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

    @NonNull
    @Override
    public String toString() {
        return configurationKey;
    }
}

