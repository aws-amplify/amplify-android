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
import com.amplifyframework.predictions.models.IdentifiedText;
import com.amplifyframework.predictions.models.Selection;
import com.amplifyframework.predictions.models.Table;
import com.amplifyframework.util.Immutable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The result of a call to identify text in an image of a document.
 */
public final class IdentifyDocumentTextResult implements IdentifyResult {
    private final String fullText;
    private final List<String> rawLineText;
    private final List<IdentifiedText> words;
    private final List<IdentifiedText> lines;
    private final List<Selection> selections;
    private final List<Table> tables;
    private final List<BoundedKeyValue> keyValues;

    private IdentifyDocumentTextResult(final Builder builder) {
        this.fullText = builder.getFullText();
        this.rawLineText = builder.getRawLineText();
        this.words = builder.getWords();
        this.lines = builder.getLines();
        this.selections = builder.getSelections();
        this.tables = builder.getTables();
        this.keyValues = builder.getKeyValues();
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
     * Gets the list of selections.
     * @return the selections
     */
    @NonNull
    public List<Selection> getSelections() {
        return Immutable.of(selections);
    }

    /**
     * Gets the list of tables.
     * @return the tables
     */
    @NonNull
    public List<Table> getTables() {
        return Immutable.of(tables);
    }

    /**
     * Gets the list of key-values.
     * @return the key-values
     */
    @NonNull
    public List<BoundedKeyValue> getKeyValues() {
        return Immutable.of(keyValues);
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
    public static final class Builder {
        private String fullText;
        private List<String> rawLineText;
        private List<IdentifiedText> words;
        private List<IdentifiedText> lines;
        private List<Selection> selections;
        private List<Table> tables;
        private List<BoundedKeyValue> keyValues;

        private Builder() {
            this.rawLineText = Collections.emptyList();
            this.words = Collections.emptyList();
            this.lines = Collections.emptyList();
            this.selections = Collections.emptyList();
            this.tables = Collections.emptyList();
            this.keyValues = Collections.emptyList();
        }

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
            return new IdentifyDocumentTextResult(this);
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

        @NonNull
        List<Selection> getSelections() {
            return Objects.requireNonNull(selections);
        }

        @NonNull
        List<Table> getTables() {
            return Objects.requireNonNull(tables);
        }

        @NonNull
        List<BoundedKeyValue> getKeyValues() {
            return Objects.requireNonNull(keyValues);
        }
    }
}
