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
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.io.writeChannel
import aws.smithy.kotlin.runtime.util.InternalApi
import com.amplifyframework.storage.s3.transfer.DownloadProgressListener
import com.amplifyframework.storage.s3.transfer.TransferDB
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

    private lateinit var downloadProgressListener: DownloadProgressListener
    private val bufferSize = 16 * 1000

    @OptIn(InternalApi::class)
    override suspend fun performWork(): Result {
        downloadProgressListener = DownloadProgressListener(transferRecord, transferStatusUpdater)
        val file = File(transferRecord.file)
        val downloadedBytes = file.length()
        val getObjectRequest = GetObjectRequest {
            key = transferRecord.key
            bucket = transferRecord.bucketName
            range = downloadedBytes.toString()
        }
        return s3.getObject(getObjectRequest) { response ->
            val totalBytes = response.contentLength
            transferRecord.bytesTotal = totalBytes
            transferRecord.bytesCurrent = downloadedBytes
            file.parentFile?.takeIf { !it.exists() }?.mkdirs()
            val byteArray = ByteArray(bufferSize)
            val byteStream = response.body as ByteStream.OneShotStream
            var bytesRead: Int
            file.writeChannel().use { channel ->
                while (byteStream.readFrom().readAvailable(byteArray)
                    .also { bytesRead = it } != -1
                ) {
                    channel.writeAvailable(byteArray)
                    downloadProgressListener.progressChanged(bytesRead.toLong())
                }
            }
            transferStatusUpdater.updateProgress(
                transferRecord.id,
                totalBytes,
                totalBytes,
                true
            )
            Result.success(outputData)
        }
    }
}
