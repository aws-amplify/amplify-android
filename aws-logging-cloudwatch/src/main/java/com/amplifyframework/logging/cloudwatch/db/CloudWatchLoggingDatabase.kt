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

import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import com.amplifyframework.core.store.EncryptedKeyValueRepository
import com.amplifyframework.logging.cloudwatch.models.CloudWatchLogEvent
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sqlcipher.database.SQLiteQueryBuilder

internal class CloudWatchLoggingDatabase(
    private val context: Context,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val logEvents = 10
    private val logEventsId = 20
    private val passphraseKey = "passphrase"
    private val mb = 1024 * 1024
    private val encryptedKeyValueRepository: EncryptedKeyValueRepository by lazy {
        EncryptedKeyValueRepository(
            context,
            "awscloudwatchloggingdb"
        )
    }
    private val database by lazy {
        System.loadLibrary("sqlcipher")
        CloudWatchDatabaseHelper(context).getWritableDatabase(getDatabasePassphrase())
    }
    private val basePath = "cloudwatchlogevents"
    private val contentUri: Uri
    private val uriMatcher: UriMatcher

    init {
        val authority = context.applicationContext.packageName
        contentUri = Uri.parse("content://$authority/$basePath")
        uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        // The Uri of LOG_EVENTS is for all records in the LogEventTable table.
        uriMatcher.addURI(authority, basePath, logEvents)
        // the URI of log_event_id is for a single record
        uriMatcher.addURI(authority, "$basePath/#", logEventsId)
    }

    internal suspend fun saveLogEvent(event: CloudWatchLogEvent): Uri = withContext(coroutineDispatcher) {
        insertEvent(event)
    }

    internal suspend fun queryAllEvents(): List<LogEvent> = withContext(coroutineDispatcher) {
        val cloudWatchLogEvents = mutableListOf<LogEvent>()
        val cursor = query(null, null, null, LogEventTable.COLUMN_TIMESTAMP, "10000")
        cursor.use {
            if (!it.moveToFirst()) {
                return@use
            }
            do {
                val id = it.getLong(LogEventTable.Column.ID.ordinal)
                val timestamp = it.getLong(LogEventTable.Column.TIMESTAMP.ordinal)
                val message = it.getString(LogEventTable.Column.MESSAGE.ordinal)
                cloudWatchLogEvents.add(LogEvent(timestamp, message, id))
            } while (cursor.moveToNext())
        }
        cloudWatchLogEvents
    }

    internal suspend fun bulkDelete(eventIds: List<Long>) = withContext(coroutineDispatcher) {
        contentUri
        val whereClause = "${LogEventTable.COLUMN_ID} in (?)"
        database.delete(
            LogEventTable.TABLE_LOG_EVENT,
            whereClause,
            arrayOf(eventIds.joinToString(","))
        )
    }

    internal fun isCacheFull(cacheSizeInMB: Int): Boolean {
        val path = context.getDatabasePath(CloudWatchDatabaseHelper.DATABASE_NAME)
        return if (path.exists()) {
            path.length() >= cacheSizeInMB * mb
        } else {
            false
        }
    }

    internal suspend fun clearDatabase() = withContext(coroutineDispatcher) {
        database.delete(LogEventTable.TABLE_LOG_EVENT, null, null)
    }

    private fun insertEvent(event: CloudWatchLogEvent): Uri {
        val contentValues = ContentValues()
        contentValues.put(LogEventTable.COLUMN_TIMESTAMP, event.timestamp)
        contentValues.put(LogEventTable.COLUMN_MESSAGE, event.message)
        val id = database.insertOrThrow(LogEventTable.TABLE_LOG_EVENT, null, contentValues)
        return Uri.parse("$basePath/$id")
    }

    private fun query(
        projection: Array<String?>? = null,
        selection: String? = null,
        selectionArgs: Array<String?>? = null,
        sortOrder: String? = null,
        limit: String? = null
    ): Cursor {
        val queryBuilder = SQLiteQueryBuilder()
        queryBuilder.tables = LogEventTable.TABLE_LOG_EVENT
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

    private fun getDatabasePassphrase(): String {
        return encryptedKeyValueRepository.get(passphraseKey) ?: kotlin.run {
            val passphrase = UUID.randomUUID().toString()
            // If the database is restored from backup and the passphrase key is not present,
            // this would result in the database file not getting loaded.
            // To avoid this error, check to see if the database file exists and, if so, delete it and then recreate the database.
            val path = context.getDatabasePath(CloudWatchDatabaseHelper.DATABASE_NAME)
            if (path.exists()) {
                path.delete()
            }
            encryptedKeyValueRepository.put(passphraseKey, passphrase)
            passphrase
        }
    }
}
