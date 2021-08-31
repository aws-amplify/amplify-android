/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.geo.options;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Stores options to use when fetching map style descriptor data.
 */
public class GetMapStyleDescriptorOptions {
    private final String mapName;

    private GetMapStyleDescriptorOptions(Builder builder) {
        this.mapName = builder.mapName;
    }

    /**
     * Returns the name of map resource to fetch style descriptor for.
     *
     * @return the name of map resource
     */
    @Nullable
    public String getMapName() {
        return mapName;
    }

    /**
     * Returns a new builder instance for constructing {@link GetMapStyleDescriptorOptions}.
     *
     * @return a new builder instance.
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for conveniently constructing options instance.
     */
    public static class Builder {
        private String mapName;

        private Builder() {}

        /**
         * Sets the name of map resource and returns itself.
         *
         * @param mapName the name of map resource.
         * @return this builder instance.
         */
        @NonNull
        public Builder mapName(@NonNull String mapName) {
            this.mapName = Objects.requireNonNull(mapName);
            return this;
        }

        /**
         * Constructs options instance with the parameters in this builder.
         *
         * @return Immutable options instance.
         */
        @NonNull
        public GetMapStyleDescriptorOptions build() {
            return new GetMapStyleDescriptorOptions(this);
        }
    }
}
