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

package com.amplifyframework.core.model.types.internal;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.types.AWSAppSyncScalarType;
import com.amplifyframework.core.model.types.JavaFieldType;
import com.amplifyframework.core.model.types.SqliteDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A utility that provides functions to convert between
 * GraphQL, Java and SQL data types.
 */
public final class TypeConverter {

    private static final Map<AWSAppSyncScalarType, JavaFieldType> AWS_GRAPH_QL_TO_JAVA = new HashMap<>();
    private static final Map<JavaFieldType, SqliteDataType> JAVA_TO_SQL = new HashMap<>();

    /**
     * Dis-allows instantiation of the static utility.
     */
    private TypeConverter() {
    }

    static {
        AWS_GRAPH_QL_TO_JAVA.put(AWSAppSyncScalarType.ID, JavaFieldType.STRING);
        AWS_GRAPH_QL_TO_JAVA.put(AWSAppSyncScalarType.STRING, JavaFieldType.STRING);
        AWS_GRAPH_QL_TO_JAVA.put(AWSAppSyncScalarType.INT, JavaFieldType.INTEGER);
        AWS_GRAPH_QL_TO_JAVA.put(AWSAppSyncScalarType.FLOAT, JavaFieldType.FLOAT);
        AWS_GRAPH_QL_TO_JAVA.put(AWSAppSyncScalarType.BOOLEAN, JavaFieldType.BOOLEAN);
        AWS_GRAPH_QL_TO_JAVA.put(AWSAppSyncScalarType.AWS_DATE, JavaFieldType.DATE);
        AWS_GRAPH_QL_TO_JAVA.put(AWSAppSyncScalarType.AWS_TIME, JavaFieldType.TIME);
        AWS_GRAPH_QL_TO_JAVA.put(AWSAppSyncScalarType.AWS_DATE_TIME, JavaFieldType.DATE);
        AWS_GRAPH_QL_TO_JAVA.put(AWSAppSyncScalarType.AWS_TIMESTAMP, JavaFieldType.LONG);
        AWS_GRAPH_QL_TO_JAVA.put(AWSAppSyncScalarType.AWS_EMAIL, JavaFieldType.STRING);
        AWS_GRAPH_QL_TO_JAVA.put(AWSAppSyncScalarType.AWS_JSON, JavaFieldType.STRING);
        AWS_GRAPH_QL_TO_JAVA.put(AWSAppSyncScalarType.AWS_URL, JavaFieldType.STRING);
        AWS_GRAPH_QL_TO_JAVA.put(AWSAppSyncScalarType.AWS_PHONE, JavaFieldType.STRING);
        AWS_GRAPH_QL_TO_JAVA.put(AWSAppSyncScalarType.AWS_IP_ADDRESS, JavaFieldType.STRING);

        JAVA_TO_SQL.put(JavaFieldType.BOOLEAN, SqliteDataType.INTEGER);
        JAVA_TO_SQL.put(JavaFieldType.LONG, SqliteDataType.INTEGER);
        JAVA_TO_SQL.put(JavaFieldType.INTEGER, SqliteDataType.INTEGER);
        JAVA_TO_SQL.put(JavaFieldType.FLOAT, SqliteDataType.REAL);
        JAVA_TO_SQL.put(JavaFieldType.STRING, SqliteDataType.TEXT);
        JAVA_TO_SQL.put(JavaFieldType.ENUM, SqliteDataType.TEXT);
        JAVA_TO_SQL.put(JavaFieldType.DATE, SqliteDataType.TEXT);
        JAVA_TO_SQL.put(JavaFieldType.TIME, SqliteDataType.TEXT);
    }

    /**
     * Retrieve the Java type for the GraphQL type.
     * @param graphQlTypeString the graphQL type
     * @return the Java type
     */
    public static JavaFieldType getJavaTypeForGraphQLType(@NonNull String graphQlTypeString) {
        final AWSAppSyncScalarType awsAppSyncScalarType =
            AWSAppSyncScalarType.fromString(Objects.requireNonNull(graphQlTypeString));
        final JavaFieldType javaFieldType = AWS_GRAPH_QL_TO_JAVA.get(awsAppSyncScalarType);
        if (null == javaFieldType) {
            throw new IllegalArgumentException(
                "No Java type mapping defined for GraphQL type = " + awsAppSyncScalarType
            );
        }
        return javaFieldType;
    }

    /**
     * Retrieve the Sql type for the Java type.
     * @param javaTypeString the Java type
     * @return the Sql type
     */
    public static SqliteDataType getSqlTypeForJavaType(@NonNull String javaTypeString) {
        final JavaFieldType javaFieldType = JavaFieldType.from(Objects.requireNonNull(javaTypeString));
        final SqliteDataType sqliteDataType = JAVA_TO_SQL.get(javaFieldType);
        if (null == sqliteDataType) {
            throw new IllegalArgumentException(
                "No SQL type mapping defined for Java type = " + javaFieldType
            );
        }
        return sqliteDataType;
    }

    /**
     * Retrieve the Sql type for the GraphQL type.
     * @param graphQlTypeString the graphQL type
     * @return the Sql type
     */
    public static SqliteDataType getSqlTypeForGraphQLType(@NonNull String graphQlTypeString) {
        final AWSAppSyncScalarType awsAppSyncScalarType =
            AWSAppSyncScalarType.fromString(Objects.requireNonNull(graphQlTypeString));
        final JavaFieldType javaFieldType = getJavaTypeForGraphQLType(awsAppSyncScalarType.stringValue());
        return getSqlTypeForJavaType(javaFieldType.stringValue());
    }
}
