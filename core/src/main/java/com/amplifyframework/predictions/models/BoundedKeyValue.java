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
import androidx.core.util.Pair;

import java.util.Objects;

/**
 * Holds the key-value detection results
 * for the predictions category.
 */
public final class BoundedKeyValue extends ImageFeature<Pair<String, String>> {

    private BoundedKeyValue(final Builder builder) {
        super(builder);
    }

    @Override
    @NonNull
    public String getTypeAlias() {
        return FeatureType.BOUNDED_KEY_VALUE.getAlias();
    }

    /**
     * Gets the detected key.
     * @return the key
     */
    @NonNull
    @SuppressWarnings("ConstantConditions") // Checked for non-null in builder
    public String getKey() {
        return getValue().first;
    }

    /**
     * Gets the detected value.
     * @return the value
     */
    @NonNull
    @SuppressWarnings("ConstantConditions") // Checked for non-null in builder
    public String getKeyValue() {
        return getValue().second;
    }

    /**
     * Gets a builder to help easily construct
     * a bounded key-result object.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BoundedKeyValue}.
     */
    public static final class Builder
            extends ImageFeature.Builder<Builder, BoundedKeyValue, Pair<String, String>> {
        /**
         * Sets the key-value pair and return this builder.
         * @param key the key
         * @param value the value
         * @return this builder instance
         */
        @NonNull
        public Builder keyValue(@NonNull String key, @NonNull String value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            return value(new Pair<>(key, value));
        }

        /**
         * Constructs a new instance of {@link BoundedKeyValue} using
         * the values assigned to this builder instance.
         * @return an instance of {@link BoundedKeyValue}
         */
        @NonNull
        public BoundedKeyValue build() {
            Objects.requireNonNull(getValue().first);
            Objects.requireNonNull(getValue().second);
            return new BoundedKeyValue(this);
        }
    }
}
