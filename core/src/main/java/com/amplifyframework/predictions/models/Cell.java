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

import java.util.Objects;

/**
 * A representation of {@link Table}'s individual cell.
 */
public final class Cell {
    private final String text;
    private final RectF box;
    private final Polygon polygon;
    private final boolean selected;
    private final int row;
    private final int column;

    private Cell(final Builder builder) {
        this.text = builder.getText();
        this.box = builder.getBox();
        this.polygon = builder.getPolygon();
        this.selected = builder.getSelected();
        this.row = builder.getRow();
        this.column = builder.getColumn();
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
     * Returns true if this cell is selected.
     * @return true if this cell is selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Gets the row span.
     * @return the row span
     */
    public int getRow() {
        return row;
    }

    /**
     * Gets the column span.
     * @return the column span
     */
    public int getColumn() {
        return column;
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
    public static final class Builder {
        private String text;
        private RectF box;
        private Polygon polygon;
        private boolean selected;
        private int row;
        private int column;

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
         * Sets the selection flag and return this builder.
         * @param selected the selection flag
         * @return this builder instance
         */
        @NonNull
        public Builder selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        /**
         * Sets the row span and return this builder.
         * @param row the row span
         * @return this builder instance
         */
        @NonNull
        public Builder row(int row) {
            this.row = row;
            return this;
        }

        /**
         * Sets the column span and return this builder.
         * @param column the column span
         * @return this builder instance
         */
        @NonNull
        public Builder column(int column) {
            this.column = column;
            return this;
        }

        /**
         * Constructs a new instance of {@link Cell} using the
         * values assigned to this builder.
         * @return An instance of {@link Cell}
         */
        @NonNull
        public Cell build() {
            return new Cell(this);
        }

        @NonNull
        String getText() {
            return Objects.requireNonNull(text);
        }

        @Nullable
        RectF getBox() {
            return box;
        }

        @Nullable
        Polygon getPolygon() {
            return polygon;
        }

        boolean getSelected() {
            return selected;
        }

        int getRow() {
            return row;
        }

        int getColumn() {
            return column;
        }
    }
}
