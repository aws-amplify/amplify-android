/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
@file:OptIn(InternalAmplifyApi::class)

package com.amplifyframework.cloudwatch.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.cloudwatch.models.CloudWatchLogEvent
import com.amplifyframework.foundation.store.AmplifyKeyValueRepository
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SQLiteQueryBuilder

/**
 * SQLCipher-backed local buffer for CloudWatch log events. The database is encrypted with a
 * randomly-generated passphrase persisted via [AmplifyKeyValueRepository].
 */
@InternalAmplifyApi
class CloudWatchDatabase(
    private val context: Context,
    private val databaseName: String,
    private val passphrasePreferencesName: String,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val passphraseKey = "passphrase"
    private val mb = 1024 * 1024
    private val amplifyKeyValueRepository: AmplifyKeyValueRepository by lazy {
        AmplifyKeyValueRepository(
            context,
            passphrasePreferencesName
        )
    }
    private val database by lazy {
        System.loadLibrary("sqlcipher")
        CloudWatchDatabaseHelper(context, databaseName, getDatabasePassphrase()).writableDatabase
    }

    suspend fun saveLogEvent(event: CloudWatchLogEvent): Long = withContext(coroutineDispatcher) {
        insertEvent(event)
    }

    suspend fun queryAllEvents(): List<LogEvent> = withContext(coroutineDispatcher) {
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

    suspend fun bulkDelete(eventIds: List<Long>) = withContext(coroutineDispatcher) {
        if (eventIds.isNotEmpty()) {
            val params = List(eventIds.size) { "?" }.joinToString(",")
            val whereClause = "${LogEventTable.COLUMN_ID} in ($params)"
            database.delete(
                LogEventTable.TABLE_LOG_EVENT,
                whereClause,
                eventIds.toTypedArray()
            )
        }
    }

    fun isCacheFull(cacheSizeInMB: Int): Boolean {
        val path = context.getDatabasePath(databaseName)
        return if (path.exists()) {
            path.length() >= cacheSizeInMB * mb
        } else {
            false
        }
    }

    suspend fun clearDatabase() = withContext(coroutineDispatcher) {
        database.delete(LogEventTable.TABLE_LOG_EVENT, null, null)
    }

    private fun insertEvent(event: CloudWatchLogEvent): Long {
        val contentValues = ContentValues()
        contentValues.put(LogEventTable.COLUMN_TIMESTAMP, event.timestamp)
        contentValues.put(LogEventTable.COLUMN_MESSAGE, event.message)
        return database.insertOrThrow(LogEventTable.TABLE_LOG_EVENT, null, contentValues)
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

    fun getDatabasePassphrase(): String = amplifyKeyValueRepository.get(passphraseKey) ?: kotlin.run {
        val passphrase = UUID.randomUUID().toString()
        // If the database is restored from backup and the passphrase key is not present,
        // this would result in the database file not getting loaded.
        // To avoid this error, check to see if the database file exists and, if so, delete it and then recreate it.
        val path = context.getDatabasePath(databaseName)
        if (path.exists()) {
            path.delete()
        }
        amplifyKeyValueRepository.put(passphraseKey, passphrase)
        passphrase
    }
}
