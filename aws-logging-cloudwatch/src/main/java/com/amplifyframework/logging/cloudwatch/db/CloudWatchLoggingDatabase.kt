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
import android.content.SharedPreferences
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.amplifyframework.logging.cloudwatch.CloudWatchLogEvent
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteQueryBuilder

internal class CloudWatchLoggingDatabase(
    private val context: Context,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val logEvents = 10
    private val logEventsId = 20
    private val cloudWatchDatabaseHelper = CloudWatchDatabaseHelper(context)
    private val passphraseKey = "passphrase"
    private val sharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            "awscloudwatchloggingdb.${getInstallationIdentifier(context)}",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    private val database by lazy {
        val path = context.getDatabasePath(CloudWatchDatabaseHelper.DATABASE_NAME)
        val db = SQLiteDatabase.openOrCreateDatabase(path, getDatabasePassphrase(), null, null)
        LogEventTable.onCreate(db, 1)
        db
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

    fun closeDB() {
        cloudWatchDatabaseHelper.close()
    }

    suspend fun saveLogEvent(event: CloudWatchLogEvent): Uri {
        return withContext(coroutineDispatcher) {
            insertEvent(contentUri, event)
        }
    }

    suspend fun queryAllEvents(): List<CloudWatchLogEvent> {
        return withContext(coroutineDispatcher) {
            val cloudWatchLogEvents = mutableListOf<CloudWatchLogEvent>()
            val cursor = query(contentUri, null, null, null, null, null)
            cursor.use {
                if (!it.moveToFirst()) {
                    return@use
                }
                do {
                    val id = it.getInt(LogEventTable.COLUMNINDEX.ID.index)
                    val timestamp = it.getLong(LogEventTable.COLUMNINDEX.TIMESTAMP.index)
                    val message = it.getString(LogEventTable.COLUMNINDEX.MESSAGE.index)
                    cloudWatchLogEvents.add(CloudWatchLogEvent(timestamp.toLong(), message, id))
                } while (cursor.moveToNext())
            }
            cloudWatchLogEvents
        }
    }

    suspend fun bulkDelete(eventIds: List<Int>) {
        return withContext(coroutineDispatcher) {
            val uri = contentUri
            val whereClause = "${LogEventTable.COLUMN_ID} in (${eventIds.joinToString(",")})"
            database.delete(
                LogEventTable.TABLE_LOG_EVENT,
                whereClause,
                null,
            )
        }
    }

    private suspend fun insertEvent(uri: Uri, event: CloudWatchLogEvent): Uri {
        val contentValues = ContentValues()
        contentValues.put(LogEventTable.COLUMN_TIMESTAMP, event.timestamp)
        contentValues.put(LogEventTable.COLUMN_MESSAGE, event.message)
        val id = database.insertOrThrow(LogEventTable.TABLE_LOG_EVENT, null, contentValues)
        return Uri.parse("$basePath/$id")
    }

    private fun query(
        uri: Uri,
        projection: Array<String?>? = null,
        selection: String? = null,
        selectionArgs: Array<String?>? = null,
        sortOrder: String? = null,
        limit: String? = null,
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
            limit,
        )
    }

    private fun getInstallationIdentifier(context: Context): String {
        val identifierFile = File(context.noBackupFilesDir, "awscloudwatchloggingdb.installationIdentifier")
        val previousIdentifier = getExistingInstallationIdentifier(identifierFile)
        return previousIdentifier ?: createInstallationIdentifier(identifierFile)
    }

    private fun getExistingInstallationIdentifier(identifierFile: File): String? {
        return if (identifierFile.exists()) {
            val identifier = identifierFile.readText()
            identifier.ifBlank { null }
        } else {
            null
        }
    }

    private fun createInstallationIdentifier(identifierFile: File): String {
        val newIdentifier = UUID.randomUUID().toString()
        try {
            identifierFile.writeText(newIdentifier)
        } catch (e: Exception) {
            // Failed to write identifier to file, session will be forced to be in memory
        }
        return newIdentifier
    }

    private fun getDatabasePassphrase(): String {
        return sharedPreferences.getString(passphraseKey, null) ?: UUID.randomUUID().toString().also { passphrase ->
            sharedPreferences.edit().putString(passphraseKey, passphrase).apply()
        }
    }
}
