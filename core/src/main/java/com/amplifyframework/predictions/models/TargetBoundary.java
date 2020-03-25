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

import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Contains geometric information to help locate
 * the target item inside an image. {@link Polygon}
 * helps define a more fine boundary that cannot be
 * sufficiently described by a {@link Rect} instance.
 */
public final class TargetBoundary {
    private final Rect box;
    private final Polygon polygon;

    private TargetBoundary(final Builder builder) {
        this.box = builder.getBox();
        this.polygon = builder.getPolygon();
    }

    /**
     * Gets the rectangular target boundary if available.
     * @return the rectangular boundary
     */
    @Nullable
    public Rect getBox() {
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
     * Gets a new builder instance.
     * @return a new builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets a default instance of {@link TargetBoundary}.
     * @return a default target boundary instance
     */
    @NonNull
    public static TargetBoundary defaultInstance() {
        return builder().build();
    }

    /**
     * Builder for {@link TargetBoundary}.
     */
    public static final class Builder {
        private Rect box;
        private Polygon polygon;

        /**
         * Sets the bounding box and return this builder.
         * @param box the rectangular boundary
         * @return this builder instance
         */
        @NonNull
        public Builder box(@Nullable Rect box) {
            this.box = box;
            return this;
        }

        /**
         * Sets the bounding polygon and return this builder.
         * @param polygon the polygonal boundary
         * @return this builder instance
         */
        @NonNull
        public Builder polygon(@Nullable Polygon polygon) {
            this.polygon = polygon;
            return this;
        }

        /**
         * Builds an instance of {@link TargetBoundary} using
         * the values assigned to this builder.
         * @return An instance of {@link TargetBoundary}
         */
        @NonNull
        public TargetBoundary build() {
            return new TargetBoundary(this);
        }

        @Nullable
        Rect getBox() {
            return box;
        }

        @Nullable
        Polygon getPolygon() {
            return polygon;
        }
    }
}
