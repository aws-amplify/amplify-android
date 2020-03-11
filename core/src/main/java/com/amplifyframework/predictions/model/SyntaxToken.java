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

package com.amplifyframework.predictions.model;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Class that holds the syntax detection results for a
 * string of text for the predictions category.
 */
public final class SyntaxToken {
    private final Integer tokenId;
    private final String targetText;
    private final Integer startIndex;
    private final Integer endIndex;
    private final PartOfSpeech partOfSpeech;

    private SyntaxToken(
            @NonNull Integer tokenId,
            @NonNull String targetText,
            @NonNull Integer startIndex,
            @NonNull Integer endIndex,
            @NonNull PartOfSpeech partOfSpeech
    ) {
        this.tokenId = tokenId;
        this.targetText = targetText;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.partOfSpeech = partOfSpeech;
    }

    /**
     * Gets the ID of this token.
     * @return the token ID
     */
    @NonNull
    public Integer getTokenId() {
        return tokenId;
    }

    /**
     * Gets the target text.
     * @return the target text
     */
    @NonNull
    public String getTargetText() {
        return targetText;
    }

    /**
     * Gets the starting index of syntax token.
     * @return the start index
     */
    @NonNull
    public Integer getStartIndex() {
        return startIndex;
    }

    /**
     * Gets the last index of syntax token.
     * @return the end index
     */
    @NonNull
    public Integer getEndIndex() {
        return endIndex;
    }

    /**
     * Gets the part of speech represented by this token.
     * @return the part of speech
     */
    @NonNull
    public PartOfSpeech getPartOfSpeech() {
        return partOfSpeech;
    }

    /**
     * Gets the builder to easily construct an instance
     * of syntax token object.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to help easily construct an instance of
     * {@link SyntaxToken} with a set of properties.
     */
    public static class Builder {
        private Integer tokenId;
        private String targetText;
        private Integer startIndex;
        private Integer endIndex;
        private PartOfSpeech partOfSpeech;

        /**
         * Sets the token ID and return this builder.
         * @param tokenId the token ID
         * @return this builder instance
         */
        @NonNull
        public Builder tokenId(@NonNull Integer tokenId) {
            this.tokenId = Objects.requireNonNull(tokenId);
            return this;
        }

        /**
         * Sets the target text and return this builder.
         * @param targetText the target text
         * @return this builder instance
         */
        @NonNull
        public Builder targetText(@NonNull String targetText) {
            this.targetText = Objects.requireNonNull(targetText);
            return this;
        }

        /**
         * Sets the starting index and return this builder.
         * @param startIndex the start index
         * @return this builder instance
         */
        @NonNull
        public Builder startIndex(@NonNull Integer startIndex) {
            this.startIndex = Objects.requireNonNull(startIndex);
            return this;
        }

        /**
         * Sets the last index and return this builder.
         * @param endIndex the end index
         * @return this builder instance
         */
        @NonNull
        public Builder endIndex(@NonNull Integer endIndex) {
            this.endIndex = Objects.requireNonNull(endIndex);
            return this;
        }

        /**
         * Sets the part of speech and return this builder.
         * @param partOfSpeech the associated part of speech
         * @return this builder instance
         */
        @NonNull
        public Builder partOfSpeech(@NonNull PartOfSpeech partOfSpeech) {
            this.partOfSpeech = Objects.requireNonNull(partOfSpeech);
            return this;
        }

        /**
         * Construct a new instance of {@link SyntaxToken} using
         * the values specified in this builder.
         * @return An instance of {@link SyntaxToken}
         */
        @NonNull
        public SyntaxToken build() {
            return new SyntaxToken(
                    Objects.requireNonNull(tokenId),
                    Objects.requireNonNull(targetText),
                    Objects.requireNonNull(startIndex),
                    Objects.requireNonNull(endIndex),
                    Objects.requireNonNull(partOfSpeech)
            );
        }
    }
}
