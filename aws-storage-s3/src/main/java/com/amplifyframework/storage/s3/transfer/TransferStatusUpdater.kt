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

import android.os.Handler
import android.os.Looper
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Updates transfer status to observers and to local DB
 **/
internal class TransferStatusUpdater(
    private val transferDB: TransferDB
) {
    private val logger =
        Amplify.Logging.logger(
            CategoryType.STORAGE,
            AWSS3StoragePlugin.AWS_S3_STORAGE_LOG_NAMESPACE.format(this::class.java.simpleName)
        )
    private val mainHandler = Handler(Looper.getMainLooper())
    private val transferStatusListenerMap:
        ConcurrentHashMap<Int, ConcurrentHashMap<TransferListener, Boolean>> by lazy { ConcurrentHashMap() }
    private val transferWorkInfoIdMap: ConcurrentHashMap<String, Int> by lazy { ConcurrentHashMap() }
    private val multiPartTransferStatusListener: ConcurrentHashMap<Int, MultiPartUploadTaskListener> by lazy {
        ConcurrentHashMap()
    }
    val activeTransferMap = object : AbstractMutableMap<Int, TransferRecord>() {

        val transferRecordMap = ConcurrentHashMap<Int, TransferRecord>()

        override fun put(key: Int, value: TransferRecord): TransferRecord? {
            return transferRecordMap.put(key, value)
        }

        override fun get(key: Int): TransferRecord? {
            if (!transferRecordMap.containsKey(key)) {
                transferDB.getTransferRecordById(key)?.let { put(key, it) }
            }
            return super.get(key)
        }

        override val entries: MutableSet<MutableMap.MutableEntry<Int, TransferRecord>>
            get() = transferRecordMap.entries
    }

    companion object {
        internal const val TEMP_FILE_PREFIX = "aws-s3-d861b25a-1edf-11eb-adc1-0242ac120002"
    }

    @Synchronized
    fun removeTransferRecord(transferRecordId: Int) {
        transferDB.getTransferRecordById(transferRecordId)?.let { it ->
            if (!it.isMainRecord()) {
                return
            }
            val path = it.file
            val file = File(path)
            file.name.takeIf { name -> name.startsWith(TEMP_FILE_PREFIX) }?.let {
                try {
                    file.delete()
                } catch (exception: Exception) {
                    logger.error("Failed to delete temp file: ${file.name} $exception")
                }
            }
            if (it.isMultipart == 1) {
                transferDB.deletePartTransferRecords(transferRecordId)
            }
            transferDB.deleteTransferRecords(transferRecordId)
            activeTransferMap.remove(transferRecordId)
        }
    }

    @Synchronized
    fun updateTransferState(transferRecordId: Int, newState: TransferState) {
        activeTransferMap[transferRecordId]?.let { transferRecord ->
            if (transferRecord.state == newState || TransferState.isInTerminalState(transferRecord.state)) {
                return
            }
            transferRecord.state = newState
            transferDB.updateState(transferRecord.id, newState)
            if (TransferState.COMPLETED == newState) {
                removeTransferRecord(transferRecord.id)
            }

            transferStatusListenerMap[transferRecord.id]?.forEach { (listener, _) ->
                mainHandler.post {
                    transferRecord.key?.let { key ->
                        listener.onStateChanged(
                            transferRecord.id,
                            newState,
                            key
                        )
                    }
                }
            }

            if (TransferState.isInTerminalState(newState)) {
                unregisterAllListener(transferRecord.id)
            }
        }
    }

    @Synchronized
    fun updateProgress(
        transferRecordId: Int,
        bytesCurrent: Long,
        bytesTotal: Long,
        notifyListener: Boolean,
        updateDB: Boolean = true
    ) {
        activeTransferMap[transferRecordId]?.let {
            it.bytesCurrent = bytesCurrent
            it.bytesTotal = bytesTotal
        }
        if (updateDB) {
            transferDB.updateBytesTransferred(transferRecordId, bytesCurrent, bytesTotal)
        }
        if (notifyListener) {
            transferStatusListenerMap[transferRecordId]?.forEach { (listener, _) ->
                mainHandler.post {
                    listener.onProgressChanged(
                        transferRecordId,
                        bytesCurrent,
                        bytesTotal
                    )
                }
            }
        }
    }

    @Synchronized
    fun updateOnError(transferRecordId: Int, exception: Exception) {
        transferStatusListenerMap[transferRecordId]?.forEach { (listener, _) ->
            mainHandler.post { listener.onError(transferRecordId, exception) }
        }
    }

    @Synchronized
    fun updateMultipartId(id: Int, multipartId: String?) {
        transferDB.updateMultipartId(id, multipartId)
        activeTransferMap[id]?.multipartId = multipartId
    }

    @Synchronized
    fun registerListener(transferRecordId: Int, transferListener: TransferListener) {
        mainHandler.post {
            transferStatusListenerMap[transferRecordId]?.put(transferListener, true) ?: run {
                transferStatusListenerMap[transferRecordId] = ConcurrentHashMap(mapOf(Pair(transferListener, true)))
            }
        }
    }

    @Synchronized
    fun registerMultiPartTransferListener(
        transferRecordId: Int,
        transferListener: MultiPartUploadTaskListener
    ) {
        multiPartTransferStatusListener[transferRecordId] = transferListener
    }

    fun getMultiPartTransferListener(transferRecordId: Int): MultiPartUploadTaskListener? {
        return multiPartTransferStatusListener[transferRecordId]
    }

    @Synchronized
    fun addWorkRequest(workRequestId: String, transferRecordId: Int, isChainedRequest: Boolean) {
        transferWorkInfoIdMap[workRequestId] = transferRecordId
        if (!isChainedRequest) {
            activeTransferMap[transferRecordId]?.apply { workManagerRequestId = workRequestId }
            transferDB.updateWorkManagerRequestId(transferRecordId, workRequestId)
        }
    }

    fun getTransferRecordIdForWorkInfo(workInfoId: String): Int? {
        return transferWorkInfoIdMap[workInfoId]
    }

    fun removeWorkInfoId(workInfoId: String) {
        transferWorkInfoIdMap.remove(workInfoId)
    }

    @Synchronized
    fun unregisterListener(transferRecordId: Int, transferListener: TransferListener) {
        mainHandler.post {
            transferStatusListenerMap[transferRecordId]?.remove(transferListener)
        }
    }

    @Synchronized
    fun unregisterAllListener(transferRecordId: Int) {
        mainHandler.post {
            transferStatusListenerMap.remove(transferRecordId)
            multiPartTransferStatusListener.remove(transferRecordId)
        }
    }
}
