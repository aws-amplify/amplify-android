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

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelAssociation;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.PrimaryKey;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.sqlite.SQLiteDataType;
import com.amplifyframework.datastore.storage.sqlite.TypeConverter;
import com.amplifyframework.util.Immutable;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Adapts a {@link ModelSchema} with additional information
 * to assist in representing and processing a SQLite table.
 */
@SuppressWarnings("WeakerAccess")
public final class SQLiteTable {

    /**
     * Primary key field name for custom primary key support.
     */
    public static final String PRIMARY_KEY_FIELD_NAME = "@@primaryKey";
    private final String name;
    private final Map<String, SQLiteColumn> columns;
    private final List<String> primaryKeyColumns;
    private final List<SQLiteColumn> sortedColumns;
    private final Model.Type type;

    private SQLiteTable(String name, Map<String, SQLiteColumn> columns,
                        List<String> primaryKeyColumns, Model.Type type) {
        this.name = name;
        this.columns = columns;
        this.type = type;
        this.sortedColumns = sortedColumns();
        this.primaryKeyColumns = primaryKeyColumns;
    }

    /**
     * Static method to convert an instance of {@link ModelSchema}
     * into a SQLite table representation. This representation will
     * be useful for storage engine when interacting with SQLite database.
     *
     * Instances of {@link ModelField} inside the schema will be converted
     * to {@link SQLiteColumn} if they meet the following conditions:
     *
     *      1) field is NOT a associated with another model OR,
     *      2) field is the foreign key of relationship
     *
     * The generated SQLite column will encapsulate additional information
     * that is not contained inside {@link ModelField}, such as
     *
     *      1) which table (model) does this field belong to?,
     *      2) what is the corresponding SQLite data type for field type,
     *      3) whether the field represents a foreign key, AND
     *      4) IF it is a foreign key, then which model does it it identify?
     *
     * @param modelSchema An instance of {@link ModelSchema}
     * @return Adapted SQLite table
     */
    @NonNull
    public static SQLiteTable fromSchema(@NonNull ModelSchema modelSchema) {
        Objects.requireNonNull(modelSchema);
        Map<String, ModelAssociation> associations = modelSchema.getAssociations();
        Map<String, SQLiteColumn> sqlColumns = new TreeMap<>();
        if (modelSchema.getModelType() == Model.Type.USER && modelSchema.getPrimaryIndexFields().size() > 1) {
            SQLiteColumn primaryKeyColumn = SQLiteColumn.builder()
                    .name(PRIMARY_KEY_FIELD_NAME)
                    .fieldName(PRIMARY_KEY_FIELD_NAME)
                    .tableName(modelSchema.getName())
                    .ownerOf(null)
                    .isNonNull(true)
                    .dataType(SQLiteDataType.TEXT)
                    .build();
            sqlColumns.put(PRIMARY_KEY_FIELD_NAME, primaryKeyColumn);
        }
        for (ModelField modelField : modelSchema.getFields().values()) {
            final ModelAssociation association = associations.get(modelField.getName());
            final boolean isAssociated = association != null;
            // Skip if the field represents an association
            // and is NOT the foreign key
            if (isAssociated && !association.isOwner()) {
                continue;
            }
            // All associated fields are also foreign keys at this point
            SQLiteColumn column = SQLiteColumn.builder()
                    .name(isAssociated
                            ? getForeignKeyColumnName(modelSchema.getVersion(), modelField.getName(), association)
                            : modelField.getName())
                    .fieldName(modelField.getName())
                    .tableName(modelSchema.getName())
                    .ownerOf(isAssociated
                            ? association.getAssociatedType()
                            : null)
                    .ownerField(isAssociated
                            ? modelField.getName()
                            : null)
                    .isNonNull(modelField.isRequired())
                    .dataType(sqlTypeFromModelField(modelField))
                    .build();
            sqlColumns.put(modelField.getName(), column);
        }

        return SQLiteTable.builder()
                .name(modelSchema.getName())
                .type(modelSchema.getModelType())
                .columns(sqlColumns)
                .primaryKeyColumns(modelSchema.getPrimaryIndexFields())
                .build();
    }

    private static SQLiteDataType sqlTypeFromModelField(ModelField modelField) {
        return TypeConverter.getSQLiteDataType(modelField);
    }

    /**
     * Returns a builder to construct SQLite table instance.
     * @return a builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the name of table.
     * @return the name of table
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Returns the primary key of this table.
     * @return the primary key of this table
     */
    @Nullable
    public SQLiteColumn getPrimaryKey() {
        for (SQLiteColumn column : sortedColumns) {
            if (type == Model.Type.USER) {
                if (primaryKeyColumns.size() > 1) {
                    if (column.getName().equals(PRIMARY_KEY_FIELD_NAME)) {
                        return column;
                    }
                } else if (primaryKeyColumns.size() == 1 && primaryKeyColumns.contains(column.getName())) {
                    return column;
                }
            }
            if ((primaryKeyColumns.size() <= 1) && column.isPrimaryKey()) {
                return column;
            }
        }
        return null;
    }

    /**
     * Returns the type of this table.
     * @return the type of this table
     */
    @NonNull
    public Model.Type getType() {
        return type;
    }

    /**
     * Returns the primary key of this table.
     * @return the primary key of this table
     */
    @NonNull
    public List<String> getPrimaryKeyColumns() {
        return primaryKeyColumns;
    }

    /**
     * Returns the column name of primary key.
     * Return "id" if no column identifies as primary key.
     * @return the column name of primary key
     */
    @NonNull
    public String getPrimaryKeyColumnName() {
        if (getPrimaryKey() == null) {
            return PrimaryKey.fieldName();
        }
        return getPrimaryKey().getQuotedColumnName();
    }

