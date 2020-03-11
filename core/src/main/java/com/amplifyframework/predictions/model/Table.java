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

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Objects;

/**
 * A representation of table.
 */
public final class Table {
    private final Integer rows;
    private final Integer columns;
    private final List<Cell> cells;

    private Table(
            @NonNull Integer rows,
            @NonNull Integer columns,
            @NonNull List<Cell> cells
    ) {
        this.rows = rows;
        this.columns = columns;
        this.cells = cells;
    }

    /**
     * Gets the row count.
     * @return the row count
     * */
    @NonNull
    public Integer getRows() {
        return rows;
    }

    /**
     * Gets the column count.
     * @return the column count
     */
    @NonNull
    public Integer getColumns() {
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
    public static class Builder {
        private Integer rows;
        private Integer columns;
        private List<Cell> cells;

        /**
         * Sets the row count and return this builder.
         * @param rows the row count
         * @return this builder instance
         */
        @NonNull
        public Builder rows(@NonNull Integer rows) {
            this.rows = Objects.requireNonNull(rows);
            return this;
        }

        /**
         * Sets the column count and return this builder.
         * @param columns the column count
         * @return this builder instance
         */
        @NonNull
        public Builder columns(@NonNull Integer columns) {
            this.columns = Objects.requireNonNull(columns);
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
            return new Table(
                    Objects.requireNonNull(rows),
                    Objects.requireNonNull(columns),
                    Objects.requireNonNull(cells)
            );
        }
    }
}
