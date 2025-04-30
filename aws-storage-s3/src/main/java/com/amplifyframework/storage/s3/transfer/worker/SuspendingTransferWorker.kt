/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.amplifyframework.storage.s3.R
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferRecord
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater
import com.amplifyframework.storage.s3.transfer.worker.BaseTransferWorker.Companion.OUTPUT_TRANSFER_RECORD_ID
import com.amplifyframework.storage.s3.transfer.worker.BaseTransferWorker.Companion.PART_RECORD_ID
import com.amplifyframework.storage.s3.transfer.worker.BaseTransferWorker.Companion.RUN_AS_FOREGROUND_TASK
import com.amplifyframework.storage.s3.transfer.worker.BaseTransferWorker.Companion.TRANSFER_RECORD_ID
import java.lang.Exception
import java.net.SocketException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/**
 * Base worker to perform transfer file task.
 */
internal abstract class SuspendingTransferWorker(
    private val transferStatusUpdater: TransferStatusUpdater,
    private val transferDB: TransferDB,
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters), BaseTransferWorker {

    internal lateinit var transferRecord: TransferRecord
    internal lateinit var outputData: Data

    private val logger =
        Amplify.Logging.logger(
            CategoryType.STORAGE,
            AWSS3StoragePlugin.AWS_S3_STORAGE_LOG_NAMESPACE.format(this::class.java.simpleName)
        )

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
                if (currentCoroutineContext().isActive) {
                    logger.error("${this.javaClass.simpleName} failed with exception: ${Log.getStackTraceString(ex)}")
                }
                if (!currentCoroutineContext().isActive && isRetryableError(ex)) {
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

    private fun isRetryableError(e: Throwable?): Boolean = !isNetworkAvailable(applicationContext) ||
        runAttemptCount < maxRetryCount ||
        e is CancellationException ||
        // SocketException is thrown when download is terminated due to network disconnection.
        e is SocketException

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
}
