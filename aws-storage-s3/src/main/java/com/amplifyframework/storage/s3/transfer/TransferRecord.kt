/**
 * Copyright 2015-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 * <p>
 * http://aws.amazon.com/apache2.0
 * <p>
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amplifyframework.storage.s3.transfer

import android.database.Cursor
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.amazonaws.util.json.JsonUtils
import com.amplifyframework.storage.s3.transfer.worker.AbortMultiPartUploadWorker
import com.amplifyframework.storage.s3.transfer.worker.BaseTransferWorker
import com.amplifyframework.storage.s3.transfer.worker.CompleteMultiPartUploadWorker
import com.amplifyframework.storage.s3.transfer.worker.DownloadWorker
import com.amplifyframework.storage.s3.transfer.worker.InitiateMultiPartUploadTransferWorker
import com.amplifyframework.storage.s3.transfer.worker.PartUploadTransferWorker
import com.amplifyframework.storage.s3.transfer.worker.RouterWorker
import com.amplifyframework.storage.s3.transfer.worker.SinglePartUploadWorker

data class TransferRecord(
    var id: Int,
    var mainUploadId: Int = 0,
    var isRequesterPays: Int = 0,
    var isMultipart: Int = 0,
    var isLastPart: Int = 0,
    var isEncrypted: Int = 0,
    var partNumber: Int = 0,
    var bytesTotal: Long = 0,
    var bytesCurrent: Long = 0,
    var speed: Long = 0,
    var rangeStart: Long = 0,
    var rangeLast: Long = 0,
    var fileOffset: Long = 0,
    var type: TransferType? = null,
    var state: TransferState? = null,
    var bucketName: String? = null,
    var key: String? = null,
    var versionId: String? = null,
    var file: String = "",
    var multipartId: String? = null,
    var eTag: String? = null,
    var headerContentType: String? = null,
    var headerContentLanguage: String? = null,
    var headerContentDisposition: String? = null,
    var headerContentEncoding: String? = null,
    var headerCacheControl: String? = null,
    var headerExpire: String? = null,
    var headerStorageClass: String? = null,
    var userMetadata: Map<String, String>? = null,
    var expirationTimeRuleId: String? = null,
    var httpExpires: String? = null,
    var sseAlgorithm: String? = null,
    var sseKMSKey: String? = null,
    var md5: String? = null,
    var cannedAcl: String? = null,
    var workManagerRequestId: String? = null
) {
    companion object {

        const val MINIMUM_UPLOAD_PART_SIZE = 5 * 1024 * 1024
        const val MAXIMUM_UPLOAD_PARTS = 10000

        @JvmStatic
        fun updateFromDB(c: Cursor): TransferRecord {
            val id = c.getInt(c.getColumnIndexOrThrow(TransferTable.COLUMN_ID))
            return TransferRecord(id).apply {
                this.mainUploadId =
                    c.getInt(c.getColumnIndexOrThrow(TransferTable.COLUMN_MAIN_UPLOAD_ID))
                this.type =
                    TransferType.valueOf(c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_TYPE)))
                this.state =
                    TransferState.getState(c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_STATE)))
                this.bucketName =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_BUCKET_NAME))
                this.key = c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_KEY))
                this.versionId =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_VERSION_ID))
                this.bytesTotal =
                    c.getLong(c.getColumnIndexOrThrow(TransferTable.COLUMN_BYTES_TOTAL))
                this.bytesCurrent =
                    c.getLong(c.getColumnIndexOrThrow(TransferTable.COLUMN_BYTES_CURRENT))
                this.speed = c.getLong(c.getColumnIndexOrThrow(TransferTable.COLUMN_SPEED))
                this.isRequesterPays =
                    c.getInt(c.getColumnIndexOrThrow(TransferTable.COLUMN_IS_REQUESTER_PAYS))
                this.isMultipart =
                    c.getInt(c.getColumnIndexOrThrow(TransferTable.COLUMN_IS_MULTIPART))
                this.isLastPart =
                    c.getInt(c.getColumnIndexOrThrow(TransferTable.COLUMN_IS_LAST_PART))
                this.isEncrypted =
                    c.getInt(c.getColumnIndexOrThrow(TransferTable.COLUMN_IS_ENCRYPTED))
                this.partNumber = c.getInt(c.getColumnIndexOrThrow(TransferTable.COLUMN_PART_NUM))
                this.eTag = c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_ETAG))
                this.file = c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_FILE))
                this.multipartId =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_MULTIPART_ID))
                this.rangeStart =
                    c.getLong(c.getColumnIndexOrThrow(TransferTable.COLUMN_DATA_RANGE_START))
                this.rangeLast =
                    c.getLong(c.getColumnIndexOrThrow(TransferTable.COLUMN_DATA_RANGE_LAST))
                this.fileOffset =
                    c.getLong(c.getColumnIndexOrThrow(TransferTable.COLUMN_FILE_OFFSET))
                this.headerContentType =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_HEADER_CONTENT_TYPE))
                this.headerContentLanguage =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_HEADER_CONTENT_LANGUAGE))
                this.headerContentDisposition =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_HEADER_CONTENT_DISPOSITION))
                this.headerContentEncoding =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_HEADER_CONTENT_ENCODING))
                this.headerCacheControl =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_HEADER_CACHE_CONTROL))
                this.headerExpire =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_HEADER_EXPIRE))
                this.userMetadata =
                    JsonUtils.jsonToMap(c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_USER_METADATA)))
                this.expirationTimeRuleId =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_EXPIRATION_TIME_RULE_ID))
                this.httpExpires =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_HTTP_EXPIRES_DATE))
                this.sseAlgorithm =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_SSE_ALGORITHM))
                this.sseKMSKey =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_SSE_KMS_KEY))
                this.md5 = c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_CONTENT_MD5))
                this.cannedAcl =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_CANNED_ACL))
                this.headerStorageClass =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_HEADER_STORAGE_CLASS))
            }
        }
    }

    internal fun isMainRecord(): Boolean {
        return isMultipart == 0 || (isMultipart == 1 && mainUploadId == 0)
    }

    internal fun start(
        pluginKey: String,
        transferStatusUpdater: TransferStatusUpdater,
        workManager: WorkManager,
        workerObserver: TransferWorkerObserver,
        transferDB: TransferDB,
        listener: TransferListener?
    ): TransferObserver {
        if (isMultipart == 1) {
            enqueueMultiPartUpload(
                pluginKey,
                workManager,
                workerObserver,
                transferStatusUpdater,
                transferDB
            )
        } else {
            enqueueTransfer(pluginKey, workManager, workerObserver, transferStatusUpdater)
        }
        return TransferObserver(id, transferStatusUpdater, bucketName, key, file, listener)
    }

    internal fun pause(
        transferStatusUpdater: TransferStatusUpdater,
        workManager: WorkManager
    ): Boolean {
        if (TransferState.isStarted(state) && !TransferState.isInTerminalState(state)) {
            workManager.cancelUniqueWork(id.toString())
            transferStatusUpdater.updateTransferState(id, TransferState.PENDING_PAUSE)
            return true
        }
        return false
    }

    internal fun resume(
        pluginKey: String,
        transferStatusUpdater: TransferStatusUpdater,
        workManager: WorkManager,
        workerObserver: TransferWorkerObserver,
        transferDB: TransferDB
    ): Boolean {
        if (!TransferState.isStarted(state) && !TransferState.isInTerminalState(state)) {
            transferStatusUpdater.updateTransferState(id, TransferState.RESUMED_WAITING)
            start(pluginKey, transferStatusUpdater, workManager, workerObserver, transferDB, null)
            return true
        }
        return false
    }

    internal fun cancel(
        pluginKey: String,
        transferStatusUpdater: TransferStatusUpdater,
        workManager: WorkManager
    ): Boolean {
        if (!TransferState.isInTerminalState(state)) {
            var nextState: TransferState = TransferState.PENDING_CANCEL
            if (TransferState.isPaused(state)) {
                if (isMultipart == 1) {
                    abortMultipartUploadRequest(pluginKey, workManager)
                } else {
                    nextState = TransferState.CANCELED
                }
            } else {
                workManager.cancelUniqueWork(id.toString())
            }
            transferStatusUpdater.updateTransferState(id, nextState)
            return true
        }
        return false
    }

    internal fun abortMultipartUploadRequest(
        pluginKey: String,
        workManager: WorkManager
    ) {
        val abortRequest = getOneTimeWorkRequest(
            Data.Builder().putAll(
                mapOf(
                    BaseTransferWorker.TRANSFER_RECORD_ID to id,
                    RouterWorker.WORKER_CLASS_NAME to AbortMultiPartUploadWorker::class.java.name,
                    BaseTransferWorker.WORKER_ID to pluginKey
                )
            ).build(),
            listOf(id.toString(), pluginKey)
        )
        workManager.enqueue(abortRequest)
    }

    private fun enqueueTransfer(
        pluginKey: String,
        workManager: WorkManager,
        transferWorkerObserver: TransferWorkerObserver,
        transferStatusUpdater: TransferStatusUpdater
    ) {
        val type = type ?: throw IllegalStateException("Transfer type missing")
        val workerClassName =
            if (type == TransferType.UPLOAD) SinglePartUploadWorker::class.java.name else DownloadWorker::class.java.name

        val transferRequest = getOneTimeWorkRequest(
            workDataOf(
                BaseTransferWorker.TRANSFER_RECORD_ID to id,
                RouterWorker.WORKER_CLASS_NAME to workerClassName,
                BaseTransferWorker.WORKER_ID to pluginKey
            ),
            listOf(pluginKey, id.toString())
        )
        workManager.beginUniqueWork(
            id.toString(),
            ExistingWorkPolicy.KEEP,
            transferRequest
        ).enqueue()
        transferStatusUpdater.addWorkRequest(transferRequest.id.toString(), id, false)
    }

    private fun enqueueMultiPartUpload(
        pluginKey: String,
        workManager: WorkManager,
        transferWorkerObserver: TransferWorkerObserver,
        transferStatusUpdater: TransferStatusUpdater,
        transferDB: TransferDB
    ) {
        multipartId?.let {
            val pendingParts = pendingParts(pluginKey, transferDB)
            if (pendingParts.size > 0) {
                workManager
                    .beginUniqueWork(
                        id.toString(),
                        ExistingWorkPolicy.KEEP,
                        pendingParts
                    )
                    .then(completeRequest(pluginKey, transferStatusUpdater))
                    .enqueue()
            } else {
                workManager
                    .beginUniqueWork(
                        id.toString(),
                        ExistingWorkPolicy.KEEP,
                        completeRequest(pluginKey, transferStatusUpdater)
                    )
                    .enqueue()
            }
        } ?: run {
            workManager.beginUniqueWork(
                id.toString(),
                ExistingWorkPolicy.KEEP,
                initiateRequest(pluginKey, transferStatusUpdater)
            )
                .then(pendingParts(pluginKey, transferDB))
                .then(completeRequest(pluginKey, transferStatusUpdater))
                .enqueue()
        }
    }

    private fun initiateRequest(
        pluginKey: String,
        transferStatusUpdater: TransferStatusUpdater
    ): OneTimeWorkRequest {
        val request = getOneTimeWorkRequest(
            workDataOf(
                BaseTransferWorker.TRANSFER_RECORD_ID to id,
                RouterWorker.WORKER_CLASS_NAME to InitiateMultiPartUploadTransferWorker::class.java.name,
                BaseTransferWorker.WORKER_ID to pluginKey
            ),
            listOf(
                id.toString(),
                BaseTransferWorker.initiationRequestTag.format(id.toString()),
                pluginKey
            )
        )
        transferStatusUpdater.addWorkRequest(request.id.toString(), id, true)
        return request
    }

    private fun pendingParts(
        pluginKey: String,
        transferDB: TransferDB
    ) = let {
        val listOfPendingParts = transferDB.getNonCompletedPartRequestsFromDB(id)
        val pendingPartRequest = mutableListOf<OneTimeWorkRequest>()
        for (part in listOfPendingParts) {
            pendingPartRequest.add(
                getOneTimeWorkRequest(
                    workDataOf(
                        BaseTransferWorker.TRANSFER_RECORD_ID to id,
                        BaseTransferWorker.PART_RECORD_ID to part,
                        BaseTransferWorker.MULTI_PART_UPLOAD_ID to multipartId,
                        RouterWorker.WORKER_CLASS_NAME to PartUploadTransferWorker::class.java.name,
                        BaseTransferWorker.WORKER_ID to pluginKey
                    ),
                    listOf(id.toString(), pluginKey, "PartUploadRequest")
                )
            )
        }
        pendingPartRequest
    }

    private fun completeRequest(
        pluginKey: String,
        transferStatusUpdater: TransferStatusUpdater
    ): OneTimeWorkRequest {
        val request = getOneTimeWorkRequest(
            workDataOf(
                BaseTransferWorker.TRANSFER_RECORD_ID to id,
                RouterWorker.WORKER_CLASS_NAME to CompleteMultiPartUploadWorker::class.java.name,
                BaseTransferWorker.WORKER_ID to pluginKey
            ),
            listOf(
                id.toString(),
                pluginKey,
                BaseTransferWorker.completionRequestTag.format(id.toString())
            )
        )
        transferStatusUpdater.addWorkRequest(request.id.toString(), id, true)
        return request
    }

    private fun getOneTimeWorkRequest(
        data: Data,
        tags: List<String>
    ): OneTimeWorkRequest {
        val type = type ?: throw IllegalStateException("Transfer type missing")
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
                if (isMultipart == 1) {
                    addTag(BaseTransferWorker.MULTIPART_UPLOAD)
                }
            }
            .addTag(type.name)
            .build()
    }
}
