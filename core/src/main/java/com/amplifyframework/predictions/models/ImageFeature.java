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

import android.graphics.RectF;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A generic class to hold information about an inferred
 * feature from image analysis and the confidence score
 * for inference.
 * Contains geometric information to help locate the
 * target item inside an image. {@link Polygon} helps
 * define a more fine boundary that cannot be sufficiently
 * described by a {@link RectF} instance.
 * @param <T> the feature type
 */
@SuppressWarnings("unchecked")
public abstract class ImageFeature<T> extends Feature<T> {
    private final RectF box;
    private final Polygon polygon;

    ImageFeature(Builder<?, ? extends ImageFeature<T>, T> builder) {
        super(builder);
        this.box = builder.getBox();
        this.polygon = builder.getPolygon();
    }

    /**
     * Gets the rectangular target boundary if available.
     * @return the rectangular boundary
     */
    @Nullable
    public RectF getBox() {
        return box;
    }

    /**
     * Gets a more finely defined target boundary if available.
     * @return the polygonal boundary
     */
    @Nullable
    public Polygon getPolygon() {
        return polygon;
    }

    /**
     * Builder for {@link ImageFeature}.
     * @param <B> Extension of this builder
     * @param <R> Extension of a {@link ImageFeature} instance
     * @param <T> Type of result held by this image feature
     */
    abstract static class Builder<B extends Builder<B, R, T>, R extends ImageFeature<T>, T>
            extends Feature.Builder<B, R, T> {
        private RectF box;
        private Polygon polygon;

        /**
         * Sets the bounding box and return this builder.
         * @param box the rectangular boundary
         * @return this builder instance
         */
        @NonNull
        public B box(@Nullable RectF box) {
            this.box = box;
            return (B) this;
        }

        /**
         * Sets the bounding polygon and return this builder.
         * @param polygon the polygonal boundary
         * @return this builder instance
         */
        @NonNull
        public B polygon(@Nullable Polygon polygon) {
            this.polygon = polygon;
            return (B) this;
        }

        @Nullable
        RectF getBox() {
            return box;
        }

        @Nullable
        Polygon getPolygon() {
            return polygon;
        }
    }
}
