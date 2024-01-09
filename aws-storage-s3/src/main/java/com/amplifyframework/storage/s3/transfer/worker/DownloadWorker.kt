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
import aws.sdk.kotlin.services.s3.withConfig
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.writeToFile
import aws.smithy.kotlin.runtime.io.SdkSource
import aws.smithy.kotlin.runtime.io.buffer
import com.amplifyframework.storage.s3.transfer.DownloadProgressListener
import com.amplifyframework.storage.s3.transfer.DownloadProgressListenerInterceptor
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
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
    private val defaultBufferSize = 8192L
    override suspend fun performWork(): Result {
        s3.withConfig {
            enableAccelerate = transferRecord.useAccelerateEndpoint == 1
        }
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

        downloadProgressListener = DownloadProgressListener(transferRecord, transferStatusUpdater)
        return s3.withConfig {
            interceptors += DownloadProgressListenerInterceptor(downloadProgressListener)
            enableAccelerate = transferRecord.useAccelerateEndpoint == 1
        }.getObject(getObjectRequest) { response ->
            val totalBytes = (response.body?.contentLength ?: 0L) + downloadedBytes
            transferRecord.bytesTotal = totalBytes
            transferRecord.bytesCurrent = downloadedBytes
            file.parentFile?.takeIf { !it.exists() }?.mkdirs()
            response.body?.let {
                writeStreamToFile(it, file)
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

    private suspend fun writeStreamToFile(
        stream: ByteStream,
        file: File
    ) {
        withContext(Dispatchers.IO) {
            when (stream) {
                is ByteStream.ChannelStream, is ByteStream.Buffer -> {
                    stream.writeToFile(file)
                }
                is ByteStream.SourceStream -> {
                    val sourceStream: SdkSource = stream.readFrom()
                    val limit = stream.contentLength ?: 0L
                    val buffer = ByteArray(defaultBufferSize.toInt())
                    val append = file.length() > 0
                    val fileOutputStream = FileOutputStream(file, append)
                    var totalRead = 0L
                    BufferedOutputStream(fileOutputStream).use { fileOutput ->
                        val copied = 0L
                        while (currentCoroutineContext().isActive) {
                            val remaining = limit - copied
                            if (remaining == 0L) break
                            val readBytes =
                                sourceStream.buffer().read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                            if (readBytes == -1) break
                            if (readBytes > 0) {
                                totalRead += readBytes
                            }
                            fileOutput.write(buffer, 0, readBytes)
                        }
                        if (sourceStream.buffer().exhausted()) {
                            fileOutput.flush()
                        }
                    }
                }
            }
        }
    }
}
