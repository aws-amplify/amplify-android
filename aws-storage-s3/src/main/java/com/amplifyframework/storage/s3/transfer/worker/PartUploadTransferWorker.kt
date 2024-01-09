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
import aws.sdk.kotlin.services.s3.uploadPart
import aws.sdk.kotlin.services.s3.withConfig
import aws.smithy.kotlin.runtime.content.asByteStream
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.s3.transfer.PartUploadProgressListener
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater
import com.amplifyframework.storage.s3.transfer.UploadProgressListenerInterceptor
import java.io.File
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/**
 * Worker to upload a part for multipart upload
 **/
internal class PartUploadTransferWorker(
    private val s3: S3Client,
    private val transferDB: TransferDB,
    private val transferStatusUpdater: TransferStatusUpdater,
    context: Context,
    workerParameters: WorkerParameters
) : BaseTransferWorker(transferStatusUpdater, transferDB, context, workerParameters) {

    private lateinit var multiPartUploadId: String
    private lateinit var partUploadProgressListener: PartUploadProgressListener
    override var maxRetryCount = 3

    override suspend fun performWork(): Result {
        if (!currentCoroutineContext().isActive) {
            return Result.retry()
        }
        transferStatusUpdater.updateTransferState(transferRecord.mainUploadId, TransferState.IN_PROGRESS)
        multiPartUploadId = inputData.keyValueMap[MULTI_PART_UPLOAD_ID] as String
        partUploadProgressListener = PartUploadProgressListener(transferRecord, transferStatusUpdater)
        return s3.withConfig {
            interceptors += UploadProgressListenerInterceptor(partUploadProgressListener)
            enableAccelerate = transferRecord.useAccelerateEndpoint == 1
        }.uploadPart {
            bucket = transferRecord.bucketName
            key = transferRecord.key
            uploadId = multiPartUploadId
            body = File(transferRecord.file).asByteStream(
                start = transferRecord.fileOffset,
                transferRecord.fileOffset + transferRecord.bytesTotal - 1
            )
            partNumber = transferRecord.partNumber
        }.let { response ->
            response.eTag?.let { tag ->
                transferDB.updateETag(transferRecord.id, tag)
                transferDB.updateState(transferRecord.id, TransferState.PART_COMPLETED)
                updateProgress()
                Result.success(outputData)
            } ?: run {
                throw IllegalStateException("Etag is empty")
            }
        }
    }

    private fun updateProgress() {
        transferStatusUpdater.updateProgress(
            transferRecord.id,
            transferRecord.bytesTotal,
            transferRecord.bytesTotal,
            false
        )
    }
}
