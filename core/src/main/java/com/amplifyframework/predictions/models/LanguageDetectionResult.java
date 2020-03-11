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
 * Class that holds the language detection results for a
 * string of text for the predictions category.
 */
public final class LanguageDetectionResult {
    private final LanguageType language;
    private final Float score;

    private LanguageDetectionResult(
            @NonNull LanguageType language,
            @Nullable Float score
    ) {
        this.language = language;
        this.score = score;
    }

    /**
     * Gets the detected language.
     * @return the detected language
     */
    @NonNull
    public LanguageType getLanguage() {
        return language;
    }

    /**
     * Gets the confidence score of the detection.
     * @return the confidence score
     */
    @Nullable
    public Float getScore() {
        return score;
    }

    /**
     * Gets the builder instance to help instantiate
     * Language detection result object.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder instance to help easily construct an
     * instance of {@link LanguageDetectionResult}.
     */
    public static class Builder {
        private LanguageType language;
        private Float score;

        /**
         * Sets the detected language and return this builder.
         * @param language the detected language type
         * @return this builder instance
         */
        @NonNull
        public Builder language(@NonNull LanguageType language) {
            this.language = Objects.requireNonNull(language);
            return this;
        }

        /**
         * Sets the confidence score and return this builder.
         * @param score the confidence score of the language detection
         * @return this builder instance
         */
        @NonNull
        public Builder score(@Nullable Float score) {
            this.score = score;
            return this;
        }

        /**
         * Construct an instance of {@link LanguageDetectionResult} with
         * the values assigned to this builder.
         * @return An instance of {@link LanguageDetectionResult}
         */
        @NonNull
        public LanguageDetectionResult build() {
            return new LanguageDetectionResult(
                    Objects.requireNonNull(language),
                    score
            );
        }
    }
}
