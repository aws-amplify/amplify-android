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
        CLEARED_V2_30_0_AND_BELOW_GROUP_SYNC_EXPRESSIONS to "INSERT OR IGNORE INTO $TABLE_NAME ($COLUMN_FLAG_NAME) VALUES ('$CLEARED_V2_30_0_AND_BELOW_GROUP_SYNC_EXPRESSIONS')"
    )

    /*
    Grab a list of all statements that a new table should run so that we don't attempt to migrate later.
     */
    @JvmStatic
    fun initialInsertStatements(): Set<String>{
        return flags.map { flag ->
            flag.value
        }.toSet()
    }
}
