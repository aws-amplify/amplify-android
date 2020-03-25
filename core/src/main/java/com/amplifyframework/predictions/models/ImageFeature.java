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
 * A generic class to hold information about an inferred
 * feature from image analysis and the confidence score
 * for inference.
 * @param <T> the feature type
 */
@SuppressWarnings("unchecked")
public abstract class ImageFeature<T> extends Feature<T> {
    private final TargetBoundary target;

    ImageFeature(Builder<?, ? extends ImageFeature<T>, T> builder) {
        super(builder);
        this.target = builder.getTarget();
    }

    /**
     * Gets the boundary of the point of interest from
     * the image.
     * @return the boundary of the image target
     */
    @NonNull
    public final TargetBoundary getTarget() {
        return target;
    }

    /**
     * Builder for {@link ImageFeature}.
     * @param <B> Extension of this builder
     * @param <R> Extension of a {@link ImageFeature} instance
     * @param <T> Type of result held by this image feature
     */
    abstract static class Builder<B extends Builder<B, R, T>, R extends ImageFeature<T>, T>
            extends Feature.Builder<B, R, T> {
        private TargetBoundary target;

        /**
         * Sets the target boundary and returns this builder.
         * @param target the target boundary
         * @return this builder instance
         */
        @NonNull
        public final B target(@NonNull TargetBoundary target) {
            this.target = Objects.requireNonNull(target);
            return (B) this;
        }

        @NonNull
        final TargetBoundary getTarget() {
            return Objects.requireNonNull(target);
        }
    }
}
