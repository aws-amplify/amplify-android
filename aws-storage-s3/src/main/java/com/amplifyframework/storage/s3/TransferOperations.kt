/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.storage.s3

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.s3.transfer.MultiPartUploadTaskListener
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferListener
import com.amplifyframework.storage.s3.transfer.TransferObserver
import com.amplifyframework.storage.s3.transfer.TransferRecord
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater
import com.amplifyframework.storage.s3.transfer.TransferType
import com.amplifyframework.storage.s3.transfer.TransferWorkerObserver
import com.amplifyframework.storage.s3.transfer.worker.AbortMultiPartUploadWorker
import com.amplifyframework.storage.s3.transfer.worker.BaseTransferWorker
import com.amplifyframework.storage.s3.transfer.worker.CompleteMultiPartUploadWorker
import com.amplifyframework.storage.s3.transfer.worker.DownloadWorker
import com.amplifyframework.storage.s3.transfer.worker.InitiateMultiPartUploadTransferWorker
import com.amplifyframework.storage.s3.transfer.worker.PartUploadTransferWorker
import com.amplifyframework.storage.s3.transfer.worker.RouterWorker
import com.amplifyframework.storage.s3.transfer.worker.SinglePartUploadWorker

internal object TransferOperations {

    internal fun start(
        transferRecord: TransferRecord,
        pluginKey: String,
        transferStatusUpdater: TransferStatusUpdater,
        workManager: WorkManager,
        workerObserver: TransferWorkerObserver,
        transferDB: TransferDB,
        listener: TransferListener?
    ): TransferObserver {
        if (transferRecord.isMultipart == 1) {
            enqueueMultiPartUpload(
                transferRecord,
                pluginKey,
                workManager,
                workerObserver,
                transferStatusUpdater,
                transferDB
            )
            transferStatusUpdater.registerMultiPartTransferListener(
                transferRecord.id,
                MultiPartUploadTaskListener(transferRecord, transferDB, transferStatusUpdater)
            )
        } else {
            enqueueTransfer(transferRecord, pluginKey, workManager, workerObserver, transferStatusUpdater)
        }
        return TransferObserver(
            transferRecord.id,
            transferStatusUpdater,
            transferRecord.bucketName,
            transferRecord.key,
            transferRecord.file,
            listener
        )
    }

    internal fun pause(
        transferRecord: TransferRecord,
        transferStatusUpdater: TransferStatusUpdater,
        workManager: WorkManager
    ): Boolean {
        if (TransferState.isStarted(transferRecord.state) && !TransferState.isInTerminalState(transferRecord.state)) {
            workManager.cancelUniqueWork(transferRecord.id.toString())
            transferStatusUpdater.updateTransferState(transferRecord.id, TransferState.PENDING_PAUSE)
            return true
        }
        return false
    }

    internal fun resume(
        transferRecord: TransferRecord,
        pluginKey: String,
        transferStatusUpdater: TransferStatusUpdater,
        workManager: WorkManager,
        workerObserver: TransferWorkerObserver,
        transferDB: TransferDB
    ): Boolean {
        if (!TransferState.isStarted(transferRecord.state) && !TransferState.isInTerminalState(transferRecord.state)) {
            start(transferRecord, pluginKey, transferStatusUpdater, workManager, workerObserver, transferDB, null)
            if (transferRecord.isMultipart == 0) {
                transferStatusUpdater.updateTransferState(transferRecord.id, TransferState.RESUMED_WAITING)
            }
            return true
        }
        return false
    }

    internal fun cancel(
        transferRecord: TransferRecord,
        pluginKey: String,
        transferStatusUpdater: TransferStatusUpdater,
        workManager: WorkManager
    ): Boolean {
        if (!TransferState.isInTerminalState(transferRecord.state)) {
            var nextState: TransferState = TransferState.PENDING_CANCEL
            if (TransferState.isPaused(transferRecord.state)) {
                if (transferRecord.isMultipart == 1) {
                    abortMultipartUploadRequest(transferRecord, pluginKey, workManager)
                }
                nextState = TransferState.CANCELED
            } else {
                workManager.cancelUniqueWork(transferRecord.id.toString())
            }
            transferStatusUpdater.updateTransferState(transferRecord.id, nextState)
            return true
        }
        return false
    }

    internal fun abortMultipartUploadRequest(
        transferRecord: TransferRecord,
        pluginKey: String,
        workManager: WorkManager
    ) {
        val abortRequest = getOneTimeWorkRequest(
            transferRecord,
            Data.Builder().putAll(
                mapOf(
                    BaseTransferWorker.TRANSFER_RECORD_ID to transferRecord.id,
                    RouterWorker.WORKER_CLASS_NAME to AbortMultiPartUploadWorker::class.java.name,
                    BaseTransferWorker.WORKER_ID to pluginKey
                )
            ).build(),
            listOf(transferRecord.id.toString(), pluginKey)
        )
        workManager.enqueue(abortRequest)
    }

