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
import aws.sdk.kotlin.services.s3.abortMultipartUpload
import aws.sdk.kotlin.services.s3.withConfig
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater

/**
 * Worker to abort pending multipart upload
 **/
internal class AbortMultiPartUploadWorker(
    private val s3: S3Client,
    private val transferDB: TransferDB,
    private val transferStatusUpdater: TransferStatusUpdater,
    context: Context,
    workerParameters: WorkerParameters
) : BaseTransferWorker(transferStatusUpdater, transferDB, context, workerParameters) {

    override suspend fun performWork(): Result {
        return s3.withConfig {
            enableAccelerate = transferRecord.useAccelerateEndpoint == 1
        }.abortMultipartUpload {
            bucket = transferRecord.bucketName
            key = transferRecord.key
            uploadId = transferRecord.multipartId
        }.let {
            val currentStatus = transferRecord.state
            if (currentStatus == TransferState.PENDING_CANCEL) {
                transferStatusUpdater.updateTransferState(transferRecord.id, TransferState.CANCELED)
            } else {
                transferStatusUpdater.updateTransferState(transferRecord.id, TransferState.FAILED)
            }
            Result.success(outputData)
        }
    }
}
