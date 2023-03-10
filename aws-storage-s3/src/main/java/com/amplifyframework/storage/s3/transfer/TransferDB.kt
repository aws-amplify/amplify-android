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

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import aws.sdk.kotlin.services.s3.model.CompletedPart
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.ObjectMetadata
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.amplifyframework.storage.s3.utils.JsonUtils
import java.io.File

/**
 * SQlite database to store transfer records
 */
@SuppressLint("VisibleForTests")
internal class TransferDB private constructor(context: Context) {

    private var transferDBHelper: TransferDBHelper = synchronized(this) {
        TransferDBHelper(context)
    }

    private val logger =
        Amplify.Logging.forNamespace(
            AWSS3StoragePlugin.AWS_S3_STORAGE_LOG_NAMESPACE.format(this::class.java.simpleName)
        )

    companion object {
        private const val QUERY_PLACE_HOLDER_STRING = ",?"
        private val instance: TransferDB? = null

        @JvmStatic
        fun getInstance(context: Context): TransferDB {
            return instance ?: TransferDB(context)
        }
    }

    fun closeDB() {
        synchronized(this) {
            transferDBHelper.close()
        }
    }

    /**
     * Inserts a part upload record into database with the given values.
     *
     * @param bucket The name of the bucket to upload to.
     * @param key The key in the specified bucket by which to store the new
     * object.
     * @param file The file to upload.
     * @param fileOffset The byte offset for the file to upload.
     * @param partNumber The part number of this part.
     * @param uploadId The multipart upload id of the upload.
     * @param bytesTotal The Total bytes of the file.
     * @param isLastPart Whether this part is the last part of the upload.
     * @return An Uri of the record inserted.
     */
    fun insertMultipartUploadRecord(
        transferId: String,
        bucket: String,
        key: String,
        file: File,
        fileOffset: Long,
        partNumber: Int,
        uploadId: String,
        bytesTotal: Long,
        isLastPart: Int,
        useAccelerateEndpoint: Boolean = false
    ): Uri {
        val values: ContentValues = generateContentValuesForMultiPartUpload(
            transferId,
            bucket, key, file,
            fileOffset, partNumber, uploadId, bytesTotal, isLastPart, ObjectMetadata(),
            null,
            useAccelerateEndpoint
        )
        return transferDBHelper.insert(transferDBHelper.contentUri, values)
    }

    /**
     * Inserts a transfer record into database with the given values.
     *
     * @param type The type of the transfer, can be "upload" or "download".
     * @param bucket The name of the bucket to upload to.
     * @param key The key in the specified bucket by which to store the new
     * object.
     * @param file The file to upload.
     * @param metadata The S3 Object metadata associated with this object
     * @param cannedAcl The canned Acl of this S3 object
     * @return An Uri of the record inserted.
     */
    fun insertSingleTransferRecord(
        transferId: String,
        type: TransferType,
        bucket: String,
        key: String,
        file: File?,
        cannedAcl: ObjectCannedAcl? = null,
        metadata: ObjectMetadata? = ObjectMetadata(),
        useAccelerateEndpoint: Boolean = false
    ): Uri {
        val values = generateContentValuesForSinglePartTransfer(
            transferId,
            type,
            bucket,
            key,
            file,
            metadata,
            cannedAcl,
            useAccelerateEndpoint
        )
        return transferDBHelper.insert(transferDBHelper.contentUri, values)
    }

    /**
     * Inserts multiple records at a time.
     *
     * @param valuesArray An array of values to insert.
     * @return The mainUploadId of the multipart records
     */
    fun bulkInsertTransferRecords(valuesArray: Array<ContentValues?>): Int {
        return transferDBHelper.bulkInsert(transferDBHelper.contentUri, valuesArray)
    }

    /**
     * Updates the current bytes of a transfer record.
     *
     * @param id The id of the transfer
     * @param currentBytes The bytes currently transferred
     * @param totalBytes The total bytes to be transferred
     * @return Number of rows updated.
     */
    fun updateBytesTransferred(id: Int, currentBytes: Long, totalBytes: Long): Int {
        val values = ContentValues()
        values.put(
            TransferTable.COLUMN_BYTES_CURRENT,
            currentBytes
        )
        values.put(
            TransferTable.COLUMN_BYTES_TOTAL,
            totalBytes
        )
        return transferDBHelper.update(getRecordUri(id), values, null, null)
    }

