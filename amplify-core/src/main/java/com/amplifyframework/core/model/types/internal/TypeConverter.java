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

import com.amplifyframework.core.model.types.JavaFieldType;
import com.amplifyframework.core.model.types.SqliteDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility that provides functions to convert between
 * GraphQL, Java and SQL data types.
 */
public final class TypeConverter {

    private static final Map<String, JavaFieldType> GRAPHQL_TYPES_TO_JAVA_TYPES = new HashMap<>();
    private static final Map<JavaFieldType, SqliteDataType> JAVA_TYPES_TO_SQL_TYPES = new HashMap<>();

    /**
     * Dis-allows instantiation of the static utility.
     */
    private TypeConverter() {
    }

    static {
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("ID", JavaFieldType.STRING);
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("String", JavaFieldType.STRING);
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("Int", JavaFieldType.INTEGER);
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("Float", JavaFieldType.FLOAT);
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("Boolean", JavaFieldType.BOOLEAN);
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("Enum", JavaFieldType.ENUM);
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSDate", JavaFieldType.DATE);
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSTime", JavaFieldType.TIME);
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSDateTime", JavaFieldType.DATE);
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSTimestamp", JavaFieldType.LONG);
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSEmail", JavaFieldType.STRING);
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSJSON", JavaFieldType.STRING);
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSURL", JavaFieldType.STRING);
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSPhone", JavaFieldType.STRING);
        GRAPHQL_TYPES_TO_JAVA_TYPES.put("AWSIPAddress", JavaFieldType.STRING);

        JAVA_TYPES_TO_SQL_TYPES.put(JavaFieldType.BOOLEAN, SqliteDataType.INTEGER);
        JAVA_TYPES_TO_SQL_TYPES.put(JavaFieldType.LONG, SqliteDataType.INTEGER);
        JAVA_TYPES_TO_SQL_TYPES.put(JavaFieldType.INTEGER, SqliteDataType.INTEGER);
        JAVA_TYPES_TO_SQL_TYPES.put(JavaFieldType.FLOAT, SqliteDataType.REAL);
        JAVA_TYPES_TO_SQL_TYPES.put(JavaFieldType.STRING, SqliteDataType.TEXT);
        JAVA_TYPES_TO_SQL_TYPES.put(JavaFieldType.ENUM, SqliteDataType.TEXT);
        JAVA_TYPES_TO_SQL_TYPES.put(JavaFieldType.DATE, SqliteDataType.TEXT);
        JAVA_TYPES_TO_SQL_TYPES.put(JavaFieldType.TIME, SqliteDataType.TEXT);
    }

    /**
     * Retrieve the Java type for the GraphQL type.
     * @param graphQLType the graphQL type
     * @return the Java type
     */
    public static JavaFieldType getJavaTypeForGraphQLType(@NonNull String graphQLType) {
        return GRAPHQL_TYPES_TO_JAVA_TYPES.get(graphQLType);
    }

    /**
     * Retrieve the Sql type for the Java type.
     * @param javaFieldType the Java type
     * @return the Sql type
     */
    public static SqliteDataType getSqlTypeForJavaType(@NonNull JavaFieldType javaFieldType) {
        return JAVA_TYPES_TO_SQL_TYPES.get(javaFieldType);
    }

    /**
     * Retrieve the Sql type for the GraphQL type.
     * @param graphQLType the graphQL type
     * @return the Sql type
     */
    public static SqliteDataType getSqlTypeForGraphQLType(@NonNull String graphQLType) {
        return getSqlTypeForJavaType(getJavaTypeForGraphQLType(graphQLType));
    }
}
