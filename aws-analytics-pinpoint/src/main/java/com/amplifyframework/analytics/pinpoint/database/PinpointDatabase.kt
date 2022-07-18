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

import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import com.amplifyframework.analytics.pinpoint.models.PinpointEvent
import java.lang.IllegalArgumentException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PinpointDatabase(
    private val context: Context,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    private val events = 10
    private val eventsId = 20
    private val basePath = "events"
    private val databaseHelper = PinpointDatabaseHelper(context)
    private val database: SQLiteDatabase = databaseHelper.writableDatabase
    private val contentUri: Uri
    private val uriMatcher: UriMatcher

    init {
        val authority = context.applicationContext.packageName
        contentUri = Uri.parse("content://$authority/$basePath")
        uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        // The Uri of EVENTS is for all records in the Event table.
        uriMatcher.addURI(authority, basePath, events)
        // The Uri of EVENT_ID is for a single record in the Event table.
        uriMatcher.addURI(authority, "$basePath/#", eventsId)
    }

    fun closeDB() {
        databaseHelper.close()
    }

    suspend fun saveEvent(event: PinpointEvent): Uri {
        return insert(getContentUri(), generateContentValuesFromEvent(event))
    }

    suspend fun queryAllEvents(): Cursor {
        return withContext(coroutineDispatcher) {
            query(contentUri, null, null, null, null, null)
        }
    }

    private fun generateContentValuesFromEvent(event: PinpointEvent): ContentValues {
        val values = ContentValues()
        val eventJsonString = event.toJsonString()
        values.put(EventTable.COLUMN_JSON, eventJsonString)
        values.put(EventTable.COLUMN_SIZE, eventJsonString.length)
        return values
    }

    private fun getContentUri(): Uri = contentUri

    private fun insert(uri: Uri, values: ContentValues): Uri {
        val uriType = uriMatcher.match(uri)
        val id: Long
        when (uriType) {
            events -> {
                id = database.insertOrThrow(EventTable.TABLE_EVENT, null, values)
            } else -> {
                throw IllegalArgumentException("Unknown Uri: $uri")
            }
        }
        return Uri.parse("$basePath/$id")
    }

    /*@Synchronized
    private fun ensureDatabaseOpen() {
        if (!database.isOpen) {
            database = databaseHelper.writableDatabase
        }
    }*/

    private fun query(
        uri: Uri,
        projection: Array<String?>? = null,
        selection: String? = null,
        selectionArgs: Array<String?>? = null,
        sortOrder: String? = null,
        limit: String? = null
    ): Cursor {
        val queryBuilder = SQLiteQueryBuilder()
        queryBuilder.tables = EventTable.TABLE_EVENT
        when (uriMatcher.match(uri)) {
            events -> {}
            eventsId -> {
                queryBuilder.appendWhere("$EventTable.COLUMN_ID=${uri.lastPathSegment}")
            }
        }
        return queryBuilder.query(
            database,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            sortOrder,
            limit
        )
    }
}
