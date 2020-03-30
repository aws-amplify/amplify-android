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
    private final TargetText target;

    TextFeature(Builder<?, ? extends TextFeature<T>, T> builder) {
        super(builder);
        this.target = builder.getTarget();
    }

    /**
     * Gets the target text and associated index.
     * @return the target text
     */
    @NonNull
    public final TargetText getTarget() {
        return target;
    }

    /**
     * Builder for {@link TextFeature}.
     * @param <B> Extension of this builder
     * @param <R> Extension of a {@link TextFeature} instance
     * @param <T> Type of result held by this text feature
     */
    abstract static class Builder<B extends Builder<B, R, T>, R extends TextFeature<T>, T>
            extends Feature.Builder<B, R, T> {
        private TargetText target;

        /**
         * Sets the target text and returns this builder.
         * @param target the target text
         * @return this builder instance
         */
        @NonNull
        public final B target(@NonNull TargetText target) {
            this.target = Objects.requireNonNull(target);
            return (B) this;
        }

        @NonNull
        final TargetText getTarget() {
            return Objects.requireNonNull(target);
        }
    }
}
