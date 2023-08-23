/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.logging.cloudwatch.db

import net.sqlcipher.database.SQLiteDatabase

internal class LogEventTable {
    companion object {
        const val TABLE_LOG_EVENT = "cloudwatchlogevent"
        const val COLUMN_ID = "event_id"
        const val COLUMN_TIMESTAMP = "event_timestamp"
        const val COLUMN_MESSAGE = "event_message"

        fun onCreate(db: SQLiteDatabase?, version: Int) {
            val createTableQuery =
                "create table if not exists $TABLE_LOG_EVENT(" +
                    "$COLUMN_ID INTEGER primary key autoincrement, " +
                    "$COLUMN_TIMESTAMP INTEGER NOT NULL, " +
                    "$COLUMN_MESSAGE TEXT NOT NULL);"
            db?.execSQL(createTableQuery)
            onUpgrade(db, 1, version)
        }

        fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            // Stub to add any future db upgrades
        }
    }

    enum class Column {
        ID,
        TIMESTAMP,
        MESSAGE
    }
}
