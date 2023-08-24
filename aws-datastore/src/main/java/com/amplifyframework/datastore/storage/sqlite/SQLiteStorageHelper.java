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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.Empty;
import com.amplifyframework.util.Wrap;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A helper class to manage database creation and version management.
 */
final class SQLiteStorageHelper extends SQLiteOpenHelper implements ModelUpdateStrategy<SQLiteDatabase, String> {

    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");

    // SQLiteDatabase Metadata is stored in tables prefixed by this prefix.
    private static final String SQLITE_SYSTEM_TABLE_PREFIX = "sqlite_";

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
     * Creates an instance of the SQLiteStorageHelper.
     * @param context Android context
     * @param databaseName name of the database
     * @param databaseVersion version of the database
     * @param createSqlCommands set of create table and create index sql commands
     * @return A new instance of the SQLiteStorageHelper
     */
    static SQLiteStorageHelper getInstance(
            @NonNull Context context,
            @NonNull String databaseName,
            int databaseVersion,
            @NonNull CreateSqlCommands createSqlCommands) {
        return new SQLiteStorageHelper(context, databaseName, databaseVersion, createSqlCommands);
    }

    /**
     * Configure the {@link SQLiteDatabase} when being created.
     * Called when the database connection is being configured, to enable features
     * such as foreign key support.
     *
     * @param sqliteDatabase the connection handle to the database.
     */
    @Override
    public void onConfigure(SQLiteDatabase sqliteDatabase) {
        super.onConfigure(sqliteDatabase);
        sqliteDatabase.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * Called when the database is created for the FIRST time.
     * If a database already exists on disk with the same DATABASE_NAME,
     * this method will NOT be called.
     *
     * @param sqliteDatabase the connection handle to the database.
     */
    @Override
    public synchronized void onCreate(SQLiteDatabase sqliteDatabase) {
        createTablesAndIndexes(sqliteDatabase);
    }

    /**
     * Called ONCE when the database is opened. It enables features
     * such as foreign key support.
     */
    @Override
    public synchronized void onOpen(SQLiteDatabase sqliteDatabase) {
        super.onOpen(sqliteDatabase);
        if (!sqliteDatabase.isReadOnly()) {
            sqliteDatabase.execSQL("PRAGMA foreign_keys = ON;");
        }
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
     * @param sqliteDatabase the connection handle to the database.
     * @param oldVersion older version number
     * @param newVersion newer version number
     */
    @Override
    public synchronized void onUpgrade(SQLiteDatabase sqliteDatabase,
                                       int oldVersion,
                                       int newVersion) {
        if (oldVersion != newVersion) {
            // Loop all the tables in the set and drop them if they exist
            // and re-create all the tables.
            sqliteDatabase.beginTransaction();
            try {
                for (final SqlCommand sqlCommand : createSqlCommands.getCreateTableCommands()) {
                    if (!Empty.check(sqlCommand.tableName())) {
                        sqliteDatabase.execSQL("drop table if exists " +
                                Wrap.inBackticks(sqlCommand.tableName()));
                    }
                }
                sqliteDatabase.setTransactionSuccessful();
            } finally {
                sqliteDatabase.endTransaction();
            }

            // After drop all exist tables, create all tables again.
            onCreate(sqliteDatabase);
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void update(
            @NonNull SQLiteDatabase sqliteDatabase,
            @NonNull String oldVersion,
            @NonNull String newVersion) {
        Objects.requireNonNull(sqliteDatabase);
        Objects.requireNonNull(oldVersion);
        Objects.requireNonNull(newVersion);

        // Currently on any model version change, drop all tables created by the DataStore.
        // TODO: This can be improved by detecting the specific changes in each Model and applying
        // the changes to the existing schema of the SQLite tables.
        if (!ObjectsCompat.equals(oldVersion, newVersion)) {
            dropAllTables(sqliteDatabase);

            // After the existing tables are dropped, call onCreate(SQLiteDatabase) to re-create
            // the required tables.
            onCreate(sqliteDatabase);
        }
    }

    private void createTablesAndIndexes(SQLiteDatabase sqliteDatabase) {
        Objects.requireNonNull(sqliteDatabase);

        // Loop all the create table sql command string in the set.
        // each sql will create a table in SQLite database.
        sqliteDatabase.beginTransaction();
        try {
            for (final SqlCommand sqlCommand : createSqlCommands.getCreateTableCommands()) {
                LOG.info("Creating table: " + sqlCommand.tableName());
                sqliteDatabase.execSQL(sqlCommand.sqlStatement());
            }

            for (final SqlCommand sqlCommand : createSqlCommands.getCreateIndexCommands()) {
                LOG.info("Creating index for table: " + sqlCommand.tableName());
                sqliteDatabase.execSQL(sqlCommand.sqlStatement());
            }
            sqliteDatabase.setTransactionSuccessful();
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    private void dropAllTables(@NonNull SQLiteDatabase sqliteDatabase) {
        Objects.requireNonNull(sqliteDatabase);
        final String queryString = "SELECT name FROM sqlite_master WHERE type='table'";
        sqliteDatabase.execSQL("PRAGMA foreign_keys = OFF;");
        try (Cursor cursor = sqliteDatabase.rawQuery(queryString, null)) {
            Objects.requireNonNull(cursor);

            final Set<String> tablesToDrop = new HashSet<>();

            while (cursor.moveToNext()) {
                tablesToDrop.add(cursor.getString(0));
            }
            sqliteDatabase.beginTransaction();
            sqliteDatabase.execSQL("PRAGMA foreign_keys = OFF;");
            for (String table : tablesToDrop) {
                // Android SQLite creates system tables to store metadata
                // and all the system tables have the prefix SQLITE_SYSTEM_TABLE_PREFIX.
                if (table.startsWith(SQLITE_SYSTEM_TABLE_PREFIX)) {
                    continue;
                }
                sqliteDatabase.execSQL("DROP TABLE IF EXISTS " + Wrap.inBackticks(table));
                LOG.debug("Dropped table: " + table);
            }
            sqliteDatabase.execSQL("PRAGMA foreign_keys = ON;");
            sqliteDatabase.setTransactionSuccessful();
            sqliteDatabase.endTransaction();
        } finally {
            sqliteDatabase.execSQL("PRAGMA foreign_keys = ON;");
        }
    }
}