    /**
     * Updates the current bytes of a transfer record.
     *
     * @param id The id of the transfer
     * @param requestId The id of the workmanager request
     * @return Number of rows updated.
     */
    fun updateWorkManagerRequestId(id: Int, requestId: String): Int {
        val values = ContentValues()
        values.put(
            TransferTable.COLUMN_WORKMANAGER_REQUEST_ID,
            requestId
        )
        return transferDBHelper.update(getRecordUri(id), values, null, null)
    }

    /**
     * Updates the total bytes of a download record.
     *
     * @param id The id of the transfer
     * @param bytes The total bytes of the download.
     * @return Number of rows updated.
     */
    fun updateBytesTotalForDownload(id: Int, bytes: Long): Int {
        val values = ContentValues()
        values.put(TransferTable.COLUMN_BYTES_TOTAL, bytes)
        return transferDBHelper.update(getRecordUri(id), values, null, null)
    }

    /**
     * Updates the state but do not notify TransferService to refresh its
     * transfer record list. Therefore, only TransferObserver knows the state
     * change of the transfer record. If the new state is STATE_FAILED, we need
     * to check the original state, because "pause", "cancel" and
     * "disconnect network" actions may also cause failure message of the
     * threads, but these are not actual failure of transfers.
     *
     * @param id The id of the transfer.
     * @param state The new state of the transfer.
     * @return Number of rows updated.
     */
    fun updateState(
        id: Int,
        state: TransferState
    ): Int {
        logger.info("update state for $id to ${state.name}")
        val values = ContentValues()
        values.put(TransferTable.COLUMN_STATE, state.toString())
        return transferDBHelper.update(getRecordUri(id), values, null, null)
    }

    /**
     * Updates the multipart id of the transfer record.
     *
     * @param id The id of the transfer.
     * @param multipartId The multipart id of the transfer.
     * @return Number of rows updated.
     */
    fun updateMultipartId(id: Int, multipartId: String?): Int {
        val values = ContentValues()
        values.put(
            TransferTable.COLUMN_MULTIPART_ID,
            multipartId
        )
        return transferDBHelper.update(getRecordUri(id), values, null, null)
    }

    /**
     * Updates states of all transfer records with the specified type which are
     * "running" and "waiting" to "pending pause".
     *
     * @param type The type of transfers to query for.
     * @return Number of rows updated.
     */
    fun pauseAllWithType(type: TransferType): Int {
        val values = ContentValues()
        values.put(
            TransferTable.COLUMN_STATE,
            TransferState.PENDING_PAUSE.toString()
        )
        val selection: String?
        val selectionArgs: Array<String>?
        if (type == TransferType.ANY) {
            selection =
                TransferTable.COLUMN_STATE + " in (?,?,?)"
            selectionArgs = arrayOf(
                TransferState.IN_PROGRESS.toString(),
                TransferState.RESUMED_WAITING.toString(),
                TransferState.WAITING.toString()
            )
        } else {
            selection =
                (TransferTable.COLUMN_STATE + " in (?,?,?) and " + TransferTable.COLUMN_TYPE + "=?")
            selectionArgs = arrayOf(
                TransferState.IN_PROGRESS.toString(),
                TransferState.RESUMED_WAITING.toString(),
                TransferState.WAITING.toString(),
                type.toString()
            )
        }
        return transferDBHelper.update(
            transferDBHelper.contentUri,
            values,
            selection,
            selectionArgs
        )
    }

    /**
     * Updates states of all transfer records with the specified which are
     * "running" and "waiting" to "pending cancel"
     *
     * @param type The type of transfers to cancel
     * @return Number of rows updated.
     */
    fun cancelAllWithType(type: TransferType): Int {
        val values = ContentValues()
        values.put(
            TransferTable.COLUMN_STATE,
            TransferState.PENDING_CANCEL.toString()
        )
        val selection: String?
        val selectionArgs: Array<String>?
        if (type == TransferType.ANY) {
            selection =
                TransferTable.COLUMN_STATE + " in (?,?,?,?,?)"
            selectionArgs = arrayOf(
                TransferState.IN_PROGRESS.toString(),
                TransferState.RESUMED_WAITING.toString(),
                TransferState.WAITING.toString(),
                TransferState.PAUSED.toString()
            )
        } else {
            selection =
                (
                    TransferTable.COLUMN_STATE + " in (?,?,?,?,?) and " +
                        TransferTable.COLUMN_TYPE + "=?"
                    )
            selectionArgs = arrayOf(
                TransferState.IN_PROGRESS.toString(),
                TransferState.RESUMED_WAITING.toString(),
                TransferState.WAITING.toString(),
                TransferState.PAUSED.toString(),
                type.toString()
            )
        }
        return transferDBHelper.update(
            transferDBHelper.contentUri,
            values,
            selection,
            selectionArgs
        )
    }

