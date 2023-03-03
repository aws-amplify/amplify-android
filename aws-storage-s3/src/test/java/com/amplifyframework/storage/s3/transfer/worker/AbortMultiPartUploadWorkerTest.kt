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
package com.amplifyframework.storage.s3.transfer.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.AbortMultipartUploadRequest
import aws.sdk.kotlin.services.s3.model.AbortMultipartUploadResponse
import aws.sdk.kotlin.services.s3.withConfig
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferRecord
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class AbortMultiPartUploadWorkerTest {
    private lateinit var context: Context
    private lateinit var s3Client: S3Client
    private lateinit var transferDB: TransferDB
    private lateinit var transferStatusUpdater: TransferStatusUpdater
    private lateinit var workerParameters: WorkerParameters

    @Before
    fun setup() {

        context = ApplicationProvider.getApplicationContext()
        workerParameters = mockk(WorkerParameters::class.java.name)
        s3Client = spyk<S3Client>(recordPrivateCalls = true)
        mockkStatic(S3Client::withConfig)
        transferDB = mockk(TransferDB::class.java.name)
        transferStatusUpdater = mockk(TransferStatusUpdater::class.java.name)
        every { workerParameters.inputData }.answers { workDataOf(BaseTransferWorker.TRANSFER_RECORD_ID to 1) }
        every { workerParameters.runAttemptCount }.answers { 1 }
        every { workerParameters.taskExecutor }.answers { ImmediateTaskExecutor() }
        every { any<S3Client>().withConfig(any()) }.answers { s3Client }
    }

    @After
    fun tearDown() {
        unmockkStatic(S3Client::withConfig)
    }

    @Test
    fun testAbortWithSuccessOnFailure() = runTest {
        val transferRecord = TransferRecord(
            1,
            UUID.randomUUID().toString(),
            bucketName = "bucket_name",
            key = "key",
            multipartId = "upload_id"
        )
        val abortRequest = AbortMultipartUploadRequest {
            bucket = "bucket_name"
            key = "key"
            uploadId = "upload_id"
        }
        coEvery { s3Client.abortMultipartUpload(abortRequest) }.answers {
            mockk(
                AbortMultipartUploadResponse::class.java.name
            )
        }
        every { transferDB.getTransferRecordById(any()) }.answers { transferRecord }
        every { transferStatusUpdater.updateTransferState(any(), any()) }.answers { }

        val worker = AbortMultiPartUploadWorker(s3Client, transferDB, transferStatusUpdater, context, workerParameters)
        val result = worker.doWork()

        val expectedResult =
            ListenableWorker.Result.success(workDataOf(BaseTransferWorker.OUTPUT_TRANSFER_RECORD_ID to 1))
        verify(exactly = 1) { transferStatusUpdater.updateTransferState(1, TransferState.FAILED) }
        verify(exactly = 1) { any<S3Client>().withConfig(any()) }
        assertEquals(expectedResult, result)
    }

    @Test
    fun testAbortWithSuccessOnCancel() = runTest {
        val transferRecord = TransferRecord(
            1,
            UUID.randomUUID().toString(),
            bucketName = "bucket_name",
            key = "key",
            multipartId = "upload_id",
            state = TransferState.PENDING_CANCEL
        )
        val abortRequest = AbortMultipartUploadRequest {
            bucket = "bucket_name"
            key = "key"
            uploadId = "upload_id"
        }
        coEvery { s3Client.abortMultipartUpload(abortRequest) }.answers {
            mockk(
                AbortMultipartUploadResponse::class.java.name
            )
        }
        every { transferDB.getTransferRecordById(any()) }.answers { transferRecord }
        every { transferStatusUpdater.updateTransferState(any(), any()) }.answers { }

        val worker = AbortMultiPartUploadWorker(s3Client, transferDB, transferStatusUpdater, context, workerParameters)
        val result = worker.doWork()

        val expectedResult =
            ListenableWorker.Result.success(workDataOf(BaseTransferWorker.OUTPUT_TRANSFER_RECORD_ID to 1))
        verify(exactly = 1) { transferStatusUpdater.updateTransferState(1, TransferState.CANCELED) }
        assertEquals(expectedResult, result)
    }

    @Test
    fun testAbortOnError() = runTest {
        val transferRecord = TransferRecord(
            1,
            UUID.randomUUID().toString(),
            bucketName = "bucket_name",
            key = "key",
            multipartId = "upload_id"
        )
        val abortRequest = AbortMultipartUploadRequest {
            bucket = "bucket_name"
            key = "key"
            uploadId = "upload_id"
        }
        val exception = IllegalArgumentException()
        coEvery { s3Client.abortMultipartUpload(abortRequest) }.answers { throw exception }
        every { transferDB.getTransferRecordById(any()) }.answers { transferRecord }
        every { transferStatusUpdater.updateTransferState(any(), any()) }.answers { }
        every { transferStatusUpdater.updateOnError(any(), any()) }.answers { }

        val worker = AbortMultiPartUploadWorker(s3Client, transferDB, transferStatusUpdater, context, workerParameters)
        val result = worker.doWork()

        val expectedResult =
            ListenableWorker.Result.failure(workDataOf(BaseTransferWorker.OUTPUT_TRANSFER_RECORD_ID to 1))
        verify(exactly = 1) { transferStatusUpdater.updateTransferState(1, TransferState.FAILED) }
        verify(exactly = 1) { transferStatusUpdater.updateOnError(1, any()) }
        assertEquals(expectedResult, result)
    }
}
