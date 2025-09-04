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
package com.amplifyframework.datastore.syncengine

/**
 * Constants for the migration_flags table.
 * This table is used to hold flags on whether or not certain migrations have been run on db tables
 */
object MigrationFlagsTable {
    const val TABLE_NAME = "migration_flags"
    const val COLUMN_FLAG_NAME = "flag_name"
    const val CREATE_SQL = "CREATE TABLE IF NOT EXISTS $TABLE_NAME ($COLUMN_FLAG_NAME TEXT PRIMARY KEY)"

    // See ClearInvalidGroupSyncExpressions for existing migrations.
    // Amplify v2.30.0 and below were improperly serializing syncExpressions with QueryPredicateGroup
    const val CLEARED_V2_30_0_AND_BELOW_GROUP_SYNC_EXPRESSIONS = "cleared_v2_30_0_and_below_group_sync_expressions"

    @JvmStatic
    val flags = mapOf(
        CLEARED_V2_30_0_AND_BELOW_GROUP_SYNC_EXPRESSIONS to
            "INSERT OR IGNORE INTO $TABLE_NAME ($COLUMN_FLAG_NAME) VALUES ('$CLEARED_V2_30_0_AND_BELOW_GROUP_SYNC_EXPRESSIONS')"
    )

    /*
    Grab a list of all statements that a new table should run so that we don't attempt to migrate later.
     */
    @JvmStatic
    fun initialInsertStatements(): Set<String> = flags.map { flag ->
        flag.value
    }.toSet()
}
