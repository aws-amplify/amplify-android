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
package com.amplifyframework.datastore.storage.sqlite

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter
import com.amplifyframework.datastore.syncengine.MigrationFlagsTable
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider
import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBePositive
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Test the creation functionality of [SQLiteStorageAdapter] operations.
 */
class SQLiteStorageAdapterCreateTest {
    private lateinit var adapter: SynchronousStorageAdapter

    /**
     * Remove any old database files, and then re-provision a new storage adapter,
     * that is able to store the Comment-Blog family of models.
     */
    @Before
    fun setup() {
        TestStorageAdapter.cleanup()
        this.adapter = TestStorageAdapter.create(AmplifyModelProvider.getInstance())
    }

    /**
     * Close the open database, and cleanup any database files that it left.
     */
    @After
    fun teardown() {
        TestStorageAdapter.cleanup(adapter)
    }

    /**
     * Test that initial creation creates migration flags table with initial entries.
     */
    @Test
    fun verifyMigrationFlagsTableExistsAndContainsRecordsOnCreate() {
        // Verify migration flags table exists and has initial entries
        val dbPath = ApplicationProvider.getApplicationContext<Context>()
            .getDatabasePath(SQLiteStorageAdapter.DEFAULT_DATABASE_NAME).absolutePath
        val database = SQLiteDatabase.openDatabase(
            dbPath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
        try {
            database.rawQuery(
                "SELECT 1 FROM " + MigrationFlagsTable.TABLE_NAME +
                    " WHERE " + MigrationFlagsTable.COLUMN_FLAG_NAME + " = ?",
                arrayOf(
                    MigrationFlagsTable.CLEARED_V2_30_0_AND_BELOW_GROUP_SYNC_EXPRESSIONS
                )
            ).use { cursor ->
                withClue("Migration flag row was not created") {
                    cursor.count.shouldBePositive()
                }
            }
        } finally {
            database.close()
        }
    }
}
