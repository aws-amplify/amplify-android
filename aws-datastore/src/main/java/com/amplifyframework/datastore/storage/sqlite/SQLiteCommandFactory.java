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

import com.amplifyframework.core.Immutable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelIndex;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.core.model.PrimaryKey;
import com.amplifyframework.core.model.query.predicate.BeginsWithQueryOperator;
import com.amplifyframework.core.model.query.predicate.BetweenQueryOperator;
import com.amplifyframework.core.model.query.predicate.ContainsQueryOperator;
import com.amplifyframework.core.model.query.predicate.EqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.GreaterOrEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.GreaterThanQueryOperator;
import com.amplifyframework.core.model.query.predicate.LessOrEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.LessThanQueryOperator;
import com.amplifyframework.core.model.query.predicate.NotEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicateGroup;
import com.amplifyframework.core.model.query.predicate.QueryPredicateOperation;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteColumn;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable;
import com.amplifyframework.util.CollectionUtils;
import com.amplifyframework.util.StringUtils;

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

    // Delimiter used in the SQLite commands.
    private static final String SQLITE_COMMAND_DELIMITER = " ";

    // Connection handle to a Sqlite Database.
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
        stringBuilder.append("CREATE TABLE IF NOT EXISTS ")
            .append(table.getName())
            .append(SQLITE_COMMAND_DELIMITER);
        if (CollectionUtils.isNullOrEmpty(table.getColumns())) {
            return new SqlCommand(table.getName(), stringBuilder.toString());
        }

        stringBuilder.append("(");
        appendColumns(stringBuilder, table);
        if (!table.getForeignKeys().isEmpty()) {
            stringBuilder.append("," + SQLITE_COMMAND_DELIMITER);
            appendForeignKeys(stringBuilder, table);
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
            stringBuilder.append("CREATE INDEX IF NOT EXISTS ")
                    .append(modelIndex.getIndexName()).append(" ON ")
                    .append(table.getName())
                    .append(SQLITE_COMMAND_DELIMITER);

            stringBuilder.append("(");
            Iterator<String> iterator = modelIndex.getIndexFieldNames().iterator();
            while (iterator.hasNext()) {
                final String indexColumnName = iterator.next();
                stringBuilder.append(indexColumnName);
                if (iterator.hasNext()) {
                    stringBuilder.append("," + SQLITE_COMMAND_DELIMITER);
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
                               @Nullable QueryPredicate predicate) {
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        final String tableName = table.getName();
        StringBuilder rawQuery = new StringBuilder();
        StringBuilder selectColumns = new StringBuilder();
        StringBuilder joinStatement = new StringBuilder();

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

            String joinType = foreignKey.isNonNull()
                    ? SqlCommand.INNER_JOIN_CLAUSE
                    : SqlCommand.LEFT_JOIN_CLAUSE;

            joinStatement.append(joinType)
                    .append(SqlCommand.DELIMITER)
                    .append(ownedTableName)
                    .append(SqlCommand.DELIMITER)
                    .append(SqlCommand.ON_CLAUSE)
                    .append(SqlCommand.DELIMITER)
                    .append(foreignKey.getColumnName())
                    .append("=")
                    .append(ownedTable.getPrimaryKey().getColumnName());

            if (foreignKeyIterator.hasNext()) {
                joinStatement.append(SqlCommand.DELIMITER);
            }
        }

        // Convert columns to comma-separated column names
        Iterator<SQLiteColumn> columnsIterator = columns.iterator();
        while (columnsIterator.hasNext()) {
            final SQLiteColumn column = columnsIterator.next();
            selectColumns.append(column.getColumnName());

            // Alias primary keys to avoid duplicate column names
            if (column.isPrimaryKey()) {
                selectColumns.append(SqlCommand.DELIMITER)
                        .append(SqlCommand.AS_CLAUSE)
                        .append(SqlCommand.DELIMITER)
                        .append(column.getAliasedName());
            }

            if (columnsIterator.hasNext()) {
                selectColumns.append(",").append(SqlCommand.DELIMITER);
            }
        }

        // Start SELECT statement.
        // SELECT columns FROM tableName
        rawQuery.append(SqlCommand.SELECT_STATEMENT)
                .append(SqlCommand.DELIMITER)
                .append(selectColumns.toString())
                .append(SqlCommand.DELIMITER)
                .append(SqlCommand.FROM_CLAUSE)
                .append(SqlCommand.DELIMITER)
                .append(tableName);

        // Append join statements.
        // INNER JOIN tableOne ON tableName.id=tableOne.foreignKey
        // LEFT JOIN tableTwo ON tableName.id=tableTwo.foreignKey
        if (!joinStatement.toString().isEmpty()) {
            rawQuery.append(SqlCommand.DELIMITER)
                    .append(joinStatement.toString());
        }

        // Append predicates.
        // WHERE condition
        if (predicate != null) {
            rawQuery.append(SqlCommand.DELIMITER)
                    .append(SqlCommand.WHERE_STATEMENT)
                    .append(SqlCommand.DELIMITER)
                    .append(parsePredicate(predicate));
        }

        rawQuery.append(";");
        final String queryString = rawQuery.toString();
        return new SqlCommand(table.getName(), queryString);
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
        stringBuilder.append("INSERT INTO ");
        stringBuilder.append(table.getName());
        stringBuilder.append(" (");
        final List<SQLiteColumn> columns = table.getSortedColumns();
        final Iterator<SQLiteColumn> columnsIterator = columns.iterator();
        while (columnsIterator.hasNext()) {
            final String columnName = columnsIterator.next().getName();
            stringBuilder.append(columnName);
            if (columnsIterator.hasNext()) {
                stringBuilder.append(", ");
            } else {
                stringBuilder.append(")");
            }
        }
        stringBuilder.append(" VALUES ");
        stringBuilder.append("(");
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
                                                  @NonNull T item) {
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("UPDATE ")
                .append(table.getName())
                .append(" SET ");
        final List<ModelField> fields = modelSchema.getSortedFields();
        final Iterator<ModelField> fieldsIterator = fields.iterator();
        while (fieldsIterator.hasNext()) {
            final ModelField field = fieldsIterator.next();
            stringBuilder.append(field.getName()).append(" = ?");
            if (fieldsIterator.hasNext()) {
                stringBuilder.append(", ");
            }
        }

        stringBuilder.append(" WHERE ")
                .append(PrimaryKey.fieldName())
                .append(" = ")
                .append(StringUtils.doubleQuote(item.getId()))
                .append(";");
        final String preparedUpdateStatement = stringBuilder.toString();
        final SQLiteStatement compiledUpdateStatement =
                databaseConnectionHandle.compileStatement(preparedUpdateStatement);
        return new SqlCommand(table.getName(), preparedUpdateStatement, compiledUpdateStatement);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public <T extends Model> SqlCommand deleteFor(@NonNull ModelSchema modelSchema,
                                                  @NonNull T item) {
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("DELETE FROM ")
                .append(table.getName())
                .append(" WHERE ")
                .append(PrimaryKey.fieldName())
                .append(" = ")
                .append(StringUtils.doubleQuote(item.getId()))
                .append(";");
        return new SqlCommand(table.getName(), stringBuilder.toString());
    }

    // Utility method to append columns in CREATE TABLE
    private void appendColumns(StringBuilder stringBuilder, SQLiteTable table) {
        final Iterator<SQLiteColumn> columnsIterator = table.getSortedColumns().iterator();
        while (columnsIterator.hasNext()) {
            final SQLiteColumn column = columnsIterator.next();
            final String columnName = column.getName();

            stringBuilder.append(columnName)
                    .append(SQLITE_COMMAND_DELIMITER)
                    .append(column.getColumnType());

            if (column.isPrimaryKey()) {
                stringBuilder.append(SQLITE_COMMAND_DELIMITER + "PRIMARY KEY");
            }

            if (column.isNonNull()) {
                stringBuilder.append(SQLITE_COMMAND_DELIMITER + "NOT NULL");
            }

            if (columnsIterator.hasNext()) {
                stringBuilder.append("," + SQLITE_COMMAND_DELIMITER);
            }
        }
    }

    // Utility method to append foreign key references in CREATE TABLE
    private void appendForeignKeys(StringBuilder stringBuilder, SQLiteTable table) {
        final Iterator<SQLiteColumn> foreignKeyIterator = table.getForeignKeys().iterator();
        while (foreignKeyIterator.hasNext()) {
            final SQLiteColumn foreignKey = foreignKeyIterator.next();
            String connectedName = foreignKey.getName();
            String connectedType = foreignKey.getOwnedType();
            final ModelSchema connectedSchema = ModelSchemaRegistry.singleton()
                    .getModelSchemaForModelClass(connectedType);
            String connectedId = SQLiteTable.fromSchema(connectedSchema)
                    .getPrimaryKey()
                    .getName();

            stringBuilder
                .append("FOREIGN KEY" + SQLITE_COMMAND_DELIMITER)
                .append("(" + connectedName + ")")
                .append(SQLITE_COMMAND_DELIMITER + "REFERENCES" + SQLITE_COMMAND_DELIMITER)
                .append(connectedType)
                .append("(" + connectedId + ")");

            if (foreignKeyIterator.hasNext()) {
                stringBuilder.append("," + SQLITE_COMMAND_DELIMITER);
            }
        }
    }

    //TODO: Just... FIX THIS HORROR PLEASE
    private String parsePredicate(QueryPredicate queryPredicate) {
        StringBuilder builder = new StringBuilder();
        if (queryPredicate instanceof QueryPredicateOperation) {
            QueryPredicateOperation qpo = (QueryPredicateOperation) queryPredicate;
            final String field = qpo.field();
            final QueryOperator op = qpo.operator();
            String opString;
            Object value;
            switch (op.type()) {
                case BETWEEN:
                    BetweenQueryOperator betweenOp = (BetweenQueryOperator) op;
                    Object start = betweenOp.start();
                    Object end = betweenOp.end();
                    QueryPredicateOperation gt = new QueryPredicateOperation(field,
                            new GreaterThanQueryOperator(start));
                    QueryPredicateOperation lt = new QueryPredicateOperation(field,
                            new LessThanQueryOperator(end));
                    return parsePredicate(gt.and(lt));
                case CONTAINS:
                    opString = "IN";
                    value = ((ContainsQueryOperator) op).value();
                    return builder.append(value.toString())
                            .append(SqlCommand.DELIMITER)
                            .append(opString) //TODO: Properly deal with type
                            .append(SqlCommand.DELIMITER)
                            .append(field) //TODO: TEST
                            .toString();
                case BEGINS_WITH:
                    opString = "LIKE";
                    value = ((BeginsWithQueryOperator) op).value();
                    return builder.append(field)
                            .append(SqlCommand.DELIMITER)
                            .append(opString) //TODO: Properly deal with type
                            .append(SqlCommand.DELIMITER)
                            .append("\'")
                            .append(value.toString()) //TODO: TEST
                            .append("%\'")
                            .toString();
                case EQUAL:
                    value = ((EqualQueryOperator) op).value();
                    opString = "=";
                    break;
                case NOT_EQUAL:
                    value = ((NotEqualQueryOperator) op).value();
                    opString = "!=";
                    break;
                case LESS_THAN:
                    value = ((LessThanQueryOperator) op).value();
                    opString = "<";
                    break;
                case GREATER_THAN:
                    value = ((GreaterThanQueryOperator) op).value();
                    opString = ">";
                    break;
                case LESS_OR_EQUAL:
                    value = ((LessOrEqualQueryOperator) op).value();
                    opString = "<=";
                    break;
                case GREATER_OR_EQUAL:
                    value = ((GreaterOrEqualQueryOperator) op).value();
                    opString = ">=";
                    break;
                default:
                    throw new UnsupportedTypeException(
                            "Tried to parse an unsupported QueryPredicateOperation",
                            null,
                            "Try changing to one of the supported values from " +
                                    "QueryPredicateOperation.Type enum.",
                            false
                    );
            }
            return builder.append(field)
                    .append(SqlCommand.DELIMITER)
                    .append(opString) //TODO: Properly deal with type
                    .append(SqlCommand.DELIMITER)
                    .append(value.toString()) //TODO: TEST
                    .toString();
        } else if (queryPredicate instanceof QueryPredicateGroup) {
            QueryPredicateGroup qpg = (QueryPredicateGroup) queryPredicate;
            switch (qpg.type()) {
                case NOT:
                    return builder.append("NOT")
                            .append(SqlCommand.DELIMITER)
                            .append(parsePredicate(qpg.predicates().get(0)))
                            .toString();
                case OR:
                case AND:
                    Iterator<QueryPredicate> predicateIterator = qpg.predicates().iterator();
                    while (predicateIterator.hasNext()) {
                        builder.append(parsePredicate(predicateIterator.next()));
                        if (predicateIterator.hasNext()) {
                            builder.append(SqlCommand.DELIMITER)
                                    .append(qpg.type().toString())
                                    .append(SqlCommand.DELIMITER);
                        }
                    }
                    return "(" + builder.toString() + ")";
                default:
                    throw new UnsupportedTypeException(
                            "Tried to parse an unsupported QueryPredicateGroup",
                            null,
                            "Try changing to one of the supported values from " +
                                    "QueryPredicateGroup.Type enum.",
                            false
                    );
            }
        } else {
            throw new UnsupportedTypeException(
                    "Tried to parse an unsupported QueryPredicate",
                    null,
                    "Try changing to one of the supported values: " +
                            "QueryPredicateOperation, QueryPredicateGroup.",
                    false
            );
        }
    }
}
