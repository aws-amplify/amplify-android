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
                               @Nullable QueryPredicate predicate) {
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        final String tableName = table.getName();
        StringBuilder rawQuery = new StringBuilder();
        StringBuilder selectColumns = new StringBuilder();
        StringBuilder joinStatement = new StringBuilder();
        List<String> selectionArgs = null;

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
                    .append(ownedTable.getPrimaryKey().getColumnName());

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
            if (column.isPrimaryKey()) {
                selectColumns.append(SqlKeyword.DELIMITER)
                        .append(SqlKeyword.AS)
                        .append(SqlKeyword.DELIMITER)
                        .append(column.getAliasedName());
            }

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
            selectionArgs = new LinkedList<>();
            rawQuery.append(SqlKeyword.DELIMITER)
                    .append(SqlKeyword.WHERE)
                    .append(SqlKeyword.DELIMITER)
                    .append(parsePredicate(predicate, selectionArgs));
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
            final ModelSchema connectedSchema = ModelSchemaRegistry.singleton()
                    .getModelSchemaForModelClass(connectedType);
            String connectedId = SQLiteTable.fromSchema(connectedSchema)
                    .getPrimaryKey()
                    .getName();

            builder.append("FOREIGN KEY")
                    .append(SqlKeyword.DELIMITER)
                    .append("(" + connectedName + ")")
                    .append(SqlKeyword.DELIMITER)
                    .append("REFERENCES")
                    .append(SqlKeyword.DELIMITER)
                    .append(connectedType)
                    .append("(" + connectedId + ")");

            if (foreignKeyIterator.hasNext()) {
                builder.append(",").append(SqlKeyword.DELIMITER);
            }
        }
        return builder;
    }

    // Utility method to recursively parse a given predicate.
    private StringBuilder parsePredicate(QueryPredicate queryPredicate, List<String> args) {
        if (queryPredicate instanceof QueryPredicateOperation) {
            QueryPredicateOperation qpo = (QueryPredicateOperation) queryPredicate;
            return parsePredicateOperation(qpo, args);
        } else if (queryPredicate instanceof QueryPredicateGroup) {
            QueryPredicateGroup qpg = (QueryPredicateGroup) queryPredicate;
            return parsePredicateGroup(qpg, args);
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

    // Utility method to recursively parse a given predicate operation.
    private StringBuilder parsePredicateOperation(QueryPredicateOperation operation, List<String> args) {
        final StringBuilder builder = new StringBuilder();
        final String field = operation.field();
        final QueryOperator op = operation.operator();
        switch (op.type()) {
            case BETWEEN:
                BetweenQueryOperator betweenOp = (BetweenQueryOperator) op;
                Object start = betweenOp.start();
                Object end = betweenOp.end();
                QueryPredicateOperation gt = new QueryPredicateOperation(field,
                        new GreaterThanQueryOperator(start));
                QueryPredicateOperation lt = new QueryPredicateOperation(field,
                        new LessThanQueryOperator(end));
                return parsePredicate(gt.and(lt), args);
            case CONTAINS:
                ContainsQueryOperator containsOp = (ContainsQueryOperator) op;
                args.add(containsOp.value().toString());
                return builder.append("?")
                        .append(SqlKeyword.DELIMITER)
                        .append(SqlKeyword.IN)
                        .append(SqlKeyword.DELIMITER)
                        .append(field);
            case BEGINS_WITH:
                BeginsWithQueryOperator beginsWithOp = (BeginsWithQueryOperator) op;
                args.add(beginsWithOp.value() + "%");
                return builder.append(field)
                        .append(SqlKeyword.DELIMITER)
                        .append(SqlKeyword.LIKE)
                        .append(SqlKeyword.DELIMITER)
                        .append("?");
            case EQUAL:
            case NOT_EQUAL:
            case LESS_THAN:
            case GREATER_THAN:
            case LESS_OR_EQUAL:
            case GREATER_OR_EQUAL:
                args.add(getOperatorValue(op).toString());
                return builder.append(field)
                        .append(SqlKeyword.DELIMITER)
                        .append(SqlKeyword.fromQueryOperator(op.type()))
                        .append(SqlKeyword.DELIMITER)
                        .append("?");
            default:
                throw new UnsupportedTypeException(
                        "Tried to parse an unsupported QueryPredicateOperation",
                        null,
                        "Try changing to one of the supported values from " +
                                "QueryPredicateOperation.Type enum.",
                        false
                );
        }
    }

    // Utility method to recursively parse a given predicate group.
    private StringBuilder parsePredicateGroup(QueryPredicateGroup group, List<String> args) {
        final StringBuilder builder = new StringBuilder();
        switch (group.type()) {
            case NOT:
                return builder.append(SqlKeyword.fromQueryPredicateGroup(group.type()))
                        .append(SqlKeyword.DELIMITER)
                        .append(parsePredicate(group.predicates().get(0), args));
            case OR:
            case AND:
                builder.append("(");
                Iterator<QueryPredicate> predicateIterator = group.predicates().iterator();
                while (predicateIterator.hasNext()) {
                    builder.append(parsePredicate(predicateIterator.next(), args));
                    if (predicateIterator.hasNext()) {
                        builder.append(SqlKeyword.DELIMITER)
                                .append(SqlKeyword.fromQueryPredicateGroup(group.type()))
                                .append(SqlKeyword.DELIMITER);
                    }
                }
                return builder.append(")");
            default:
                throw new UnsupportedTypeException(
                        "Tried to parse an unsupported QueryPredicateGroup",
                        null,
                        "Try changing to one of the supported values from " +
                                "QueryPredicateGroup.Type enum.",
                        false
                );
        }
    }

    // Utility method to extract the parameter value from a given operator.
    private Object getOperatorValue(QueryOperator qOp) throws UnsupportedTypeException {
        switch (qOp.type()) {
            case NOT_EQUAL:
                return ((NotEqualQueryOperator) qOp).value();
            case EQUAL:
                return ((EqualQueryOperator) qOp).value();
            case LESS_OR_EQUAL:
                return ((LessOrEqualQueryOperator) qOp).value();
            case LESS_THAN:
                return ((LessThanQueryOperator) qOp).value();
            case GREATER_OR_EQUAL:
                return ((GreaterOrEqualQueryOperator) qOp).value();
            case GREATER_THAN:
                return ((GreaterThanQueryOperator) qOp).value();
            default:
                throw new UnsupportedTypeException(
                        "Tried to parse an unsupported QueryOperator type",
                        null,
                        "Check if a new QueryOperator.Type enum has been created which is not supported" +
                                "in the AppSyncGraphQLRequestFactory.",
                        false
                );
        }
    }
}
