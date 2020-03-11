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
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Class that holds the entity detection results for a
 * string of text for the predictions category.
 */
public final class EntityDetectionResult {
    private final EntityType type;
    private final String targetText;
    private final Integer startIndex;
    private final Integer endIndex;
    private final Float score;

    private EntityDetectionResult(
            @NonNull EntityType type,
            @NonNull String targetText,
            @NonNull Integer startIndex,
            @NonNull Integer endIndex,
            @Nullable Float score
    ) {
        this.type = type;
        this.targetText = targetText;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.score = score;
    }

    /**
     * Gets the entity type.
     * @return the entity type
     */
    @NonNull
    public EntityType getType() {
        return type;
    }

    /**
     * Gets the target text of the entity.
     * @return the target text
     */
    @NonNull
    public String getTargetText() {
        return targetText;
    }

    /**
     * Gets the start index of the entity.
     * @return the start index
     */
    @NonNull
    public Integer getStartIndex() {
        return startIndex;
    }

    /**
     * Gets the end index of the entity.
     * @return the end index
     */
    @NonNull
    public Integer getEndIndex() {
        return endIndex;
    }

    /**
     * Gets the confidence score of the result.
     * @return the confidence score
     */
    @Nullable
    public Float getScore() {
        return score;
    }

    /**
     * Gets a builder instance to construct the result.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class to easily construct the result.
     */
    public static class Builder {
        private EntityType type;
        private String targetText;
        private Integer startIndex;
        private Integer endIndex;
        private Float score;

        /**
         * Sets the entity type and return this builder.
         * @param type the type of this entity
         * @return this builder instance
         */
        @NonNull
        public Builder type(@NonNull EntityType type) {
            this.type = Objects.requireNonNull(type);
            return this;
        }

        /**
         * Sets the target text and return this builder.
         * @param targetText the target text
         * @return this builder instance
         */
        @NonNull
        public Builder targetText(@NonNull String targetText) {
            this.targetText = Objects.requireNonNull(targetText);
            return this;
        }

        /**
         * Sets the starting index and return this builder.
         * @param startIndex the starting index of the entity
         * @return this builder instance
         */
        @NonNull
        public Builder startIndex(@NonNull Integer startIndex) {
            this.startIndex = Objects.requireNonNull(startIndex);
            return this;
        }

        /**
         * Sets the last index and return this builder.
         * @param endIndex the last index of the entity
         * @return this builder instance
         */
        @NonNull
        public Builder endIndex(@NonNull Integer endIndex) {
            this.endIndex = Objects.requireNonNull(endIndex);
            return this;
        }

        /**
         * Sets the confidence score and return this builder.
         * @param score the confidence score of the result
         * @return this builder instance
         */
        @NonNull
        public Builder score(@Nullable Float score) {
            this.score = score;
            return this;
        }

        /**
         * Constructs a new instance {@link EntityDetectionResult} with
         * the given properties.
         * @return An instance of {@link EntityDetectionResult}
         */
        @NonNull
        public EntityDetectionResult build() {
            return new EntityDetectionResult(
                    Objects.requireNonNull(type),
                    Objects.requireNonNull(targetText),
                    Objects.requireNonNull(startIndex),
                    Objects.requireNonNull(endIndex),
                    score
            );
        }
    }
}
