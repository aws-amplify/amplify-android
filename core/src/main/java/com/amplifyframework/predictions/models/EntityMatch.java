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
 * Holds the entity match results for the predictions category.
 * Given a source image, each entity will be assigned a match
 * with known sources of external image identifiers.
 */
public final class EntityMatch extends ImageFeature<String> {
    private EntityMatch(final Builder builder) {
        super(builder);
    }

    @Override
    @NonNull
    public String getTypeAlias() {
        return FeatureType.ENTITY_MATCH.getAlias();
    }

    /**
     * Gets the ID of the image that this entity
     * was matched with.
     * @return the external image's ID
     */
    @NonNull
    public String getExternalImageId() {
        return getValue();
    }

    /**
     * Gets the builder to help easily construct the
     * result of matching entities.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link EntityMatch}.
     */
    public static final class Builder extends ImageFeature.Builder<Builder, EntityMatch, String> {
        /**
         * Sets the image ID and return this builder.
         * @param externalImageId the ID of external image being matched against
         * @return this builder instance
         */
        @NonNull
        public Builder externalImageId(@NonNull String externalImageId) {
            return value(Objects.requireNonNull(externalImageId));
        }

        /**
         * Construct a new instance of {@link EntityMatch} using
         * the values assigned to this builder instance.
         * @return An instance of {@link EntityMatch}
         */
        @NonNull
        public EntityMatch build() {
            return new EntityMatch(this);
        }
    }
}
