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

/**
 * A helper class to manage database creation and version management.
 */
final class SQLiteStorageHelper extends SQLiteOpenHelper {

    // Logcat tag
    private static final String TAG = SQLiteStorageHelper.class.getSimpleName();

    // The singleton instance.
    private static SQLiteStorageHelper sQLiteStorageHelperInstance;

    // Contains all create table and create index commands.
    private final CreateSqlCommands createSqlCommands;

    private SQLiteStorageHelper(@NonNull Context context,
                                @NonNull String databaseName,
                                int databaseVersion,
                                @NonNull CreateSqlCommands createSqlCommands) {
        // Passing null to CursorFactory which is used to create cursor objects
        // as there is no need for a CursorFactory so far.
        super(context, databaseName, null, databaseVersion);
        this.createSqlCommands = createSqlCommands;
    }

    /**
     * Create / Retrieve the singleton instance of the SQLiteStorageHelper.
     *
     * @param context Android context
     * @param databaseName name of the database
     * @param databaseVersion version of the database
     * @param createSqlCommands set of create table and create index sql commands
     * @return the singleton instance
     */
    static synchronized SQLiteStorageHelper getInstance(
            @NonNull Context context,
            @NonNull String databaseName,
            int databaseVersion,
            @NonNull CreateSqlCommands createSqlCommands) {
        if (sQLiteStorageHelperInstance == null) {
            sQLiteStorageHelperInstance = new SQLiteStorageHelper(
                    context, databaseName, databaseVersion, createSqlCommands);
        }
        return sQLiteStorageHelperInstance;
    }

    /**
     * Configure the {@link SQLiteDatabase} when being created.
     * Called when the database connection is being configured, to enable features
     * such as foreign key support.
     *
     * @param sqLiteDatabase the connection handle to the database.
     */
    @Override
    public void onConfigure(SQLiteDatabase sqLiteDatabase) {
        super.onConfigure(sqLiteDatabase);

        sqLiteDatabase.beginTransaction();
        try {
            sqLiteDatabase.execSQL("PRAGMA foreign_keys = ON");
            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
        }
    }

    /**
     * Called when the database is created for the FIRST time.
     * If a database already exists on disk with the same DATABASE_NAME,
     * this method will NOT be called.
     *
     * @param sqLiteDatabase the connection handle to the database.
     */
    @Override
    public synchronized void onCreate(SQLiteDatabase sqLiteDatabase) {
        createTablesAndIndexes(sqLiteDatabase);
    }

    /**
     * Called when the database needs to be upgraded.
     * This method will only be called if a database already exists on disk with the
     * same DATABASE_NAME, but the DATABASE_VERSION is different than the version of
     * the database that exists on disk.
     *
     * When the new db version is bigger than current exist db version,
     * this method will be invoked. It always drop all tables and then call
     * onCreate() method to create all table again.
     *
     * @param sqLiteDatabase the connection handle to the database.
     * @param oldVersion older version number
     * @param newVersion newer version number
     */
    @Override
    public synchronized void onUpgrade(SQLiteDatabase sqLiteDatabase,
                                       int oldVersion,
                                       int newVersion) {
        if (oldVersion != newVersion) {
            // Loop all the tables in the set and drop them if they exist
            // and re-create all the tables.
            sqLiteDatabase.beginTransaction();
            try {
                for (final SqlCommand sqlCommand : createSqlCommands.getCreateTableCommands()) {
                    if (!TextUtils.isEmpty(sqlCommand.tableName())) {
                        sqLiteDatabase.execSQL("drop table if exists " +
                                sqlCommand.tableName());
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

    private void createTablesAndIndexes(SQLiteDatabase sqLiteDatabase) {
        // Loop all the create table sql command string in the set.
        // each sql will create a table in SQLite database.
        sqLiteDatabase.beginTransaction();
        try {
            for (final SqlCommand sqlCommand : createSqlCommands.getCreateTableCommands()) {
                Log.i(TAG, "Creating table: " + sqlCommand.tableName());
                sqLiteDatabase.execSQL(sqlCommand.sqlStatement());
            }

            for (final SqlCommand sqlCommand : createSqlCommands.getCreateIndexCommands()) {
                Log.i(TAG, "Creating index for table: " + sqlCommand.tableName());
                sqLiteDatabase.execSQL(sqlCommand.sqlStatement());
            }
            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
        }
    }
}
