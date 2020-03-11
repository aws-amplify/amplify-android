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
 * Metadata to store details of an entity.
 */
public final class EntityMetadata {
    private final Pose pose;
    private final Float score;

    private EntityMetadata(
            @NonNull Pose pose,
            @NonNull Float score
    ) {
        this.pose = pose;
        this.score = score;
    }

    /**
     * Gets the detected pose of this entity.
     * @return the pose
     */
    @NonNull
    public Pose getPose() {
        return pose;
    }

    /**
     * Gets the confidence score of this entity pose.
     * @return the confidence score
     */
    @NonNull
    public Float getScore() {
        return score;
    }

    /**
     * Gets a builder instance for entity metadata.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link EntityMetadata}.
     */
    public static class Builder {
        private Pose pose;
        private Float score;

        /**
         * Sets the pose and return this builder.
         * @param pose the pose
         * @return this builder instance
         */
        @NonNull
        public Builder pose(@NonNull Pose pose) {
            this.pose = Objects.requireNonNull(pose);
            return this;
        }

        /**
         * Sets the score and return this builder.
         * @param score the score
         * @return this builder instance
         */
        @NonNull
        public Builder score(@NonNull Float score) {
            this.score = Objects.requireNonNull(score);
            return this;
        }

        /**
         * Construct a new instance of {@link EntityMetadata}
         * with the values assigned to this builder instance.
         * @return An instance of {@link EntityMetadata}
         */
        @NonNull
        public EntityMetadata build() {
            return new EntityMetadata(
                    Objects.requireNonNull(pose),
                    Objects.requireNonNull(score)
            );
        }
    }
}
