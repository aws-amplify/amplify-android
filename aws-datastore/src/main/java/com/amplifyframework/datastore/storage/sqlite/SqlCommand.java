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

import android.database.sqlite.SQLiteStatement;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import java.util.Objects;

/**
 * An encapsulation of the information required to
 * create a SQL table.
 */
final class SqlCommand {

    static final String DELIMITER = " ";
    static final String SELECT_STATEMENT = "SELECT";
    static final String FROM_CLAUSE = "FROM";
    static final String WHERE_STATEMENT = "WHERE";
    static final String INNER_JOIN_CLAUSE = "INNER JOIN";
    static final String LEFT_JOIN_CLAUSE = "LEFT JOIN";
    static final String ON_CLAUSE = "ON";
    static final String AS_CLAUSE = "AS";

    // The name of the SQL table
    private final String tableName;

    // A SQL command in string representation
    private final String sqlStatement;

    // A pre-compiled Sql statement that can be bound with
    // inputs later and executed. This object is not thread-safe. No two
    // threads can operate on the same SQLiteStatement object.
    private final SQLiteStatement compiledSqlStatement;

    /**
     * Construct a SqlCommand object.
     *
     * @param tableName name of the SQL table
     * @param sqlStatement create table command in string representation
     */
    SqlCommand(@NonNull String tableName,
               @NonNull String sqlStatement) {
        this(tableName, sqlStatement, null);
    }

    /**
     * Construct a SqlCommand object.
     *
     * @param tableName name of the SQL table
     * @param sqlStatement create table command in string representation
     * @param compiledSqlStatement a compiled Sql statement that can be bound with
     *                             inputs later and executed.
     */
    SqlCommand(@NonNull String tableName,
               @NonNull String sqlStatement,
               @Nullable SQLiteStatement compiledSqlStatement) {
        this.tableName = Objects.requireNonNull(tableName);
        this.sqlStatement = Objects.requireNonNull(sqlStatement);
        this.compiledSqlStatement = compiledSqlStatement;
    }

    /**
     * Return the name of the SQL table.
     * @return the name of the SQL table.
     */
    String tableName() {
        return tableName;
    }

    /**
     * Return the create table SQL command in string representation.
     * @return the create table SQL command in string representation.
     */
    String sqlStatement() {
        return sqlStatement;
    }

    /**
     * Return the compiled SQLite statement that can bound with inputs
     * and executed later.
     * @return the compiled SQLite statement that can bound with inputs
     *         and executed later.
     */
    SQLiteStatement getCompiledSqlStatement() {
        return compiledSqlStatement;
    }

    /**
     * Return true if compiledSqlStatement is not null
     * and false otherwise.
     * @return true if compiledSqlStatement is not null,
     *         false otherwise.
     */
    boolean hasCompiledSqlStatement() {
        return compiledSqlStatement != null;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        SqlCommand that = (SqlCommand) thatObject;

        if (!ObjectsCompat.equals(tableName, that.tableName)) {
            return false;
        }
        if (!ObjectsCompat.equals(sqlStatement, that.sqlStatement)) {
            return false;
        }
        return ObjectsCompat.equals(compiledSqlStatement, that.compiledSqlStatement);
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Override
    public int hashCode() {
        int result = tableName != null ? tableName.hashCode() : 0;
        result = 31 * result + (sqlStatement != null ? sqlStatement.hashCode() : 0);
        result = 31 * result + (compiledSqlStatement != null ? compiledSqlStatement.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SqlCommand{" +
            "tableName='" + tableName + '\'' +
            ", sqlStatement='" + sqlStatement + '\'' +
            ", compiledSqlStatement=" + compiledSqlStatement +
            '}';
    }
}
