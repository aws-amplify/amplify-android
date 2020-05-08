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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A representation of 2-D table in given dimensions.
 * A table can be detected from an image of a document
 * and be organized into this data type.
 */
public final class Table extends ImageFeature<List<Cell>> {
    private final int rowSize;
    private final int columnSize;

    private Table(final Builder builder) {
        super(builder);
        this.rowSize = builder.getRowSize();
        this.columnSize = builder.getColumnSize();
    }

    @NonNull
    @Override
    public String getTypeAlias() {
        return FeatureType.TABLE.getAlias();
    }

    /**
     * Gets the row count.
     * @return the row count
     * */
    public int getRowSize() {
        return rowSize;
    }

    /**
     * Gets the column count.
     * @return the column count
     */
    public int getColumnSize() {
        return columnSize;
    }

    /**
     * Gets the list of cells in this table.
     * @return the cells
     */
    @NonNull
    public List<Cell> getCells() {
        return getValue();
    }

    /**
     * Gets a builder to help easily construct
     * a table object.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Table}.
     */
    public static final class Builder
            extends ImageFeature.Builder<Builder, Table, List<Cell>> {
        private int rowSize;
        private int columnSize;
        private List<Cell> cells;

        private Builder() {
            this.cells = Collections.emptyList();
        }

        /**
         * Sets the row count and return this builder.
         * @param rows the row count
         * @return this builder instance
         */
        @NonNull
        public Builder rowSize(int rows) {
            this.rowSize = rows;
            return this;
        }

        /**
         * Sets the column count and return this builder.
         * @param columns the column count
         * @return this builder instance
         */
        @NonNull
        public Builder columnSize(int columns) {
            this.columnSize = columns;
            return this;
        }

        /**
         * Sets the list of cells and return this builder.
         * @param cells the cells of this table
         * @return this builder instance
         */
        @NonNull
        public Builder cells(@NonNull List<Cell> cells) {
            return value(cells);
        }

        /**
         * Constructs a new instance of {@link Table} using the
         * values assigned to this builder.
         * @return An instance of {@link Table}
         */
        @NonNull
        public Table build() {
            return new Table(this);
        }

        int getRowSize() {
            return rowSize;
        }

        int getColumnSize() {
            return columnSize;
        }

        @NonNull
        List<Cell> getCells() {
            return Objects.requireNonNull(cells);
        }
    }
}
