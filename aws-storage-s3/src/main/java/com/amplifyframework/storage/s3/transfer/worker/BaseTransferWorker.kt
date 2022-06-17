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

package com.amplifyframework.storage.s3.transfer.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.RequestPayer
import aws.sdk.kotlin.services.s3.model.ServerSideEncryption
import aws.sdk.kotlin.services.s3.model.StorageClass
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.io.SdkByteReadChannel
import aws.smithy.kotlin.runtime.io.readChannel
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.InternalApi
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.ObjectMetadata
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.amplifyframework.storage.s3.R
import com.amplifyframework.storage.s3.transfer.ProgressListener
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferRecord
import com.amplifyframework.storage.s3.transfer.TransferState
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater
import java.io.File
import java.lang.Exception
import java.nio.ByteBuffer

/**
 * Base worker to perform transfer file task.
 */
internal abstract class BaseTransferWorker(
    private val transferStatusUpdater: TransferStatusUpdater,
    private val transferDB: TransferDB,
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    internal lateinit var transferRecord: TransferRecord
    internal lateinit var outputData: Data
    internal val logger =
        Amplify.Logging.forNamespace(
            AWSS3StoragePlugin.AWS_S3_STORAGE_LOG_NAMESPACE.format(this::class.java.simpleName)
        )

    companion object {
        internal const val PART_RECORD_ID = "PART_RECORD_ID"
        internal const val WORKER_ID = "WORKER_ID"
        private const val OBJECT_TAGS_DELIMITER = "&"
        private const val OBJECT_TAG_KEY_VALUE_SEPARATOR = "="
        private const val REQUESTER_PAYS = "requester"
        private val CANNED_ACL_MAP =
            ObjectCannedAcl.values().map { it.value to it }.toMap()
        internal const val MULTI_PART_UPLOAD_ID = "multipartUploadId"
        internal const val TRANSFER_RECORD_ID = "TRANSFER_RECORD_ID"
        internal const val OUTPUT_TRANSFER_RECORD_ID = "OUTPUT_TRANSFER_RECORD_ID"
        internal const val completionRequestTag: String = "COMPLETION_REQUEST_TAG_%s"
        internal const val initiationRequestTag: String = "INITIATION_REQUEST_TAG_%s"
        internal const val MULTIPART_UPLOAD: String = "MULTIPART_UPLOAD"
    }

    override suspend fun doWork(): Result {
        setForegroundAsync(getForegroundInfo())
        val result = runCatching {
            val transferRecordId =
                inputData.keyValueMap[PART_RECORD_ID] as? Int ?: inputData.keyValueMap[TRANSFER_RECORD_ID] as Int
            outputData = workDataOf(OUTPUT_TRANSFER_RECORD_ID to transferRecordId)
            transferDB.getTransferRecordById(transferRecordId)?.let { tr ->
                transferRecord = tr
                performWork()
            } ?: return run {
                Result.failure(outputData)
            }
        }

        return when {
            result.isSuccess -> {
                result.getOrThrow()
            }
            else -> {
                val ex = result.exceptionOrNull()
                logger.error("TransferWorker failed with exception: $ex")
                if (isRetryableError()) {
                    Result.retry()
                } else {
                    transferStatusUpdater.updateOnError(transferRecord.id, Exception(ex))
                    transferStatusUpdater.updateTransferState(
                        transferRecord.id,
                        TransferState.FAILED
                    )
                    Result.failure(outputData)
                }
            }
        }
    }

    abstract suspend fun performWork(): Result

    internal open var maxRetryCount = 0

    override suspend fun getForegroundInfo(): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
        val appIcon = takeIf { applicationContext.applicationInfo.icon > 0 }?.let {
            applicationContext.applicationInfo.icon
        } ?: R.drawable.ic_notification_test
        return ForegroundInfo(
            1,
            NotificationCompat.Builder(
                applicationContext,
                applicationContext.getString(R.string.amplify_storage_notification_channel_id)
            )
                .setSmallIcon(appIcon)
                .setContentTitle(applicationContext.getString(R.string.amplify_storage_notification_title))
                .build()
        )
    }

    private fun isRetryableError(): Boolean {
        if (isStopped || !isNetworkAvailable(applicationContext) || runAttemptCount < maxRetryCount) {
            return true
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                applicationContext.getString(R.string.amplify_storage_notification_channel_id),
                applicationContext.getString(R.string.amplify_storage_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return true
                    }
                }
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                return true
            }
        }
        return false
    }

    internal suspend fun createPutObjectRequest(
        transferRecord: TransferRecord,
        progressListener: ProgressListener?
    ): PutObjectRequest {
        val file = File(transferRecord.file)
        return PutObjectRequest {
            bucket = transferRecord.bucketName
            key = transferRecord.key
            body = ByteStream.readWithProgressUpdates(file, progressListener = progressListener)
            cacheControl = transferRecord.headerCacheControl
            contentDisposition = transferRecord.headerContentDisposition
            serverSideEncryption = transferRecord.sseAlgorithm?.let {
                ServerSideEncryption.fromValue(it)
            }
            sseCustomerKey = transferRecord.sseKMSKey
            contentEncoding = transferRecord.headerContentEncoding
            contentType = transferRecord.headerContentType
            expires = transferRecord.httpExpires?.let { Instant.fromEpochSeconds(it) }
            metadata = transferRecord.userMetadata
            contentMd5 = transferRecord.md5
            storageClass = transferRecord.headerStorageClass?.let { StorageClass.fromValue(it) }
            websiteRedirectLocation = transferRecord.userMetadata?.get(
                ObjectMetadata.REDIRECT_LOCATION
            )
            acl = transferRecord.cannedAcl?.let { CANNED_ACL_MAP[it] }
            requestPayer = transferRecord.userMetadata?.get(ObjectMetadata.REQUESTER_PAYS_HEADER)
                ?.let { RequestPayer.fromValue(it) }
            tagging = transferRecord.userMetadata?.get(ObjectMetadata.S3_TAGGING)
        }
    }

    @OptIn(InternalApi::class)
    fun ByteStream.Companion.readWithProgressUpdates(
        file: File,
        start: Long = 0,
        length: Long = file.length(),
        progressListener: ProgressListener?
    ): ByteStream {
        return object : ByteStream.OneShotStream() {
            override val contentLength: Long = length
            override fun readFrom(): SdkByteReadChannel {
                val oneShotStream = file.readChannel(start, start + length - 1)
                return object : SdkByteReadChannel {
                    override val availableForRead: Int
                        get() = oneShotStream.availableForRead
                    override val isClosedForRead: Boolean
                        get() = oneShotStream.isClosedForRead
                    override val isClosedForWrite: Boolean
                        get() = oneShotStream.isClosedForWrite

                    override suspend fun awaitContent() {
                        oneShotStream.awaitContent()
                    }

                    override fun cancel(cause: Throwable?): Boolean {
                        return oneShotStream.cancel(cause)
                    }

                    override suspend fun readAvailable(sink: ByteBuffer): Int {
                        return oneShotStream.readAvailable(sink).also {
                            if (it > 0) {
                                progressListener?.progressChanged(it.toLong())
                            }
                        }
                    }

                    override suspend fun readAvailable(sink: ByteArray, offset: Int, length: Int): Int {
                        return oneShotStream.readAvailable(sink, offset, length).also {
                            if (it > 0) {
                                progressListener?.progressChanged(it.toLong())
                            }
                        }
                    }

                    override suspend fun readFully(sink: ByteArray, offset: Int, length: Int) {
                        return oneShotStream.readFully(sink, offset, length).also {
                            if (sink.isNotEmpty()) {
                                progressListener?.progressChanged(sink.size.toLong())
                            }
                        }
                    }

                    override suspend fun readRemaining(limit: Int): ByteArray {
                        return readRemaining(limit).also {
                            if (it.isNotEmpty()) {
                                progressListener?.progressChanged(it.size.toLong())
                            }
                        }
                    }
                }
            }
        }
    }
}
