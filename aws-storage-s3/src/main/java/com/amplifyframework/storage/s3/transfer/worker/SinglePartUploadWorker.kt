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

/**
 * Worker to perform single part upload file task.
 */
package com.amplifyframework.storage.s3.transfer.worker

import android.content.Context
import androidx.work.WorkerParameters
import aws.sdk.kotlin.services.s3.S3Client
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater

internal class SinglePartUploadWorker(
    private val s3: S3Client,
    private val transferDB: TransferDB,
    private val transferStatusUpdater: TransferStatusUpdater,
    context: Context,
    workerParameters: WorkerParameters
) : BaseTransferWorker(transferStatusUpdater, transferDB, context, workerParameters) {

    // private lateinit var transferProgressListener: TransferProgressListener

    override suspend fun performWork(): Result {
        val putObjectRequest = createPutObjectRequest(transferRecord)
        s3.putObject(putObjectRequest).let {
            return Result.success(outputData)
        }
    }

    /*inner class TransferProgressListener(private val transferRecord: TransferRecord) :
        ProgressListener {
        private var bytesTransferredSoFar = 0L
        private var resetProgress = false

        @Synchronized
        override fun progressChanged(progressEvent: ProgressEvent) {
            progressEvent.eventCode.takeIf { it == ProgressEvent.RESET_EVENT_CODE }?.let {
                bytesTransferredSoFar = 0L
                transferRecord.bytesCurrent = bytesTransferredSoFar
                updateProgress(false)
                resetProgress = true
            } ?: run {
                bytesTransferredSoFar += progressEvent.bytesTransferred
                transferRecord.bytesCurrent = bytesTransferredSoFar
                updateProgress(true)
            }
        }

        private fun updateProgress(notifyListener: Boolean) {
            if (!resetProgress) {
                transferStatusUpdater.updateProgress(
                    transferRecord.id,
                    transferRecord.bytesCurrent,
                    transferRecord.bytesTotal,
                    notifyListener
                )
            }
        }
    }*/
}
