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
 * Stores detection attribute where the result
 * is a boolean value. Its attribute type is flexible
 * and each instance must be provided with a specific
 * name for identification.
 */
public final class BooleanAttribute extends Attribute<Boolean> {
    private final String type;

    private BooleanAttribute(final Builder builder) {
        super(builder);
        this.type = builder.getType();
    }

    @Override
    @NonNull
    public String getType() {
        return type;
    }

    /**
     * Gets a builder to construct an attribute.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BooleanAttribute}.
     */
    public static final class Builder extends Attribute.Builder<Builder, BooleanAttribute, Boolean> {
        private String type;

        /**
         * Sets the attribute type and return this builder.
         * @param type the type of attribute
         * @return this builder instance
         */
        @NonNull
        public Builder type(@NonNull String type) {
            this.type = Objects.requireNonNull(type);
            return this;
        }

        @Override
        @NonNull
        public BooleanAttribute build() {
            return new BooleanAttribute(this);
        }

        @NonNull
        String getType() {
            return Objects.requireNonNull(type);
        }
    }
}
