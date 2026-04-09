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
import com.amplifyframework.storage.result.StorageGetUrlResult
import com.amplifyframework.storage.s3.StorageAccessMethod
import com.amplifyframework.storage.s3.request.AWSS3StoragePathGetPresignedUrlRequest
import com.amplifyframework.storage.s3.service.StorageService
import com.google.common.util.concurrent.MoreExecutors
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import java.net.URL
import org.junit.Before
import org.junit.Test

class AWSS3StoragePathGetUrlOperationMethodTest {

    private lateinit var storageService: StorageService
    private lateinit var authCredentialsProvider: AuthCredentialsProvider

    private val expectedExpires = 10
    private val testUrl = URL("https://test-bucket.s3.amazonaws.com/test-key")

    @Before
    fun setup() {
        storageService = mockk<StorageService>(relaxed = true)
        authCredentialsProvider = mockk()
    }

    @Test
    fun `GET method calls getPresignedUrl`() {
        // GIVEN
        val path = StoragePath.fromString("public/photo.jpg")
        val expectedServiceKey = "public/photo.jpg"
        val request = AWSS3StoragePathGetPresignedUrlRequest(
            path,
            expectedExpires,
            useAccelerateEndpoint = false,
            validateObjectExistence = false,
            method = StorageAccessMethod.GET
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        val operation = AWSS3StoragePathGetPresignedUrlOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        operation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify { storageService.getPresignedUrl(expectedServiceKey, StorageAccessMethod.GET, expectedExpires, false) }
    }

    @Test
    fun `PUT method calls getPresignedUrl`() {
        // GIVEN
        val path = StoragePath.fromString("uploads/photo.jpg")
        val expectedServiceKey = "uploads/photo.jpg"
        val request = AWSS3StoragePathGetPresignedUrlRequest(
            path,
            expectedExpires,
            useAccelerateEndpoint = false,
            validateObjectExistence = false,
            method = StorageAccessMethod.PUT
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        val operation = AWSS3StoragePathGetPresignedUrlOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        operation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify { storageService.getPresignedUrl(expectedServiceKey, StorageAccessMethod.PUT, expectedExpires, false) }
    }

    @Test
    fun `PUT method skips validateObjectExistence even when enabled`() {
        // GIVEN
        val path = StoragePath.fromString("uploads/photo.jpg")
        val request = AWSS3StoragePathGetPresignedUrlRequest(
            path,
            expectedExpires,
            useAccelerateEndpoint = false,
            validateObjectExistence = true,
            method = StorageAccessMethod.PUT
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        val operation = AWSS3StoragePathGetPresignedUrlOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        operation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify(exactly = 0) { storageService.validateObjectExists(any()) }
        verify { storageService.getPresignedUrl(any(), any(), any(), any()) }
    }

    @Test
    fun `GET method still validates object existence when enabled`() {
        // GIVEN
        val path = StoragePath.fromString("public/photo.jpg")
        val expectedServiceKey = "public/photo.jpg"
        val request = AWSS3StoragePathGetPresignedUrlRequest(
            path,
            expectedExpires,
            useAccelerateEndpoint = false,
            validateObjectExistence = true,
            method = StorageAccessMethod.GET
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        val operation = AWSS3StoragePathGetPresignedUrlOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        operation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify { storageService.validateObjectExists(expectedServiceKey) }
        verify { storageService.getPresignedUrl(expectedServiceKey, StorageAccessMethod.GET, expectedExpires, false) }
    }

    @Test
    fun `PUT method with accelerate endpoint`() {
        // GIVEN
        val path = StoragePath.fromString("uploads/photo.jpg")
        val expectedServiceKey = "uploads/photo.jpg"
        val request = AWSS3StoragePathGetPresignedUrlRequest(
            path,
            expectedExpires,
            useAccelerateEndpoint = true,
            validateObjectExistence = false,
            method = StorageAccessMethod.PUT
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        val operation = AWSS3StoragePathGetPresignedUrlOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        operation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify { storageService.getPresignedUrl(expectedServiceKey, StorageAccessMethod.PUT, expectedExpires, true) }
    }

    @Test
    fun `PUT method returns success result`() {
        // GIVEN
        val path = StoragePath.fromString("uploads/photo.jpg")
        coEvery { storageService.getPresignedUrl(any(), any(), any(), any()) } returns testUrl
        val request = AWSS3StoragePathGetPresignedUrlRequest(
            path,
            expectedExpires,
            useAccelerateEndpoint = false,
            validateObjectExistence = false,
            method = StorageAccessMethod.PUT
        )
        val onSuccess = mockk<Consumer<StorageGetUrlResult>>(relaxed = true)
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        val operation = AWSS3StoragePathGetPresignedUrlOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = onSuccess,
            onError = onError
        )

        // WHEN
        operation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify { onSuccess.accept(any()) }
    }

    @Test
    fun `default method is GET when not specified`() {
        // GIVEN
        val path = StoragePath.fromString("public/photo.jpg")
        val expectedServiceKey = "public/photo.jpg"
        val request = AWSS3StoragePathGetPresignedUrlRequest(
            path,
            expectedExpires,
            useAccelerateEndpoint = false,
            validateObjectExistence = false,
            method = StorageAccessMethod.GET
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        val operation = AWSS3StoragePathGetPresignedUrlOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        operation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify { storageService.getPresignedUrl(expectedServiceKey, StorageAccessMethod.GET, expectedExpires, false) }
    }

    @Test
    fun `PUT method with identityId storage path`() {
        // GIVEN
        coEvery { authCredentialsProvider.getIdentityId() } returns "user123"
        val path = StoragePath.fromIdentityId { "protected/$it/photo.jpg" }
        val expectedServiceKey = "protected/user123/photo.jpg"
        val request = AWSS3StoragePathGetPresignedUrlRequest(
            path,
            expectedExpires,
            useAccelerateEndpoint = false,
            validateObjectExistence = false,
            method = StorageAccessMethod.PUT
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        val operation = AWSS3StoragePathGetPresignedUrlOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            onSuccess = {},
            onError = onError
        )

        // WHEN
        operation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify { storageService.getPresignedUrl(expectedServiceKey, StorageAccessMethod.PUT, expectedExpires, false) }
    }
}
