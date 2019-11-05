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

import androidx.annotation.NonNull;

import com.amplifyframework.datastore.model.Model;
import com.amplifyframework.datastore.model.ModelField;
import com.amplifyframework.datastore.model.ModelSchema;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class AndroidSQLiteCommandFactory implements SQLCommandFactory {

    // the singleton instance.
    private static AndroidSQLiteCommandFactory singletonInstance;

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

    private AndroidSQLiteCommandFactory() {
    }

    /**
     * Retrieves the singleton instance of the AndroidSQLiteCommandFactory.
     * @return the singleton instance of the AndroidSQLiteCommandFactory.
     */
    public static synchronized AndroidSQLiteCommandFactory getInstance() {
        if (singletonInstance == null) {
            singletonInstance = new AndroidSQLiteCommandFactory();
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
    public CreateSqlCommand createTableFor(@NonNull ModelSchema modelSchema) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CREATE TABLE IF NOT EXISTS " + modelSchema.getName() + " ");
        if (modelSchema.getFields() == null || modelSchema.getFields().isEmpty()) {
            return new CreateSqlCommand(modelSchema.getName(), stringBuilder.toString());
        }

        final Iterator<Map.Entry<String, ModelField>> modelFieldMapIterator =
                modelSchema.getFields().entrySet().iterator();
        stringBuilder.append("(");
        while (modelFieldMapIterator.hasNext()) {
            final Map.Entry<String, ModelField> entry = modelFieldMapIterator.next();
            final String modelFieldName = entry.getKey();
            final ModelField modelField = entry.getValue();
            stringBuilder.append(modelFieldName +
                    " " +
                    getSqlDataTypeForGraphQLType(modelField) +
                    " ");

            if (modelField.isPrimaryKey()) {
                stringBuilder.append("PRIMARY KEY");
            } else if (modelField.isRequired()) {
                stringBuilder.append("NOT NULL");
            }

            if (modelFieldMapIterator.hasNext()) {
                stringBuilder.append(", ");
            }
        }
        stringBuilder.append(");");

        final String createSqlStatement = stringBuilder.toString();
        return new CreateSqlCommand(modelSchema.getName(), createSqlStatement);
    }

    private static String getSqlDataTypeForGraphQLType(@NonNull final ModelField modelField) {
        return JAVA_TYPES_TO_SQL_TYPES.get(GRAPHQL_TYPES_TO_JAVA_TYPES.get(modelField.getTargetType()));
    }
}
