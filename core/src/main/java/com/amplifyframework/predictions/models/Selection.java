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
 * Represents a selection from an image. It contains
 * the bounding geometry of a selection and an
 * indicator to regard its selection state.
 */
public final class Selection {
    private final RectF box;
    private final Polygon polygon;
    private final boolean selected;

    private Selection(RectF box, Polygon polygon, boolean selected) {
        this.box = box;
        this.polygon = polygon;
        this.selected = selected;
    }

    /**
     * Gets the rectangular boundary. It fetches the
     * box portion of registered target boundary.
     * @return the bounding box
     */
    @Nullable
    public RectF getBox() {
        return box;
    }

    /**
     * Gets the polygonal boundary. It fetches the
     * polygon portion of registered target boundary.
     * @return the polygon
     */
    @Nullable
    public Polygon getPolygon() {
        return polygon;
    }

    /**
     * Returns true if this selection is selected.
     * @return true if this selection is selected
     */
    public boolean isSelected() {
        return selected;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private RectF box;
        private Polygon polygon;
        private boolean selected;

        /**
         * Sets the bounding box and return this builder.
         * @param box the rectangular boundary
         * @return this builder instance
         */
        @NonNull
        public Builder box(@Nullable RectF box) {
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
         * Sets the selection status and return this builder.
         * @param selected the selection status to set to
         * @return this builder instance
         */
        @NonNull
        public Builder selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        public Selection build() {
            return new Selection(box, polygon, selected);
        }

        @Nullable
        RectF getBox() {
            return box;
        }

        @Nullable
        Polygon getPolygon() {
            return polygon;
        }

        boolean isSelected() {
            return selected;
        }
    }
}
