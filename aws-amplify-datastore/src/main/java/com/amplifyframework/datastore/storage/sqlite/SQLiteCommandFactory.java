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

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.amplifyframework.datastore.model.Model;
import com.amplifyframework.datastore.model.ModelIndex;
import com.amplifyframework.datastore.model.ModelField;
import com.amplifyframework.datastore.model.ModelSchema;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A factory that produces the SQLite commands for a given
 * {@link Model} and {@link ModelSchema}.
 */
final class SQLiteCommandFactory implements SQLCommandFactory {

    // the singleton instance.
    private static SQLiteCommandFactory singletonInstance;

    // Delimiter used in the SQLite commands.
    private static final String SQLITE_COMMAND_DELIMITER = " ";

    private static final Map<String, String> GRAPHQL_TYPES_TO_JAVA_TYPES = new HashMap<>();
    private static final Map<String, String> JAVA_TYPES_TO_SQL_TYPES = new HashMap<>();

    static {
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("ID", String.class.getSimpleName());
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("String", String.class.getSimpleName());
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("Int", int.class.getSimpleName());
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("Float", float.class.getSimpleName());
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("Boolean", boolean.class.getSimpleName());
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("Enum", Enum.class.getSimpleName());
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSDate", java.util.Date.class.getSimpleName());
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSTime", java.sql.Time.class.getSimpleName());
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSDateTime", java.util.Date.class.getSimpleName());
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSTimestamp", long.class.getSimpleName());
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSEmail", String.class.getSimpleName());
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSJSON", String.class.getSimpleName());
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSURL", String.class.getSimpleName());
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSPhone", String.class.getSimpleName());
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSIPAddress", String.class.getSimpleName());

        JAVA_TYPES_TO_SQL_TYPES.put(String.class.getSimpleName(), "TEXT");
        JAVA_TYPES_TO_SQL_TYPES.put(int.class.getSimpleName(), "INTEGER");
        JAVA_TYPES_TO_SQL_TYPES.put(float.class.getSimpleName(), "REAL");
        JAVA_TYPES_TO_SQL_TYPES.put(long.class.getSimpleName(), "REAL");
        JAVA_TYPES_TO_SQL_TYPES.put(boolean.class.getSimpleName(), "INTEGER");
        JAVA_TYPES_TO_SQL_TYPES.put(Enum.class.getSimpleName(), "TEXT");
        JAVA_TYPES_TO_SQL_TYPES.put(java.util.Date.class.getSimpleName(), "TEXT");
        JAVA_TYPES_TO_SQL_TYPES.put(java.sql.Time.class.getSimpleName(), "TEXT");
    }

    private SQLiteCommandFactory() {
    }

    /**
     * Retrieves the singleton instance of the SQLiteCommandFactory.
     * @return the singleton instance of the SQLiteCommandFactory.
     */
    public static synchronized SQLiteCommandFactory getInstance() {
        if (singletonInstance == null) {
            singletonInstance = new SQLiteCommandFactory();
        }
        return singletonInstance;
    }

    /**
     * Generates the CREATE TABLE SQL command from the {@link ModelSchema}.
     *
     * @param modelSchema the schema of a {@link Model}
     *                    for which a CREATE TABLE SQL command needs to be generated.
     * @return the CREATE TABLE SQL command
     */
    @Override
    public SqlCommand createTableFor(@NonNull ModelSchema modelSchema) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CREATE TABLE IF NOT EXISTS " +
                modelSchema.getName() +
                SQLITE_COMMAND_DELIMITER);
        if (modelSchema.getFields() == null || modelSchema.getFields().isEmpty()) {
            return new SqlCommand(modelSchema.getName(), stringBuilder.toString());
        }

        final Iterator<Map.Entry<String, ModelField>> modelFieldMapIterator =
                modelSchema.getFields().entrySet().iterator();
        stringBuilder.append("(");
        while (modelFieldMapIterator.hasNext()) {
            final Map.Entry<String, ModelField> entry = modelFieldMapIterator.next();
            final String modelFieldName = entry.getKey();
            final ModelField modelField = entry.getValue();
            stringBuilder.append(modelFieldName +
                    SQLITE_COMMAND_DELIMITER +
                    getSqlDataTypeForGraphQLType(modelField) +
                    SQLITE_COMMAND_DELIMITER);

            if (modelField.isPrimaryKey()) {
                stringBuilder.append("PRIMARY KEY" + SQLITE_COMMAND_DELIMITER);
            }

            if (modelField.isRequired()) {
                stringBuilder.append("NOT NULL");
            }

            if (modelFieldMapIterator.hasNext()) {
                stringBuilder.append("," + SQLITE_COMMAND_DELIMITER);
            }
        }
        stringBuilder.append(");");

        final String createSqlStatement = stringBuilder.toString();
        return new SqlCommand(modelSchema.getName(), createSqlStatement);
    }

    /**
     * Generates the CREATE INDEX SQL command from the {@link ModelSchema}.
     *
     * @param modelSchema the schema of a {@link Model}
     *                    for which a CREATE INDEX SQL command needs to be generated.
     * @return the CREATE INDEX SQL command
     */
    @Override
    public SqlCommand createIndexFor(@NonNull ModelSchema modelSchema) {
        final ModelIndex modelIndex = modelSchema.getModelIndex();
        if (modelIndex == null ||
            TextUtils.isEmpty(modelIndex.getIndexName()) ||
            modelIndex.getIndexFieldNames() == null ||
            modelIndex.getIndexFieldNames().isEmpty()) {
            return null;
        }

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CREATE INDEX IF NOT EXISTS " +
                modelIndex.getIndexName() +
                " ON " +
                modelSchema.getName() +
                SQLITE_COMMAND_DELIMITER);

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
        return new SqlCommand(modelSchema.getName(), stringBuilder.toString());
    }

    private static String getSqlDataTypeForGraphQLType(@NonNull final ModelField modelField) {
        return JAVA_TYPES_TO_SQL_TYPES.get(GRAPHQL_TYPES_TO_JAVA_TYPES.get(modelField.getTargetType()));
    }
}
