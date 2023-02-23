/**
 * Copyright 2015-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.database.sqlite.SQLiteDatabase
import java.util.UUID

internal class TransferTable {
    companion object {
        // Database table name
        const val TABLE_TRANSFER = "awstransfer"

        // A unique id of the transfer record
        const val COLUMN_ID = "_id"

        // For upload part record only, the transfer id of the main record of the part record.
        const val COLUMN_MAIN_UPLOAD_ID = "main_upload_id"

        // Transfer type, can be whether "upload" or "download"
        const val COLUMN_TYPE = "type"

        // The current state of the transfer, values of all states are in `TransferConstants`.
        const val COLUMN_STATE = "state"

        // The name of the bucket.
        const val COLUMN_BUCKET_NAME = "bucket_name"

        // A key in the bucket.
        const val COLUMN_KEY = "key"

        // The total bytes to transfer.
        const val COLUMN_BYTES_TOTAL = "bytes_total"

        // The bytes currently transferred.
        const val COLUMN_BYTES_CURRENT = "bytes_current"

        // The path of the file to transfer.
        const val COLUMN_FILE = "file"

        // The bytes offset of the file.
        const val COLUMN_FILE_OFFSET = "file_offset"

        // Whether the transfer is a multi-part transfer.
        const val COLUMN_IS_MULTIPART = "is_multipart"

        // Whether the part is the last part of the file.
        const val COLUMN_IS_LAST_PART = "is_last_part"

        // The number of the part in the transfer.
        const val COLUMN_PART_NUM = "part_num"

        // The multipart upload id.
        const val COLUMN_MULTIPART_ID = "multipart_id"

        // The Etag of the transfer
        const val COLUMN_ETAG = "etag"

        // The range's start index in the file.
        const val COLUMN_DATA_RANGE_START = "range_start"

        // The range's end index in the file.
        const val COLUMN_DATA_RANGE_LAST = "range_last"

        // Whether the transfer is encrypted.
        const val COLUMN_IS_ENCRYPTED = "is_encrypted"

        // the following columns are not used yet
        const val COLUMN_SPEED = "speed"
        const val COLUMN_VERSION_ID = "version_id"
        const val COLUMN_HEADER_EXPIRE = "header_expire"

        // If the object requires the requester to pay
        const val COLUMN_IS_REQUESTER_PAYS = "is_requester_pays"

        // User specified content Type
        const val COLUMN_HEADER_CONTENT_TYPE = "header_content_type"

        // User specified content language
        const val COLUMN_HEADER_CONTENT_LANGUAGE = "header_content_language"

        // User specified content disposition
        const val COLUMN_HEADER_CONTENT_DISPOSITION = "header_content_disposition"

        // User specified content encoding
        const val COLUMN_HEADER_CONTENT_ENCODING = "header_content_encoding"

        // User specified cache control
        const val COLUMN_HEADER_CACHE_CONTROL = "header_cache_control"

        // User specified storage class
        const val COLUMN_HEADER_STORAGE_CLASS = "header_storage_class"

        // User specified lifecycle configuration expiration time rule id
        const val COLUMN_EXPIRATION_TIME_RULE_ID = "expiration_time_rule_id"

        // User specified lifecycle configuration expiration time rule id
        const val COLUMN_HTTP_EXPIRES_DATE = "http_expires_date"

        // User specified server side encryption algorithm
        const val COLUMN_SSE_ALGORITHM = "sse_algorithm"

        // User specified content MD5
        const val COLUMN_CONTENT_MD5 = "content_md5"

        // Json serialization of user metadata to store with the Object
        const val COLUMN_USER_METADATA = "user_metadata"

        // User specified KMS key for server side encryption
        const val COLUMN_SSE_KMS_KEY = "kms_key"

        // Canned ACL of this upload.
        const val COLUMN_CANNED_ACL = "canned_acl"

        // The allowed connection types a transfer can use.
        const val COLUMN_TRANSFER_UTILITY_OPTIONS = "transfer_utility_options"

        const val COLUMN_WORKMANAGER_REQUEST_ID = "workmanager_request_id"

        // A unique transfer id for user to query
        const val COLUMN_TRANSFER_ID = "transfer_id"

        const val COLUMN_USE_ACCELERATE_ENDPOINT = "useAccelerateEndpoint"

        private const val TABLE_VERSION_2 = 2
        private const val TABLE_VERSION_3 = 3
        private const val TABLE_VERSION_4 = 4
        private const val TABLE_VERSION_5 = 5
        private const val TABLE_VERSION_6 = 6
        private const val TABLE_VERSION_7 = 7
        private const val TABLE_VERSION_8 = 8
        private const val TABLE_VERSION_9 = 9

        // Database creation SQL statement
        const val DATABASE_CREATE = "create table $TABLE_TRANSFER (" +
            "$COLUMN_ID integer primary key autoincrement, " +
            "$COLUMN_MAIN_UPLOAD_ID integer, " +
            "$COLUMN_TYPE  text not null, " +
            "$COLUMN_STATE text not null, " +
            "$COLUMN_BUCKET_NAME text not null, " +
            "$COLUMN_KEY text not null," +
            "$COLUMN_VERSION_ID text, " +
            "$COLUMN_BYTES_TOTAL bigint, " +
            "$COLUMN_BYTES_CURRENT bigint, " +
            "$COLUMN_SPEED bigint, " +
            "$COLUMN_IS_REQUESTER_PAYS integer, " +
            "$COLUMN_IS_ENCRYPTED integer, " +
            "$COLUMN_FILE text not null, " +
            "$COLUMN_FILE_OFFSET bigint, " +
            "$COLUMN_IS_MULTIPART int, " +
            "$COLUMN_PART_NUM int not null, " +
            "$COLUMN_IS_LAST_PART integer, " +
            "$COLUMN_MULTIPART_ID text, " +
            "$COLUMN_ETAG text, " +
            "$COLUMN_DATA_RANGE_START bigint, " +
            "$COLUMN_DATA_RANGE_LAST bigint, " +
            "$COLUMN_HEADER_CONTENT_TYPE text, " +
            "$COLUMN_HEADER_CONTENT_LANGUAGE text, " +
            "$COLUMN_HEADER_CONTENT_DISPOSITION text, " +
            "$COLUMN_HEADER_CONTENT_ENCODING text, " +
            "$COLUMN_HEADER_CACHE_CONTROL text, " +
            "$COLUMN_HEADER_EXPIRE text);"

        /**
         * Creates the database.
         *
         * @param database An SQLiteDatabase instance.
         */
        @JvmStatic
        fun onCreate(database: SQLiteDatabase, version: Int) {
            database.execSQL(DATABASE_CREATE)
            onUpgrade(database, 1, version)
        }

        /**
         * Upgrades the database.
         *
         * @param database An SQLiteDatabase instance.
         * @param oldVersion The old version of the database.
         * @param newVersion The new version of the database.
         */
        @JvmStatic
        fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            database.beginTransaction()

            if (TABLE_VERSION_2 in (oldVersion + 1)..newVersion) {
                addVersion2Columns(database)
            }
            if (TABLE_VERSION_3 in (oldVersion + 1)..newVersion) {
                addVersion3Columns(database)
            }
            if (TABLE_VERSION_4 in (oldVersion + 1)..newVersion) {
                addVersion4Columns(database)
            }
            if (TABLE_VERSION_5 in (oldVersion + 1)..newVersion) {
                addVersion5Columns(database)
            }
            if (TABLE_VERSION_6 in (oldVersion + 1)..newVersion) {
                addVersion6Columns(database)
            }
            if (TABLE_VERSION_7 in (oldVersion + 1)..newVersion) {
                addVersion7Columns(database)
            }
            if (TABLE_VERSION_8 in (oldVersion + 1)..newVersion) {
                addVersion8Columns(database)
            }
            if (TABLE_VERSION_9 in (oldVersion + 1)..newVersion) {
                addVersion9Columns(database)
            }
            database.setTransactionSuccessful()
            database.endTransaction()
        }

        /**
         * Adds columns that were introduced in version 2 to the database
         */
        private fun addVersion2Columns(database: SQLiteDatabase) {
            val addUserMetadata = "ALTER TABLE $TABLE_TRANSFER ADD COLUMN $COLUMN_USER_METADATA text;"
            val addExpirationTimeRuleId = "ALTER TABLE $TABLE_TRANSFER ADD COLUMN $COLUMN_EXPIRATION_TIME_RULE_ID text;"
            val addHttpExpires = "ALTER TABLE $TABLE_TRANSFER ADD COLUMN $COLUMN_HTTP_EXPIRES_DATE text;"
            val addSSEAlgorithm = "ALTER TABLE $TABLE_TRANSFER ADD COLUMN $COLUMN_SSE_ALGORITHM text;"
            val addContentMD5 = "ALTER TABLE $TABLE_TRANSFER ADD COLUMN $COLUMN_CONTENT_MD5 text;"
            database.execSQL(addUserMetadata)
            database.execSQL(addExpirationTimeRuleId)
            database.execSQL(addHttpExpires)
            database.execSQL(addSSEAlgorithm)
            database.execSQL(addContentMD5)
        }

        /**
         * Adds columns that were introduced in version 3 to the database
         */
        private fun addVersion3Columns(database: SQLiteDatabase) {
            val addKMSKey = "ALTER TABLE $TABLE_TRANSFER ADD COLUMN $COLUMN_SSE_KMS_KEY text;"
            database.execSQL(addKMSKey)
        }

        /**
         * Adds columns that were introduced in version 4 to the database
         */
        private fun addVersion4Columns(database: SQLiteDatabase) {
            val addCannedAcl = "ALTER TABLE $TABLE_TRANSFER ADD COLUMN $COLUMN_CANNED_ACL text;"
            database.execSQL(addCannedAcl)
        }

        /**
         * Adds columns that were introduced in version 5 to the database
         */
        private fun addVersion5Columns(database: SQLiteDatabase) {
            val addStorageClass = "ALTER TABLE $TABLE_TRANSFER ADD COLUMN $COLUMN_HEADER_STORAGE_CLASS text;"
            database.execSQL(addStorageClass)
        }

        /**
         * Adds columns that were introduced in version 6 to the database
         */
        private fun addVersion6Columns(database: SQLiteDatabase) {
            val addConnectionType = "ALTER TABLE $TABLE_TRANSFER ADD COLUMN $COLUMN_TRANSFER_UTILITY_OPTIONS text;"
            database.execSQL(addConnectionType)
        }

        /**
         * Adds columns that were introduced in version 7 to the database
         */
        private fun addVersion7Columns(database: SQLiteDatabase) {
            val addConnectionType = "ALTER TABLE $TABLE_TRANSFER ADD COLUMN $COLUMN_WORKMANAGER_REQUEST_ID text;"
            database.execSQL(addConnectionType)
        }

        /**
         * Adds columns that were introduced in version 8 to the database
         */
        private fun addVersion8Columns(database: SQLiteDatabase) {
            val addConnectionType = "ALTER TABLE $TABLE_TRANSFER ADD COLUMN $COLUMN_TRANSFER_ID text " +
                "DEFAULT '${UUID.randomUUID()}';"
            database.execSQL(addConnectionType)
        }

        /**
         * Adds columns that were introduced in version 8 to the database
         */
        private fun addVersion9Columns(database: SQLiteDatabase) {
            val addConnectionType = "ALTER TABLE $TABLE_TRANSFER ADD COLUMN $COLUMN_USE_ACCELERATE_ENDPOINT int " +
                "DEFAULT 0;"
            database.execSQL(addConnectionType)
        }
    }
}
