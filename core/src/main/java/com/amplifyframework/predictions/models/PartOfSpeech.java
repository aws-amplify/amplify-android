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
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Class that holds the speech type detection
 * results for the predictions category.
 */
public final class PartOfSpeech {
    private final SpeechType tag;
    private final Float score;

    private PartOfSpeech(
            @NonNull SpeechType tag,
            @Nullable Float score
    ) {
        this.tag = tag;
        this.score = score;
    }

    /**
     * Gets the speech type tag.
     * @return the speech type
     */
    @NonNull
    public SpeechType getTag() {
        return tag;
    }

    /**
     * Gets the confidence score of speech type
     * detection result.
     * @return the confidence score
     */
    @Nullable
    public Float getScore() {
        return score;
    }

    /**
     * Gets the builder instance to help construct
     * a part of speech object.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to help easily construct an instance
     * of {@link PartOfSpeech}.
     */
    public static class Builder {
        private SpeechType tag;
        private Float score;

        /**
         * Sets the speech type tag and return this builder.
         * @param tag the speech type
         * @return this builder instance
         */
        @NonNull
        public Builder tag(@NonNull SpeechType tag) {
            this.tag = Objects.requireNonNull(tag);
            return this;
        }

        /**
         * Sets the confidence score and return this builder.
         * @param score the confidence score
         * @return this builder instance
         */
        @NonNull
        public Builder score(@Nullable Float score) {
            this.score = score;
            return this;
        }

        /**
         * Creates a new instance of {@link PartOfSpeech} with
         * the properties of this builder instance.
         * @return An instance of {@link PartOfSpeech}
         */
        @NonNull
        public PartOfSpeech build() {
            return new PartOfSpeech(
                    Objects.requireNonNull(tag),
                    score
            );
        }
    }
}
