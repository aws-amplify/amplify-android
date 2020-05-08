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

/**
 * A representation of {@link Table}'s individual cell.
 */
public final class Cell extends ImageFeature<String> {
    private final boolean selected;
    private final int row;
    private final int column;

    private Cell(final Builder builder) {
        super(builder);
        this.selected = builder.getSelected();
        this.row = builder.getRow();
        this.column = builder.getColumn();
    }

    @NonNull
    @Override
    public String getTypeAlias() {
        return FeatureType.CELL.getAlias();
    }

    /**
     * Gets the text inside the cell.
     * @return the text
     */
    @NonNull
    public String getText() {
        return getValue();
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
    public static final class Builder
            extends ImageFeature.Builder<Builder, Cell, String> {
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
            return value(text);
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
