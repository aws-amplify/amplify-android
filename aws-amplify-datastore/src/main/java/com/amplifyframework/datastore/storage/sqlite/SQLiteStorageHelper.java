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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Set;

/**
 * A helper class to manage database creation and version management.
 */
final class SQLiteStorageHelper extends SQLiteOpenHelper {

    // Database Version
    @VisibleForTesting
    static final int DATABASE_VERSION = 1;

    // Logcat tag
    private static final String TAG = SQLiteStorageHelper.class.getSimpleName();

    // Database Name
    private static final String DATABASE_NAME = "AmplifyDatastore.db";

    // The singleton instance.
    private static SQLiteStorageHelper sQLiteStorageHelperInstance;

    // Contains all table name string list.
    private final Set<CreateSqlCommand> createSqlCommands;

    private SQLiteStorageHelper(@NonNull Context context,
                                @NonNull Set<CreateSqlCommand> createSqlCommands) {
        // Passing null to CursorFactory which is used to create cursor objects
        // as there is no need for a CursorFactory so far.
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.createSqlCommands = createSqlCommands;
    }

    /**
     * Create / Retrieve the singleton instance of the SQLiteStorageHelper.
     *
     * @param context Android context
     * @param createTableCommands set of table names and their create table sql commands
     * @return the singleton instance
     */
    public static synchronized SQLiteStorageHelper getInstance(@NonNull Context context,
                                                               @NonNull Set<CreateSqlCommand> createTableCommands) {
        if (sQLiteStorageHelperInstance == null) {
            sQLiteStorageHelperInstance = new SQLiteStorageHelper(context, createTableCommands);
        }
        return sQLiteStorageHelperInstance;
    }

    /**
     * Create all the required SQL tables.
     *
     * @param sqLiteDatabase the connection handle to the database.
     */
    @Override
    public synchronized void onCreate(SQLiteDatabase sqLiteDatabase) {
        createTablesAndIndexes(sqLiteDatabase);
    }

    private void createTablesAndIndexes(SQLiteDatabase sqLiteDatabase) {
        // Loop all the create table sql command string in the list.
        // each sql will create a table in SQLite database.
        sqLiteDatabase.beginTransaction();
        try {
            // TODO: Set PRAGMAS default encoding to UTF8.
            // TODO: Enable foreign keys
            // TODO: AutoVaccuum: caching
            for (final CreateSqlCommand createSqlCommand: createSqlCommands) {
                Log.i(TAG, "Creating table: " + createSqlCommand.tableName());
                sqLiteDatabase.execSQL(createSqlCommand.sqlStatement());
            }
            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
        }
    }

    /**
     * When the new db version is bigger than current exist db version, this method will be invoked.
     * It always drop all tables and then call onCreate() method to create all table again.
     *
     * @param sqLiteDatabase the connection handle to the database.
     * @param oldVersion older version number
     * @param newVersion newer version number
     */
    @Override
    public synchronized void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // Loop all the tables in the list and drop them if they exist
        // and re-create all the tables.
        sqLiteDatabase.beginTransaction();
        try {
            for (final CreateSqlCommand createSqlCommand: createSqlCommands) {
                if (!TextUtils.isEmpty(createSqlCommand.tableName())) {
                    sqLiteDatabase.execSQL("drop table if exists " + createSqlCommand.tableName());
                }
            }
            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
        }

        // After drop all exist tables, create all tables again.
        onCreate(sqLiteDatabase);
    }
}
