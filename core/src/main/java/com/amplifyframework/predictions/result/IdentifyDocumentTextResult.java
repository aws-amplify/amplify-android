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

import com.amplifyframework.predictions.models.BoundedKeyValue;
import com.amplifyframework.predictions.models.IdentifiedLine;
import com.amplifyframework.predictions.models.IdentifiedWord;
import com.amplifyframework.predictions.models.Selection;
import com.amplifyframework.predictions.models.Table;

import java.util.List;
import java.util.Objects;

/**
 * The result of the call to identify document text from an image.
 */
public final class IdentifyDocumentTextResult implements IdentifyResult {
    private final String fullText;
    private final List<IdentifiedWord> words;
    private final List<String> rawLineText;
    private final List<IdentifiedLine> identifiedLines;
    private final List<Selection> selections;
    private final List<Table> tables;
    private final List<BoundedKeyValue> keyValues;

    private IdentifyDocumentTextResult(
            @NonNull String fullText,
            @NonNull List<IdentifiedWord> words,
            @NonNull List<String> rawLineText,
            @NonNull List<IdentifiedLine> identifiedLines,
            @NonNull List<Selection> selections,
            @NonNull List<Table> tables,
            @NonNull List<BoundedKeyValue> keyValues
    ) {
        this.fullText = fullText;
        this.words = words;
        this.rawLineText = rawLineText;
        this.identifiedLines = identifiedLines;
        this.selections = selections;
        this.tables = tables;
        this.keyValues = keyValues;
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
     * Gets the list of identified words.
     * @return the identified words
     */
    @NonNull
    public List<IdentifiedWord> getWords() {
        return words;
    }

    /**
     * Gets the list of raw lines of text.
     * @return the raw lines of text
     */
    @NonNull
    public List<String> getRawLineText() {
        return rawLineText;
    }

    /**
     * Gets the list of identified lines.
     * @return the identified lines
     */
    @NonNull
    public List<IdentifiedLine> getIdentifiedLines() {
        return identifiedLines;
    }

    /**
     * Gets the list of selections.
     * @return the selections
     */
    @NonNull
    public List<Selection> getSelections() {
        return selections;
    }

    /**
     * Gets the list of tables.
     * @return the tables
     */
    @NonNull
    public List<Table> getTables() {
        return tables;
    }

    /**
     * Gets the list of key-values.
     * @return the key-values
     */
    @NonNull
    public List<BoundedKeyValue> getKeyValues() {
        return keyValues;
    }

    /**
     * Gets a builder instance to help easily construct an
     * instance of document text identification result.
     * @return an unassigned instance of builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to help easily construct an instance of
     * {@link IdentifyDocumentTextResult}.
     */
    public static class Builder {
        private String fullText;
        private List<IdentifiedWord> words;
        private List<String> rawLineText;
        private List<IdentifiedLine> identifiedLines;
        private List<Selection> selections;
        private List<Table> tables;
        private List<BoundedKeyValue> keyValues;

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
         * Sets the identified words and return this builder.
         * @param words the identififed words
         * @return this builder instance
         */
        @NonNull
        public Builder words(@NonNull List<IdentifiedWord> words) {
            this.words = Objects.requireNonNull(words);
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
         * Sets the identified lines and return this builder.
         * @param identifiedLines the identified lines
         * @return this builder instance
         */
        @NonNull
        public Builder identifiedLines(@NonNull List<IdentifiedLine> identifiedLines) {
            this.identifiedLines = Objects.requireNonNull(identifiedLines);
            return this;
        }

        /**
         * Sets the selections and return this builder.
         * @param selections the selections
         * @return this builder instance
         */
        @NonNull
        public Builder selections(@NonNull List<Selection> selections) {
            this.selections = Objects.requireNonNull(selections);
            return this;
        }

        /**
         * Sets the tables and return this builder.
         * @param tables the tables
         * @return this builder instance
         */
        @NonNull
        public Builder tables(@NonNull List<Table> tables) {
            this.tables = Objects.requireNonNull(tables);
            return this;
        }

        /**
         * Sets the key-value and return this builder.
         * @param keyValues the key-value
         * @return this builder instance
         */
        @NonNull
        public Builder keyValues(@NonNull List<BoundedKeyValue> keyValues) {
            this.keyValues = Objects.requireNonNull(keyValues);
            return this;
        }

        /**
         * Construct a new instance of {@link IdentifyDocumentTextResult}
         * from the values assigned to this builder instance.
         * @return An instance of {@link IdentifyDocumentTextResult}
         */
        @NonNull
        public IdentifyDocumentTextResult build() {
            return new IdentifyDocumentTextResult(
                    Objects.requireNonNull(fullText),
                    Objects.requireNonNull(words),
                    Objects.requireNonNull(rawLineText),
                    Objects.requireNonNull(identifiedLines),
                    Objects.requireNonNull(selections),
                    Objects.requireNonNull(tables),
                    Objects.requireNonNull(keyValues)
            );
        }
    }
}
