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
package com.amplifyframework.storage.s3.transfer

import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * ProgressListener to track progress for any S3 transfer request.
 **/
interface ProgressListener {
    fun progressChanged(bytesTransferred: Long)
}

/**
 * ProgressListener to track progress for mulitpart S3 upload request.
 **/
internal class MultiPartUploadTaskListener(
    private val transferRecord: TransferRecord,
    private val transferDB: TransferDB,
    private val transferStatusUpdater: TransferStatusUpdater
) :
    ProgressListener {

    private var totalBytesTransferred: AtomicLong = AtomicLong(0L)
    private var progressUpdateSink: AtomicLong = AtomicLong(0L)

    init {
        val previouslyTransferBytes =
            transferDB.queryBytesTransferredByMainUploadId(transferRecord.id)
        totalBytesTransferred.getAndAdd(previouslyTransferBytes)
    }

    @Synchronized
    override fun progressChanged(bytesTransferred: Long) {
        totalBytesTransferred.getAndAdd(bytesTransferred)
        transferRecord.bytesCurrent = totalBytesTransferred.get()
        if (transferRecord.bytesCurrent > transferRecord.bytesTotal) {
            transferRecord.bytesCurrent = transferRecord.bytesTotal
        }
        progressUpdateSink.getAndAdd(bytesTransferred)
        if (progressUpdateSink.get() >=
            min(transferRecord.bytesTotal / 100, transferRecord.bytesTotal - transferRecord.bytesCurrent)
        ) {
            updateProgress(true)
            progressUpdateSink.getAndSet(0L)
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
}

/**
 * ProgressListener to track progress for mulitpart S3 part upload request.
 **/
internal class PartUploadProgressListener(
    private val transferRecord: TransferRecord,
    private val transferStatusUpdater: TransferStatusUpdater
) : ProgressListener {

    private var resetProgress = false

    override fun progressChanged(bytesTransferred: Long) {
        transferRecord.bytesCurrent += bytesTransferred
        transferStatusUpdater.getMultiPartTransferListener(transferRecord.mainUploadId)
            ?.progressChanged(bytesTransferred)
    }

    fun resetProgress() {
        resetProgress = true
        transferStatusUpdater.getMultiPartTransferListener(transferRecord.mainUploadId)
            ?.progressChanged(0L)
    }
}

/**
 * ProgressListener to track progress for single S3 upload request.
 **/
internal class UploadProgressListener(
    private val transferRecord: TransferRecord,
    private val transferStatusUpdater: TransferStatusUpdater
) :
    ProgressListener {
    private var bytesTransferredSoFar = 0L
    private var progressUpdateSink = 0L

    @Synchronized
    override fun progressChanged(byteTransferred: Long) {
        progressUpdateSink += byteTransferred
        bytesTransferredSoFar += byteTransferred
        transferRecord.bytesCurrent = bytesTransferredSoFar
        if (progressUpdateSink >=
            min(transferRecord.bytesTotal / 100, transferRecord.bytesTotal - transferRecord.bytesCurrent)
        ) {
            updateProgress(true)
            progressUpdateSink = 0L
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
}

/**
 * ProgressListener to track progress for S3 download request.
 **/
internal class DownloadProgressListener(
    private val transferRecord: TransferRecord,
    private val transferStatusUpdater: TransferStatusUpdater
) : ProgressListener {
    private var bytesTransferredSoFar = 0L
    private var progressUpdateSink = 0L

    init {
        bytesTransferredSoFar = transferRecord.bytesCurrent
    }

    override fun progressChanged(bytesTransferred: Long) {
        bytesTransferredSoFar += bytesTransferred
        progressUpdateSink += bytesTransferred
        transferRecord.bytesCurrent = bytesTransferredSoFar
        if (transferRecord.bytesCurrent > transferRecord.bytesTotal) {
            transferRecord.bytesCurrent = transferRecord.bytesTotal
        }
        if (progressUpdateSink >=
            min(transferRecord.bytesTotal / 100, transferRecord.bytesTotal - transferRecord.bytesCurrent)
        ) {
            updateProgress()
            progressUpdateSink = 0L
        }
    }

    private fun updateProgress() {
        transferStatusUpdater.updateProgress(
            transferRecord.id,
            transferRecord.bytesCurrent,
            transferRecord.bytesTotal,
            true,
            // writing progress to DB is not required for download operation, as current progress could be inferred from the file length
            updateDB = false
        )
    }
}
