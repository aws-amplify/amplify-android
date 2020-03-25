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

import android.graphics.Rect;
import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Holds the entity match results
 * for the predictions category.
 */
public final class EntityMatch {
    private final Rect boundingBox;
    private final EntityMatchMetadata metadata;

    private EntityMatch(
            @NonNull Rect boundingBox,
            @NonNull EntityMatchMetadata metadata
    ) {
        this.boundingBox = boundingBox;
        this.metadata = metadata;
    }

    /**
     * Gets the rectangular boundary.
     * @return the bounding box
     */
    @NonNull
    public Rect getBoundingBox() {
        return boundingBox;
    }

    /**
     * Gets the associated metadata.
     * @return the metadata
     */
    @NonNull
    public EntityMatchMetadata getMetadata() {
        return metadata;
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
    public static final class Builder {
        private Rect boundingBox;
        private EntityMatchMetadata metadata;

        /**
         * Sets the rectangular boundary and return this builder.
         * @param boundingBox the bounding box
         * @return this builder instance
         */
        @NonNull
        public Builder boundingBox(@NonNull Rect boundingBox) {
            this.boundingBox = Objects.requireNonNull(boundingBox);
            return this;
        }

        /**
         * Sets the metadata and return this builder.
         * @param metadata the metadata
         * @return this builder instance
         */
        @NonNull
        public Builder metadata(@NonNull EntityMatchMetadata metadata) {
            this.metadata = Objects.requireNonNull(metadata);
            return this;
        }

        /**
         * Construct a new instance of {@link EntityMatch} using
         * the values assigned to this builder instance.
         * @return An instance of {@link EntityMatch}
         */
        @NonNull
        public EntityMatch build() {
            return new EntityMatch(
                    Objects.requireNonNull(boundingBox),
                    Objects.requireNonNull(metadata)
            );
        }
    }
}
