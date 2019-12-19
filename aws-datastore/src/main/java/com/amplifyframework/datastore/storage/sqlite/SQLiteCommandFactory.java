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

package com.amplifyframework.datastore.storage.sqlite;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelIndex;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.core.model.PrimaryKey;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLPredicate;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteColumn;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable;
import com.amplifyframework.util.CollectionUtils;
import com.amplifyframework.util.Immutable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A factory that produces the SQLite commands for a given
 * {@link Model} and {@link ModelSchema}.
 */
final class SQLiteCommandFactory implements SQLCommandFactory {

    // Connection handle to a SQLiteDatabase.
    private final SQLiteDatabase databaseConnectionHandle;

    /**
     * Default constructor.
     */
    SQLiteCommandFactory() {
        this.databaseConnectionHandle = null;
    }

    /**
     * Constructor with databaseConnectionHandle.
     * @param databaseConnectionHandle connection to a SQLiteDatabase.
     */
    SQLiteCommandFactory(@NonNull SQLiteDatabase databaseConnectionHandle) {
        this.databaseConnectionHandle = databaseConnectionHandle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SqlCommand createTableFor(@NonNull ModelSchema modelSchema) {
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CREATE TABLE IF NOT EXISTS")
                .append(SqlKeyword.DELIMITER)
                .append(table.getName())
                .append(SqlKeyword.DELIMITER);
        if (CollectionUtils.isNullOrEmpty(table.getColumns())) {
            return new SqlCommand(table.getName(), stringBuilder.toString());
        }

        stringBuilder.append("(").append(parseColumns(table));
        if (!table.getForeignKeys().isEmpty()) {
            stringBuilder.append(",")
                    .append(SqlKeyword.DELIMITER)
                    .append(parseForeignKeys(table));
        }
        stringBuilder.append(");");

        final String createSqlStatement = stringBuilder.toString();
        return new SqlCommand(table.getName(), createSqlStatement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<SqlCommand> createIndexesFor(@NonNull ModelSchema modelSchema) {
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        Set<SqlCommand> indexCommands = new HashSet<>();

        for (ModelIndex modelIndex : modelSchema.getIndexes().values()) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CREATE INDEX IF NOT EXISTS")
                    .append(SqlKeyword.DELIMITER)
                    .append(modelIndex.getIndexName())
                    .append(SqlKeyword.DELIMITER)
                    .append(SqlKeyword.ON)
                    .append(SqlKeyword.DELIMITER)
                    .append(table.getName())
                    .append(SqlKeyword.DELIMITER);

            stringBuilder.append("(");
            Iterator<String> iterator = modelIndex.getIndexFieldNames().iterator();
            while (iterator.hasNext()) {
                final String indexColumnName = iterator.next();
                stringBuilder.append(indexColumnName);
                if (iterator.hasNext()) {
                    stringBuilder.append(",").append(SqlKeyword.DELIMITER);
                }
            }
            stringBuilder.append(");");
            indexCommands.add(new SqlCommand(table.getName(), stringBuilder.toString()));
        }

        return Immutable.of(indexCommands);
    }

    /**
     * {@inheritDoc}
     *
     * This method should be invoked from a worker thread and not from the main thread
     * as this method calls {@link SQLiteDatabase#compileStatement(String)}.
     */
    @WorkerThread
    @Override
    public SqlCommand queryFor(@NonNull ModelSchema modelSchema,
                               @Nullable QueryPredicate predicate) throws DataStoreException {
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        final String tableName = table.getName();
        StringBuilder rawQuery = new StringBuilder();
        StringBuilder selectColumns = new StringBuilder();
        StringBuilder joinStatement = new StringBuilder();
        List<Object> selectionArgs = null;

        // Track the list of columns to return
        List<SQLiteColumn> columns = new LinkedList<>(table.getSortedColumns());

        // Joins the foreign keys
        // LEFT JOIN if foreign key is optional, INNER JOIN otherwise.
        final Iterator<SQLiteColumn> foreignKeyIterator = table.getForeignKeys().iterator();
        while (foreignKeyIterator.hasNext()) {
            final SQLiteColumn foreignKey = foreignKeyIterator.next();
            final String ownedTableName = foreignKey.getOwnedType();
            final ModelSchema ownedSchema = ModelSchemaRegistry.singleton()
                    .getModelSchemaForModelClass(ownedTableName);
            final SQLiteTable ownedTable = SQLiteTable.fromSchema(ownedSchema);

            columns.addAll(ownedTable.getSortedColumns());

            SqlKeyword joinType = foreignKey.isNonNull()
                    ? SqlKeyword.INNER_JOIN
                    : SqlKeyword.LEFT_JOIN;

            joinStatement.append(joinType)
                    .append(SqlKeyword.DELIMITER)
                    .append(ownedTableName)
                    .append(SqlKeyword.DELIMITER)
                    .append(SqlKeyword.ON)
                    .append(SqlKeyword.DELIMITER)
                    .append(foreignKey.getColumnName())
                    .append(SqlKeyword.EQUAL)
                    .append(ownedTable.getPrimaryKeyColumnName());

            if (foreignKeyIterator.hasNext()) {
                joinStatement.append(SqlKeyword.DELIMITER);
            }
        }

        // Convert columns to comma-separated column names
        Iterator<SQLiteColumn> columnsIterator = columns.iterator();
        while (columnsIterator.hasNext()) {
            final SQLiteColumn column = columnsIterator.next();
            selectColumns.append(column.getColumnName());

            // Alias primary keys to avoid duplicate column names
            selectColumns.append(SqlKeyword.DELIMITER)
                    .append(SqlKeyword.AS)
                    .append(SqlKeyword.DELIMITER)
                    .append(column.getAliasedName());

            if (columnsIterator.hasNext()) {
                selectColumns.append(",").append(SqlKeyword.DELIMITER);
            }
        }

        // Start SELECT statement.
        // SELECT columns FROM tableName
        rawQuery.append(SqlKeyword.SELECT)
                .append(SqlKeyword.DELIMITER)
                .append(selectColumns.toString())
                .append(SqlKeyword.DELIMITER)
                .append(SqlKeyword.FROM)
                .append(SqlKeyword.DELIMITER)
                .append(tableName);

        // Append join statements.
        // INNER JOIN tableOne ON tableName.id=tableOne.foreignKey
        // LEFT JOIN tableTwo ON tableName.id=tableTwo.foreignKey
        if (!joinStatement.toString().isEmpty()) {
            rawQuery.append(SqlKeyword.DELIMITER)
                    .append(joinStatement.toString());
        }

        // Append predicates.
        // WHERE condition
        if (predicate != null) {
            final SQLPredicate sqlPredicate = new SQLPredicate(predicate);
            selectionArgs = sqlPredicate.getSelectionArgs();
            rawQuery.append(SqlKeyword.DELIMITER)
                    .append(SqlKeyword.WHERE)
                    .append(SqlKeyword.DELIMITER)
                    .append(sqlPredicate);
        }

        rawQuery.append(";");
        final String queryString = rawQuery.toString();
        return new SqlCommand(table.getName(), queryString, selectionArgs);
    }

    /**
     * {@inheritDoc}
     *
     * This method should be invoked from a worker thread and not from the main thread
     * as this method calls {@link SQLiteDatabase#compileStatement(String)}.
     */
    @WorkerThread
    @Override
    public SqlCommand insertFor(@NonNull ModelSchema modelSchema) {
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("INSERT INTO")
                .append(SqlKeyword.DELIMITER)
                .append(table.getName())
                .append(SqlKeyword.DELIMITER)
                .append("(");
        final List<SQLiteColumn> columns = table.getSortedColumns();
        final Iterator<SQLiteColumn> columnsIterator = columns.iterator();
        while (columnsIterator.hasNext()) {
            final String columnName = columnsIterator.next().getName();
            stringBuilder.append(columnName);
            if (columnsIterator.hasNext()) {
                stringBuilder.append(",").append(SqlKeyword.DELIMITER);
            }
        }
        stringBuilder.append(")")
                .append(SqlKeyword.DELIMITER)
                .append("VALUES")
                .append(SqlKeyword.DELIMITER)
                .append("(");
        for (int i = 0; i < columns.size(); i++) {
            if (i == columns.size() - 1) {
                stringBuilder.append("?");
            } else {
                stringBuilder.append("?, ");
            }
        }
        stringBuilder.append(")");
        final String preparedInsertStatement = stringBuilder.toString();
        final SQLiteStatement compiledInsertStatement =
                databaseConnectionHandle.compileStatement(preparedInsertStatement);
        return new SqlCommand(table.getName(), preparedInsertStatement, compiledInsertStatement);
    }

    /**
     * {@inheritDoc}
     *
     * This method should be invoked from a worker thread and not from the main thread
     * as this method calls {@link SQLiteDatabase#compileStatement(String)}.
     */
    @WorkerThread
    @Override
    public <T extends Model> SqlCommand updateFor(@NonNull ModelSchema modelSchema,
                                                  @NonNull T item,
                                                  @NonNull QueryPredicate predicate) throws DataStoreException {
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UPDATE")
                .append(SqlKeyword.DELIMITER)
                .append(table.getName())
                .append(SqlKeyword.DELIMITER)
                .append("SET")
                .append(SqlKeyword.DELIMITER);

        // Previously, we figured out the correct column names from the model schema.
        // Instead of figuring out the correct column names again, just iterate
        // over whatever is actually there (since it was "right".)
        final List<SQLiteColumn> columns = table.getSortedColumns();
        final Iterator<SQLiteColumn> columnsIterator = columns.iterator();
        while (columnsIterator.hasNext()) {
            final String columnName = columnsIterator.next().getName();
            stringBuilder.append(columnName)
                    .append(SqlKeyword.DELIMITER)
                    .append(SqlKeyword.EQUAL)
                    .append(SqlKeyword.DELIMITER)
                    .append("?");
            if (columnsIterator.hasNext()) {
                stringBuilder.append(", ");
            }
        }

        // Append WHERE statement
        SQLPredicate sqlPredicate = new SQLPredicate(predicate);
        stringBuilder.append(SqlKeyword.DELIMITER)
                .append(SqlKeyword.WHERE)
                .append(SqlKeyword.DELIMITER)
                .append(sqlPredicate)
                .append(";");

        final String preparedUpdateStatement = stringBuilder.toString();
        final SQLiteStatement compiledUpdateStatement =
                databaseConnectionHandle.compileStatement(preparedUpdateStatement);
        return new SqlCommand(table.getName(),
                preparedUpdateStatement,
                compiledUpdateStatement,
                sqlPredicate.getSelectionArgs()
        );
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public <T extends Model> SqlCommand deleteFor(@NonNull ModelSchema modelSchema,
                                                  @NonNull T item,
                                                  @NonNull QueryPredicate predicate) throws DataStoreException {
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        final StringBuilder stringBuilder = new StringBuilder();
        final SQLPredicate sqlPredicate = new SQLPredicate(predicate);
        stringBuilder.append("DELETE FROM")
                .append(SqlKeyword.DELIMITER)
                .append(table.getName())
                .append(SqlKeyword.DELIMITER)
                .append(SqlKeyword.WHERE)
                .append(SqlKeyword.DELIMITER)
                .append(sqlPredicate)
                .append(";");

        final String preparedDeleteStatement = stringBuilder.toString();
        final SQLiteStatement compiledDeleteStatement =
                databaseConnectionHandle.compileStatement(preparedDeleteStatement);
        return new SqlCommand(table.getName(),
                preparedDeleteStatement,
                compiledDeleteStatement,
                sqlPredicate.getSelectionArgs()
        );
    }

    // Utility method to parse columns in CREATE TABLE
    private StringBuilder parseColumns(SQLiteTable table) {
        final StringBuilder builder = new StringBuilder();
        final Iterator<SQLiteColumn> columnsIterator = table.getSortedColumns().iterator();
        while (columnsIterator.hasNext()) {
            final SQLiteColumn column = columnsIterator.next();
            final String columnName = column.getName();

            builder.append(columnName)
                    .append(SqlKeyword.DELIMITER)
                    .append(column.getColumnType());

            if (column.isPrimaryKey()) {
                builder.append(SqlKeyword.DELIMITER).append("PRIMARY KEY");
            }

            if (column.isNonNull()) {
                builder.append(SqlKeyword.DELIMITER).append("NOT NULL");
            }

            if (columnsIterator.hasNext()) {
                builder.append(",").append(SqlKeyword.DELIMITER);
            }
        }
        return builder;
    }

    // Utility method to parse foreign key references in CREATE TABLE
    private StringBuilder parseForeignKeys(SQLiteTable table) {
        final StringBuilder builder = new StringBuilder();
        final Iterator<SQLiteColumn> foreignKeyIterator = table.getForeignKeys().iterator();
        while (foreignKeyIterator.hasNext()) {
            final SQLiteColumn foreignKey = foreignKeyIterator.next();
            String connectedName = foreignKey.getName();
            String connectedType = foreignKey.getOwnedType();
            String connectedId = PrimaryKey.fieldName();

            builder.append("FOREIGN KEY")
                    .append(SqlKeyword.DELIMITER)
                    .append("(" + connectedName + ")")
                    .append(SqlKeyword.DELIMITER)
                    .append("REFERENCES")
                    .append(SqlKeyword.DELIMITER)
                    .append(connectedType)
                    .append("(" + connectedId + ")")
                    .append(SqlKeyword.DELIMITER)
                    .append("ON DELETE CASCADE");

            if (foreignKeyIterator.hasNext()) {
                builder.append(",").append(SqlKeyword.DELIMITER);
            }
        }
        return builder;
    }
}
