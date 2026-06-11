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
import com.amplifyframework.storage.ProgressStallTimeoutException
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.s3.transfer.PartUploadProgressListener
import com.amplifyframework.storage.s3.transfer.ProgressListener
import com.amplifyframework.storage.s3.transfer.StallDetectingProgressListener
import com.amplifyframework.storage.s3.transfer.StorageTransferClientProvider
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater
import com.amplifyframework.storage.s3.transfer.UploadProgressListenerInterceptor
import com.amplifyframework.storage.s3.transfer.worker.BaseTransferWorker.Companion.MULTI_PART_UPLOAD_ID
import com.amplifyframework.storage.s3.transfer.worker.BaseTransferWorker.Companion.PROGRESS_STALL_TIMEOUT_SECONDS
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking

/**
 * Worker to upload a part for multipart upload
 **/
internal class PartUploadTransferWorker(
    private val clientProvider: StorageTransferClientProvider,
    private val transferDB: TransferDB,
    private val transferStatusUpdater: TransferStatusUpdater,
    context: Context,
    workerParameters: WorkerParameters
) : BlockingTransferWorker(transferStatusUpdater, transferDB, context, workerParameters) {

    private lateinit var multiPartUploadId: String
    private lateinit var partUploadProgressListener: PartUploadProgressListener
    override var maxRetryCount = 3

    override fun performWork(): Result {
        transferStatusUpdater.updateTransferState(transferRecord.mainUploadId, TransferState.IN_PROGRESS)
        multiPartUploadId = inputData.keyValueMap[MULTI_PART_UPLOAD_ID] as String
        partUploadProgressListener = PartUploadProgressListener(transferRecord, transferStatusUpdater)
        val stallTimeoutSeconds = (inputData.keyValueMap[PROGRESS_STALL_TIMEOUT_SECONDS] as? Long) ?: 0L
        val stallDetected = AtomicBoolean(false)
        val s3: S3Client = clientProvider.getStorageTransferClient(transferRecord.region, transferRecord.bucketName)

        val uploadPartResponse = try {
            runBlocking {
                val uploadJob = coroutineContext[Job]
                val stallDecorator: StallDetectingProgressListener? = if (stallTimeoutSeconds > 0L) {
                    StallDetectingProgressListener(
                        delegate = partUploadProgressListener,
                        stallTimeoutSeconds = stallTimeoutSeconds,
                        onStall = {
                            if (stallDetected.compareAndSet(false, true)) {
                                uploadJob?.cancel(CancellationException("Progress stall timeout"))
                            }
                        }
                    )
                } else {
                    null
                }
                val effectiveListener: ProgressListener = stallDecorator ?: partUploadProgressListener
                try {
                    stallDecorator?.start()
                    s3.withConfig {
                        interceptors += UploadProgressListenerInterceptor(effectiveListener)
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
                    }
                } finally {
                    stallDecorator?.close()
                }
            }
        } catch (cancellation: CancellationException) {
            if (stallDetected.get()) {
                throw ProgressStallTimeoutException(
                    "Upload cancelled due to progress stall timeout.",
                    "Increase the configured progress stall timeout or verify the network conditions, " +
                        "then retry the upload."
                )
            }
            throw cancellation
        }

        return uploadPartResponse.eTag?.let { tag ->
            transferDB.updateETag(transferRecord.id, tag)
            transferDB.updateState(transferRecord.id, TransferState.PART_COMPLETED)
            updateProgress()
            Result.success(outputData)
        } ?: run {
            throw IllegalStateException("Etag is empty")
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
