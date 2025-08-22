/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.storage.s3.request.AWSS3StoragePathGetPresignedUrlRequest
import com.amplifyframework.storage.s3.service.StorageService
import com.google.common.util.concurrent.MoreExecutors
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class AWSS3StoragePathGetUrlOperationTest {

    private lateinit var awsS3StorageGetPresignedUrlOperation: AWSS3StoragePathGetPresignedUrlOperation
    private lateinit var storageService: StorageService
    private lateinit var authCredentialsProvider: AuthCredentialsProvider

    private val expectedExpires = 10

    @Before
    fun setup() {
        storageService = mockk<StorageService>(relaxed = true)
        authCredentialsProvider = mockk()
    }

    @Test
    fun `success string storage path`() {
        // GIVEN
        val path = StoragePath.fromString("public/123")
        val expectedServiceKey = "public/123"
        val request = AWSS3StoragePathGetPresignedUrlRequest(
            path,
            expectedExpires,
            false,
            validateObjectExistence = false
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        awsS3StorageGetPresignedUrlOperation = AWSS3StoragePathGetPresignedUrlOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        awsS3StorageGetPresignedUrlOperation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify {
            storageService.getPresignedUrl(
                expectedServiceKey,
                expectedExpires,
                false
            )
        }
    }

    @Test
    fun `success identityId storage path`() {
        // GIVEN
        coEvery { authCredentialsProvider.getIdentityId() } returns "123"
        val path = StoragePath.fromIdentityId { "protected/$it/picture.jpg" }
        val expectedServiceKey = "protected/123/picture.jpg"
        val request = AWSS3StoragePathGetPresignedUrlRequest(
            path,
            expectedExpires,
            false,
            validateObjectExistence = false
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        awsS3StorageGetPresignedUrlOperation = AWSS3StoragePathGetPresignedUrlOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        awsS3StorageGetPresignedUrlOperation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify {
            storageService.getPresignedUrl(
                expectedServiceKey,
                expectedExpires,
                false
            )
        }
    }

    @Test
    fun `invalid storage path fails with invalid path`() {
        // GIVEN
        coEvery { authCredentialsProvider.getIdentityId() } returns "123"
        val path = StoragePath.fromIdentityId { "/protected/$it/picture.jpg" }
        val request = AWSS3StoragePathGetPresignedUrlRequest(
            path,
            expectedExpires,
            false,
            validateObjectExistence = false
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        awsS3StorageGetPresignedUrlOperation = AWSS3StoragePathGetPresignedUrlOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        awsS3StorageGetPresignedUrlOperation.start()

        // THEN
        verify { onError.accept(StoragePathValidationException.invalidStoragePathException()) }
        verify(exactly = 0) {
            storageService.getPresignedUrl(any(), any(), any())
        }
    }

    @Test
    fun `invalid storage path fails with failed identityId resolution`() {
        // GIVEN
        val expectedException = Exception("test")
        coEvery { authCredentialsProvider.getIdentityId() } throws expectedException
        val path = StoragePath.fromIdentityId { "protected/$it/picture.jpg" }
        val request = AWSS3StoragePathGetPresignedUrlRequest(
            path,
            expectedExpires,
            false,
            validateObjectExistence = false
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        awsS3StorageGetPresignedUrlOperation = AWSS3StoragePathGetPresignedUrlOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        awsS3StorageGetPresignedUrlOperation.start()

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
            storageService.getPresignedUrl(any(), any(), any())
        }
    }

    @Test
    fun `invalid storage path fails with unsupported storage path type`() {
        // GIVEN
        val path = UnsupportedStoragePath()
        val request = AWSS3StoragePathGetPresignedUrlRequest(
            path,
            expectedExpires,
            false,
            validateObjectExistence = false
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        awsS3StorageGetPresignedUrlOperation = AWSS3StoragePathGetPresignedUrlOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        awsS3StorageGetPresignedUrlOperation.start()

        // THEN
        verify { onError.accept(StoragePathValidationException.unsupportedStoragePathException()) }
        verify(exactly = 0) {
            storageService.getPresignedUrl(any(), any(), any())
        }
    }

    @Test
    fun `getPresignedUrl fails with non existent S3 path when validateObjectExistence is enabled`() {
        // GIVEN
        val path = StoragePath.fromString("public/123")
        val expectedException = StorageException("Test", "Test")
        coEvery { storageService.validateObjectExists(any()) } throws expectedException
        val request = AWSS3StoragePathGetPresignedUrlRequest(
            path,
            expectedExpires,
            false,
            validateObjectExistence = true
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        awsS3StorageGetPresignedUrlOperation = AWSS3StoragePathGetPresignedUrlOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        awsS3StorageGetPresignedUrlOperation.start()

        // THEN
        verify(exactly = 1) { onError.accept(expectedException) }
        verify(exactly = 0) {
            storageService.getPresignedUrl(any(), any(), any())
        }
    }

    @Test
    fun `getPresignedUrl succeeds when validateObjectExistence is enabled`() {
        // GIVEN
        val path = StoragePath.fromString("public/123")
        val expectedServiceKey = "public/123"
        val request = AWSS3StoragePathGetPresignedUrlRequest(
            path,
            expectedExpires,
            false,
            validateObjectExistence = true
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        awsS3StorageGetPresignedUrlOperation = AWSS3StoragePathGetPresignedUrlOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        awsS3StorageGetPresignedUrlOperation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify {
            storageService.getPresignedUrl(
                expectedServiceKey,
                expectedExpires,
                false
            )
        }
    }

    class UnsupportedStoragePath : StoragePath()
}
