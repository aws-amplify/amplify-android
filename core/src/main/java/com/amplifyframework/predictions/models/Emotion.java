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
 * Class that holds the emotion detection results
 * for the predictions category.
 */
public final class Emotion {
    private final EmotionType emotion;
    private final Float score;

    private Emotion(
            @NonNull EmotionType emotion,
            @NonNull Float score
    ) {
        this.emotion = emotion;
        this.score = score;
    }

    /**
     * Gets the detected emotion type.
     * @return the emotion
     */
    @NonNull
    public EmotionType getEmotion() {
        return emotion;
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
     * Builder for {@link Emotion}.
     */
    public static class Builder {
        private EmotionType emotion;
        private Float score;

        /**
         * Sets the emotion and return this builder.
         * @param emotion the emotion
         * @return this builder instance
         */
        @NonNull
        public Builder emotion(@NonNull EmotionType emotion) {
            this.emotion = Objects.requireNonNull(emotion);
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
         * Constructs a new instance of {@link Emotion}
         * using the values assigned to this builder.
         * @return An instance of {@link Emotion}
         */
        @NonNull
        public Emotion build() {
            return new Emotion(
                    Objects.requireNonNull(emotion),
                    Objects.requireNonNull(score)
            );
        }
    }
}