    /**
     * Returns the column name of foreign key for the associated field passed in.
     * @param schemaVersion int
     * @param fieldName String associated filedName ModelAssociation association
     * @param association ModelAssociation
     * @return the column name of foreign key
     */
    @NonNull
    public static String getForeignKeyColumnName(int schemaVersion, String fieldName, ModelAssociation association) {
        if (schemaVersion == 0) {
            return association.getTargetName();
        }
        if (association.getTargetNames().length > 1) {
            return "@@" + fieldName + "ForeignKey";
        }
        return association.getTargetNames()[0];
    }

    /**
     * Returns the list of foreign keys of this table.
     * @return the list of foreign keys of this table
     */
    @NonNull
    public List<SQLiteColumn> getForeignKeys() {
        List<SQLiteColumn> foreignKeys = new LinkedList<>();
        for (SQLiteColumn column : sortedColumns) {
            if (column.isForeignKey()) {
                foreignKeys.add(column);
            }
        }
        return Immutable.of(foreignKeys);
    }

    /**
     * Returns the SQLiteColumn for the provided column name.
     * @param columnName name of the column
     * @return the SQLiteColumn for the provided column name.
     * @throws DataStoreException if no column is found for the provided columnName.
     */
    @NonNull
    public SQLiteColumn getColumn(@NonNull String columnName) throws DataStoreException {
        SQLiteColumn column = columns.get(Objects.requireNonNull(columnName));
        if (column == null) {
            throw new DataStoreException("Column with columnName " + columnName + " not found in " + name,
                    AmplifyException.TODO_RECOVERY_SUGGESTION);
        }
        return column;
    }

    /**
     * Returns the map of field names to columns of this table.
     * @return the map of field names to columns of this table
     */
    @NonNull
    public Map<String, SQLiteColumn> getColumns() {
        return Immutable.of(columns);
    }

    /**
     * Returns the sorted list of columns of this table.
     * @return the sorted list of columns of this table
     */
    @NonNull
    public List<SQLiteColumn> getSortedColumns() {
        return Immutable.of(sortedColumns);
    }

    private List<SQLiteColumn> sortedColumns() {
        if (columns == null) {
            return null;
        }

        // Create a list from elements of sortedColumns
        final List<SQLiteColumn> columnEntries = new LinkedList<>(columns.values());

        // Returns an array of the values sorted by some pre-defined rules:
        //
        // 1. primary key comes always first
        // 2. foreign keys come always at the end
        // 3. the other columns are sorted alphabetically
        //
        // This is useful so code that uses the sortedColumns to generate queries and other
        // persistence-related operations guarantee that the results are always consistent.
        Collections.sort(columnEntries, (columnOne, columnOther) -> {
            if (columnOne.isPrimaryKey()) {
                return -1;
            }
            if (columnOther.isPrimaryKey()) {
                return 1;
            }
            if (columnOne.isForeignKey() && !columnOther.isForeignKey()) {
                return 1;
            }
            if (!columnOne.isForeignKey() && columnOther.isForeignKey()) {
                return -1;
            }
            return columnOne.getName().compareTo(columnOther.getName());
        });

        return columnEntries;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SQLiteTable that = (SQLiteTable) obj;
        return ObjectsCompat.equals(name, that.name) &&
                ObjectsCompat.equals(columns, that.columns) &&
                ObjectsCompat.equals(sortedColumns, that.sortedColumns);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(name, columns, sortedColumns);
    }

    @Override
    public String toString() {
        return "SQLiteTable{" +
                "name='" + name + '\'' +
                ", columns=" + columns +
                ", sortedColumns=" + sortedColumns +
                '}';
    }

    /**
     * A builder to construct immutable SQLite table.
     */
    public static final class Builder {
        private final Map<String, SQLiteColumn> columns;
        private List<String> primaryKeyColumns;
        private String name;
        private Model.Type type;

        Builder() {
            this.columns = new HashMap<>();
        }

        /**
         * Sets the name of the table.
         * @param name the name of the table
         * @return builder instance with given name
         */
        @NonNull
        public Builder name(@NonNull String name) {
            this.name = Objects.requireNonNull(name);
            return this;
        }

        /**
         * Sets the type of the table.
         * @param type the type of the table.
         * @return builder instance with given type.
         */
        @NonNull
        public Builder type(@NonNull Model.Type type) {
            this.type = Objects.requireNonNull(type);
            return this;
        }

        /**
         * Sets the list of primary key columns of the table.
         * @param primaryKeyColumns the name of the table
         * @return builder instance with given name
         */
        @NonNull
        public Builder primaryKeyColumns(@NonNull List<String> primaryKeyColumns) {
            this.primaryKeyColumns = Objects.requireNonNull(primaryKeyColumns);
            return this;
        }

        /**
         * Sets the map of columns of the table.
         * @param columns the map of columns of the table
         * @return builder instance with given name
         */
        @NonNull
        public Builder columns(@NonNull Map<String, SQLiteColumn> columns) {
            Objects.requireNonNull(columns);
            this.columns.clear();
            this.columns.putAll(columns);
            return this;
        }

        /**
         * Constructs an instance of table using this builder.
         * @return Constructed instance of table
         */
        @SuppressLint("SyntheticAccessor")
        @NonNull
        public SQLiteTable build() {
            return new SQLiteTable(this.name, Immutable.of(this.columns), primaryKeyColumns, type);
        }
    }
}
