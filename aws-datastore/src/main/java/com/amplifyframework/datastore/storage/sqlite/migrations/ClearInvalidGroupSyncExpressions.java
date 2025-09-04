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
import com.amplifyframework.datastore.syncengine.MigrationFlagsTable;
import com.amplifyframework.logging.Logger;

/**
 * Clear syncExpression for rows containing "_type":"GROUP".
 * Amplify 2.30.0 and older were improperly serializing QueryPredicateGroup expressions.
 */
final class ClearInvalidGroupSyncExpressions implements ModelMigration {
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");
    private static final String MIGRATION_FLAG = MigrationFlagsTable.CLEARED_V2_30_0_AND_BELOW_GROUP_SYNC_EXPRESSIONS;
    private final SQLiteDatabase database;

    /**
     * Constructor for the migration class.
     * @param database Connection to the SQLite database.
     */
    ClearInvalidGroupSyncExpressions(SQLiteDatabase database) {
        this.database = database;
    }

    @Override
    public void apply() {
        database.execSQL(MigrationFlagsTable.CREATE_SQL);
        if (isAlreadyCompleted()) {
            return;
        }
        clearGroupSyncExpressions();
        markMigrationCompleted();
        LOG.debug("Cleared potentially invalid QueryPredicateGroup sync expressions");
    }

    private boolean isAlreadyCompleted() {
        try (Cursor cursor = database.rawQuery(
                "SELECT 1 FROM " + MigrationFlagsTable.TABLE_NAME + 
                " WHERE " + MigrationFlagsTable.COLUMN_FLAG_NAME + " = ?", 
                new String[]{MIGRATION_FLAG})) {
            return cursor.getCount() > 0;
        } catch (Exception exception) {
            return false;
        }
    }

    private void clearGroupSyncExpressions() {
        database.execSQL(
                "UPDATE LastSyncMetadata " +
                        "SET syncExpression = NULL " +
                        "WHERE syncExpression LIKE '%\"_type\":\"GROUP\"%'"
        );
    }

    private void markMigrationCompleted() {
        if (MigrationFlagsTable.getFlags()
                .containsKey(MigrationFlagsTable.CLEARED_V2_30_0_AND_BELOW_GROUP_SYNC_EXPRESSIONS)
        ) {
            database.execSQL(
                    MigrationFlagsTable.getFlags().get(
                            MigrationFlagsTable.CLEARED_V2_30_0_AND_BELOW_GROUP_SYNC_EXPRESSIONS
                    )
            );
        }
    }
}
