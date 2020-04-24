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

package com.amplifyframework.predictions.models;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Stores detected feature where the result
 * is a boolean value. Its feature type is flexible
 * and each instance must be provided with a specific
 * name for identification.
 */
public final class BinaryFeature extends Feature<Boolean> {
    private final String type;

    private BinaryFeature(final Builder builder) {
        super(builder);
        this.type = builder.getType();
    }

    @Override
    @NonNull
    public String getTypeAlias() {
        return type;
    }

    /**
     * Gets a builder to construct an feature.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BinaryFeature}.
     */
    public static final class Builder extends Feature.Builder<Builder, BinaryFeature, Boolean> {
        private String type;

        /**
         * Sets the feature type and return this builder.
         * @param type the type of feature
         * @return this builder instance
         */
        @NonNull
        public Builder type(@NonNull String type) {
            this.type = Objects.requireNonNull(type);
            return this;
        }

        @Override
        @NonNull
        public BinaryFeature build() {
            return new BinaryFeature(this);
        }

        @NonNull
        String getType() {
            return Objects.requireNonNull(type);
        }
    }
}
