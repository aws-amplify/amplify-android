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

import com.amplifyframework.predictions.models.IdentifiedText;
import com.amplifyframework.util.Immutable;

import java.util.List;
import java.util.Objects;

/**
 * The result of the call to identify text from an image.
 */
public final class IdentifyTextResult implements IdentifyResult {
    private final String fullText;
    private final List<String> rawLineText;
    private final List<IdentifiedText> words;
    private final List<IdentifiedText> lines;

    private IdentifyTextResult(final Builder builder) {
        this.fullText = builder.getFullText();
        this.rawLineText = builder.getRawLineText();
        this.words = builder.getWords();
        this.lines = builder.getLines();
    }

    /**
     * Gets the full text from the image.
     * @return the full text
     */
    @NonNull
    public String getFullText() {
        return fullText;
    }

    /**
     * Gets the list of raw lines of text.
     * @return the raw lines of text
     */
    @NonNull
    public List<String> getRawLineText() {
        return Immutable.of(rawLineText);
    }

    /**
     * Gets the list of identified words.
     * @return the identified words
     */
    @NonNull
    public List<IdentifiedText> getWords() {
        return Immutable.of(words);
    }

    /**
     * Gets the list of identified lines.
     * @return the identified lines
     */
    @NonNull
    public List<IdentifiedText> getLines() {
        return Immutable.of(lines);
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
    public static final class Builder {
        private String fullText;
        private List<String> rawLineText;
        private List<IdentifiedText> words;
        private List<IdentifiedText> lines;

        /**
         * Sets the full text and return this builder.
         * @param fullText the full text
         * @return this builder instance
         */
        @NonNull
        public Builder fullText(@NonNull String fullText) {
            this.fullText = Objects.requireNonNull(fullText);
            return this;
        }

        /**
         * Sets the raw lines of text and return this builder.
         * @param rawLineText the raw lines of text
         * @return this builder instance
         */
        @NonNull
        public Builder rawLineText(@NonNull List<String> rawLineText) {
            this.rawLineText = Objects.requireNonNull(rawLineText);
            return this;
        }

        /**
         * Sets the identified words and return this builder.
         * @param words the identififed words
         * @return this builder instance
         */
        @NonNull
        public Builder words(@NonNull List<IdentifiedText> words) {
            this.words = Objects.requireNonNull(words);
            return this;
        }

        /**
         * Sets the identified lines and return this builder.
         * @param line the identified lines
         * @return this builder instance
         */
        @NonNull
        public Builder lines(@NonNull List<IdentifiedText> line) {
            this.lines = Objects.requireNonNull(line);
            return this;
        }

        /**
         * Construct a new instance of {@link IdentifyTextResult}
         * from the values assigned to this builder instance.
         * @return An instance of {@link IdentifyTextResult}
         */
        @NonNull
        public IdentifyTextResult build() {
            return new IdentifyTextResult(this);
        }

        @NonNull
        String getFullText() {
            return Objects.requireNonNull(fullText);
        }

        @NonNull
        List<String> getRawLineText() {
            return Objects.requireNonNull(rawLineText);
        }

        @NonNull
        List<IdentifiedText> getWords() {
            return Objects.requireNonNull(words);
        }

        @NonNull
        List<IdentifiedText> getLines() {
            return Objects.requireNonNull(lines);
        }
    }
}
