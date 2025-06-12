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
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.amplifyframework.storage.s3.transfer.StorageTransferClientProvider
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater

/**
 * Worker factory to provide workers for various transfer types
 **/
internal class TransferWorkerFactory(
    private val transferDB: TransferDB,
    private val clientProvider: StorageTransferClientProvider,
    private val transferStatusUpdater: TransferStatusUpdater
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker {
        when (workerClassName) {
            DownloadWorker::class.java.name ->
                return DownloadWorker(
                    clientProvider,
                    transferDB,
                    transferStatusUpdater,
                    appContext,
                    workerParameters
                )
            SinglePartUploadWorker::class.java.name ->
                return SinglePartUploadWorker(
                    clientProvider,
                    transferDB,
                    transferStatusUpdater,
                    appContext,
                    workerParameters
                )
            InitiateMultiPartUploadTransferWorker::class.java.name ->
                return InitiateMultiPartUploadTransferWorker(
                    clientProvider,
                    transferDB,
                    transferStatusUpdater,
                    appContext,
                    workerParameters
                )
            PartUploadTransferWorker::class.java.name ->
                return PartUploadTransferWorker(
                    clientProvider,
                    transferDB,
                    transferStatusUpdater,
                    appContext,
                    workerParameters
                )
            CompleteMultiPartUploadWorker::class.java.name ->
                return CompleteMultiPartUploadWorker(
                    clientProvider,
                    transferDB,
                    transferStatusUpdater,
                    appContext,
                    workerParameters
                )
            AbortMultiPartUploadWorker::class.java.name ->
                return AbortMultiPartUploadWorker(
                    clientProvider,
                    transferDB,
                    transferStatusUpdater,
                    appContext,
                    workerParameters
                )
            else ->
                throw IllegalStateException("Failed to find matching Worker for $workerClassName")
        }
    }
}
