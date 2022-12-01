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
import aws.smithy.kotlin.runtime.util.InternalApi
import com.amplifyframework.storage.s3.transfer.DownloadProgressListener
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    private val defaultBufferSize = 4096

    @OptIn(InternalApi::class)
    override suspend fun performWork(): Result {
        val file = File(transferRecord.file)
        val downloadedBytes = file.length()
        if (downloadedBytes > 0 && transferRecord.bytesTotal == downloadedBytes) {
            return Result.success(outputData)
        }
        val getObjectRequest = GetObjectRequest {
            key = transferRecord.key
            bucket = transferRecord.bucketName
            range = "bytes=$downloadedBytes-"
        }
        return s3.getObject(getObjectRequest) { response ->
            val totalBytes = (response.body?.contentLength ?: 0L) + downloadedBytes
            transferRecord.bytesTotal = totalBytes
            transferRecord.bytesCurrent = downloadedBytes
            file.parentFile?.takeIf { !it.exists() }?.mkdirs()
            downloadProgressListener = DownloadProgressListener(transferRecord, transferStatusUpdater)
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
        withContext(Dispatchers.IO) {
            val sdkByteReadChannel = stream.readFrom()
            val limit = stream.contentLength ?: 0L
            val buffer = ByteArray(defaultBufferSize)
            val append = file.length() > 0
            val fileOutputStream = FileOutputStream(file, append)
            var totalRead = 0L
            BufferedOutputStream(fileOutputStream).use { fileOutput ->
                val copied = 0L
                while (!isStopped) {
                    val remaining = limit - copied
                    if (remaining == 0L) break
                    val readBytes =
                        sdkByteReadChannel.readAvailable(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    if (readBytes == -1) break
                    if (readBytes > 0) {
                        totalRead += readBytes
                        progressListener.progressChanged(readBytes.toLong())
                    }
                    fileOutput.write(buffer, 0, readBytes)
                    if (sdkByteReadChannel.availableForRead == 0) {
                        fileOutput.flush()
                    }
                }
            }
        }
    }
}
