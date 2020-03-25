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

import java.util.Objects;

/**
 * Class to represent a selection from an image.
 */
public final class Selection {
    private final Rect boundingBox;
    private final Polygon polygon;
    private final Boolean isSelected;

    private Selection(
            @NonNull Rect boundingBox,
            @NonNull Polygon polygon,
            @NonNull Boolean isSelected
    ) {
        this.boundingBox = boundingBox;
        this.polygon = polygon;
        this.isSelected = isSelected;
    }

    /**
     * Gets the rectangular boundary.
     * @return the bounding box
     */
    @NonNull
    public Rect getBoundingBox() {
        return boundingBox;
    }

    /**
     * Gets the polygonal boundary.
     * @return the polygon
     */
    @NonNull
    public Polygon getPolygon() {
        return polygon;
    }

    /**
     * Returns true if this selection is selected.
     * @return true if this selection is selected
     */
    @NonNull
    public Boolean isSelected() {
        return isSelected;
    }

    /**
     * Gets a builder to construct a selection object.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Selection}.
     */
    public static final class Builder {
        private Rect boundingBox;
        private Polygon polygon;
        private Boolean isSelected;

        /**
         * Sets the rectangular boundary and return this builder.
         * @param boundingBox the bounding box
         * @return this builder instance
         */
        @NonNull
        public Builder boundingBox(@NonNull Rect boundingBox) {
            this.boundingBox = Objects.requireNonNull(boundingBox);
            return this;
        }

        /**
         * Sets the polygonal boundary and return this builder.
         * @param polygon the polygon
         * @return this builder instance
         */
        @NonNull
        public Builder polygon(@NonNull Polygon polygon) {
            this.polygon = Objects.requireNonNull(polygon);
            return this;
        }

        /**
         * Sets the selection flag and return this builder.
         * @param isSelected the selection flag
         * @return this builder instance
         */
        @NonNull
        public Builder isSelected(@NonNull Boolean isSelected) {
            this.isSelected = Objects.requireNonNull(isSelected);
            return this;
        }

        /**
         * Constructs a new instance of {@link Selection} using
         * the values assigned to this builder.
         * @return An instance of {@link Selection}
         */
        @NonNull
        public Selection build() {
            return new Selection(
                    Objects.requireNonNull(boundingBox),
                    Objects.requireNonNull(polygon),
                    Objects.requireNonNull(isSelected)
            );
        }
    }
}