    /**
     * Queries all the records which have the given type.
     *
     * @param type The type of transfers to query for.
     * @return A Cursor pointing to records in the database with the given type.
     */
    fun queryAllTransfersWithType(type: TransferType): Cursor? {
        return if (type == TransferType.ANY) {
            transferDBHelper.query(transferDBHelper.contentUri)
        } else {
            transferDBHelper.query(
                transferDBHelper.contentUri,
                selection = TransferTable.COLUMN_TYPE + "=?",
                selectionArgs = arrayOf(
                    type.toString()
                ),
            )
        }
    }

    /**
     * Queries all the records which have the given type and states.
     *
     * @param type   The type of Transfer
     * @param states The list of Transfer States whose Transfer Records are required.
     * @return A Cursor pointing to records in the database in any of the given states.
     */
    fun queryTransfersWithTypeAndStates(
        type: TransferType,
        states: Array<TransferState>
    ): Cursor? {
        val selection: String
        val selectionArgs: Array<String?>
        var index: Int
        val numStates = states.size
        val placeholderString: String? = createPlaceholders(numStates)
        if (type == TransferType.ANY) {
            selection = "${TransferTable.COLUMN_STATE} in ($placeholderString)"
            selectionArgs = arrayOfNulls(numStates)
            index = 0
            while (index < numStates) {
                selectionArgs[index] = states[index].toString()
                index++
            }
        } else {
            selection =
                "${TransferTable.COLUMN_STATE} in ($placeholderString) and $TransferTable.COLUMN_TYPE=?"
            selectionArgs = arrayOfNulls(numStates + 1)
            index = 0
            while (index < numStates) {
                selectionArgs[index] = states[index].toString()
                index++
            }
            selectionArgs[index] = type.toString()
        }
        return transferDBHelper.query(
            transferDBHelper.contentUri,
            selection = selection,
            selectionArgs = selectionArgs
        )
    }

    /**
     * Queries the transfer record specified by id.
     *
     * @param id The id of the transfer.
     * @return The result Cursor of the query.
     */
    fun queryTransferById(id: Int): Cursor? {
        return transferDBHelper.query(getRecordUri(id))
    }

    /**
     * Gets the TransferRecord by id.
     *
     * @param id transfer id
     * @return a TransferRecord if exists, null otherwise
     */
    fun getTransferRecordById(id: Int): TransferRecord? {
        var transferRecord: TransferRecord? = null
        var c: Cursor? = null
        try {
            c = queryTransferById(id)
            c?.use {
                if (it.moveToFirst()) {
                    transferRecord = TransferRecord.updateFromDB(c)
                }
            }
        } catch (exception: Exception) {
            logger.error("Transfer Record Not Found", exception)
        }

        return transferRecord
    }

    fun getTransferByTransferId(transferId: String): TransferRecord? {
        var transferRecord: TransferRecord? = null
        var c: Cursor? = null
        try {
            c = transferDBHelper.query(getTransferRecordIdUri(transferId))
            c.use {
                if (it.moveToFirst()) {
                    transferRecord = TransferRecord.updateFromDB(c)
                }
            }
        } catch (exception: Exception) {
            logger.error("Transfer Record Not Found", exception)
        }
        return transferRecord
    }

    /**
     * Queries the transfer record specified by main upload id.
     *
     * @param mainUploadId The mainUploadId of a multipart upload task
     * @return The bytes already uploaded for this multipart upload task
     */
    fun queryBytesTransferredByMainUploadId(mainUploadId: Int): Long {
        var bytesTotal: Long = 0
        var c: Cursor = transferDBHelper.query(getPartUri(mainUploadId))
        c.use { cursor ->
            while (cursor.moveToNext()) {
                val state =
                    cursor.getString(cursor.getColumnIndexOrThrow(TransferTable.COLUMN_STATE))
                state?.takeIf { TransferState.PART_COMPLETED == TransferState.getState(state) }
                    ?.let {
                        bytesTotal += cursor.getLong(
                            cursor.getColumnIndexOrThrow(TransferTable.COLUMN_BYTES_TOTAL)
                        )
                    }
            }
        }
        return bytesTotal
    }

