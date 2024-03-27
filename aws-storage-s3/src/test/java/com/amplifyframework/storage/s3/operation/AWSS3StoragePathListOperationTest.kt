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

package com.amplifyframework.storage.s3.operation

import com.amplifyframework.auth.AuthCredentialsProvider
import com.amplifyframework.core.Consumer
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.StoragePath
import com.amplifyframework.storage.StoragePathValidationException
import com.amplifyframework.storage.s3.extensions.invalidStoragePathException
import com.amplifyframework.storage.s3.extensions.unsupportedStoragePathException
import com.amplifyframework.storage.s3.request.AWSS3StoragePathListRequest
import com.amplifyframework.storage.s3.service.AWSS3StorageService
import com.google.common.util.concurrent.MoreExecutors
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AWSS3StoragePathListOperationTest {

    private lateinit var listOperation: AWSS3StoragePathListOperation
    private lateinit var storageService: AWSS3StorageService
    private lateinit var authCredentialsProvider: AuthCredentialsProvider

    private val expectedPageSize = 10
    private val expectedNextToken = "next"

    @Before
    fun setup() {
        storageService = mockk<AWSS3StorageService>(relaxed = true)
        authCredentialsProvider = mockk()
    }

    @Test
    fun `success string storage path`() {
        // GIVEN
        val path = StoragePath.fromString("public/123")
        val expectedServiceKey = "public/123"
        val request = AWSS3StoragePathListRequest(
            path,
            expectedPageSize,
            expectedNextToken
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        listOperation = AWSS3StoragePathListOperation(
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            request = request,
            {},
            onError
        )

        // WHEN
        listOperation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify {
            storageService.listFiles(
                expectedServiceKey,
                expectedPageSize,
                expectedNextToken
            )
        }
    }

    @Test
    fun `success identityId storage path`() {
        // GIVEN
        coEvery { authCredentialsProvider.getIdentityId() } returns "123"
        val path = StoragePath.fromIdentityId { "protected/$it/picture.jpg" }
        val expectedServiceKey = "protected/123/picture.jpg"
        val request = AWSS3StoragePathListRequest(
            path,
            expectedPageSize,
            expectedNextToken
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        listOperation = AWSS3StoragePathListOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        listOperation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify {
            storageService.listFiles(
                expectedServiceKey,
                expectedPageSize,
                expectedNextToken
            )
        }
    }

    @Test
    fun `invalid storage path fails with invalid path`() {
        // GIVEN
        coEvery { authCredentialsProvider.getIdentityId() } returns "123"
        val path = StoragePath.fromIdentityId { "/protected/$it/picture.jpg" }
        val request = AWSS3StoragePathListRequest(
            path,
            expectedPageSize,
            expectedNextToken
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        listOperation = AWSS3StoragePathListOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        listOperation.start()

        // THEN
        verify { onError.accept(StoragePathValidationException.invalidStoragePathException()) }
        verify(exactly = 0) {
            storageService.listFiles(any(), any(), any())
        }
    }

    @Test
    fun `invalid storage path fails with failed identityId resolution`() {
        // GIVEN
        val expectedException = Exception("test")
        coEvery { authCredentialsProvider.getIdentityId() } throws expectedException
        val path = StoragePath.fromIdentityId { "protected/$it/picture.jpg" }
        val request = AWSS3StoragePathListRequest(
            path,
            expectedPageSize,
            expectedNextToken
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        listOperation = AWSS3StoragePathListOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        listOperation.start()

        // THEN
        verify {
            onError.accept(
                StorageException(
                    "Failed to fetch identity ID",
                    expectedException,
                    "See included exception for more details and suggestions to fix."
                )
            )
        }
        verify(exactly = 0) {
            storageService.listFiles(any(), any(), any())
        }
    }

    @Test
    fun `invalid storage path fails with unsupported storage path type`() {
        // GIVEN
        val path = UnsupportedStoragePath()
        val request = AWSS3StoragePathListRequest(
            path,
            expectedPageSize,
            expectedNextToken
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        listOperation = AWSS3StoragePathListOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        listOperation.start()

        // THEN
        verify { onError.accept(StoragePathValidationException.unsupportedStoragePathException()) }
        verify(exactly = 0) {
            storageService.listFiles(any(), any(), any())
        }
    }

    class UnsupportedStoragePath : StoragePath()
}
