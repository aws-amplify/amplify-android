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
import com.amplifyframework.storage.ObjectMetadata
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.amplifyframework.storage.s3.transfer.worker.RouterWorker
import com.amplifyframework.storage.s3.transfer.worker.TransferWorkerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.IllegalArgumentException
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
    private val transferStatusUpdater: TransferStatusUpdater =
        TransferStatusUpdater.getInstance(context)
    private val logger =
        Amplify.Logging.forNamespace(AWSS3StoragePlugin.AWS_S3_STORAGE_LOG_NAMESPACE.format(this::class.java.simpleName))
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
        bucket: String,
        key: String,
        file: File,
        metadata: ObjectMetadata,
        cannedAcl: ObjectCannedAcl? = null,
        listener: TransferListener? = null
    ): TransferObserver {
        val transferRecordId = if (shouldUploadInMultipart(file)) {
            createMultipartUploadRecords(bucket, key, file, metadata, cannedAcl)
        } else {
            val uri = transferDB.insertSingleTransferRecord(
                TransferType.UPLOAD,
                bucket,
                key,
                file,
                cannedAcl,
                metadata
            )
            uri.lastPathSegment?.toInt()
                ?: throw IllegalStateException("Invalid TransferRecord ID ${uri.lastPathSegment}")
        }
        val transferRecord = transferDB.getTransferRecordById(transferRecordId)
            ?: throw IllegalStateException("Failed to find transferRecord")
        val transferObserver = transferRecord.start(
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
        key: String,
        inputStream: InputStream,
        options: UploadOptions
    ): TransferObserver {
        val file = writeInputStreamToFile(inputStream)
        return upload(
            options.bucket,
            key,
            file,
            options.objectMetadata,
            options.cannedAcl,
            options.transferListener
        )
    }

    @JvmOverloads
    fun download(
        bucket: String,
        key: String,
        file: File,
        listener: TransferListener? = null
    ): TransferObserver {
        if (file.isDirectory) {
            throw IllegalArgumentException("Invalid file: $file")
        }
        val uri = transferDB.insertSingleTransferRecord(TransferType.DOWNLOAD, bucket, key, file)
        val transferRecordId: Int = uri.lastPathSegment?.toInt()
            ?: throw IllegalStateException("Invalid TransferRecord ID ${uri.lastPathSegment}")
        if (file.isFile) {
            logger.warn("Overwriting existing file: $file")
            file.delete()
        }
        val transferRecord = transferDB.getTransferRecordById(transferRecordId)
            ?: throw IllegalStateException("Failed to find transferRecord")
        val transferObserver = transferRecord.start(
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
        return transferRecord?.pause(transferStatusUpdater, workManager) ?: false
    }

    fun resume(transferRecordId: Int): Boolean {
        val transferRecord = transferStatusUpdater.activeTransferMap[transferRecordId]
        return transferRecord?.resume(
            pluginKey,
            transferStatusUpdater,
            workManager,
            transferWorkerObserver,
            transferDB
        ) ?: false
    }

    fun cancel(transferRecordId: Int): Boolean {
        val transferRecord = transferStatusUpdater.activeTransferMap[transferRecordId]
        return transferRecord?.cancel(
            pluginKey,
            transferStatusUpdater,
            workManager
        ) ?: false
    }

    private fun createMultipartUploadRecords(
        bucket: String,
        key: String,
        file: File,
        metadata: ObjectMetadata,
        cannedAcl: ObjectCannedAcl?,
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
            bucket,
            key,
            file,
            fileOffset,
            0,
            null,
            file.length(),
            0,
            metadata,
            cannedAcl
        )
        repeat(partCount) {
            val bytesForPart = min(optimalPartSize, remainingLength)
            contentValues[partNum] = transferDB.generateContentValuesForMultiPartUpload(
                bucket,
                key,
                file,
                fileOffset,
                partNum,
                "",
                bytesForPart,
                if (remainingLength - optimalPartSize <= 0) 1 else 0,
                metadata,
                cannedAcl
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
        // TODO("Mulitpart upload is not yet working in kotlin sdk, issue:https://github.com/awslabs/aws-sdk-kotlin/issues/536")
        return false
        // return file.length() > TransferRecord.MINIMUM_UPLOAD_PART_SIZE
    }
}
