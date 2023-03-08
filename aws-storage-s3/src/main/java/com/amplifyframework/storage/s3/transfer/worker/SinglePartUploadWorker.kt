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
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater
import com.amplifyframework.storage.s3.transfer.UploadProgressListener
import com.amplifyframework.storage.s3.transfer.UploadProgressListenerInterceptor

internal class SinglePartUploadWorker(
    private val s3: S3Client,
    private val transferDB: TransferDB,
    private val transferStatusUpdater: TransferStatusUpdater,
    context: Context,
    workerParameters: WorkerParameters
) : BaseTransferWorker(transferStatusUpdater, transferDB, context, workerParameters) {

    private lateinit var uploadProgressListener: UploadProgressListener

    override suspend fun performWork(): Result {
        uploadProgressListener = UploadProgressListener(transferRecord, transferStatusUpdater)
        val putObjectRequest = createPutObjectRequest(transferRecord, uploadProgressListener)
        return s3.withConfig {
            interceptors += UploadProgressListenerInterceptor(uploadProgressListener)
            enableAccelerate = transferRecord.useAccelerateEndpoint == 1
        }.putObject(putObjectRequest).let {
            Result.success(outputData)
        }
    }
}