    /**
     * Deletes the record with the given id.
     *
     * @param id The id of the transfer to be deleted.
     * @return Number of rows deleted.
     */
    fun deleteTransferRecords(id: Int): Int {
        return transferDBHelper.delete(getRecordUri(id))
    }

    /**
     * Deletes the part transfer record with the given main id.
     *
     * @param id The main transfer id of the transfers to be deleted.
     * @return Number of rows deleted.
     */
    fun deletePartTransferRecords(id: Int): Int {
        return transferDBHelper.delete(getPartUri(id))
    }

    /**
     * Gets the Uri of part records of a multipart upload.
     *
     * @param mainUploadId The main upload id of the transfer.
     * @return The Uri of the part upload records that have the given
     * mainUploadId value.
     */
    private fun getTransferRecordIdUri(transferId: String): Uri {
        return Uri.parse(
            transferDBHelper.contentUri.toString() + "/transferId/" + transferId
        )
    }

    /**
     * Gets the Uri of part records of a multipart upload.
     *
     * @param mainUploadId The main upload id of the transfer.
     * @return The Uri of the part upload records that have the given
     * mainUploadId value.
     */
    private fun getPartUri(mainUploadId: Int): Uri {
        return Uri.parse(
            transferDBHelper.contentUri.toString() + "/part/" + mainUploadId
        )
    }

    /**
     * Gets the Uri of the records that have the given state.
     *
     * @param state The state of transfers
     * @return The Uri that is used to query transfer records with the given
     * state.
     */
    fun getStateUri(state: TransferState): Uri? {
        return Uri.parse(
            "${transferDBHelper.contentUri}/state/$state"
        )
    }

    /**
     * Create a string with the required number of placeholders
     *
     * @param numPlaceHolders Number of placeholders needed
     * @return String with the required placeholders
     */
    private fun createPlaceholders(numPlaceHolders: Int): String? {
        if (numPlaceHolders <= 0) {
            logger.error("Cannot create a string of 0 or less placeholders.")
            return null
        }
        val stringBuilder = StringBuilder(
            numPlaceHolders * QUERY_PLACE_HOLDER_STRING.length - 1
        )
        stringBuilder.append("?")
        for (index in 1 until numPlaceHolders) {
            stringBuilder.append(QUERY_PLACE_HOLDER_STRING)
        }
        return stringBuilder.toString()
    }

    /**
     * Gets the Uri of a record.
     *
     * @param id The id of the transfer.
     * @return The Uri of the record specified by the id.
     */
    private fun getRecordUri(id: Int): Uri {
        return Uri.parse(transferDBHelper.contentUri.toString() + "/" + id)
    }

    /**
     * Inserts a transfer record into database with the given values.
     *
     * @param type The type of the transfer, can be "upload" or "download".
     * @param bucket The name of the bucket to upload to.
     * @param key The key in the specified bucket by which to store the new
     * object.
     * @param file The file to upload.
     * @param metadata The S3 Object metadata associated with this object
     * @param cannedAcl The canned Acl of this S3 object
     * @return An Uri of the record inserted.
     */

    private fun insertSingleTransferRecord(
        transferId: String,
        type: TransferType,
        bucket: String,
        key: String,
        file: File,
        metadata: ObjectMetadata?,
        cannedAcl: ObjectCannedAcl?,
        useAccelerateEndpoint: Boolean
    ): Uri {
        val values = generateContentValuesForSinglePartTransfer(
            transferId,
            type,
            bucket,
            key,
            file,
            metadata,
            cannedAcl,
            useAccelerateEndpoint
        )
        return transferDBHelper.insert(
            transferDBHelper.contentUri,
            values
        )
    }

