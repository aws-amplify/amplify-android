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

package com.amplifyframework.core.model.internal.types;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility that provides functions to convert between
 * GraphQL, Java and SQL data types.
 */
public final class TypeConverter {

    private static final Map<String, String> GRAPHQL_TYPES_TO_JAVA_TYPES = new HashMap<>();
    private static final Map<String, String> JAVA_TYPES_TO_SQL_TYPES = new HashMap<>();

    /**
     * Dis-allows instantiation of the static utility.
     */
    private TypeConverter() {
    }

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

    /**
     * Retrieve the Java type for the GraphQL type.
     * @param graphQLType the graphQL type
     * @return the Java type
     */
    public static String getJavaTypeForGraphQLType(@NonNull String graphQLType) {
        return GRAPHQL_TYPES_TO_JAVA_TYPES.get(graphQLType);
    }

    /**
     * Retrieve the Sql type for the Java type.
     * @param javaType the Java type
     * @return the Sql type
     */
    public static String getSqlTypeForJavaType(@NonNull String javaType) {
        return JAVA_TYPES_TO_SQL_TYPES.get(javaType);
    }

    /**
     * Retrieve the Sql type for the GraphQL type.
     * @param graphQLType the graphQL type
     * @return the Sql type
     */
    public static String getSqlTypeForGraphQLType(@NonNull String graphQLType) {
        return getSqlTypeForJavaType(getJavaTypeForGraphQLType(graphQLType));
    }
}
