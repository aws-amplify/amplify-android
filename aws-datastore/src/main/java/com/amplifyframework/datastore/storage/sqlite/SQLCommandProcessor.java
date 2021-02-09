/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.logging.Logger;

import java.util.List;
import java.util.Objects;

final class SQLCommandProcessor {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final SQLiteDatabase sqliteDatabase;

    SQLCommandProcessor(@NonNull SQLiteDatabase sqliteDatabase) {
        this.sqliteDatabase = sqliteDatabase;
    }

    Cursor rawQuery(SqlCommand command) throws DataStoreException {
        SQLiteStatement sqliteStatement = sqliteDatabase.compileStatement(command.sqlStatement());
        try {
            long startTime = System.currentTimeMillis();
            Cursor result = sqliteDatabase.rawQuery(command.sqlStatement(), command.getBindingsAsArray());
            LOG.verbose("SQLCommandProcessor rawQuery in " + (System.currentTimeMillis() - startTime)
                    + " ms inTransaction: " + sqliteDatabase.inTransaction() + " SQL: " + command.sqlStatement());
            return result;
        } catch (SQLException sqlException) {
            throw dataStoreException(sqlException, sqliteStatement);
        }
    }

    boolean executeExists(SqlCommand command) throws DataStoreException {
        SQLiteStatement sqliteStatement = sqliteDatabase.compileStatement(command.sqlStatement());
        try {
            long startTime = System.currentTimeMillis();
            bindValuesToStatement(sqliteStatement, command.getBindings());
            boolean result = sqliteStatement.simpleQueryForLong() > 0;
            LOG.verbose("SQLCommandProcessor executeExists in " + (System.currentTimeMillis() - startTime)
                    + " ms inTransaction: " + sqliteDatabase.inTransaction() + " SQL: " + command.sqlStatement());
            return result;
        } catch (SQLException sqlException) {
            throw dataStoreException(sqlException, sqliteStatement);
        }
    }

    void execute(SqlCommand command) throws DataStoreException {
        SQLiteStatement sqliteStatement = sqliteDatabase.compileStatement(command.sqlStatement());
        try {
            long startTime = System.currentTimeMillis();
            bindValuesToStatement(sqliteStatement, command.getBindings());
            sqliteStatement.execute();
            LOG.verbose("SQLCommandProcessor execute in " + (System.currentTimeMillis() - startTime)
                    + " ms inTransaction: " + sqliteDatabase.inTransaction() + " SQL: " + command.sqlStatement());
        } catch (SQLException sqlException) {
            throw dataStoreException(sqlException, sqliteStatement);
        }
    }

    private DataStoreException dataStoreException(SQLException sqlException, SQLiteStatement sqliteStatement) {
        return new DataStoreException(
                "Invalid SQL statement: " + sqliteStatement,
                sqlException,
                AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
        );
    }

    private void bindValuesToStatement(
            SQLiteStatement statement,
            List<Object> values
    ) throws DataStoreException {
        // remove any bindings if there is any
        statement.clearBindings();

        // 1-based index for columns
        int columnIndex = 1;
        // apply stored bindings after columns were bound
        for (Object value : Objects.requireNonNull(values)) {
            bindValueToStatement(statement, columnIndex++, value);
        }
    }

    private void bindValueToStatement(
            SQLiteStatement statement,
            int columnIndex,
            Object value
    ) throws DataStoreException {
        LOG.verbose("SQLCommandProcessor.bindValueToStatement(..., value = " + value);
        if (value == null) {
            statement.bindNull(columnIndex);
        } else if (value instanceof String) {
            statement.bindString(columnIndex, (String) value);
        } else if (value instanceof Long) {
            statement.bindLong(columnIndex, (Long) value);
        } else if (value instanceof Integer) {
            statement.bindLong(columnIndex, (Integer) value);
        } else if (value instanceof Float) {
            statement.bindDouble(columnIndex, (Float) value);
        } else if (value instanceof Double) {
            statement.bindDouble(columnIndex, (Double) value);
        } else {
            throw new DataStoreException(
                    "Failed to bind " + value + " to SQL statement. " +
                            value.getClass().getSimpleName() + " is an unsupported type.",
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            );
        }
    }
}
