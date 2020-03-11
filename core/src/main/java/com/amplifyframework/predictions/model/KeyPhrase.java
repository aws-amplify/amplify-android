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
 * Class that holds the key phrase detection results for a
 * string of text for the predictions category.
 */
public final class KeyPhrase {
    private final String targetText;
    private final Integer startIndex;
    private final Integer endIndex;
    private final Float score;

    private KeyPhrase(
            @NonNull String targetText,
            @NonNull Integer startIndex,
            @NonNull Integer endIndex,
            @Nullable Float score
    ) {
        this.targetText = targetText;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.score = score;
    }

    /**
     * Gets the target text of key phrase.
     * @return the target text
     */
    @NonNull
    public String getTargetText() {
        return targetText;
    }

    /**
     * Gets the starting index of the key phrase.
     * @return the starting index
     */
    @NonNull
    public Integer getStartIndex() {
        return startIndex;
    }

    /**
     * Gets the last index of the key phrase.
     * @return the last index
     */
    @NonNull
    public Integer getEndIndex() {
        return endIndex;
    }

    /**
     * Gets the confidence score of the detection result.
     * @return the confidence score
     */
    @Nullable
    public Float getScore() {
        return score;
    }

    /**
     * Get a new builder instance to construct the key phrase.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to help easily construct a key phrase instance.
     */
    public static class Builder {
        private String targetText;
        private Integer startIndex;
        private Integer endIndex;
        private Float score;

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
         * @param startIndex the starting index of key phrase
         * @return this builder instance
         */
        @NonNull
        public Builder startIndex(@NonNull Integer startIndex) {
            this.startIndex = Objects.requireNonNull(startIndex);
            return this;
        }

        /**
         * Sets the last index and return this builder.
         * @param endIndex the last index of key phrase
         * @return this builder instance
         */
        @NonNull
        public Builder endIndex(@NonNull Integer endIndex) {
            this.endIndex = Objects.requireNonNull(endIndex);
            return this;
        }

        /**
         * Sets the confidence score and return this builder.
         * @param score the confidence score of this detection
         * @return this builder instance
         */
        @NonNull
        public Builder score(@Nullable Float score) {
            this.score = score;
            return this;
        }

        /**
         * Construct an instance of {@link KeyPhrase} with this
         * builder instance.
         * @return An instance of {@link KeyPhrase}
         */
        @NonNull
        public KeyPhrase build() {
            return new KeyPhrase(
                    Objects.requireNonNull(targetText),
                    Objects.requireNonNull(startIndex),
                    Objects.requireNonNull(endIndex),
                    score
            );
        }
    }
}
