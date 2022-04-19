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
import android.os.Handler
import android.os.Looper
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * Updates transfer status to observers and to local DB
 **/
internal class TransferStatusUpdater private constructor(
    private val transferDB: TransferDB
) {
    private val logger =
        Amplify.Logging.forNamespace(
            AWSS3StoragePlugin.AWS_S3_STORAGE_LOG_NAMESPACE.format(this::class.java.simpleName)
        )
    private val mainHandler = Handler(Looper.getMainLooper())
    private val transferStatusListenerMap:
        MutableMap<Int, MutableList<WeakReference<TransferListener>>> by lazy { ConcurrentHashMap() }
    private val transferWorkInfoIdMap: MutableMap<String, Int> by lazy { ConcurrentHashMap() }
    private val multiPartTransferStatusListener: MutableMap<Int, MultiPartUploadTaskListener> by lazy {
        ConcurrentHashMap()
    }
    val activeTransferMap = object : AbstractMutableMap<Int, TransferRecord>() {

        val transferRecordMap = mutableMapOf<Int, TransferRecord>()

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

        @JvmStatic
        fun getInstance(context: Context): TransferStatusUpdater {
            return TransferStatusUpdater(TransferDB.getInstance(context))
        }
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
        transferDB.updateState(transferRecordId, newState).takeIf { it == 0 }?.let {
            logger.error("Failed to update, transferRecord $transferRecordId not found")
        }
        activeTransferMap[transferRecordId]?.apply {
            state = newState
        }

        if (TransferState.COMPLETED == newState) {
            removeTransferRecord(transferRecordId)
        }

        transferStatusListenerMap[transferRecordId]?.forEach { listener ->
            run {
                mainHandler.post { listener.get()?.onStateChanged(transferRecordId, newState) }
            }
        }

        if (TransferState.isInTerminalState(newState)) {
            unregisterAllListener(transferRecordId)
        }
    }

    @Synchronized
    fun updateProgress(
        transferRecordId: Int,
        bytesCurrent: Long,
        bytesTotal: Long,
        notifyListener: Boolean
    ) {
        activeTransferMap[transferRecordId]?.let {
            it.bytesCurrent = bytesCurrent
            it.bytesTotal = bytesTotal
        }
        transferDB.updateBytesTransferred(transferRecordId, bytesCurrent, bytesTotal)
        if (notifyListener) {
            transferStatusListenerMap[transferRecordId]?.forEach {
                mainHandler.post {
                    it.get()?.onProgressChanged(
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
        transferStatusListenerMap[transferRecordId]?.forEach {
            mainHandler.post { it.get()?.onError(transferRecordId, exception) }
        }
    }

    @Synchronized
    fun updateMultipartId(id: Int, multipartId: String?) {
        transferDB.updateMultipartId(id, multipartId)
        activeTransferMap[id]?.multipartId = multipartId
    }

    @Synchronized
    fun registerListener(transferRecordId: Int, transferListener: TransferListener) {
        transferStatusListenerMap[transferRecordId]?.let {
            val weakRefTransferListener = WeakReference(transferListener)
            if (!it.contains(weakRefTransferListener)) {
                it.add(weakRefTransferListener)
            }
        } ?: run {
            transferStatusListenerMap[transferRecordId] =
                mutableListOf(WeakReference(transferListener))
        }
    }

    @Synchronized
    fun registerMultiPartTransferListener(
        transferRecordId: Int,
        transferListener: MultiPartUploadTaskListener
    ) {
        if (!multiPartTransferStatusListener.containsKey(transferRecordId)) {
            multiPartTransferStatusListener[transferRecordId] = transferListener
        }
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

    @Synchronized
    fun unregisterListener(transferRecordId: Int, transferListener: TransferListener) {
        transferStatusListenerMap[transferRecordId]?.remove(WeakReference(transferListener))
    }

    @Synchronized
    fun unregisterAllListener(transferRecordId: Int) {
        mainHandler.post {
            transferStatusListenerMap.remove(transferRecordId)
            multiPartTransferStatusListener.remove(transferRecordId)
        }
    }
}
