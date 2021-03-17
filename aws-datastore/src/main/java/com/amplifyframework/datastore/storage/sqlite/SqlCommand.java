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
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.util.Immutable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An encapsulation of the information required to
 * create a SQL table.
 */
final class SqlCommand {
    // The name of the SQL table
    private final String tableName;

    // A SQL command in string representation
    private final String sqlStatement;

    // A list of arguments to be bound to the sqlStatement
    private final List<Object> bindings;

    /**
     * Construct a SqlCommand object.
     *
     * @param tableName name of the SQL table
     * @param sqlStatement create table command in string representation
     */
    SqlCommand(@NonNull String tableName,
               @NonNull String sqlStatement) {
        this(tableName, sqlStatement, Collections.emptyList());
    }

    /**
     * Construct a SqlCommand object.
     *
     * @param tableName name of the SQL table
     * @param sqlStatement create table command in string representation
     * @param bindings a list of arguments to be bound to the sqlStatement
     */
    SqlCommand(@NonNull String tableName,
               @NonNull String sqlStatement,
               @NonNull List<Object> bindings) {
        this.tableName = Objects.requireNonNull(tableName);
        this.sqlStatement = Objects.requireNonNull(sqlStatement);
        this.bindings = Objects.requireNonNull(bindings);
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
     * Return the list of arguments to be bound to the sqlStatement.
     * @return the list of arguments to be bound to the sqlStatement
     */
    List<Object> getBindings() {
        return Immutable.of(bindings);
    }

    /**
     * Return the list of arguments to be bound to the sqlStatement
     * as an array of strings.
     * @return the list of arguments to be bound to the sqlStatement
     *         as an array of strings.
     */
    String[] getBindingsAsArray() {
        if (!hasBindings()) {
            return null;
        }
        /*
         Potentially inefficient to do this per call, but
         this should never need to be called more than once.

         Doing `Arrays.copyOf(selectionArgs.toArray(), selectionArgs.size(), String[].class);`
         does NOT work because not every object (e.g. Integer) can be cast to string.
         */
        final int length = bindings.size();
        final String[] array = new String[length];
        for (int index = 0; index < length; index++) {
            array[index] = bindings.get(index).toString();
        }
        return Arrays.copyOf(array, array.length);
    }

    /**
     * Return true if selectionArgs is not null and not empty.
     * @return true if selectionArgs is not null and not empty.
     */
    boolean hasBindings() {
        return bindings != null && !bindings.isEmpty();
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
        return ObjectsCompat.equals(bindings, that.bindings);
    }

    @Override
    public int hashCode() {
        int result = tableName != null ? tableName.hashCode() : 0;
        result = 31 * result + (sqlStatement != null ? sqlStatement.hashCode() : 0);
        result = 31 * result + (bindings != null ? bindings.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SqlCommand{" +
                "tableName='" + tableName + '\'' +
                ", sqlStatement='" + sqlStatement + '\'' +
                ", bindings=" + bindings +
                '}';
    }
}
