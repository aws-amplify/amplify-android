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
 * attribute from image analysis and the confidence score
 * for inference.
 * @param <T> the attribute type
 */
@SuppressWarnings("unchecked")
public abstract class ImageAttribute<T> extends Attribute<T> {
    private final TargetBoundary target;

    ImageAttribute(Builder<?, ? extends ImageAttribute<T>, T> builder) {
        super(builder);
        this.target = builder.getTarget();
    }

    /**
     * Gets the boundary of the point of interest from
     * the image.
     * @return the boundary of the image target
     */
    @NonNull
    public TargetBoundary getTarget() {
        return target;
    }

    abstract static class Builder<B extends Builder<B, R, T>, R extends ImageAttribute<T>, T>
            extends Attribute.Builder<B, R, T> {
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
