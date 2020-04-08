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

import java.util.Objects;

/**
 * A generic representation of an inferred feature from
 * analyzing a piece of text. Holds the portion of input
 * text where the feature is relevant and the confidence
 * score for inference.
 * @param <T> the feature type
 */
@SuppressWarnings("unchecked")
public abstract class TextFeature<T> extends Feature<T> {
    private final String targetText;
    private final int startIndex;
    private final int length;

    TextFeature(Builder<?, ? extends TextFeature<T>, T> builder) {
        super(builder);
        this.targetText = builder.getTargetText();
        this.startIndex = builder.getStartIndex();
        this.length = targetText.length();
    }

    /**
     * Gets the target text to which this feature applies.
     * @return the target portion of the input text
     */
    @NonNull
    public final String getTargetText() {
        return targetText;
    }

    /**
     * Gets the starting position of the target text
     * with respect to the full input text.
     * @return the starting index of target text
     */
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * Gets the length of target text.
     * @return the length of target text
     */
    public int getLength() {
        return length;
    }

    /**
     * Builder for {@link TextFeature}.
     * @param <B> Extension of this builder
     * @param <R> Extension of a {@link TextFeature} instance
     * @param <T> Type of result held by this text feature
     */
    abstract static class Builder<B extends Builder<B, R, T>, R extends TextFeature<T>, T>
            extends Feature.Builder<B, R, T> {
        private String targetText;
        private int startIndex;

        /**
         * Sets the target text and returns this builder.
         * @param targetText the target text
         * @return this builder instance
         */
        @NonNull
        public final B targetText(@NonNull String targetText) {
            this.targetText = Objects.requireNonNull(targetText);
            return (B) this;
        }

        /**
         * Sets the start index and returns this builder.
         * @param startIndex the starting index of the target
         * @return this builder instance
         */
        @NonNull
        public final B startIndex(int startIndex) {
            this.startIndex = startIndex;
            return (B) this;
        }

        @NonNull
        final String getTargetText() {
            return Objects.requireNonNull(targetText);
        }

        final int getStartIndex() {
            return startIndex;
        }
    }
}
