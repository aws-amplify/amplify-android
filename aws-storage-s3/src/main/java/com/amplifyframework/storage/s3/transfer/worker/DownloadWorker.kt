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

import android.content.Context
import androidx.work.WorkerParameters
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.smithy.kotlin.runtime.content.writeToFile
import com.amazonaws.event.ProgressEvent
import com.amazonaws.event.ProgressListener
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferRecord
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater
import java.io.File

/**
 * Worker to perform download file task.
 */
internal class DownloadWorker(
    private val s3: S3Client,
    private val transferDB: TransferDB,
    private val transferStatusUpdater: TransferStatusUpdater,
    context: Context,
    workerParameters: WorkerParameters
) : BaseTransferWorker(transferStatusUpdater, transferDB, context, workerParameters) {

    private lateinit var transferProgressListener: DownloadProgressListener

    override suspend fun performWork(): Result {
        transferProgressListener = DownloadProgressListener(transferRecord)
        val file = File(transferRecord.file)
        val downloadedBytes = file.length()
        val getObjectRequest = GetObjectRequest {
            key = transferRecord.key
            bucket = transferRecord.bucketName
            range = downloadedBytes.toString()
        }
        // TODO("Append userAgent")
        // TODO("Attach progress listener")
        return s3.getObject(getObjectRequest) { response ->
            val totalBytes = response.contentLength
            transferRecord.bytesTotal = totalBytes
            transferRecord.bytesCurrent = downloadedBytes
            file.parentFile?.takeIf { !it.exists() }?.mkdirs()
            response.body?.writeToFile(file)
            transferStatusUpdater.updateProgress(
                transferRecord.id,
                downloadedBytes,
                totalBytes,
                true
            )
            Result.success(outputData)
        }
    }

    inner class DownloadProgressListener(private val transferRecord: TransferRecord) :
        ProgressListener {
        private var bytesTransferredSoFar = 0L
        private var resetProgress = false

        init {
            bytesTransferredSoFar = transferRecord.bytesCurrent
        }

        @Synchronized
        override fun progressChanged(progressEvent: ProgressEvent) {
            progressEvent.eventCode.takeIf { it == ProgressEvent.RESET_EVENT_CODE }?.let {
                bytesTransferredSoFar = 0L
                resetProgress = true
            } ?: run {
                bytesTransferredSoFar += progressEvent.bytesTransferred
                transferRecord.bytesCurrent = bytesTransferredSoFar
                updateProgress()
            }
        }

        private fun updateProgress() {
            if (!resetProgress) {
                transferStatusUpdater.updateProgress(
                    transferRecord.id,
                    transferRecord.bytesCurrent,
                    transferRecord.bytesTotal,
                    true
                )
            }
        }
    }
}
