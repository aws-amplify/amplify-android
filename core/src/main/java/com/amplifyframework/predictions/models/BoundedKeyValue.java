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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds the key-value detection results
 * for the predictions category.
 */
public final class BoundedKeyValue extends ImageFeature<Map.Entry<String, String>> {
    /**
     * Feature type for {@link BoundedKeyValue}.
     */
    public static final String FEATURE_TYPE = BoundedKeyValue.class.getSimpleName();

    private BoundedKeyValue(final Builder builder) {
        super(builder);
    }

    @Override
    @NonNull
    public String getType() {
        return FEATURE_TYPE;
    }

    /**
     * Gets the detected key.
     * @return the key
     */
    @NonNull
    public String getKey() {
        return getFeature().getKey();
    }

    /**
     * Gets the detected value.
     * @return the value
     */
    @NonNull
    public String getValue() {
        return getFeature().getValue();
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
            extends ImageFeature.Builder<Builder, BoundedKeyValue, Map.Entry<String, String>> {
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
            return feature(new HashMap.SimpleEntry<>(key, value));
        }

        /**
         * Constructs a new instance of {@link BoundedKeyValue} using
         * the values assigned to this builder instance.
         * @return an instance of {@link BoundedKeyValue}
         */
        @NonNull
        public BoundedKeyValue build() {
            return new BoundedKeyValue(this);
        }
    }
}
