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

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.types.JavaFieldType;
import com.amplifyframework.datastore.appsync.AWSAppSyncScalarType;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility that provides functions to convert between
 * GraphQL, Java and SQL data types.
 */
public final class TypeConverter {
    private static final Map<AWSAppSyncScalarType, JavaFieldType> AWS_GRAPH_QL_TO_JAVA = new HashMap<>();
    private static final Map<JavaFieldType, SQLiteDataType> JAVA_TO_SQL = new HashMap<>();

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

        JAVA_TO_SQL.put(JavaFieldType.BOOLEAN, SQLiteDataType.INTEGER);
        JAVA_TO_SQL.put(JavaFieldType.LONG, SQLiteDataType.INTEGER);
        JAVA_TO_SQL.put(JavaFieldType.INTEGER, SQLiteDataType.INTEGER);
        JAVA_TO_SQL.put(JavaFieldType.FLOAT, SQLiteDataType.REAL);
        JAVA_TO_SQL.put(JavaFieldType.STRING, SQLiteDataType.TEXT);
        JAVA_TO_SQL.put(JavaFieldType.ENUM, SQLiteDataType.TEXT);
        JAVA_TO_SQL.put(JavaFieldType.DATE, SQLiteDataType.TEXT);
        JAVA_TO_SQL.put(JavaFieldType.DATE_TIME, SQLiteDataType.TEXT);
        JAVA_TO_SQL.put(JavaFieldType.TIME, SQLiteDataType.TEXT);
        JAVA_TO_SQL.put(JavaFieldType.MODEL, SQLiteDataType.TEXT);
        JAVA_TO_SQL.put(JavaFieldType.CUSTOM_TYPE, SQLiteDataType.TEXT);
    }

    static JavaFieldType getJavaFieldType(@NonNull ModelField field) {
        if (field.isModel()) {
            return JavaFieldType.MODEL;
        }
        if (field.isEnum()) {
            return JavaFieldType.ENUM;
        }
        try {
            return JavaFieldType.from(field.getType().getSimpleName());
        } catch (IllegalArgumentException exception) {
            // fallback to custom type, which will result in the field being converted to a JSON string
            return JavaFieldType.CUSTOM_TYPE;
        }
    }

    /**
     * Gets the {@link JavaFieldType} from the value class.
     * @param value The value to guess the type from.
     * @return the {@link JavaFieldType} from the value class.
     */
    public static JavaFieldType getJavaFieldTypeFromValue(@NonNull Object value) {
        if (value instanceof Model) {
            return JavaFieldType.MODEL;
        }
        if (value instanceof Enum) {
            return JavaFieldType.ENUM;
        }
        try {
            return JavaFieldType.from(value.getClass().getSimpleName());
        } catch (IllegalArgumentException exception) {
            // fallback to custom type, which will result in the field being converted to a JSON string
            return JavaFieldType.CUSTOM_TYPE;
        }
    }

    /**
     * Retrieve the Sql type for the GraphQL type.
     *
     * @param field the Model field
     * @return the SQL type enum value
     */
    public static SQLiteDataType getSQLiteDataType(@NonNull ModelField field) {
        final JavaFieldType javaFieldType = getJavaFieldType(field);
        return JAVA_TO_SQL.get(javaFieldType);
    }

}
