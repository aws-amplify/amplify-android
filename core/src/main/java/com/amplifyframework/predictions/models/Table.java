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
public final class Table {
    private final int rows;
    private final int columns;
    private final List<Cell> cells;

    private Table(final Builder builder) {
        this.rows = builder.getRows();
        this.columns = builder.getColumns();
        this.cells = builder.getCells();
    }

    /**
     * Gets the row count.
     * @return the row count
     * */
    public int getRows() {
        return rows;
    }

    /**
     * Gets the column count.
     * @return the column count
     */
    public int getColumns() {
        return columns;
    }

    /**
     * Gets the list of cells in this table.
     * @return the cells
     */
    @NonNull
    public List<Cell> getCells() {
        return cells;
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
    public static final class Builder {
        private int rows;
        private int columns;
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
        public Builder rows(int rows) {
            this.rows = rows;
            return this;
        }

        /**
         * Sets the column count and return this builder.
         * @param columns the column count
         * @return this builder instance
         */
        @NonNull
        public Builder columns(int columns) {
            this.columns = columns;
            return this;
        }

        /**
         * Sets the list of cells and return this builder.
         * @param cells the cells of this table
         * @return this builder instance
         */
        @NonNull
        public Builder cells(@NonNull List<Cell> cells) {
            this.cells = Objects.requireNonNull(cells);
            return this;
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

        int getRows() {
            return rows;
        }

        int getColumns() {
            return columns;
        }

        @NonNull
        List<Cell> getCells() {
            return Objects.requireNonNull(cells);
        }
    }
}
