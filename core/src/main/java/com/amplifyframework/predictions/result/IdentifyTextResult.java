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
import androidx.annotation.Nullable;

import com.amplifyframework.predictions.models.IdentifiedLine;
import com.amplifyframework.predictions.models.IdentifiedWord;

import java.util.List;

/**
 * The result of the call to identify text from an image.
 */
public final class IdentifyTextResult implements IdentifyResult {
    private final String fullText;
    private final List<IdentifiedWord> words;
    private final List<String> rawLineText;
    private final List<IdentifiedLine> identifiedLines;

    private IdentifyTextResult(
            @Nullable String fullText,
            @Nullable List<IdentifiedWord> words,
            @Nullable List<String> rawLineText,
            @Nullable List<IdentifiedLine> identifiedLines
    ) {
        this.fullText = fullText;
        this.words = words;
        this.rawLineText = rawLineText;
        this.identifiedLines = identifiedLines;
    }

    /**
     * Gets the full text from the image.
     * @return the full text
     */
    @Nullable
    public String getFullText() {
        return fullText;
    }

    /**
     * Gets the list of identified words.
     * @return the identified words
     */
    @Nullable
    public List<IdentifiedWord> getWords() {
        return words;
    }

    /**
     * Gets the list of raw lines of text.
     * @return the raw lines of text
     */
    @Nullable
    public List<String> getRawLineText() {
        return rawLineText;
    }

    /**
     * Gets the list of identified lines.
     * @return the identified lines
     */
    @Nullable
    public List<IdentifiedLine> getIdentifiedLines() {
        return identifiedLines;
    }

    /**
     * Gets a builder instance to help easily construct an
     * instance of text identification result.
     * @return an unassigned instance of builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to help easily construct an instance of
     * {@link IdentifyTextResult}.
     */
    public static class Builder {
        private String fullText;
        private List<IdentifiedWord> words;
        private List<String> rawLineText;
        private List<IdentifiedLine> identifiedLines;

        /**
         * Sets the full text and return this builder.
         * @param fullText the full text
         * @return this builder instance
         */
        @NonNull
        public Builder fullText(@Nullable String fullText) {
            this.fullText = fullText;
            return this;
        }

        /**
         * Sets the identified words and return this builder.
         * @param words the identififed words
         * @return this builder instance
         */
        @NonNull
        public Builder words(@Nullable List<IdentifiedWord> words) {
            this.words = words;
            return this;
        }

        /**
         * Sets the raw lines of text and return this builder.
         * @param rawLineText the raw lines of text
         * @return this builder instance
         */
        @NonNull
        public Builder rawLineText(@Nullable List<String> rawLineText) {
            this.rawLineText = rawLineText;
            return this;
        }

        /**
         * Sets the identified lines and return this builder.
         * @param identifiedLines the identified lines
         * @return this builder instance
         */
        @NonNull
        public Builder identifiedLines(@Nullable List<IdentifiedLine> identifiedLines) {
            this.identifiedLines = identifiedLines;
            return this;
        }

        /**
         * Construct a new instance of {@link IdentifyTextResult}
         * from the values assigned to this builder instance.
         * @return An instance of {@link IdentifyTextResult}
         */
        @NonNull
        public IdentifyTextResult build() {
            return new IdentifyTextResult(
                    fullText,
                    words,
                    rawLineText,
                    identifiedLines
            );
        }
    }
}
