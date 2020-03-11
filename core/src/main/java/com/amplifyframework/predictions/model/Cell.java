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

import android.graphics.Rect;
import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * A representation of table's individual cell.
 */
public final class Cell {
    private final String text;
    private final Rect boundingBox;
    private final Polygon polygon;
    private final Boolean isSelected;
    private final Integer rowSpan;
    private final Integer columnSpan;

    private Cell(
            @NonNull String text,
            @NonNull Rect boundingBox,
            @NonNull Polygon polygon,
            @NonNull Boolean isSelected,
            @NonNull Integer rowSpan,
            @NonNull Integer columnSpan
    ) {
        this.text = text;
        this.boundingBox = boundingBox;
        this.polygon = polygon;
        this.isSelected = isSelected;
        this.rowSpan = rowSpan;
        this.columnSpan = columnSpan;
    }

    /**
     * Gets the text inside the cell.
     * @return the text
     */
    @NonNull
    public String getText() {
        return text;
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
     * Returns true if this cell is selected.
     * @return true if this cell is selected
     */
    @NonNull
    public Boolean isSelected() {
        return isSelected;
    }

    /**
     * Gets the row span value.
     * @return the row span
     */
    @NonNull
    public Integer getRowSpan() {
        return rowSpan;
    }

    /**
     * Gets the column span value.
     * @return the column span
     */
    @NonNull
    public Integer getColumnSpan() {
        return columnSpan;
    }

    /**
     * Gets a builder to help easily construct
     * a cell object.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Cell}.
     */
    public static class Builder {
        private String text;
        private Rect boundingBox;
        private Polygon polygon;
        private Boolean isSelected;
        private Integer rowSpan;
        private Integer columnSpan;

        /**
         * Sets the text and return this builder.
         * @param text the text
         * @return this builder instance
         */
        @NonNull
        public Builder text(@NonNull String text) {
            this.text = Objects.requireNonNull(text);
            return this;
        }

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
         * Sets the polygon and return this builder.
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
         * Sets the row span and return this builder.
         * @param rowSpan the row span
         * @return this builder instance
         */
        @NonNull
        public Builder rowSpan(@NonNull Integer rowSpan) {
            this.rowSpan = Objects.requireNonNull(rowSpan);
            return this;
        }

        /**
         * Sets the column span and return this builder.
         * @param columnSpan the column span
         * @return this builder instance
         */
        @NonNull
        public Builder columnSpan(@NonNull Integer columnSpan) {
            this.columnSpan = Objects.requireNonNull(columnSpan);
            return this;
        }

        /**
         * Constructs a new instance of {@link Cell} using the
         * values assigned to this builder.
         * @return An instance of {@link Cell}
         */
        @NonNull
        public Cell build() {
            return new Cell(
                    Objects.requireNonNull(text),
                    Objects.requireNonNull(boundingBox),
                    Objects.requireNonNull(polygon),
                    Objects.requireNonNull(isSelected),
                    Objects.requireNonNull(rowSpan),
                    Objects.requireNonNull(columnSpan)
            );
        }
    }
}
