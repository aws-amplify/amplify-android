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
 * Class that holds the gender detection results
 * for the predictions category.
 */
public final class GenderAttribute {
    private final GenderType gender;
    private final Float score;

    private GenderAttribute(
            @NonNull GenderType gender,
            @NonNull Float score
    ) {
        this.gender = gender;
        this.score = score;
    }

    /**
     * Gets the detected gender type.
     * @return the gender
     */
    @NonNull
    public GenderType getGender() {
        return gender;
    }

    /**
     * Gets the confidence score of detection.
     * @return the confidence score
     */
    @NonNull
    public Float getScore() {
        return score;
    }

    /**
     * Gets a builder to construct an attribute.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GenderAttribute}.
     */
    public static class Builder {
        private GenderType gender;
        private Float score;

        /**
         * Sets the gender and return this builder.
         * @param gender the gender
         * @return this builder instance
         */
        @NonNull
        public Builder gender(@NonNull GenderType gender) {
            this.gender = Objects.requireNonNull(gender);
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
         * Constructs a new instance of {@link GenderAttribute}
         * using the values assigned to this builder.
         * @return An instance of {@link GenderAttribute}
         */
        @NonNull
        public GenderAttribute build() {
            return new GenderAttribute(
                    Objects.requireNonNull(gender),
                    Objects.requireNonNull(score)
            );
        }
    }
}
