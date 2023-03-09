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
import android.util.Log
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
import aws.smithy.kotlin.runtime.content.fromFile
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.ObjectMetadata
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.amplifyframework.storage.s3.R
import com.amplifyframework.storage.s3.transfer.ProgressListener
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferRecord
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater
import java.io.File
import java.lang.Exception
import java.net.SocketException
import kotlinx.coroutines.CancellationException

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
    private val logger =
        Amplify.Logging.forNamespace(
            AWSS3StoragePlugin.AWS_S3_STORAGE_LOG_NAMESPACE.format(this::class.java.simpleName)
        )

    companion object {
        internal const val PART_RECORD_ID = "PART_RECORD_ID"
        internal const val RUN_AS_FOREGROUND_TASK = "RUN_AS_FOREGROUND_TASK"
        internal const val WORKER_ID = "WORKER_ID"
        private const val OBJECT_TAGS_DELIMITER = "&"
        private const val OBJECT_TAG_KEY_VALUE_SEPARATOR = "="
        private const val REQUESTER_PAYS = "requester"
        private val CANNED_ACL_MAP =
            ObjectCannedAcl.values().associateBy { it.value }
        internal const val MULTI_PART_UPLOAD_ID = "multipartUploadId"
        internal const val TRANSFER_RECORD_ID = "TRANSFER_RECORD_ID"
        internal const val OUTPUT_TRANSFER_RECORD_ID = "OUTPUT_TRANSFER_RECORD_ID"
        internal const val completionRequestTag: String = "COMPLETION_REQUEST_TAG_%s"
        internal const val initiationRequestTag: String = "INITIATION_REQUEST_TAG_%s"
        internal const val MULTIPART_UPLOAD: String = "MULTIPART_UPLOAD"
    }

    override suspend fun doWork(): Result {
        // Foreground task is disabled until the foreground notification behavior and the recent customer feedback,
        // it will be enabled in future based on the customer request.
        val isForegroundTask: Boolean = (inputData.keyValueMap[RUN_AS_FOREGROUND_TASK] ?: false) as Boolean
        if (isForegroundTask) {
            setForegroundAsync(getForegroundInfo())
        }
        val result = runCatching {
            val transferRecordId =
                inputData.keyValueMap[PART_RECORD_ID] as? Int ?: inputData.keyValueMap[TRANSFER_RECORD_ID] as Int
            outputData = workDataOf(OUTPUT_TRANSFER_RECORD_ID to inputData.keyValueMap[TRANSFER_RECORD_ID] as Int)
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
                if (!isStopped) {
                    logger.error("${this.javaClass.simpleName} failed with exception: ${Log.getStackTraceString(ex)}")
                }
                if (isRetryableError(ex)) {
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
        val appIcon = R.drawable.amplify_storage_transfer_notification_icon
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

    private fun isRetryableError(e: Throwable?): Boolean {
        return isStopped ||
            !isNetworkAvailable(applicationContext) ||
            runAttemptCount < maxRetryCount ||
            e is CancellationException ||
            // SocketException is thrown when download is terminated due to network disconnection.
            e is SocketException
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
            body = ByteStream.fromFile(file)
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
}
