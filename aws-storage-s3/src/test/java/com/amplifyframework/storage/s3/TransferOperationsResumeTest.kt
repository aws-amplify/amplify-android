/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferRecord
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater
import com.amplifyframework.storage.s3.transfer.TransferType
import com.amplifyframework.storage.s3.transfer.TransferWorkerObserver
import com.amplifyframework.storage.s3.transfer.worker.BaseTransferWorker
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

class TransferOperationsResumeTest {

    /**
     * Single-part upload resume must propagate the plugin-level default progress-stall timeout
     * into the enqueued [androidx.work.WorkRequest] so the worker can re-arm stall detection.
     * Without this, resumed uploads silently lose the stall-cancel behavior the developer
     * configured on the plugin (parity with the iOS Amplify Storage plugin).
     *
     * - Given: a paused single-part upload `TransferRecord` and a `progressStallTimeoutSeconds`
     *   value of 42 carried from the plugin configuration
     * - When: `TransferOperations.resume(...)` is invoked
     * - Then: the work data enqueued for the worker contains
     *   `PROGRESS_STALL_TIMEOUT_SECONDS = 42`
     */
    @Test
    fun `resume single-part upload propagates plugin default progressStallTimeoutSeconds`() {
        val transferRecord = TransferRecord(
            id = 1,
            transferId = "transfer-id",
            isMultipart = 0,
            type = TransferType.UPLOAD,
            state = TransferState.PAUSED,
            bucketName = "bucket",
            region = "us-east-1",
            key = "key",
            file = "file"
        )
        val workManager = mockk<WorkManager>(relaxed = true) {
            every { enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>()) } returns
                mockk(relaxed = true)
        }
        val transferStatusUpdater = mockk<TransferStatusUpdater>(relaxed = true)
        val transferDB = mockk<TransferDB>(relaxed = true)
        val workerObserver = mockk<TransferWorkerObserver>(relaxed = true)

        val workRequestSlot = slot<OneTimeWorkRequest>()
        every {
            workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), capture(workRequestSlot))
        } returns mockk(relaxed = true)

        val resumed = TransferOperations.resume(
            transferRecord,
            "pluginKey",
            transferStatusUpdater,
            workManager,
            workerObserver,
            transferDB,
            42L
        )

        resumed shouldBe true
        verify {
            workManager.enqueueUniqueWork(
                transferRecord.id.toString(),
                ExistingWorkPolicy.KEEP,
                any<OneTimeWorkRequest>()
            )
        }
        val workData = workRequestSlot.captured.workSpec.input
        workData.getLong(BaseTransferWorker.PROGRESS_STALL_TIMEOUT_SECONDS, -1L) shouldBe 42L
    }

    /**
     * Validates the legacy/backward-compatible call path where no plugin default is supplied. The
     * default `0L` must continue to flow through the resume path so existing callers (and
     * downloads, which never opted into stall detection) keep their pre-feature behavior.
     *
     * - Given: a paused single-part upload `TransferRecord` and the default
     *   `progressStallTimeoutSeconds` parameter (i.e. `0L`)
     * - When: `TransferOperations.resume(...)` is invoked without a stall-timeout argument
     * - Then: the enqueued work data carries `PROGRESS_STALL_TIMEOUT_SECONDS = 0`
     */
    @Test
    fun `resume without plugin default keeps zero seconds for backward compatibility`() {
        val transferRecord = TransferRecord(
            id = 7,
            transferId = "transfer-id-7",
            isMultipart = 0,
            type = TransferType.UPLOAD,
            state = TransferState.PAUSED,
            bucketName = "bucket",
            region = "us-east-1",
            key = "key",
            file = "file"
        )
        val workManager = mockk<WorkManager>(relaxed = true)
        val workRequestSlot = slot<OneTimeWorkRequest>()
        every {
            workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), capture(workRequestSlot))
        } returns mockk(relaxed = true)

        val resumed = TransferOperations.resume(
            transferRecord,
            "pluginKey",
            mockk(relaxed = true),
            workManager,
            mockk(relaxed = true),
            mockk(relaxed = true)
        )

        resumed shouldBe true
        val workData = workRequestSlot.captured.workSpec.input
        workData.getLong(BaseTransferWorker.PROGRESS_STALL_TIMEOUT_SECONDS, -1L) shouldBe 0L
    }
}