    /**
     * Generates a ContentValues object to insert into the database with the
     * given values for a multipart upload record.
     *
     * @param bucket The name of the bucket to upload to.
     * @param key The key in the specified bucket by which to store the new
     * object.
     * @param file The file to upload.
     * @param fileOffset The byte offset for the file to upload.
     * @param partNumber The part number of this part.
     * @param uploadId The multipart upload id of the upload.
     * @param bytesTotal The Total bytes of the file.
     * @param isLastPart Whether this part is the last part of the upload.
     * @param metadata The S3 ObjectMetadata to send along with the object
     * @param cannedAcl The canned ACL associated with the object
     * @return The ContentValues object generated.
     */
    fun generateContentValuesForMultiPartUpload(
        transferId: String,
        bucket: String?,
        key: String?,
        file: File,
        fileOffset: Long,
        partNumber: Int,
        uploadId: String?,
        bytesTotal: Long,
        isLastPart: Int,
        metadata: ObjectMetadata?,
        cannedAcl: ObjectCannedAcl?,
        useAccelerateEndpoint: Boolean
    ): ContentValues {
        val values = ContentValues()
        values.put(TransferTable.COLUMN_TRANSFER_ID, transferId)
        values.put(TransferTable.COLUMN_TYPE, TransferType.UPLOAD.toString())
        values.put(TransferTable.COLUMN_STATE, TransferState.WAITING.toString())
        values.put(TransferTable.COLUMN_BUCKET_NAME, bucket)
        values.put(TransferTable.COLUMN_KEY, key)
        values.put(TransferTable.COLUMN_FILE, file.absolutePath)
        values.put(TransferTable.COLUMN_BYTES_CURRENT, 0L)
        values.put(TransferTable.COLUMN_BYTES_TOTAL, bytesTotal)
        values.put(TransferTable.COLUMN_IS_MULTIPART, 1)
        values.put(TransferTable.COLUMN_PART_NUM, partNumber)
        values.put(TransferTable.COLUMN_FILE_OFFSET, fileOffset)
        values.put(TransferTable.COLUMN_MULTIPART_ID, uploadId)
        values.put(TransferTable.COLUMN_IS_LAST_PART, isLastPart)
        values.put(TransferTable.COLUMN_IS_ENCRYPTED, 0)
        values.put(TransferTable.COLUMN_USE_ACCELERATE_ENDPOINT, if (useAccelerateEndpoint) 1 else 0)
        values.putAll(generateContentValuesForObjectMetadata(metadata))
        cannedAcl?.let {
            values.put(TransferTable.COLUMN_CANNED_ACL, it.value)
        }
        return values
    }

    /**
     * Queries uncompleted partUpload tasks of a multipart upload and constructs
     *
     *
     * @param mainUploadId The mainUploadId of a multipart upload task
     * @return A list of recordID for non-completed multiPartId
     */
    fun getNonCompletedPartRequestsFromDB(mainUploadId: Int): List<Int> {
        val cursor = transferDBHelper.query(
            getPartUri(mainUploadId),
            null,
            selection = TransferTable.COLUMN_STATE + "!=?",
            arrayOf(
                TransferState.PART_COMPLETED.toString()
            ),
            null
        )
        val result = mutableListOf<Int>()
        cursor.use {
            while (it.moveToNext()) {
                val transferRecordId = it.getInt(it.getColumnIndexOrThrow(TransferTable.COLUMN_ID))
                result.add(transferRecordId)
            }
        }
        return result
    }

    /**
     * Updates the Etag of the transfer record.
     *
     * @param id The id of the transfer.
     * @param eTag The eTag of the transfer.
     * @return Number of rows updated.
     */
    fun updateETag(id: Int, eTag: String): Int {
        val values = ContentValues()
        values.put(
            TransferTable.COLUMN_ETAG,
            eTag
        )
        return transferDBHelper.update(getRecordUri(id), values, null, null)
    }

