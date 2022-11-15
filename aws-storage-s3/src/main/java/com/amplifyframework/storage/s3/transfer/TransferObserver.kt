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

import com.amplifyframework.storage.TransferState

/**
 * Observer to observe update for a transfer
 **/
internal data class TransferObserver @JvmOverloads constructor(
    val id: Int,
    private val transferStatusUpdater: TransferStatusUpdater,
    val bucket: String? = null,
    val key: String? = null,
    val filePath: String? = null,
    private val listener: TransferListener? = null,
    var transferState: TransferState = TransferState.WAITING
) {

    var bytesTransferred: Long = 0L
    var totalBytes: Long = 0L
    private var transferStatusListener: TransferStatusListener?
    private var transferListener: TransferListener? = listener

    init {
        transferStatusListener = TransferStatusListener()
        listener?.let { setTransferListener(it) }
    }

    fun setTransferListener(listener: TransferListener) {
        clearTransferListener()
        transferListener = listener
        transferStatusUpdater.registerListener(id, listener)
        if (transferStatusListener == null) {
            transferStatusListener = TransferStatusListener()
        }
        transferStatusListener?.let { transferStatusUpdater.registerListener(id, it) }
    }

    private fun clearTransferListener() {
        transferListener?.let {
            transferStatusUpdater.unregisterListener(id, it)
        }
        transferStatusListener?.let {
            transferStatusUpdater.unregisterListener(id, it)
        }
        transferStatusListener = null
        transferListener = null
    }

    private inner class TransferStatusListener : TransferListener {
        override fun onError(id: Int, ex: Exception) {
            listener?.onError(id, ex)
        }

        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
            bytesTransferred = bytesCurrent
            totalBytes = bytesTotal
        }

        override fun onStateChanged(id: Int, state: TransferState, key: String) {
            transferState = state
        }
    }
}
