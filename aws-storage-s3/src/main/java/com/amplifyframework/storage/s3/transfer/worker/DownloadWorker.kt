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
    private val defaultBufferSize = 16 * 1024

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
            val bufferSize = takeIf { response.contentLength < defaultBufferSize }?.let {
                response.contentLength
            } ?: defaultBufferSize
            val byteArray = ByteArray(bufferSize.toInt())
            writeToFileWithProgressUpdates(response.body as ByteStream.OneShotStream, file, downloadProgressListener)
            transferStatusUpdater.updateProgress(
                transferRecord.id,
                totalBytes,
                totalBytes,
                true
            )
            Result.success(outputData)
        }
    }

    @OptIn(InternalApi::class)
    private suspend fun writeToFileWithProgressUpdates(
        stream: ByteStream.OneShotStream,
        file: File,
        progressListener: DownloadProgressListener
    ) {
        val limit = stream.contentLength ?: 0L
        val buffer = ByteArray(defaultBufferSize)
        val sdkByteReadChannel = stream.readFrom()
        file.writeChannel().use { destination ->
            val flushDst = !destination.autoFlush
            val copied = 0L
            while (true) {
                val remaining = limit - copied
                if (remaining == 0L) break
                val readBytes =
                    sdkByteReadChannel.readAvailable(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (readBytes == -1) break
                if (readBytes > 0) {
                    progressListener.progressChanged(readBytes.toLong())
                }
                destination.writeFully(buffer, 0, readBytes)
                if (flushDst && stream.readFrom().availableForRead == 0) {
                    destination.flush()
                }
            }
        }
    }
}
