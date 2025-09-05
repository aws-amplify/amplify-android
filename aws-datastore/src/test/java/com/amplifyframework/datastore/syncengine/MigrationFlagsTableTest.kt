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

import org.junit.Assert.assertEquals
import org.junit.Test

class MigrationFlagsTableTest {

    @Test
    fun `validate initial insert entries`() {
        assertEquals(
            setOf(
                "INSERT OR IGNORE INTO migration_flags (flag_name) VALUES ('cleared_v2_30_0_and_below_group_sync_expressions')"
            ),
            MigrationFlagsTable.initialInsertStatements()
        )
    }

    @Test
    fun `validate create statement`() {
        assertEquals(
            "CREATE TABLE IF NOT EXISTS migration_flags (flag_name TEXT PRIMARY KEY)",
            MigrationFlagsTable.CREATE_SQL
        )
    }
}
