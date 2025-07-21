/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.datastore.storage.sqlite.migrations;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.Wrap;

/**
 * Add SyncExpression (TEXT) column to LastSyncMetadata table.
 */
final class AddSyncExpressionToLastSyncMetadata implements ModelMigration {
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");
    private final SQLiteDatabase database;
    private final String newSyncExpColumnName = "syncExpression";

    /**
     * Constructor for the migration class.
     * @param database Connection to the SQLite database.
     */
    AddSyncExpressionToLastSyncMetadata(SQLiteDatabase database) {
        this.database = database;
    }

    @Override
    public void apply() {
        if (!needsMigration()) {
            LOG.debug("No LastSyncMetadata migration needed.");
            return;
        }
        addNewSyncExpColumnName();
    }

    /**
     * Alter LastSyncMetadata table with new column.
     * Existing rows in LasySyncMetadata will have 'null' for ${newSyncExpColumnName} value,
     * until the next sync/hydrate operation.
     */
    private void addNewSyncExpColumnName() {
        try {
            database.beginTransaction();
            final String addColumnSql = "ALTER TABLE LastSyncMetadata ADD COLUMN " +
                    newSyncExpColumnName + " TEXT";
            database.execSQL(addColumnSql);
            database.setTransactionSuccessful();
            LOG.debug("Successfully upgraded LastSyncMetadata table with new field: " + newSyncExpColumnName);
        } finally {
            if (database.inTransaction()) {
                database.endTransaction();
            }
        }
    }

    private boolean needsMigration() {
        final String checkColumnSql = "SELECT COUNT(*) FROM pragma_table_info('LastSyncMetadata') " +
            "WHERE name=" + Wrap.inSingleQuotes(newSyncExpColumnName);
        try (Cursor queryResults = database.rawQuery(checkColumnSql, new String[]{})) {
            if (queryResults.moveToNext()) {
                int recordNum = queryResults.getInt(0);
                return recordNum == 0; // needs to be upgraded if there's no column named ${newSyncExpColumnName}
            }
        }
        return false;
    }
}
