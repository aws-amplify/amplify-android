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

package com.amplifyframework.predictions.result;

import androidx.annotation.NonNull;

import com.amplifyframework.core.async.Result;

import java.util.Objects;

/**
 * The result of the call to transcribe text from audio.
 */
public final class SpeechToTextResult implements Result {
    private final String transcription;

    private SpeechToTextResult(final Builder builder) {
        this.transcription = builder.getTranscription();
    }

    /**
     * Gets the transcribed text.
     * @return the transcribed text
     */
    @NonNull
    public String getTranscription() {
        return transcription;
    }

    /**
     * Return a new instance of builder to help easily construct
     * a new instance of result.
     * @return An unassigned instance of builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SpeechToTextResult}.
     */
    public static final class Builder {
        private String transcription;

        /**
         * Sets the transcribed text and return this builder instance.
         * @param text The transcribed text
         * @return This builder instance
         */
        @NonNull
        public Builder transcription(@NonNull String text) {
            this.transcription = Objects.requireNonNull(text);
            return this;
        }

        /**
         * Return a result object containing transcribed text.
         * Throw if any of the properties is null.
         * @return An instance of {@link SpeechToTextResult}
         */
        @NonNull
        public SpeechToTextResult build() {
            return new SpeechToTextResult(this);
        }

        @NonNull
        String getTranscription() {
            return Objects.requireNonNull(transcription);
        }
    }
}
