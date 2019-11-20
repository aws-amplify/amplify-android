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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the functions of {@link SQLiteStorageHelper}.
 */
public class SQLiteStorageHelperInstrumentedTest {

    private SQLiteStorageHelper sqLiteStorageHelper;
    private SQLiteDatabase sqLiteDatabase;
    private Set<SqlCommand> createTableCommands;

    /**
     * Setup the required information for SQLiteStorageHelper construction.
     */
    @Before
    public void setUp() {
        deleteDatabase();

        createTableCommands = new HashSet<>();
        createTableCommands.add(
                new SqlCommand("Person",
                        "CREATE TABLE IF NOT EXISTS Person (ID TEXT PRIMARY KEY, NAME TEXT NOT NULL);"));
        createTableCommands.add(
                new SqlCommand("Car",
                        "CREATE TABLE IF NOT EXISTS Car (ID TEXT PRIMARY KEY, NAME TEXT NOT NULL);"));
        sqLiteStorageHelper = SQLiteStorageHelper.getInstance(
                ApplicationProvider.getApplicationContext(),
                SQLiteStorageAdapter.DATABASE_NAME,
                SQLiteStorageAdapter.DATABASE_VERSION,
                new CreateSqlCommands(createTableCommands, Collections.emptySet()));
        sqLiteDatabase = sqLiteStorageHelper.getWritableDatabase();
    }

    /**
     * Drop all tables and database, close and delete the database.
     */
    @After
    public void tearDown() {
        sqLiteDatabase.close();
        sqLiteStorageHelper.close();
        deleteDatabase();
    }

    /**
     * Assert the construction of the SQLiteStorageHelper.
     */
    @Test
    public void getInstanceIsNotNull() {
        assertNotNull(sqLiteStorageHelper);
    }

    /**
     * Assert if the database connection is opened.
     */
    @Test
    public void isDatabaseOpen() {
        assertTrue(sqLiteDatabase.isOpen());
    }

    /**
     * Assert that {@link SQLiteStorageHelper#onCreate(SQLiteDatabase)}
     * creates the specified tables.
     */
    @Test
    public void onCreateCreatesTables() {
        // Getting an instance to the writable database
        // invokes onCreate on the SQLiteStorageHelper.
        final List<String> tableNamesFromDatabase = getTableNames(sqLiteDatabase);
        Log.d("onCreateCreatesTables", Arrays.toString(tableNamesFromDatabase.toArray()));
        for (SqlCommand sqlCommand : createTableCommands) {
            assertTrue(
                    sqlCommand.tableName() + " was not in the list: " + tableNamesFromDatabase,
                    tableNamesFromDatabase.contains(sqlCommand.tableName())
            );
        }
    }

    private List<String> getTableNames(SQLiteDatabase sqLiteDatabase) {
        final ArrayList<String> tableNamesFromDatabase = new ArrayList<>();
        final Cursor cursor = sqLiteDatabase.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'",
                null);

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                final String tableName = cursor.getString(cursor.getColumnIndex("name"));
                if (!"android_metadata".equals(tableName)) {
                    tableNamesFromDatabase.add(tableName);
                }
                cursor.moveToNext();
            }
        }
        return tableNamesFromDatabase;
    }

    private void deleteDatabase() {
        ApplicationProvider.getApplicationContext()
                .deleteDatabase(SQLiteStorageAdapter.DATABASE_NAME);
    }
}
