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

package com.amplifyframework.storage.s3.transfer

import android.content.Context
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.amplifyframework.storage.s3.TransferOperations
import com.amplifyframework.storage.s3.transfer.worker.BaseTransferWorker
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class TransferWorkerObserver private constructor(
    context: Context,
    private val pluginKey: String,
    private val workManager: WorkManager,
    private val transferStatusUpdater: TransferStatusUpdater,
    private val transferDB: TransferDB
) : Observer<MutableList<WorkInfo>> {

    private val coroutineScope =
        CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    private val logger =
        Amplify.Logging.logger(
            CategoryType.STORAGE,
            AWSS3StoragePlugin.AWS_S3_STORAGE_LOG_NAMESPACE.format(this::class.java.simpleName)
        )

    init {
        attachObserverForPendingTransfer()
    }

    companion object {
        private var INSTANCE: TransferWorkerObserver? = null

        @JvmStatic
        fun getInstance(
            context: Context,
            pluginKey: String,
            workManager: WorkManager,
            transferStatusUpdater: TransferStatusUpdater,
            transferDB: TransferDB
        ): TransferWorkerObserver {
            return TransferWorkerObserver.INSTANCE ?: run {
                val transferWorkerObserver = TransferWorkerObserver(
                    context,
                    pluginKey,
                    workManager,
                    transferStatusUpdater,
                    transferDB
                )
                INSTANCE = transferWorkerObserver
                transferWorkerObserver
            }
        }
    }

    override fun onChanged(workInfoList: MutableList<WorkInfo>?) {
        coroutineScope.launch {
            workInfoList?.forEach { workInfo ->
                val transferRecordId =
                    transferStatusUpdater.getTransferRecordIdForWorkInfo(workInfo.id.toString())
                        ?: workInfo.outputData.getInt(BaseTransferWorker.OUTPUT_TRANSFER_RECORD_ID, -1)

                transferRecordId.takeIf { it != -1 }?.let {
                    val transferRecord = transferStatusUpdater.activeTransferMap[transferRecordId]
                    transferRecord?.let {
                        if (!TransferState.isInTerminalState(transferRecord.state)) {
                            if (workInfo.tags.contains(BaseTransferWorker.MULTIPART_UPLOAD)) {
                                handleMultipartUploadStatusUpdate(workInfo, it)
                            } else {
                                handleTransferStatusUpdate(workInfo, it)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleTransferStatusUpdate(
        workInfo: WorkInfo,
        transferRecord: TransferRecord
    ) {
        val workManagerToAmplifyStatesMap = mapOf(
            WorkInfo.State.ENQUEUED to TransferState.WAITING,
            WorkInfo.State.BLOCKED to TransferState.WAITING,
            WorkInfo.State.RUNNING to TransferState.IN_PROGRESS,
            WorkInfo.State.CANCELLED to TransferState.CANCELED,
            WorkInfo.State.FAILED to TransferState.FAILED,
            WorkInfo.State.SUCCEEDED to TransferState.COMPLETED
        )
        updateTransferState(transferRecord.id, workManagerToAmplifyStatesMap[workInfo.state], workInfo.id.toString())
        if (workInfo.state.isFinished || transferRecord.state == TransferState.PAUSED) {
            logger.debug("remove observer for ${transferRecord.id}")
            removeObserver(transferRecord.id.toString())
        }
    }

    private suspend fun handleMultipartUploadStatusUpdate(
        workInfo: WorkInfo,
        transferRecord: TransferRecord
    ) {
        val workManagerToAmplifyStatesMap = mapOf(
            WorkInfo.State.ENQUEUED to TransferState.WAITING,
            WorkInfo.State.BLOCKED to TransferState.WAITING,
            WorkInfo.State.RUNNING to TransferState.IN_PROGRESS,
            WorkInfo.State.CANCELLED to TransferState.PENDING_CANCEL,
            WorkInfo.State.FAILED to TransferState.PENDING_FAILED,
            WorkInfo.State.SUCCEEDED to TransferState.COMPLETED
        )
        val initializationTag =
            BaseTransferWorker.initiationRequestTag.format(transferRecord.id)
        val completionTag = BaseTransferWorker.completionRequestTag.format(transferRecord.id)
        if (workInfo.tags.contains(completionTag)) {
            if (abortRequest(transferRecord, workInfo.state)) {
                TransferOperations.abortMultipartUploadRequest(transferRecord, pluginKey, workManager)
            }
            if (workInfo.state.isFinished) {
                updateTransferState(
                    transferRecord.id,
                    workManagerToAmplifyStatesMap[workInfo.state],
                    workInfo.id.toString()
                )
                logger.debug("remove observer for ${transferRecord.id}")
                removeObserver(transferRecord.id.toString())
            }
        }
    }

    private fun updateTransferState(transferRecordId: Int, transferState: TransferState?, workInfoId: String) {
        transferStatusUpdater.activeTransferMap[transferRecordId]?.state?.let { state ->
            var nextState = transferState ?: TransferState.UNKNOWN
            if (TransferState.isPaused(state)) {
                nextState = TransferState.PAUSED
                transferStatusUpdater.removeWorkInfoId(workInfoId)
            }
            if (TransferState.isCancelled(state)) {
                nextState = TransferState.CANCELED
                transferStatusUpdater.removeWorkInfoId(workInfoId)
            }
            if (!TransferState.isInTerminalState(state)) {
                transferStatusUpdater.updateTransferState(transferRecordId, nextState)
            }
        }
    }

    private fun abortRequest(
        transferRecord: TransferRecord,
        workState: WorkInfo.State
    ): Boolean {
        return transferRecord.isMultipart == 1 &&
            (transferRecord.state == TransferState.PENDING_CANCEL || workState == WorkInfo.State.FAILED)
    }

    private fun attachObserverForPendingTransfer() {
        coroutineScope.launch {
            transferDB.queryTransfersWithTypeAndStates(
                TransferType.ANY,
                arrayOf(
                    TransferState.IN_PROGRESS,
                    TransferState.WAITING
                )
            )?.use {
                while (it.moveToNext()) {
                    val id = it.getInt(it.getColumnIndexOrThrow(TransferTable.COLUMN_ID))
                    attachObserver(id.toString())
                }
            }
        }
    }

    private suspend fun attachObserver(tag: String) {
        withContext(Dispatchers.Main) {
            val liveData = workManager.getWorkInfosByTagLiveData(tag)
            liveData.observeForever(this@TransferWorkerObserver)
        }
    }

    private suspend fun removeObserver(tag: String) {
        withContext(Dispatchers.Main) {
            workManager.getWorkInfosByTagLiveData(tag)
                .removeObserver(this@TransferWorkerObserver)
        }
    }
}
