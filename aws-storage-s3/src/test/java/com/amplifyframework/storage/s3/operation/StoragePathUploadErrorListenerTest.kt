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

package com.amplifyframework.storage.s3.operation

import com.amplifyframework.auth.AuthCredentialsProvider
import com.amplifyframework.core.Consumer
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.StoragePath
import com.amplifyframework.storage.operation.StorageTransferOperation
import com.amplifyframework.storage.s3.ServerSideEncryption
import com.amplifyframework.storage.s3.request.AWSS3StoragePathUploadRequest
import com.amplifyframework.storage.s3.service.StorageService
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.ExecutorService
import org.junit.Test

class StoragePathUploadErrorListenerTest {
    @Test
    fun `path resolution failures tolerate detached error listeners`() {
        for (stream in listOf(false, true)) {
            for (identityFailure in listOf(false, true)) {
                for (state in listOf(ListenerState.CLEARED, ListenerState.DETACHED)) {
                    checkPathFailure(stream, identityFailure, state)
                }
            }
        }
    }

    @Test
    fun `path resolution failures preserve attached and initially absent listeners`() {
        for (stream in listOf(false, true)) {
            for (identityFailure in listOf(false, true)) {
                for (state in listOf(ListenerState.ATTACHED, ListenerState.INITIALLY_ABSENT)) {
                    checkPathFailure(stream, identityFailure, state)
                }
            }
        }
    }

    private fun checkPathFailure(stream: Boolean, identityFailure: Boolean, state: ListenerState) {
        // Arrange: defer the upload task until the listener state has been changed.
        val service = mockk<StorageService>()
        val credentials = mockk<AuthCredentialsProvider>()
        coEvery { credentials.getIdentityId() } throws IllegalStateException("Identity unavailable")
        val path = if (identityFailure) {
            StoragePath.fromIdentityId { "private/$it/file" }
        } else {
            StoragePath.fromString("/invalid")
        }
        val task = slot<Runnable>()
        val executor = mockk<ExecutorService>()
        every { executor.submit(capture(task)) } returns mockk(relaxed = true)
        var notified = 0
        val listener = if (state == ListenerState.INITIALLY_ABSENT) {
            null
        } else {
            Consumer<StorageException> { notified++ }
        }
        val operation: StorageTransferOperation<*, *> = if (stream) {
            val request = AWSS3StoragePathUploadRequest<InputStream>(
                path, ByteArrayInputStream(byteArrayOf()), "text/plain", ServerSideEncryption.NONE, emptyMap(), false
            )
            AWSS3StoragePathUploadInputStreamOperation(
                "id", request, service, executor, credentials, onError = listener
            )
        } else {
            val request = AWSS3StoragePathUploadRequest(
                path, File("unused-before-path-validation"), "text/plain", ServerSideEncryption.NONE, emptyMap(), false
            )
            AWSS3StoragePathUploadFileOperation(
                "id", request, service, executor, credentials, onError = listener
            )
        }

        // Act: use the public Java listener APIs before the Kotlin task handles the failure.
        operation.start()
        when (state) {
            ListenerState.CLEARED -> operation.clearAllListeners()
            ListenerState.DETACHED -> operation.setOnError(null)
            else -> Unit
        }
        // Run the captured task directly so ExecutorService.submit cannot hide exceptions in a Future.
        task.captured.run()

        // Assert: detached listeners are skipped and attached listeners still receive the failure.
        notified shouldBe if (state == ListenerState.ATTACHED) 1 else 0
        verify { service wasNot Called }
    }

    private enum class ListenerState {
        ATTACHED,
        INITIALLY_ABSENT,
        CLEARED,
        DETACHED
    }
}
