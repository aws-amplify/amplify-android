/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.storage.s3.transfer

import android.content.ContentValues
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.work.WorkManager
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.storage.ObjectMetadata
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.amplifyframework.storage.s3.TransferOperations
import com.amplifyframework.storage.s3.transfer.worker.RouterWorker
import com.amplifyframework.storage.s3.transfer.worker.TransferWorkerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * TransferManager is a high-level class for applications to upload and
 * download files. It inserts upload and download records into the database and
 * enqueue a WorkRequest for WorkManager to service the transfer
 */
internal class TransferManager @JvmOverloads constructor(
    context: Context,
    s3: S3Client,
    private val pluginKey: String,
    private val workManager: WorkManager = WorkManager.getInstance(context)
) {

    private val transferDB: TransferDB = TransferDB.getInstance(context)
    val transferStatusUpdater: TransferStatusUpdater = TransferStatusUpdater.getInstance(context)
    private val logger =
        Amplify.Logging.logger(
            CategoryType.STORAGE,
            AWSS3StoragePlugin.AWS_S3_STORAGE_LOG_NAMESPACE.format(this::class.java.simpleName)
        )
    private val transferWorkerObserver =
        TransferWorkerObserver.getInstance(
            context,
            pluginKey,
            workManager,
            transferStatusUpdater,
            transferDB
        )
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        RouterWorker.workerFactories[pluginKey] = TransferWorkerFactory(
            transferDB,
            s3,
            transferStatusUpdater
        )
    }

    /**
     * Starts uploading the file to the given bucket, using the given key. The file
     * must be a valid file. Directory isn't supported.
     *
     * @param bucket    The name of the bucket to upload the new object to.
     * @param key       The key in the specified bucket by which to store the new
     *                  object.
     * @param file      The file to upload.
     * @param metadata  The S3 metadata to associate with this object
     * @param cannedAcl The canned ACL to associate with this object
     * @param listener  Listener to attach to transfer observer.
     * @return A TransferObserver used to track upload progress and state
     */
    @JvmOverloads
    fun upload(
        transferId: String,
        bucket: String,
        key: String,
        file: File,
        metadata: ObjectMetadata,
        cannedAcl: ObjectCannedAcl? = null,
        listener: TransferListener? = null,
        useAccelerateEndpoint: Boolean = false
    ): TransferObserver {
        val transferRecordId = if (shouldUploadInMultipart(file)) {
            createMultipartUploadRecords(transferId, bucket, key, file, metadata, cannedAcl, useAccelerateEndpoint)
        } else {
            val uri = transferDB.insertSingleTransferRecord(
                transferId,
                TransferType.UPLOAD,
                bucket,
                key,
                file,
                cannedAcl,
                metadata,
                useAccelerateEndpoint = useAccelerateEndpoint
            )
            uri.lastPathSegment?.toInt()
                ?: throw IllegalStateException("Invalid TransferRecord ID ${uri.lastPathSegment}")
        }
        val transferRecord = transferDB.getTransferRecordById(transferRecordId)
            ?: throw IllegalStateException("Failed to find transferRecord")
        val transferObserver = TransferOperations.start(
            transferRecord,
            pluginKey,
            transferStatusUpdater,
            workManager,
            transferWorkerObserver,
            transferDB,
            listener
        )
        mainHandler.post {
            workManager
                .getWorkInfosForUniqueWorkLiveData(transferRecordId.toString())
                .observeForever(transferWorkerObserver)
        }
        return transferObserver
    }

    @Throws(IOException::class)
    fun upload(
        transferId: String,
        key: String,
        inputStream: InputStream,
        options: UploadOptions,
        useAccelerateEndpoint: Boolean
    ): TransferObserver {
        val file = writeInputStreamToFile(inputStream)
        return upload(
            transferId,
            options.bucket,
            key,
            file,
            options.objectMetadata,
            options.cannedAcl,
            options.transferListener,
            useAccelerateEndpoint
        )
    }

    @JvmOverloads
    fun download(
        transferId: String,
        bucket: String,
        key: String,
        file: File,
        listener: TransferListener? = null,
        useAccelerateEndpoint: Boolean = false
    ): TransferObserver {
        if (file.isDirectory) {
            throw IllegalArgumentException("Invalid file: $file")
        }
        val uri = transferDB.insertSingleTransferRecord(
            transferId,
            TransferType.DOWNLOAD,
            bucket,
            key,
            file,
            useAccelerateEndpoint = useAccelerateEndpoint
        )
        val transferRecordId: Int = uri.lastPathSegment?.toInt()
            ?: throw IllegalStateException("Invalid TransferRecord ID ${uri.lastPathSegment}")
        if (file.isFile) {
            logger.warn("Overwriting existing file: $file")
            file.delete()
        }
        val transferRecord = transferDB.getTransferRecordById(transferRecordId)
            ?: throw IllegalStateException("Failed to find transferRecord")
        val transferObserver = TransferOperations.start(
            transferRecord,
            pluginKey,
            transferStatusUpdater,
            workManager,
            transferWorkerObserver,
            transferDB,
            listener
        )
        mainHandler.post {
            workManager
                .getWorkInfosForUniqueWorkLiveData(transferRecordId.toString())
                .observeForever(transferWorkerObserver)
        }
        return transferObserver
    }

    fun pause(transferRecordId: Int): Boolean {
        val transferRecord = transferStatusUpdater.activeTransferMap[transferRecordId]
        return transferRecord?.let { TransferOperations.pause(it, transferStatusUpdater, workManager) } ?: false
    }

    fun resume(transferRecordId: Int): Boolean {
        val transferRecord = transferStatusUpdater.activeTransferMap[transferRecordId]
        return transferRecord?.let {
            TransferOperations.resume(
                it,
                pluginKey,
                transferStatusUpdater,
                workManager,
                transferWorkerObserver,
                transferDB
            )
            mainHandler.post {
                workManager
                    .getWorkInfosForUniqueWorkLiveData(transferRecordId.toString())
                    .observeForever(transferWorkerObserver)
            }
        } ?: false
    }

    fun cancel(transferRecordId: Int): Boolean {
        val transferRecord = transferStatusUpdater.activeTransferMap[transferRecordId]
        return transferRecord?.let {
            TransferOperations.cancel(
                it,
                pluginKey,
                transferStatusUpdater,
                workManager
            )
        } ?: false
    }

    fun getTransferOperationById(
        transferId: String
    ): TransferRecord? {
        return transferDB.getTransferByTransferId(transferId)
    }

    private fun createMultipartUploadRecords(
        transferId: String,
        bucket: String,
        key: String,
        file: File,
        metadata: ObjectMetadata,
        cannedAcl: ObjectCannedAcl?,
        useAccelerateEndpoint: Boolean
    ): Int {
        var remainingLength = file.length()
        val partSize =
            ceil(remainingLength / TransferRecord.MAXIMUM_UPLOAD_PARTS.toDouble()).toInt()
        val optimalPartSize = max(partSize, TransferRecord.MINIMUM_UPLOAD_PART_SIZE).toLong()
        val partCount = ceil(remainingLength.toDouble() / optimalPartSize.toDouble()).toInt()
        var partNum = 1
        var fileOffset = 0L
        val contentValues = arrayOfNulls<ContentValues>(partCount + 1)
        contentValues[0] = transferDB.generateContentValuesForMultiPartUpload(
            transferId,
            bucket,
            key,
            file,
            fileOffset,
            0,
            null,
            file.length(),
            0,
            metadata,
            cannedAcl,
            useAccelerateEndpoint
        )
        repeat(partCount) {
            val bytesForPart = min(optimalPartSize, remainingLength)
            contentValues[partNum] = transferDB.generateContentValuesForMultiPartUpload(
                UUID.randomUUID().toString(),
                bucket,
                key,
                file,
                fileOffset,
                partNum,
                "",
                bytesForPart,
                if (remainingLength - optimalPartSize <= 0) 1 else 0,
                metadata,
                cannedAcl,
                useAccelerateEndpoint
            )
            partNum++
            fileOffset += optimalPartSize
            remainingLength -= optimalPartSize
        }

        return transferDB.bulkInsertTransferRecords(contentValues)
    }

    private fun writeInputStreamToFile(inputStream: InputStream): File {
        val file = File.createTempFile(TransferStatusUpdater.TEMP_FILE_PREFIX, ".tmp")
        FileOutputStream(file).use {
            try {
                inputStream.copyTo(it)
            } catch (exception: IOException) {
                file.delete()
                throw IOException("Error writing the inputStream into a file,  $exception")
            }
        }
        return file
    }

    private fun shouldUploadInMultipart(file: File): Boolean {
        return file.length() > TransferRecord.MINIMUM_UPLOAD_PART_SIZE
    }
}
