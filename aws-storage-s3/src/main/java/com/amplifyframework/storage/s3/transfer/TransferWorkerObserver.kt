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
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
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
    private val workManagerToAmplifyStatesMap = mapOf(
        WorkInfo.State.ENQUEUED to TransferState.WAITING,
        WorkInfo.State.BLOCKED to TransferState.WAITING,
        WorkInfo.State.RUNNING to TransferState.IN_PROGRESS,
        WorkInfo.State.CANCELLED to TransferState.CANCELED,
        WorkInfo.State.FAILED to TransferState.FAILED,
        WorkInfo.State.SUCCEEDED to TransferState.COMPLETED
    )

    init {
        attachObserverForPendingTransfer()
    }

    private val logger =
        Amplify.Logging.forNamespace(
            AWSS3StoragePlugin.AWS_S3_STORAGE_LOG_NAMESPACE.format(this::class.java.simpleName)
        )

    companion object {
        private val INSTANCE: TransferWorkerObserver? = null

        @JvmStatic
        fun getInstance(
            context: Context,
            pluginKey: String,
            workManager: WorkManager,
            transferStatusUpdater: TransferStatusUpdater,
            transferDB: TransferDB
        ): TransferWorkerObserver {
            return TransferWorkerObserver.INSTANCE ?: TransferWorkerObserver(
                context,
                pluginKey,
                workManager,
                transferStatusUpdater,
                transferDB
            )
        }
    }

    override fun onChanged(workInfoList: MutableList<WorkInfo>?) {
        coroutineScope.launch {
            workInfoList?.forEach { workInfo ->
                val transferRecordId =
                    transferStatusUpdater.getTransferRecordIdForWorkInfo(workInfo.id.toString())
                transferRecordId.takeIf { it != -1 }?.let {
                    val transferRecord = transferStatusUpdater.activeTransferMap[transferRecordId]
                    transferRecord?.let {
                        // logger.debug("onChanged for ${workInfo.id} to ${workInfo.state}")
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

    private suspend fun handleTransferStatusUpdate(
        workInfo: WorkInfo,
        transferRecord: TransferRecord
    ) {
        updateTransferState(transferRecord, workInfo.state)
        if (workInfo.state.isFinished) {
            logger.debug("remove observer for ${transferRecord.id}")
            removeObserver(transferRecord.id.toString())
        }
    }

    private suspend fun handleMultipartUploadStatusUpdate(
        workInfo: WorkInfo,
        transferRecord: TransferRecord
    ) {
        val initializationTag =
            BaseTransferWorker.initiationRequestTag.format(transferRecord.id)
        val completionTag = BaseTransferWorker.completionRequestTag.format(transferRecord.id)
        if (workInfo.tags.contains(initializationTag)) {
            if (listOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.RUNNING).contains(workInfo.state)) {
                updateTransferState(transferRecord, WorkInfo.State.RUNNING)
                return
            }
        } else if (workInfo.tags.contains(completionTag)) {
            if (abortRequest(transferRecord, workInfo.state)) {
                transferRecord.abortMultipartUploadRequest(pluginKey, workManager)
            }
            updateTransferState(transferRecord, workInfo.state)
            if (workInfo.state.isFinished) {
                logger.debug("remove observer for ${transferRecord.id}")
                removeObserver(transferRecord.id.toString())
            }
        }
    }

    private fun updateTransferState(transferRecord: TransferRecord, workInfoState: WorkInfo.State) {
        transferRecord.state?.let {
            var nextState = workManagerToAmplifyStatesMap[workInfoState]!!
            transferRecord.state?.let { state ->
                if (TransferState.isPaused(state)) {
                    nextState = TransferState.PAUSED
                }
                if (TransferState.isCancelled(state)) {
                    nextState = TransferState.CANCELED
                }
            }
            if (!TransferState.isInTerminalState(transferRecord.state)) {
                transferStatusUpdater.updateTransferState(transferRecord.id, nextState)
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
                    TransferState.WAITING,
                )
            )?.use {
                while (it.moveToNext()) {
                    val id = it.getInt(it.getColumnIndexOrThrow(TransferTable.COLUMN_ID))
                    // observer should be attached on main thread
                    attachObserver(id.toString())
                }
            }
        }
    }

    private suspend fun attachObserver(tag: String) {
        withContext(Dispatchers.Main) {
            workManager.getWorkInfosByTagLiveData(tag)
                .observeForever(this@TransferWorkerObserver)
        }
    }

    private suspend fun removeObserver(tag: String) {
        withContext(Dispatchers.Main) {
            workManager.getWorkInfosByTagLiveData(tag)
                .removeObserver(this@TransferWorkerObserver)
        }
    }

    /*
    TODO(implement when progress listener is supported by kotlin sdk)
    internal inner class MultiPartUploadTaskListener(private val transferRecord: TransferRecord) :
        ProgressListener {

        private var totalBytesTransferred: AtomicLong = AtomicLong(0L)

        init {
            val previouslyTransferBytes =
                transferDB.queryBytesTransferredByMainUploadId(transferRecord.id)
            totalBytesTransferred.getAndAdd(previouslyTransferBytes)
        }

        override fun progressChanged(progressEvent: ProgressEvent?) {
            progressEvent?.let {
                if (progressEvent.eventCode == ProgressEvent.RESET_EVENT_CODE) {
                    totalBytesTransferred.getAndAdd(progressEvent.bytesTransferred * -1)
                    if (totalBytesTransferred.get() < 0) {
                        totalBytesTransferred.set(0L)
                    }
                    transferRecord.bytesCurrent = totalBytesTransferred.get()
                    updateProgress(false)
                } else {
                    totalBytesTransferred.getAndAdd(progressEvent.bytesTransferred)
                    transferRecord.bytesCurrent = totalBytesTransferred.get()
                    if (transferRecord.bytesCurrent > transferRecord.bytesTotal) {
                        transferRecord.bytesCurrent = transferRecord.bytesTotal
                    }
                    updateProgress(true)
                }
            }
        }

        private fun updateProgress(notifyListener: Boolean) {
            transferStatusUpdater.updateProgress(
                transferRecord.id,
                transferRecord.bytesCurrent,
                transferRecord.bytesTotal,
                notifyListener
            )
        }
    }*/
}
