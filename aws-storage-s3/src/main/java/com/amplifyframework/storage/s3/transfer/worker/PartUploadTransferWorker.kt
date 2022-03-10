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
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.io.SdkByteReadChannel
import aws.smithy.kotlin.runtime.io.readChannel
import aws.smithy.kotlin.runtime.util.InternalApi
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferState
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater
import java.io.File
import kotlin.properties.Delegates

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
    //private var transferProgressListener: TransferProgressListener? = null
    private var transferRecordId by Delegates.notNull<Int>()
    override var maxRetryCount = 3

    override suspend fun performWork(): Result {
        if (isStopped) {
            return Result.retry()
        }
        multiPartUploadId = inputData.keyValueMap[MULTI_PART_UPLOAD_ID] as String
        // TODO("Add progress listener")
        return s3.uploadPart {
            contentLength = transferRecord.bytesTotal
            bucket = transferRecord.bucketName
            key = transferRecord.key
            uploadId = multiPartUploadId
            body = ByteStream.chunk(File(transferRecord.file), transferRecord.fileOffset, transferRecord.bytesTotal)
            partNumber = transferRecord.partNumber
        }.let { response ->
            response.eTag?.let { tag ->
                transferDB.updateETag(transferRecordId, tag)
                transferDB.updateState(transferRecordId, TransferState.PART_COMPLETED)
                updateProgress()
                Result.success(outputData)
            } ?: run {
                throw IllegalStateException("Etag is empty")
            }
        }
    }

    private val shouldRetry = !isStopped && runAttemptCount < maxRetryCount

    @OptIn(InternalApi::class)
    private fun ByteStream.Companion.chunk(
        file: File,
        start: Long,
        length: Long
    ): ByteStream {
        return object : ByteStream.OneShotStream() {
            override fun readFrom(): SdkByteReadChannel {
                return file.readChannel(start, start + length - 1)
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

    //TODO("Progress listener support is missing in kotlin sdk")
    /*inner class TransferProgressListener(private val transferRecord: TransferRecord) :
        ProgressListener {

        private var resetProgress = false

        override fun progressChanged(progressEvent: ProgressEvent) {
            if (!resetProgress) {
                transferRecord.bytesCurrent += progressEvent.bytesTransferred
                *//*transferStatusUpdater.getMultiPartTransferListener(transferRecord.mainUploadId)
                    ?.progressChanged(progressEvent)*//*
            }
        }

        fun resetProgress() {
            resetProgress = true
            *//*transferStatusUpdater.getMultiPartTransferListener(transferRecord.mainUploadId)
                ?.progressChanged(
                    ProgressEvent(
                        ProgressEvent.RESET_EVENT_CODE,
                        transferRecord.bytesCurrent
                    )
                )*//*
        }
    }*/
}
