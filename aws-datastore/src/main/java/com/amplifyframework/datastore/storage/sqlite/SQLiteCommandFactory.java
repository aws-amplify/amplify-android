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

import android.util.Log;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelAssociation;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelIndex;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.PrimaryKey;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.QueryPaginationInput;
import com.amplifyframework.core.model.query.QuerySortBy;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicateOperation;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLPredicate;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteColumn;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable;
import com.amplifyframework.util.Empty;
import com.amplifyframework.util.Immutable;
import com.amplifyframework.util.UserAgent;
import com.amplifyframework.util.Wrap;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A factory that produces the SQLite commands for a given
 * {@link Model} and {@link ModelSchema}.
 */
final class SQLiteCommandFactory implements SQLCommandFactory {
    /**
     * Undefined is the name of the index annotation created by codegen for custom primary key based on the design for
     * Custom primary key.
     */
    public static final String UNDEFINED = "undefined";

    private final SchemaRegistry schemaRegistry;
    private final Gson gson;

    /**
     * Default constructor.
     */
    SQLiteCommandFactory(
            @NonNull SchemaRegistry schemaRegistry,
            @NonNull Gson gson) {
        this.schemaRegistry = Objects.requireNonNull(schemaRegistry);
        this.gson = Objects.requireNonNull(gson);
    }

    @NonNull
    @Override
    public SqlCommand createTableFor(@NonNull ModelSchema modelSchema) {
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CREATE TABLE IF NOT EXISTS")
                .append(SqlKeyword.DELIMITER)
                .append(Wrap.inBackticks(table.getName()))
                .append(SqlKeyword.DELIMITER);
        if (Empty.check(table.getColumns())) {
            return new SqlCommand(table.getName(), stringBuilder.toString());
        }

        stringBuilder.append("(").append(parseColumns(table))
                .append(",")
                .append(SqlKeyword.DELIMITER)
                .append(createPrimaryKey(modelSchema).toString());
        if (!table.getForeignKeys().isEmpty()) {
            stringBuilder.append(",")
                    .append(SqlKeyword.DELIMITER)
                    .append(parseForeignKeys(table));
        }
        stringBuilder.append(");");

        final String createSqlStatement = stringBuilder.toString();
        return new SqlCommand(table.getName(), createSqlStatement);
    }

    @NonNull
    @Override
    public Set<SqlCommand> createIndexesFor(@NonNull ModelSchema modelSchema) {
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        Set<SqlCommand> indexCommands = new HashSet<>();
        for (ModelIndex modelIndex : modelSchema.getIndexes().values()) {
            if (shouldCreateIndex(modelIndex, modelSchema.getAssociations())) {
                indexCommands.add(createIndexCommand(table.getName(), modelIndex.getIndexName(),
                        modelIndex.getIndexFieldNames()));
            }
        }
        return Immutable.of(indexCommands);
    }

    @NonNull
    public Set<SqlCommand> createIndexesForForeignKeys(@NonNull ModelSchema modelSchema) {
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        Set<SqlCommand> indexCommands = new HashSet<>();
        for (SQLiteColumn foreignKey : table.getForeignKeys()) {
            String connectedId = foreignKey.getName();
            String fkIndexName = table.getName() + connectedId;
            indexCommands.add(createIndexCommand(table.getName(),
                    fkIndexName, Collections.singletonList(connectedId)));
        }
        return Immutable.of(indexCommands);
    }

    @NonNull
    private SqlCommand createIndexCommand(String tableName,
                                          String indexName,
                                          List<String> indexFieldNames) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CREATE INDEX IF NOT EXISTS")
                .append(SqlKeyword.DELIMITER)
                .append(Wrap.inBackticks(getIndexName(indexName,
                        indexFieldNames)))
                .append(SqlKeyword.DELIMITER)
                .append(SqlKeyword.ON)
                .append(SqlKeyword.DELIMITER)
                .append(Wrap.inBackticks(tableName))
                .append(SqlKeyword.DELIMITER);

