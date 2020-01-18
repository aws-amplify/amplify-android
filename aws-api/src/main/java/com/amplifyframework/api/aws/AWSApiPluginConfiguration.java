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

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.util.Immutable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration bundle for the AWS API plugin.
 */
public final class AWSApiPluginConfiguration {
    private final Map<String, ApiConfiguration> apiDetails;

    private AWSApiPluginConfiguration(@NonNull Map<String, ApiConfiguration> apiDetails) {
        this.apiDetails = new HashMap<>();
        this.apiDetails.putAll(apiDetails);
    }

    /**
     * Gets API configuration associated with given name.
     * @param name API name
     * @return API configuration associated with name, null if there is no config
     */
    @Nullable
    ApiConfiguration getApi(@SuppressWarnings("SameParameterValue") @NonNull String name) {
        return apiDetails.get(name);
    }

    /**
     * Gets all of the API configurations.
     * @return list of every API configurations
     */
    @NonNull
    Map<String, ApiConfiguration> getApis() {
        return Immutable.of(apiDetails);
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        AWSApiPluginConfiguration that = (AWSApiPluginConfiguration) thatObject;

        return ObjectsCompat.equals(apiDetails, that.apiDetails);
    }

    @Override
    public int hashCode() {
        return apiDetails.hashCode();
    }

    /**
     * Gets a configuration builder instance.
     * @return A configuration builder instance
     */
    @SuppressLint("SyntheticAccessor")
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Constructs configuration objects through fluent method chaining.
     */
    public static final class Builder {
        private Map<String, ApiConfiguration> apiDetails;

        private Builder() {
            Builder.this.apiDetails = new HashMap<>();
        }

        /**
         * Adds an API spec to the configurations.
         * @param apiName Name of the API
         * @param apiConfiguration Configuration for the API
         * @return Current builder instance, for fluent method chaining
         */
        @SuppressWarnings("UnusedReturnValue")
        @NonNull
        Builder addApi(@NonNull String apiName, @NonNull ApiConfiguration apiConfiguration) {
            Objects.requireNonNull(apiName);
            Objects.requireNonNull(apiConfiguration);
            Builder.this.apiDetails.put(apiName, apiConfiguration);
            return this;
        }

        /**
         * Constructs a new instance of the AWSApiPluginConfiguration using the staged values
         * that have been provided to the current builder instance.
         * @return A new immutable AWSApiPluginConfiguration instance.
         */
        @SuppressLint("SyntheticAccessor")
        @NonNull
        public AWSApiPluginConfiguration build() {
            return new AWSApiPluginConfiguration(Builder.this.apiDetails);
        }
    }
}
