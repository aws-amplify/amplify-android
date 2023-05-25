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
import androidx.work.Data
import androidx.work.WorkerParameters
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.createMultipartUpload
import aws.sdk.kotlin.services.s3.withConfig
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater

/**
 * Worker to initiate multipart upload
 **/
internal class InitiateMultiPartUploadTransferWorker(
    private val s3: S3Client,
    private val transferDB: TransferDB,
    private val transferStatusUpdater: TransferStatusUpdater,
    context: Context,
    workerParameters: WorkerParameters
) : BaseTransferWorker(transferStatusUpdater, transferDB, context, workerParameters) {

    override suspend fun performWork(): Result {
        transferStatusUpdater.updateTransferState(transferRecord.id, TransferState.IN_PROGRESS)
        val putObjectRequest = createPutObjectRequest(transferRecord, null)
        return s3.withConfig {
            enableAccelerate = transferRecord.useAccelerateEndpoint == 1
        }.createMultipartUpload {
            bucket = putObjectRequest.bucket
            key = putObjectRequest.key
            acl = putObjectRequest.acl
            metadata = putObjectRequest.metadata
            tagging = putObjectRequest.tagging
        }.let {
            transferStatusUpdater.updateMultipartId(transferRecord.id, it.uploadId)
            val output = Data.Builder().putInt(TRANSFER_RECORD_ID, transferRecord.id)
                .putString(MULTI_PART_UPLOAD_ID, it.uploadId).build()
            Result.success(output)
        }
    }
}
