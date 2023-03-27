/**
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 * <p>
 * http://aws.amazon.com/apache2.0
 * <p>
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.storage.s3.transfer

import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.s3.AWSS3StoragePlugin

@VisibleForTesting
internal class TransferDBHelper(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    internal val contentUri: Uri
    private val uriMatcher: UriMatcher
    private var database: SQLiteDatabase
    private val logger = Amplify.Logging.forNamespace(
        AWSS3StoragePlugin.AWS_S3_STORAGE_LOG_NAMESPACE.format(this::class.java.simpleName)
    )

    companion object {
        private const val DATABASE_NAME = "awss3transfertable.db"

        // This represents the latest database version.
        // Update this when the database is being upgraded.
        private const val DATABASE_VERSION = 9
        private const val BASE_PATH = "transfers"
        private const val TRANSFERS = 10
        private const val TRANSFER_ID = 20
        private const val TRANSFER_PART = 30
        private const val TRANSFER_STATE = 40
        private const val TRANSFER_RECORD_ID = 50
    }

    init {
        val authority = context.applicationContext.packageName
        database = writableDatabase
        contentUri = Uri.parse("content://$authority/$BASE_PATH")
        uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        // The Uri of TRANSFERS is for all records in the table.
        uriMatcher.addURI(authority, BASE_PATH, TRANSFERS)

        // The Uri of TRANSFER_ID is for a single transfer record.
        uriMatcher.addURI(authority, "$BASE_PATH/#", TRANSFER_ID)

        // The Uri of TRANSFER_PART is for part records of a multipart upload.
        uriMatcher.addURI(authority, "$BASE_PATH/part/#", TRANSFER_PART)

        // The Uri of TRANSFER_STATE is for records with a specific state.
        uriMatcher.addURI(authority, "$BASE_PATH/state/*", TRANSFER_STATE)

        uriMatcher.addURI(authority, "$BASE_PATH/transferId/*", TRANSFER_RECORD_ID)
    }

    override fun onCreate(database: SQLiteDatabase) {
        TransferTable.onCreate(database, DATABASE_VERSION)
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        TransferTable.onUpgrade(database, oldVersion, newVersion)
    }

    override fun onDowngrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        context.deleteDatabase(DATABASE_NAME)
        onCreate(database)
    }

    /**
     * Inserts a record to the table.
     *
     * @param uri The Uri of a table.
     * @param values The values of a record.
     * @return The Uri of the inserted record.
     */
    internal fun insert(uri: Uri, values: ContentValues): Uri {
        val uriType: Int = uriMatcher.match(uri)
        ensureDatabaseOpen()
        val id: Long = when (uriType) {
            TRANSFERS -> database.insertOrThrow(
                TransferTable.TABLE_TRANSFER,
                null,
                values
            )
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        return Uri.parse("$BASE_PATH/$id")
    }

    /**
     * Query records from the database.
     *
     * @param uri A Uri indicating which part of data to query.
     * @param projection The projection of columns.
     * @param selection The "where" clause of sql.
     * @param selectionArgs Strings in the "where" clause.
     * @param sortOrder Sorting order of the query.
     * @return A Cursor pointing to records.
     */
    internal fun query(
        uri: Uri,
        projection: Array<String?>? = null,
        selection: String? = null,
        selectionArgs: Array<String?>? = null,
        sortOrder: String? = null
    ): Cursor {
        val queryBuilder = SQLiteQueryBuilder()
        queryBuilder.tables = TransferTable.TABLE_TRANSFER

        when (uriMatcher.match(uri)) {
            TRANSFERS -> queryBuilder.appendWhere("${TransferTable.COLUMN_PART_NUM}=0")
            TRANSFER_ID -> queryBuilder.appendWhere(
                "${TransferTable.COLUMN_ID}=${uri.lastPathSegment}"
            )
            TRANSFER_PART -> queryBuilder.appendWhere(
                "${TransferTable.COLUMN_MAIN_UPLOAD_ID}=${uri.lastPathSegment}"
            )
            TRANSFER_STATE -> {
                queryBuilder.appendWhere(TransferTable.COLUMN_STATE + "=")
                queryBuilder.appendWhereEscapeString(uri.lastPathSegment!!)
            }
            TRANSFER_RECORD_ID -> {
                queryBuilder.appendWhere(
                    "${TransferTable.COLUMN_TRANSFER_ID}='${uri.lastPathSegment}'"
                )
            }
            else -> throw java.lang.IllegalArgumentException("Unknown URI: $uri")
        }
        ensureDatabaseOpen()
        return queryBuilder.query(
            database,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            sortOrder
        )
    }

    /**
     * Updates records in the table synchronously.
     *
     * @param uri A Uri of the specific record.
     * @param values The values to update.
     * @param whereClause The "where" clause of sql.
     * @param whereArgs Strings in the "where" clause.
     * @return Number of rows updated.
     */
    @Synchronized
    internal fun update(
        uri: Uri,
        values: ContentValues,
        whereClause: String?,
        whereArgs: Array<String>?
    ): Int {
        val uriType = uriMatcher.match(uri)
        ensureDatabaseOpen()
        return when (uriType) {
            TRANSFERS -> database.update(
                TransferTable.TABLE_TRANSFER,
                values,
                whereClause,
                whereArgs
            )
            TRANSFER_ID -> {
                val id = uri.lastPathSegment
                if (TextUtils.isEmpty(whereClause)) {
                    database.update(
                        TransferTable.TABLE_TRANSFER,
                        values,
                        "${TransferTable.COLUMN_ID}=$id",
                        null
                    )
                } else {
                    database.update(
                        TransferTable.TABLE_TRANSFER,
                        values,
                        "${TransferTable.COLUMN_ID}=$id and $whereClause",
                        whereArgs
                    )
                }
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    /**
     * Deletes a record in the table.
     *
     * @param uri A Uri of the specific record.
     * @param selection The "where" clause of sql.
     * @param selectionArgs Strings in the "where" clause.
     * @return Number of rows deleted.
     */
    internal fun delete(
        uri: Uri,
        selection: String? = null,
        selectionArgs: Array<String>? = null
    ): Int {
        val uriType = uriMatcher.match(uri)
        ensureDatabaseOpen()
        return when (uriType) {
            TRANSFERS -> database.delete(
                TransferTable.TABLE_TRANSFER,
                selection,
                selectionArgs
            )
            TRANSFER_ID, TRANSFER_PART, TRANSFER_RECORD_ID -> {
                val columnName = when (uriType) {
                    TRANSFER_PART -> { TransferTable.COLUMN_MAIN_UPLOAD_ID }
                    TRANSFER_RECORD_ID -> { TransferTable.COLUMN_TRANSFER_ID }
                    else -> { TransferTable.COLUMN_ID }
                }
                val id = uri.lastPathSegment
                if (TextUtils.isEmpty(selection)) {
                    database.delete(
                        TransferTable.TABLE_TRANSFER,
                        "$columnName=$id",
                        null
                    )
                } else {
                    database.delete(
                        TransferTable.TABLE_TRANSFER,
                        "$$columnName=$id and $selection",
                        selectionArgs
                    )
                }
            }
            else -> throw java.lang.IllegalArgumentException("Unknown URI: $uri")
        }
    }

    /**
     * @param uri The Uri of a table.
     * @param valuesArray A array of values to insert.
     * @return The mainUploadId of the multipart transfer records
     */
    internal fun bulkInsert(uri: Uri, valuesArray: Array<ContentValues?>): Int {
        val uriType = uriMatcher.match(uri)
        var mainUploadId = 0
        ensureDatabaseOpen()
        when (uriType) {
            TRANSFERS ->
                try {
                    database.beginTransaction()
                    mainUploadId = database.insertOrThrow(
                        TransferTable.TABLE_TRANSFER,
                        null,
                        valuesArray[0]
                    ).toInt()
                    for (i in 1 until valuesArray.size) {
                        valuesArray[i]?.put(
                            TransferTable.COLUMN_MAIN_UPLOAD_ID,
                            mainUploadId
                        )
                        database.insertOrThrow(
                            TransferTable.TABLE_TRANSFER,
                            null,
                            valuesArray[i]
                        )
                    }
                    database.setTransactionSuccessful()
                } catch (e: Exception) {
                    logger.error("bulkInsert error : ", e)
                } finally {
                    database.endTransaction()
                }
            else -> throw java.lang.IllegalArgumentException("Unknown URI: $uri")
        }
        return mainUploadId
    }

    @Synchronized
    private fun ensureDatabaseOpen() {
        if (!database.isOpen) {
            database = writableDatabase
        }
    }
}
