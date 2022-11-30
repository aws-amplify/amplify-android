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

package com.amplifyframework.geo.location.database

import android.database.sqlite.SQLiteDatabase

internal class LocationTable {

    enum class Column {
        LocationId,
        DeviceId,
        Tracker,
        DateTime,
        Latitude,
        Longitude
    }

    companion object {
        const val TABLE_NAME = "locations"

        @JvmStatic
        fun onCreate(db: SQLiteDatabase?, version: Int) {
            val createTableQuery =
                "create table if not exists $TABLE_NAME(" +
                    "${Column.LocationId} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "${Column.DeviceId} STRING NOT NULL, " +
                    "${Column.Tracker} STRING NOT NULL, " +
                    "${Column.DateTime} INTEGER NOT NULL, " +
                    "${Column.Latitude} REAL NOT NULL, " +
                    "${Column.Longitude} REAL NOT NULL);"
            db?.execSQL(createTableQuery)
            onUpgrade(db, 1, version)
        }

        @JvmStatic
        fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            // Stub to add any future db upgrades
        }
    }
}
