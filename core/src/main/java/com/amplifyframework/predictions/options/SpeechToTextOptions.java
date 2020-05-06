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

package com.amplifyframework.predictions.options;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.async.Options;
import com.amplifyframework.predictions.models.LanguageType;

/**
 * Options for text transcription operation.
 */
public final class SpeechToTextOptions implements Options {
    private final LanguageType language;

    private SpeechToTextOptions(final Builder builder) {
        this.language = builder.getLanguage();
    }

    /**
     * Gets the language of the audio to transcribe.
     * Null if not specified.
     * @return the audio language
     */
    @Nullable
    public LanguageType getLanguage() {
        return language;
    }

    /**
     * Creates an instance of options with default values assigned.
     * @return Default instance of options
     */
    public static SpeechToTextOptions defaults() {
        return new SpeechToTextOptions(builder());
    }

    /**
     * Gets a new builder for this options.
     * @return new builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SpeechToTextOptions}.
     */
    public static final class Builder {
        private LanguageType language;

        /**
         * Sets the language of the audio input and return
         * this builder.
         * @param language the audio language
         * @return this builder instance
         */
        @NonNull
        public Builder voiceType(@Nullable LanguageType language) {
            this.language = language;
            return this;
        }

        /**
         * Constructs a new {@link SpeechToTextOptions} with
         * the values assigned to this builder.
         * @return the new options instance
         */
        @NonNull
        public SpeechToTextOptions build() {
            return new SpeechToTextOptions(this);
        }

        LanguageType getLanguage() {
            return language;
        }
    }
}
