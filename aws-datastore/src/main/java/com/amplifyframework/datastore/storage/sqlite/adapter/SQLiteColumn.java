/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.datastore.storage.sqlite.adapter;

import com.amplifyframework.core.model.PrimaryKey;
import com.amplifyframework.datastore.storage.sqlite.SQLiteDataType;
import com.amplifyframework.util.Wrap;

/**
 * Adapts a {@link com.amplifyframework.core.model.ModelField}
 * with additional information to assist in representing and
 * processing a SQLite column.
 */
public final class SQLiteColumn {
    private static final String SQLITE_NAME_DELIMITER = ".";
    private static final String CUSTOM_ALIAS_DELIMITER = "_";

    private final String name;
    private final String fieldName;
    private final String tableName;
    private final String ownedType;
    private final boolean isNonNull;
    private final SQLiteDataType dataType;

    private SQLiteColumn(Builder builder) {
        this.name = builder.name;
        this.fieldName = builder.fieldName;
        this.tableName = builder.tableName;
        this.ownedType = builder.ownedType;
        this.isNonNull = builder.isNonNull;
        this.dataType = builder.dataType;
    }

    /**
     * Returns a builder to construct a SQLite column.
     * @return a builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the name of column.
     * @return the name of column
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the name of the field linked to the column.
     * @return the name of the field linked to the column
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the name of table containing this column.
     * @return the name of table containing this column
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Returns the aliased name of column.
     * @return the aliased name of column.
     */
    public String getAliasedName() {
        return tableName + CUSTOM_ALIAS_DELIMITER + name;
    }

    /**
     * Returns the unambiguous name of column.
     * @return the unambiguous name of column
     */
    public String getQuotedColumnName() {
        return Wrap.inBackticks(tableName) + SQLITE_NAME_DELIMITER + Wrap.inBackticks(name);
    }

    /**
     * Returns the name of model that this column is foreign key to.
     * Null if the column is not a foreign key.
     * @return the name of model that this column is foreign key to
     *         and null if this column is not a foreign key
     */
    public String getOwnedType() {
        return ownedType;
    }

    /**
     * Returns true if this column is primary key.
     * @return true if this column is primary key
     */
    public boolean isPrimaryKey() {
        return PrimaryKey.matches(name);
    }

    /**
     * Returns true if this column is foreign key.
     * @return true if this column is foreign key
     */
    public boolean isForeignKey() {
        return ownedType != null;
    }

    /**
     * Returns true if this column must be non-null.
     * @return true if this column must be non-null
     */
    public boolean isNonNull() {
        return isNonNull;
    }

    /**
     * Returns the SQLite-compatible data type of this column.
     * @return the SQLite-compatible data type of this column
     */
    public String getColumnType() {
        return dataType.getSqliteDataType();
    }

    /**
     * The builder instance to construct immutable instance
     * of SQLite column.
     */
    public static final class Builder {
        private String name;
        private String fieldName;
        private String tableName;
        private String ownedType;
        private boolean isNonNull = false;
        private SQLiteDataType dataType;

        /**
         * Sets the name of this column.
         * @param name the name of this column
         * @return builder instance with given name
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the name of the field linked to this column.
         * @param fieldName the name of the field linked to this column
         * @return builder instance with given name
         */
        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        /**
         * Sets the name of the table to which this column belongs.
         * @param tableName the name of the table to which this column belongs
         * @return builder instance with given table name
         */
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * Sets the name of table that this foreign key refers to.
         * This should be null if this is column is not a foreign key.
         * @param ownedType the name of table that this foreign key refers to
         * @return builder instance with given type
         */
        public Builder ownerOf(String ownedType) {
            this.ownedType = ownedType;
            return this;
        }

        /**
         * Sets the flag whether this column must be non-null.
         * @param isNonNull true if this column must be non-null
         * @return builder instance with given flag
         */
        public Builder isNonNull(boolean isNonNull) {
            this.isNonNull = isNonNull;
            return this;
        }

        /**
         * Sets the SQLite-compatible data type of this column.
         * @param dataType data type of this column
         * @return builder instance with given SQLite data type
         */
        public Builder dataType(SQLiteDataType dataType) {
            this.dataType = dataType;
            return this;
        }

        /**
         * Constructs an instance of SQLite column with this builder.
         * @return Constructed instance of SQLite column
         */
        public SQLiteColumn build() {
            return new SQLiteColumn(this);
        }
    }
}