    private fun enqueueTransfer(
        transferRecord: TransferRecord,
        pluginKey: String,
        workManager: WorkManager,
        transferWorkerObserver: TransferWorkerObserver,
        transferStatusUpdater: TransferStatusUpdater
    ) {
        val type = transferRecord.type ?: throw IllegalStateException("Transfer type missing")
        val workerClassName =
            if (type == TransferType.UPLOAD)
                SinglePartUploadWorker::class.java.name else
                DownloadWorker::class.java.name

        val transferRequest = getOneTimeWorkRequest(
            transferRecord,
            workDataOf(
                BaseTransferWorker.TRANSFER_RECORD_ID to transferRecord.id,
                RouterWorker.WORKER_CLASS_NAME to workerClassName,
                BaseTransferWorker.WORKER_ID to pluginKey
            ),
            listOf(pluginKey, transferRecord.id.toString())
        )
        workManager.enqueueUniqueWork(
            transferRecord.id.toString(),
            ExistingWorkPolicy.KEEP,
            transferRequest
        )
        transferStatusUpdater.addWorkRequest(transferRequest.id.toString(), transferRecord.id, false)
    }

    private fun enqueueMultiPartUpload(
        transferRecord: TransferRecord,
        pluginKey: String,
        workManager: WorkManager,
        transferWorkerObserver: TransferWorkerObserver,
        transferStatusUpdater: TransferStatusUpdater,
        transferDB: TransferDB
    ) {
        transferRecord.multipartId?.let {
            val pendingParts = pendingParts(transferRecord, pluginKey, transferDB)
            if (pendingParts.size > 0) {
                workManager
                    .beginUniqueWork(
                        transferRecord.id.toString(),
                        ExistingWorkPolicy.KEEP,
                        pendingParts
                    )
                    .then(completeRequest(transferRecord, pluginKey, transferStatusUpdater))
                    .enqueue()
            } else {
                workManager
                    .enqueueUniqueWork(
                        transferRecord.id.toString(),
                        ExistingWorkPolicy.KEEP,
                        completeRequest(transferRecord, pluginKey, transferStatusUpdater)
                    )
            }
            transferStatusUpdater.updateTransferState(transferRecord.id, TransferState.IN_PROGRESS)
        } ?: run {
            workManager.beginUniqueWork(
                transferRecord.id.toString(),
                ExistingWorkPolicy.KEEP,
                initiateRequest(transferRecord, pluginKey, transferStatusUpdater)
            )
                .then(pendingParts(transferRecord, pluginKey, transferDB))
                .then(completeRequest(transferRecord, pluginKey, transferStatusUpdater))
                .enqueue()
            transferStatusUpdater.updateTransferState(transferRecord.id, TransferState.WAITING)
        }
    }

    private fun initiateRequest(
        transferRecord: TransferRecord,
        pluginKey: String,
        transferStatusUpdater: TransferStatusUpdater
    ): OneTimeWorkRequest {
        val request = getOneTimeWorkRequest(
            transferRecord,
            workDataOf(
                BaseTransferWorker.TRANSFER_RECORD_ID to transferRecord.id,
                RouterWorker.WORKER_CLASS_NAME to InitiateMultiPartUploadTransferWorker::class.java.name,
                BaseTransferWorker.WORKER_ID to pluginKey
            ),
            listOf(
                transferRecord.id.toString(),
                BaseTransferWorker.initiationRequestTag.format(transferRecord.id.toString()),
                pluginKey
            )
        )
        transferStatusUpdater.addWorkRequest(request.id.toString(), transferRecord.id, true)
        return request
    }

    private fun pendingParts(
        transferRecord: TransferRecord,
        pluginKey: String,
        transferDB: TransferDB
    ) = let {
        val listOfPendingParts = transferDB.getNonCompletedPartRequestsFromDB(transferRecord.id)
        val pendingPartRequest = mutableListOf<OneTimeWorkRequest>()
        for (part in listOfPendingParts) {
            pendingPartRequest.add(
                getOneTimeWorkRequest(
                    transferRecord,
                    workDataOf(
                        BaseTransferWorker.TRANSFER_RECORD_ID to transferRecord.id,
                        BaseTransferWorker.PART_RECORD_ID to part,
                        BaseTransferWorker.MULTI_PART_UPLOAD_ID to transferRecord.multipartId,
                        RouterWorker.WORKER_CLASS_NAME to PartUploadTransferWorker::class.java.name,
                        BaseTransferWorker.WORKER_ID to pluginKey
                    ),
                    listOf(transferRecord.id.toString(), pluginKey, "PartUploadRequest")
                )
            )
        }
        pendingPartRequest
    }

    private fun completeRequest(
        transferRecord: TransferRecord,
        pluginKey: String,
        transferStatusUpdater: TransferStatusUpdater
    ): OneTimeWorkRequest {
        val request = getOneTimeWorkRequest(
            transferRecord,
            workDataOf(
                BaseTransferWorker.TRANSFER_RECORD_ID to transferRecord.id,
                RouterWorker.WORKER_CLASS_NAME to CompleteMultiPartUploadWorker::class.java.name,
                BaseTransferWorker.WORKER_ID to pluginKey
            ),
            listOf(
                transferRecord.id.toString(),
                pluginKey,
                BaseTransferWorker.completionRequestTag.format(transferRecord.id.toString())
            )
        )
        transferStatusUpdater.addWorkRequest(request.id.toString(), transferRecord.id, true)
        return request
    }

    private fun getOneTimeWorkRequest(
        transferRecord: TransferRecord,
        data: Data,
        tags: List<String>
    ): OneTimeWorkRequest {
        val type = transferRecord.type ?: throw IllegalStateException("Transfer type missing")
        return OneTimeWorkRequest.Builder(RouterWorker::class.java)
            .setInputData(
                data
            )
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            ).apply {
                tags.forEach {
                    addTag(it)
                }
                if (transferRecord.isMultipart == 1) {
                    addTag(BaseTransferWorker.MULTIPART_UPLOAD)
                }
            }
            .addTag(type.name)
            .build()
    }
}
