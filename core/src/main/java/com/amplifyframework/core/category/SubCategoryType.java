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
public enum SubCategoryType {
    /**
     * Push notifications.
     */
    PUSH_NOTIFICATIONS("push"),

    /**
     * In-App messaging.
     */
    INAPP_MESSAGING("inapp_messaging");

    /**
     * The key this category is listed under in the config JSON.
     */
    private final String configurationKey;

    /**
     * Construct the enum with the config key.
     * @param configurationKey The key this category is listed under in the config JSON.
     */
    SubCategoryType(final String configurationKey) {
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
