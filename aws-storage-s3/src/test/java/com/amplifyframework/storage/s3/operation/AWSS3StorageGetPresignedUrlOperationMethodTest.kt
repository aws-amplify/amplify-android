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
import com.amplifyframework.storage.StorageAccessLevel
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.s3.StorageAccessMethod
import com.amplifyframework.storage.s3.configuration.AWSS3StoragePluginConfiguration
import com.amplifyframework.storage.s3.request.AWSS3StorageGetPresignedUrlRequest
import com.amplifyframework.storage.s3.service.StorageService
import com.google.common.util.concurrent.MoreExecutors
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

@Suppress("DEPRECATION")
class AWSS3StorageGetPresignedUrlOperationMethodTest {

    private lateinit var storageService: StorageService
    private lateinit var authCredentialsProvider: AuthCredentialsProvider

    @Before
    fun setup() {
        storageService = mockk<StorageService>(relaxed = true)
        authCredentialsProvider = mockk()
    }

    @Test
    fun `GET method calls getPresignedUrl`() {
        // GIVEN
        val key = "photo.jpg"
        val expectedKey = "public/photo.jpg"
        val request = AWSS3StorageGetPresignedUrlRequest(
            key,
            StorageAccessLevel.PUBLIC,
            "",
            10,
            false,
            false,
            StorageAccessMethod.GET
        )
        coEvery { authCredentialsProvider.getIdentityId() } returns "abc"
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        val operation = AWSS3StorageGetPresignedUrlOperation(
            storageService,
            MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider,
            request,
            AWSS3StoragePluginConfiguration {},
            {},
            onError
        )

        // WHEN
        operation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify { storageService.getPresignedUrl(expectedKey, StorageAccessMethod.GET, 10, false) }
    }

    @Test
    fun `PUT method calls getPresignedUrl`() {
        // GIVEN
        val key = "photo.jpg"
        val request = AWSS3StorageGetPresignedUrlRequest(
            key,
            StorageAccessLevel.PUBLIC,
            "",
            10,
            false,
            false,
            StorageAccessMethod.PUT
        )
        coEvery { authCredentialsProvider.getIdentityId() } returns "abc"
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        val operation = AWSS3StorageGetPresignedUrlOperation(
            storageService,
            MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider,
            request,
            AWSS3StoragePluginConfiguration {},
            {},
            onError
        )

        // WHEN
        operation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify { storageService.getPresignedUrl(any(), StorageAccessMethod.PUT, any(), any()) }
    }

    @Test
    fun `PUT method skips validateObjectExistence even when enabled`() {
        // GIVEN
        val key = "photo.jpg"
        val request = AWSS3StorageGetPresignedUrlRequest(
            key,
            StorageAccessLevel.PUBLIC,
            "",
            10,
            false,
            true,
            StorageAccessMethod.PUT
        )
        coEvery { authCredentialsProvider.getIdentityId() } returns "abc"
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        val operation = AWSS3StorageGetPresignedUrlOperation(
            storageService,
            MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider,
            request,
            AWSS3StoragePluginConfiguration {},
            {},
            onError
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
        val key = "photo.jpg"
        val expectedKey = "public/photo.jpg"
        val request = AWSS3StorageGetPresignedUrlRequest(
            key,
            StorageAccessLevel.PUBLIC,
            "",
            10,
            false,
            true,
            StorageAccessMethod.GET
        )
        coEvery { authCredentialsProvider.getIdentityId() } returns "abc"
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        val operation = AWSS3StorageGetPresignedUrlOperation(
            storageService,
            MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider,
            request,
            AWSS3StoragePluginConfiguration {},
            {},
            onError
        )

        // WHEN
        operation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify { storageService.validateObjectExists(expectedKey) }
        verify { storageService.getPresignedUrl(expectedKey, StorageAccessMethod.GET, 10, false) }
    }

    @Test
    fun `PUT method with accelerate endpoint`() {
        // GIVEN
        val key = "photo.jpg"
        val expectedKey = "public/photo.jpg"
        val request = AWSS3StorageGetPresignedUrlRequest(
            key,
            StorageAccessLevel.PUBLIC,
            "",
            10,
            true,
            false,
            StorageAccessMethod.PUT
        )
        coEvery { authCredentialsProvider.getIdentityId() } returns "abc"
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        val operation = AWSS3StorageGetPresignedUrlOperation(
            storageService,
            MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider,
            request,
            AWSS3StoragePluginConfiguration {},
            {},
            onError
        )

        // WHEN
        operation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify { storageService.getPresignedUrl(expectedKey, StorageAccessMethod.PUT, 10, true) }
    }
}