        stringBuilder.append("(");
        Iterator<String> iterator = indexFieldNames.iterator();
        while (iterator.hasNext()) {
            final String indexColumnName = iterator.next();
            stringBuilder.append(Wrap.inBackticks(indexColumnName));
            if (iterator.hasNext()) {
                stringBuilder.append(",").append(SqlKeyword.DELIMITER);
            }
        }
        stringBuilder.append(");");
        return new SqlCommand(tableName, stringBuilder.toString());
    }

    @NonNull
    @Override
    public SqlCommand queryFor(@NonNull ModelSchema modelSchema,
                               @NonNull QueryOptions options) throws DataStoreException {
        // Initialize table schema and name
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        final String tableName = table.getName();

        // Builders for different parts of the SQL query
        StringBuilder rawQuery = new StringBuilder();
        StringBuilder selectColumns = new StringBuilder();
        StringBuilder joinStatement = new StringBuilder();

        // To hold query parameters for prepared statements
        final List<Object> bindings = new ArrayList<>();

        // Map to hold columns to be selected from each table
        Map<String, List<SQLiteColumn>> columns = new HashMap<>();
        columns.put(tableName, table.getSortedColumns());

        // Set to track tables that have already been joined
        Set<String> joinedTables = new HashSet<>();
        recursivelyBuildJoins(table, columns, joinStatement, "", joinedTables, new ArrayList<>());

        // Constructing the SELECT part of the SQL query
        for (Map.Entry<String, List<SQLiteColumn>> entry : columns.entrySet()) {
            String tableAlias = entry.getKey();
            // Ensure that the column list is not null
            List<SQLiteColumn> columnList = Objects.requireNonNull(columns.get(tableAlias),
                    "Column list cannot be null for table alias: " + tableAlias);

            for (SQLiteColumn column : columnList) {
                if (selectColumns.length() > 0) {
                    selectColumns.append(",").append(SqlKeyword.DELIMITER);;
                }
                String columnName = column.getQuotedColumnName().replaceFirst(column.getTableName(), tableAlias);
                String columnAlias = column.getAliasedName();

                selectColumns.append(columnName)
                        .append(SqlKeyword.DELIMITER)
                        .append(SqlKeyword.AS)
                        .append(SqlKeyword.DELIMITER)
                        .append(Wrap.inBackticks(columnAlias));
            }
        }

        // Start SELECT statement.
        // SELECT columns FROM tableName
        rawQuery.append(SqlKeyword.SELECT)
                .append(SqlKeyword.DELIMITER)
                .append(selectColumns)
                .append(SqlKeyword.DELIMITER)
                .append(SqlKeyword.FROM)
                .append(SqlKeyword.DELIMITER)
                .append(Wrap.inBackticks(tableName));


        // Append join statements.
        // INNER JOIN tableOne ON tableName.id=tableOne.foreignKey
        // LEFT JOIN tableTwo ON tableName.id=tableTwo.foreignKey
        if (!joinStatement.toString().isEmpty()) {
            rawQuery.append(SqlKeyword.DELIMITER)
                    .append(joinStatement);
        }

        // Append predicates.
        // WHERE condition
        final QueryPredicate predicate = options.getQueryPredicate();
        if (!QueryPredicates.all().equals(predicate)) {
            final SQLPredicate sqlPredicate = new SQLPredicate(predicate);
            bindings.addAll(sqlPredicate.getBindings());
            String sqlPredicateString = sqlPredicate.toString();
            if (predicate instanceof QueryPredicateOperation) {
                QueryPredicateOperation<?> predicateOperation = (QueryPredicateOperation<?>) predicate;
                String predicateOperationField = predicateOperation.field();
                sqlPredicateString = getFlutterString(sqlPredicateString, predicateOperation);
                if (predicateOperationField.equals(PrimaryKey.fieldName()) && predicateOperation.modelName() == null
                        && predicateOperation.operator().type() == QueryOperator.Type.EQUAL) {
                    // The WHERE condition is Where.id("some-ID") but no model name is given.
                    sqlPredicateString = sqlPredicateString.replace(predicateOperationField,
                            tableName + "." + predicateOperationField);
                }
            }
            rawQuery.append(SqlKeyword.DELIMITER)
                    .append(SqlKeyword.WHERE)
                    .append(SqlKeyword.DELIMITER)
                    .append(sqlPredicateString);
        }

        // Append order by
        final List<QuerySortBy> sortByList = options.getSortBy();
        if (sortByList != null) {
            rawQuery.append(SqlKeyword.DELIMITER)
                    .append(SqlKeyword.ORDER_BY)
                    .append(SqlKeyword.DELIMITER);
            Iterator<QuerySortBy> sortByIterator = sortByList.iterator();
            while (sortByIterator.hasNext()) {
                final QuerySortBy sortBy = sortByIterator.next();
                String modelName = Wrap.inBackticks(sortBy.getModelName());
                String fieldName = Wrap.inBackticks(sortBy.getField());
                if (modelName == null) {
                    modelName = Wrap.inBackticks(tableName);
                }
                final String columnName = modelName + "." + fieldName;
                rawQuery.append(columnName)
                        .append(SqlKeyword.DELIMITER)
                        .append(SqlKeyword.fromQuerySortOrder(sortBy.getSortOrder()));

                if (sortByIterator.hasNext()) {
                    rawQuery.append(",")
                            .append(SqlKeyword.DELIMITER);
                }
            }
        }

        // Append pagination after order by
        final QueryPaginationInput paginationInput = options.getPaginationInput();
        if (paginationInput != null) {
            rawQuery.append(SqlKeyword.DELIMITER)
                .append(SqlKeyword.LIMIT)
                .append(SqlKeyword.DELIMITER)
                .append("?")
                .append(SqlKeyword.DELIMITER)
                .append(SqlKeyword.OFFSET)
                .append(SqlKeyword.DELIMITER)
                .append("?");
            bindings.add(paginationInput.getLimit());
            bindings.add(paginationInput.getPage() * paginationInput.getLimit());
        }

        rawQuery.append(";");
        final String queryString = rawQuery.toString();
        Log.d("SQLiteCommandFactory","Query: "+queryString);
        return new SqlCommand(table.getName(), queryString, bindings);
    }

    private String getFlutterString(String sqlPredicateString, QueryPredicateOperation<?> predicateOperation) {
        String predicateOperationField = predicateOperation.field();
        String updatedSqlPredicateString = sqlPredicateString;
        if (UserAgent.isFlutter() && !Empty.check(predicateOperation.field())
                && predicateOperationField.startsWith("@@") && predicateOperation.modelName() == null) {
            updatedSqlPredicateString = updatedSqlPredicateString.replace(predicateOperationField,
                    Wrap.inBackticks(predicateOperationField));
        }
        return updatedSqlPredicateString;
    }

    @NonNull
    @Override
    public SqlCommand existsFor(@NonNull ModelSchema modelSchema,
                                @NonNull QueryPredicate predicate) throws DataStoreException {
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        final String tableName = table.getName();
        StringBuilder rawQuery = new StringBuilder();
        final List<Object> bindings = new ArrayList<>();

        // Start SELECT statement.
        // SELECT EXISTS(SELECT 1 FROM tableName
        rawQuery.append(SqlKeyword.SELECT)
                .append(SqlKeyword.DELIMITER)
                .append(SqlKeyword.EXISTS)
                .append("(")
                .append(SqlKeyword.SELECT)
                .append(SqlKeyword.DELIMITER)
                .append("1")
                .append(SqlKeyword.DELIMITER)
                .append(SqlKeyword.FROM)
                .append(SqlKeyword.DELIMITER)
                .append(Wrap.inBackticks(tableName));

        // Append predicates.
        // WHERE condition
        if (!QueryPredicates.all().equals(predicate)) {
            final SQLPredicate sqlPredicate = new SQLPredicate(predicate);
            bindings.addAll(sqlPredicate.getBindings());
            rawQuery.append(SqlKeyword.DELIMITER)
                    .append(SqlKeyword.WHERE)
                    .append(SqlKeyword.DELIMITER)
                    .append(sqlPredicate);
        }

        // Close the parentheses for EXISTS, and end with a semicolon.
        rawQuery.append(");");
        final String queryString = rawQuery.toString();
        return new SqlCommand(table.getName(), queryString, bindings);
    }

    @NonNull
    @Override
    public <T extends Model> SqlCommand insertFor(@NonNull ModelSchema modelSchema,
                                                  @NonNull T item) throws DataStoreException {
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("INSERT INTO")
                .append(SqlKeyword.DELIMITER)
                .append(Wrap.inBackticks(table.getName()))
                .append(SqlKeyword.DELIMITER)
                .append("(");
        final List<SQLiteColumn> columns = table.getSortedColumns();
        final Iterator<SQLiteColumn> columnsIterator = columns.iterator();
        while (columnsIterator.hasNext()) {
            final String columnName = columnsIterator.next().getName();
            stringBuilder.append(Wrap.inBackticks(columnName));
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

        return new SqlCommand(table.getName(),
                preparedInsertStatement,
                extractFieldValues(item) // VALUES clause
        );
    }

    @NonNull
    @Override
    public <T extends Model> SqlCommand updateFor(@NonNull ModelSchema modelSchema,
                                                  @NonNull T model) throws DataStoreException {
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UPDATE")
                .append(SqlKeyword.DELIMITER)
                .append(Wrap.inBackticks(table.getName()))
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
            stringBuilder.append(Wrap.inBackticks(columnName))
                    .append(SqlKeyword.DELIMITER)
                    .append(SqlKeyword.EQUAL)
                    .append(SqlKeyword.DELIMITER)
                    .append("?");
            if (columnsIterator.hasNext()) {
                stringBuilder.append(", ");
            }
        }

        // Append WHERE statement
        final SQLiteTable sqliteTable = SQLiteTable.fromSchema(modelSchema);
        final String primaryKeyName = sqliteTable.getPrimaryKeyColumnName();
        final QueryPredicate matchId = QueryField.field(primaryKeyName).eq(model.getPrimaryKeyString());
        SQLPredicate sqlPredicate = new SQLPredicate(matchId);
        stringBuilder.append(SqlKeyword.DELIMITER)
                .append(SqlKeyword.WHERE)
                .append(SqlKeyword.DELIMITER)
                .append(sqlPredicate)
                .append(";");

        final String preparedUpdateStatement = stringBuilder.toString();
        List<Object> bindings = extractFieldValues(model); // SET clause
        bindings.addAll(sqlPredicate.getBindings()); // WHERE clause
        return new SqlCommand(table.getName(),
                preparedUpdateStatement,
                bindings);
    }

    @NonNull
    @Override
    public SqlCommand deleteFor(@NonNull ModelSchema modelSchema,
                                @NonNull QueryPredicate predicate) throws DataStoreException {
        final SQLiteTable table = SQLiteTable.fromSchema(modelSchema);
        final SQLPredicate sqlPredicate = new SQLPredicate(predicate);

        final String preparedDeleteStatement =
                "DELETE FROM" +
                SqlKeyword.DELIMITER +
                Wrap.inBackticks(table.getName()) +
                SqlKeyword.DELIMITER +
                SqlKeyword.WHERE +
                SqlKeyword.DELIMITER +
                sqlPredicate +
                ";";
        return new SqlCommand(table.getName(),
                preparedDeleteStatement,
                sqlPredicate.getBindings() // WHERE clause
        );
    }

    private String getIndexName(String indexName, List<String> indexFieldNames) {
        if (indexName.equals(UNDEFINED)) {
            StringBuilder indexNameBuilder = new StringBuilder();
            indexNameBuilder.append(UNDEFINED + "_");
            Iterator<String> indexFieldIterator = indexFieldNames.listIterator();
            while (indexFieldIterator.hasNext()) {
                indexNameBuilder.append(indexFieldIterator.next());
                if (indexFieldIterator.hasNext()) {
                    indexNameBuilder.append("_");
                }
            }
            return indexNameBuilder.toString();
        }
        return indexName;
    }

    // extract model field values to save in database
    private List<Object> extractFieldValues(@NonNull Model model) throws DataStoreException {
        final String modelName = model.getModelName();
        final ModelSchema schema = schemaRegistry.getModelSchemaForModelClass(modelName);
        final SQLiteTable table = SQLiteTable.fromSchema(schema);
        final SQLiteModelFieldTypeConverter converter =
                new SQLiteModelFieldTypeConverter(schema, schemaRegistry, gson);
        final Map<String, ModelField> modelFields = schema.getFields();
        final List<Object> bindings = new ArrayList<>();
        Object fieldValue;
        for (SQLiteColumn column : table.getSortedColumns()) {
            if (column.getName().equals(SQLiteTable.PRIMARY_KEY_FIELD_NAME)) {
                fieldValue = model.getPrimaryKeyString();
            } else if (column.isForeignKey()) {
                final ModelField modelField = Objects.requireNonNull(modelFields.get(column.getOwnedField()));
                fieldValue = converter.convertValueFromTarget(model, modelField);
            } else {
                final ModelField modelField = Objects.requireNonNull(modelFields.get(column.getFieldName()));
                fieldValue = converter.convertValueFromTarget(model, modelField);
            }
            bindings.add(fieldValue);
        }
        return bindings;
    }

    /**
     * Recursively builds SQL JOIN statements for a given table and its related tables.
     *
     * @param table The SQLiteTable object representing the current table.
     * @param columns A map that accumulates columns from each table with their respective aliases.
     * @param joinStatement The StringBuilder object to accumulate the JOIN clause of the SQL query.
     * @param parentAlias The alias of the parent table in the JOIN relationship.
     * @param joinedTables A set to keep track of tables that have already been joined to avoid duplicate joins.
     * @param joinPath A list tracking the current path of joins to detect circular references.
     */
    private void recursivelyBuildJoins(SQLiteTable table, Map<String, List<SQLiteColumn>> columns,
                                       StringBuilder joinStatement, String parentAlias, Set<String> joinedTables, List<String> joinPath) {
        if(joinPath.contains(table.getName()))
            return;  // Circular reference detected, stop recursion

        // Add the current table to the join path
        joinPath.add(table.getName());

        for (SQLiteColumn foreignKey : table.getForeignKeys()) {
            String ownedTableName = foreignKey.getOwnedType();
            ModelSchema ownedSchema = schemaRegistry.getModelSchemaForModelClass(ownedTableName);
            // Check if the schema is null and handle the error
            if (ownedSchema == null) {
                throw new IllegalStateException("Could not retrieve schema for the model " + ownedTableName + ", verify that datastore is initialized.");
            }
            SQLiteTable ownedTable = SQLiteTable.fromSchema(ownedSchema);

            String ownedTableAlias = parentAlias.isEmpty() ? ownedTableName : parentAlias + "." + ownedTableName;

            // Check if this join is already performed
            if (joinedTables.contains(ownedTableAlias)) {
                continue;
            }
            joinedTables.add(ownedTableAlias);

            for (SQLiteColumn column : ownedTable.getSortedColumns()) {
                columns.computeIfAbsent(ownedTableAlias, k -> new ArrayList<>()).add(column);
            }

            String joinType = foreignKey.isNonNull() ? String.valueOf(SqlKeyword.INNER_JOIN) : String.valueOf(SqlKeyword.LEFT_JOIN);

            joinStatement.append(joinType)
                    .append(SqlKeyword.DELIMITER)
                    .append(Wrap.inBackticks(ownedTableName))
                    .append(SqlKeyword.DELIMITER)
                    .append(SqlKeyword.AS)
                    .append(SqlKeyword.DELIMITER)
                    .append(Wrap.inBackticks(ownedTableAlias))
                    .append(SqlKeyword.DELIMITER)
                    .append(SqlKeyword.ON)
                    .append(SqlKeyword.DELIMITER)
                    .append(Wrap.inBackticks(parentAlias.isEmpty() ? table.getName() : parentAlias))
                    .append(".")
                    .append(Wrap.inBackticks(foreignKey.getName()))
                    .append(SqlKeyword.DELIMITER)
                    .append(SqlKeyword.EQUAL)
                    .append(SqlKeyword.DELIMITER)
                    .append(Wrap.inBackticks(ownedTableAlias))
                    .append(".")
                    .append(Wrap.inBackticks(ownedTable.getPrimaryKey().getName()))
                    .append(" ");

            recursivelyBuildJoins(ownedTable, columns, joinStatement, ownedTableAlias, joinedTables, new ArrayList<>(joinPath));
        }
        // Remove the current table from the join path before returning
        joinPath.remove(joinPath.size() - 1);
    }


    // Utility method to parse columns in CREATE TABLE
    private StringBuilder parseColumns(SQLiteTable table) {
        final StringBuilder builder = new StringBuilder();
        final Iterator<SQLiteColumn> columnsIterator = table.getSortedColumns().iterator();
        while (columnsIterator.hasNext()) {
            final SQLiteColumn column = columnsIterator.next();
            final String columnName = column.getName();

            builder.append(Wrap.inBackticks(columnName))
                    .append(SqlKeyword.DELIMITER)
                    .append(column.getColumnType());
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
    @SuppressWarnings("StringConcatenationInsideStringBufferAppend") // Maintains groupings
    private StringBuilder parseForeignKeys(SQLiteTable table) {
        final StringBuilder builder = new StringBuilder();
        final Iterator<SQLiteColumn> foreignKeyIterator = table.getForeignKeys().iterator();
        while (foreignKeyIterator.hasNext()) {
            final SQLiteColumn foreignKey = foreignKeyIterator.next();
            String connectedName = foreignKey.getName();
            String connectedType = foreignKey.getOwnedType();
            ModelSchema connectedSchema = schemaRegistry.getModelSchemaForModelClass(connectedType);
            String connectedId = getIdField(connectedSchema.getPrimaryIndexFields(),
                    connectedSchema.getModelType());

            builder.append("FOREIGN KEY")
                    .append(SqlKeyword.DELIMITER)
                    .append("(" + Wrap.inBackticks(connectedName) + ")")
                    .append(SqlKeyword.DELIMITER)
                    .append("REFERENCES")
                    .append(SqlKeyword.DELIMITER)
                    .append(Wrap.inBackticks(connectedType))
                    .append("(" + Wrap.inBackticks(connectedId) + ")")
                    .append(SqlKeyword.DELIMITER)
                    .append("ON DELETE CASCADE");

            if (foreignKeyIterator.hasNext()) {
                builder.append(",").append(SqlKeyword.DELIMITER);
            }
        }
        return builder;
    }

    private String getIdField(List<String> indexFields, Model.Type type) {
        if (type == Model.Type.USER && indexFields.size() > 1) {
            return SQLiteTable.PRIMARY_KEY_FIELD_NAME;
        } else {
            return indexFields.get(0);
        }
    }

    // Utility method to create SQL for Primary key on table
    private StringBuilder createPrimaryKey(@NonNull ModelSchema modelSchema) {
        final StringBuilder builder = new StringBuilder();
        final List<String> indexFields = modelSchema.getPrimaryIndexFields();
        if (indexFields.size() > 0) {
            builder.append("PRIMARY KEY")
                    .append(SqlKeyword.DELIMITER)
                    .append("(")
                    .append(SqlKeyword.DELIMITER).append("'")
                    .append(getIdField(indexFields, modelSchema.getModelType()))
                    .append("'");
        } else {
            builder.append(SqlKeyword.DELIMITER).append("'")
                    .append(indexFields.get(0))
                    .append("'");
        }
        builder.append(")");
        return builder;
    }

    private boolean shouldCreateIndex(ModelIndex modelIndex, Map<String, ModelAssociation> associationMap) {
        if (modelIndex.getIndexName().equals(UNDEFINED) && modelIndex.getIndexFieldNames().size() == 1) {
            return false;
        }
        for (Map.Entry<String, ModelAssociation> associationEntry : associationMap.entrySet()) {
            if (associationEntry.getValue().isOwner()) {
                for (String targetName : associationEntry.getValue().getTargetNames()) {
                    if (modelIndex.getIndexFieldNames().contains(targetName)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