    /**
     * Queries all the PartETags of completed parts from the multipart upload
     * specified by the mainUploadId. The list of PartETags is used to complete
     * a multipart upload, so it's usually called after all partUpload tasks are
     * finished.
     *
     * @param mainUploadId The mainUploadId of a multipart upload task
     * @return A list of PartEtag of completed parts
     */
    fun queryPartETagsOfUpload(mainUploadId: Int): List<CompletedPart> {
        val partETags = mutableListOf<CompletedPart>()
        val cursor = transferDBHelper.query(getPartUri(mainUploadId), null, null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                val part = it.getInt(it.getColumnIndexOrThrow(TransferTable.COLUMN_PART_NUM))
                val tag = it.getString(it.getColumnIndexOrThrow(TransferTable.COLUMN_ETAG))
                partETags.add(
                    CompletedPart {
                        partNumber = part
                        eTag = tag
                    }
                )
            }
        }
        return partETags
    }

    /**
     * Generates a ContentValues object to insert into the database with the
     * given values for a single chunk upload or download.
     *
     * @param type The type of the transfer, can be "upload" or "download".
     * @param bucket The name of the bucket to upload to.
     * @param key The key in the specified bucket by which to store the new
     * object.
     * @param file The file to upload.
     * @param metadata The S3 ObjectMetadata to send along with the object
     * @param cannedAcl The canned ACL associated with the object
     * @return The ContentValues object generated.
     */
    private fun generateContentValuesForSinglePartTransfer(
        transferId: String,
        type: TransferType,
        bucket: String,
        key: String,
        file: File?,
        metadata: ObjectMetadata?,
        cannedAcl: ObjectCannedAcl?,
        useAccelerateEndpoint: Boolean
    ): ContentValues {
        val values = ContentValues()
        values.put(TransferTable.COLUMN_TRANSFER_ID, transferId)
        values.put(TransferTable.COLUMN_TYPE, type.toString())
        values.put(TransferTable.COLUMN_STATE, TransferState.WAITING.toString())
        values.put(TransferTable.COLUMN_BUCKET_NAME, bucket)
        values.put(TransferTable.COLUMN_KEY, key)
        values.put(TransferTable.COLUMN_FILE, file?.absolutePath)
        values.put(TransferTable.COLUMN_BYTES_CURRENT, 0L)
        if (type == TransferType.UPLOAD) {
            values.put(TransferTable.COLUMN_BYTES_TOTAL, file?.length())
        }
        values.put(TransferTable.COLUMN_IS_MULTIPART, 0)
        values.put(TransferTable.COLUMN_PART_NUM, 0)
        values.put(TransferTable.COLUMN_IS_ENCRYPTED, 0)
        values.putAll(generateContentValuesForObjectMetadata(metadata))
        values.put(TransferTable.COLUMN_CANNED_ACL, cannedAcl?.value)
        values.put(TransferTable.COLUMN_USE_ACCELERATE_ENDPOINT, if (useAccelerateEndpoint) 1 else 0)
        return values
    }

    /**
     * Adds mappings to a ContentValues object for the data in the passed in
     * ObjectMetadata
     *
     * @param metadata The ObjectMetadata the content values should be filled
     * with
     * @return the ContentValues
     */
    private fun generateContentValuesForObjectMetadata(metadata: ObjectMetadata?): ContentValues {
        val values = ContentValues()
        metadata?.let {
            values.apply {
                put(TransferTable.COLUMN_USER_METADATA, JsonUtils.mapToString(it.userMetadata))
                it.metaData[ObjectMetadata.CONTENT_TYPE]?.let {
                    put(TransferTable.COLUMN_HEADER_CONTENT_TYPE, it.toString())
                }
                it.metaData[ObjectMetadata.CONTENT_ENCODING]?.let {
                    put(TransferTable.COLUMN_HEADER_CONTENT_ENCODING, it.toString())
                }
                it.metaData[ObjectMetadata.CACHE_CONTROL]?.let {
                    put(TransferTable.COLUMN_HEADER_CACHE_CONTROL, it.toString())
                }
                it.metaData[ObjectMetadata.CONTENT_MD5]?.let {
                    put(TransferTable.COLUMN_CONTENT_MD5, it.toString())
                }
                it.metaData[ObjectMetadata.CONTENT_DISPOSITION]?.let {
                    put(TransferTable.COLUMN_HEADER_CONTENT_DISPOSITION, it.toString())
                }
                it.metaData[ObjectMetadata.SERVER_SIDE_ENCRYPTION]?.let {
                    put(TransferTable.COLUMN_SSE_ALGORITHM, it.toString())
                }
                it.metaData[ObjectMetadata.SERVER_SIDE_ENCRYPTION_KMS_KEY_ID]?.let {
                    put(TransferTable.COLUMN_SSE_KMS_KEY, it.toString())
                }
                it.expirationTimeRuleId?.let {
                    put(TransferTable.COLUMN_EXPIRATION_TIME_RULE_ID, it)
                }
                it.httpExpiresDate?.let {
                    put(TransferTable.COLUMN_HTTP_EXPIRES_DATE, it.time)
                }
                it.metaData[ObjectMetadata.STORAGE_CLASS]?.let {
                    put(TransferTable.COLUMN_HEADER_STORAGE_CLASS, it.toString())
                }
            }
        }
        return values
    }
}
