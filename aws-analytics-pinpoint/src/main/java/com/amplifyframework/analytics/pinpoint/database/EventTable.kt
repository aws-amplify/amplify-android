/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.analytics.pinpoint.database

import android.database.sqlite.SQLiteDatabase

internal class EventTable {
    companion object {
        const val TABLE_EVENT = "pinpointevent1"
        const val COLUMN_ID = "event_id"
        const val COLUMN_JSON = "event_json"
        const val COLUMN_SIZE = "event_size"

        @JvmStatic
        fun onCreate(db: SQLiteDatabase?, version: Int) {
            val createTableQuery =
                "create table if not exists $TABLE_EVENT(" +
                    "$COLUMN_ID integer primary key autoincrement, " +
                    "$COLUMN_SIZE INTEGER NOT NULL, " +
                    "$COLUMN_JSON TEXT NOT NULL);"
            db?.execSQL(createTableQuery)
            onUpgrade(db, 1, version)
        }

        @JvmStatic
        fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            // Stub to add any future db upgrades
        }
    }

    enum class COLUMNINDEX(index: Int) {
        ID(0),
        SIZE(1),
        JSON(2);
    }
}
