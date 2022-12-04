/*
 *  Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.geo.location.database

import android.content.ContentValues
import com.amplifyframework.geo.location.database.LocationTable.Column
import java.time.Instant
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SQLiteDatabase

internal data class LocationEntity(
    val locationId: Long = -1,
    val deviceId: String,
    val tracker: String,
    val datetime: Instant,
    val latitude: Double,
    val longitude: Double
)

internal class LocationDao(
    private val database: SQLiteDatabase,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) {
    suspend fun insert(entity: LocationEntity) = withContext(coroutineContext) {
        val values = ContentValues().apply {
            put(Column.DeviceId.name, entity.deviceId)
            put(Column.Tracker.name, entity.tracker)
            put(Column.DateTime.name, entity.datetime.epochSecond)
            put(Column.Latitude.name, entity.latitude)
            put(Column.Longitude.name, entity.longitude)
        }
        database.insert(LocationTable.TABLE_NAME, null, values).toInt()
    }

    /**
     * Removes all captured locations for the given deviceId and tracker combination
     */
    suspend fun removeAll(deviceId: String, tracker: String) = withContext(coroutineContext) {
        val clause = "${Column.DeviceId} = ? AND ${Column.Tracker} = ?"
        database.delete(LocationTable.TABLE_NAME, clause, arrayOf(deviceId, tracker))
    }

    /**
     * Removes all the given location entities
     */
    suspend fun removeAll(locations: Collection<LocationEntity>) = withContext(coroutineContext) {
        val ids = locations.map { it.locationId }.joinToString(",")
        val clause = "${Column.LocationId} IN (?)"
        database.delete(LocationTable.TABLE_NAME, clause, arrayOf(ids))
    }

    /**
     * Returns all captured locations
     */
    suspend fun getAll() = withContext(coroutineContext) {
        database.query(
            LocationTable.TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "${Column.DateTime} ASC"
        ).use {
            return@withContext generateSequence { if (it.moveToNext()) it else null }
                .map { cursor ->
                    LocationEntity(
                        locationId = cursor.getLong(Column.LocationId.ordinal),
                        deviceId = cursor.getString(Column.DeviceId.ordinal),
                        tracker = cursor.getString(Column.Tracker.ordinal),
                        datetime = Instant.ofEpochSecond(cursor.getLong(Column.DateTime.ordinal)),
                        latitude = cursor.getDouble(Column.Latitude.ordinal),
                        longitude = cursor.getDouble(Column.Latitude.ordinal)
                    )
                }
                .toList()
        }
    }
}
