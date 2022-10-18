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
 * Listener interface for transfer state and progress changes. All callbacks
 * will be invoked on the main thread.
 */
interface TransferListener {
    /**
     * Called when the state of the transfer is changed.
     *
     * @param id The id of the transfer record.
     * @param state The new state of the transfer.
     */
    fun onStateChanged(id: Int, state: TransferState)

    /**
     * Called when more bytes are transferred.
     *
     * @param id The id of the transfer record.
     * @param bytesCurrent Bytes transferred currently.
     * @param bytesTotal The total bytes to be transferred.
     */
    fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long)

    /**
     * Called when an exception happens.
     *
     * @param id The id of the transfer record.
     * @param ex An exception object.
     */
    fun onError(id: Int, ex: Exception)
}
