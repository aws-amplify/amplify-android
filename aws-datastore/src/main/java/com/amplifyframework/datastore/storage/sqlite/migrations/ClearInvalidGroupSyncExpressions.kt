/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.datastore.storage.sqlite.migrations

import android.database.sqlite.SQLiteDatabase
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.datastore.syncengine.MigrationFlagsTable
import com.amplifyframework.datastore.syncengine.MigrationFlagsTable.flags

/**
 * Clear syncExpression for rows containing "_type":"GROUP".
 * Amplify 2.30.0 and older were improperly serializing QueryPredicateGroup expressions.
 */
internal class ClearInvalidGroupSyncExpressions(private val database: SQLiteDatabase) : ModelMigration {

    override fun apply() {
        database.execSQL(MigrationFlagsTable.CREATE_SQL)
        if (isAlreadyCompleted) {
            return
        }
        try {
            database.beginTransaction()
            clearGroupSyncExpressions()
            markMigrationCompleted()
            database.setTransactionSuccessful()
            LOG.debug("Cleared potentially invalid QueryPredicateGroup sync expressions")
        } finally {
            database.endTransaction()
        }
    }

    private val isAlreadyCompleted: Boolean
        get() {
            try {
                database.rawQuery(
                    "SELECT 1 FROM ${MigrationFlagsTable.TABLE_NAME} WHERE ${MigrationFlagsTable.COLUMN_FLAG_NAME} = ?",
                    arrayOf(MIGRATION_FLAG)
                ).use { cursor ->
                    return cursor.count > 0
                }
            } catch (_: Exception) {
                return false
            }
        }

    private fun clearGroupSyncExpressions() {
        database.execSQL(
            "UPDATE LastSyncMetadata SET syncExpression = NULL WHERE syncExpression LIKE '%\"_type\":\"GROUP\"%'"
        )
    }

    private fun markMigrationCompleted() {
        if (flags.containsKey(MigrationFlagsTable.CLEARED_V2_30_0_AND_BELOW_GROUP_SYNC_EXPRESSIONS)) {
            database.execSQL(flags[MigrationFlagsTable.CLEARED_V2_30_0_AND_BELOW_GROUP_SYNC_EXPRESSIONS])
        }
    }

    companion object {
        private val LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore")
        private const val MIGRATION_FLAG = MigrationFlagsTable.CLEARED_V2_30_0_AND_BELOW_GROUP_SYNC_EXPRESSIONS
    }
}
