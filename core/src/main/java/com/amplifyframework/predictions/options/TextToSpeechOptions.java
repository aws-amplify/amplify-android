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

/**
 * Options for text to speech conversion operation.
 */
public final class TextToSpeechOptions implements Options {
    private final String voiceType;

    private TextToSpeechOptions(final Builder builder) {
        this.voiceType = builder.getVoiceType();
    }

    /**
     * Gets the custom voice type if specified.
     * Null otherwise.
     * @return the custom voice type
     */
    @Nullable
    public String getVoiceType() {
        return voiceType;
    }

    /**
     * Creates an instance of options with default values assigned.
     * @return Default instance of options
     */
    @NonNull
    public static TextToSpeechOptions defaults() {
        return new TextToSpeechOptions(builder());
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
     * Builder for {@link TextToSpeechOptions}.
     */
    public static final class Builder {
        private String voiceType;

        /**
         * Sets the voice type of the synthesized speech and
         * return this builder.
         * @param voiceType the desired voice type
         * @return this builder instance
         */
        @NonNull
        public Builder voiceType(@Nullable String voiceType) {
            this.voiceType = voiceType;
            return this;
        }

        /**
         * Constructs a new {@link TextToSpeechOptions} with
         * the values assigned to this builder.
         * @return the new options instance
         */
        @NonNull
        public TextToSpeechOptions build() {
            return new TextToSpeechOptions(this);
        }

        String getVoiceType() {
            return voiceType;
        }
    }
}
