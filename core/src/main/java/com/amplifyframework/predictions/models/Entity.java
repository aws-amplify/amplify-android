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
 * Entity is categorization of a specific phrase
 * detected from input text.
 */
public final class Entity extends TextFeature<EntityType> {

    private Entity(final Builder builder) {
        super(builder);
    }

    @Override
    @NonNull
    public String getType() {
        return FeatureType.ENTITY.getFeatureName();
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
     * Builder for {@link Entity}.
     */
    public static final class Builder extends TextFeature.Builder<Builder, Entity, EntityType> {
        @Override
        @NonNull
        public Entity build() {
            return new Entity(this);
        }
    }
}
