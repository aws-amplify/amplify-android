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
import aws.sdk.kotlin.services.s3.withConfig
import com.amplifyframework.storage.ProgressStallTimeoutException
import com.amplifyframework.storage.s3.transfer.ProgressListener
import com.amplifyframework.storage.s3.transfer.StallDetectingProgressListener
import com.amplifyframework.storage.s3.transfer.StorageTransferClientProvider
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater
import com.amplifyframework.storage.s3.transfer.UploadProgressListener
import com.amplifyframework.storage.s3.transfer.UploadProgressListenerInterceptor
import com.amplifyframework.storage.s3.transfer.worker.BaseTransferWorker.Companion.PROGRESS_STALL_TIMEOUT_SECONDS
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope

internal class SinglePartUploadWorker(
    private val clientProvider: StorageTransferClientProvider,
    private val transferDB: TransferDB,
    private val transferStatusUpdater: TransferStatusUpdater,
    context: Context,
    workerParameters: WorkerParameters
) : SuspendingTransferWorker(transferStatusUpdater, transferDB, context, workerParameters) {

    private lateinit var uploadProgressListener: UploadProgressListener

    override suspend fun performWork(): Result {
        uploadProgressListener = UploadProgressListener(transferRecord, transferStatusUpdater)
        val stallTimeoutSeconds = (inputData.keyValueMap[PROGRESS_STALL_TIMEOUT_SECONDS] as? Long) ?: 0L
        val stallDetected = AtomicBoolean(false)

        return try {
            coroutineScope {
                val uploadJob = coroutineContext[Job]
                val stallDecorator: StallDetectingProgressListener? = if (stallTimeoutSeconds > 0L) {
                    StallDetectingProgressListener(
                        delegate = uploadProgressListener,
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
                val effectiveListener: ProgressListener = stallDecorator ?: uploadProgressListener
                val putObjectRequest = createPutObjectRequest(transferRecord, effectiveListener)
                val s3: S3Client = clientProvider.getStorageTransferClient(
                    transferRecord.region,
                    transferRecord.bucketName
                )
                try {
                    stallDecorator?.start()
                    s3.withConfig {
                        interceptors += UploadProgressListenerInterceptor(effectiveListener)
                        enableAccelerate = transferRecord.useAccelerateEndpoint == 1
                    }.putObject(putObjectRequest)
                } finally {
                    stallDecorator?.close()
                }
                Result.success(outputData)
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
    }
}
