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

package com.amplifyframework.predictions.model;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Metadata to store the details of an entity match result.
 */
public final class EntityMatchMetadata {
    private final String externalImageId;
    private final Double similarity;

    private EntityMatchMetadata(
            @NonNull String externalImageId,
            @NonNull Double similarity
    ) {
        this.externalImageId = externalImageId;
        this.similarity = similarity;
    }

    /**
     * Gets the external image ID.
     * @return the external image ID
     */
    @NonNull
    public String getExternalImageId() {
        return externalImageId;
    }

    /**
     * Gets the similarity score.
     * @return the similarity score
     */
    @NonNull
    public Double getSimilarity() {
        return similarity;
    }

    /**
     * Gets an instance of builder.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link EntityMatchMetadata}.
     */
    public static class Builder {
        private String externalImageId;
        private Double similarity;

        /**
         * Sets the external image ID and return this builder.
         * @param externalImageId the external image ID
         * @return this builder instance
         */
        @NonNull
        public Builder externalImageId(@NonNull String externalImageId) {
            this.externalImageId = Objects.requireNonNull(externalImageId);
            return this;
        }

        /**
         * Sets the similarity score and return this builder.
         * @param similarity the similarity score
         * @return this builder instance
         */
        @NonNull
        public Builder similarity(@NonNull Double similarity) {
            this.similarity = Objects.requireNonNull(similarity);
            return this;
        }

        /**
         * Construct a new instance of {@link EntityMatchMetadata}
         * from the values assigned to this builder instance.
         * @return An instance of {@link EntityMatchMetadata}
         */
        @NonNull
        public EntityMatchMetadata build() {
            return new EntityMatchMetadata(
                    Objects.requireNonNull(externalImageId),
                    Objects.requireNonNull(similarity)
            );
        }
    }
}
