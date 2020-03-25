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

/**
 * Holds the entity detection results from text
 * for the predictions category.
 */
public final class TextEntity extends TextFeature<EntityType> {
    /**
     * Feature type for {@link TextEntity}.
     */
    public static final String FEATURE_TYPE = EntityType.class.getSimpleName();

    private TextEntity(final Builder builder) {
        super(builder);
    }

    @Override
    @NonNull
    public String getType() {
        return FEATURE_TYPE;
    }

    /**
     * Gets a builder to construct a entity feature.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TextEntity}.
     */
    public static final class Builder extends TextFeature.Builder<Builder, TextEntity, EntityType> {
        @Override
        @NonNull
        public TextEntity build() {
            return new TextEntity(this);
        }
    }
}
