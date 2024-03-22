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
import com.amplifyframework.storage.s3.ServerSideEncryption
import com.amplifyframework.storage.s3.extensions.invalidStoragePathException
import com.amplifyframework.storage.s3.extensions.unsupportedStoragePathException
import com.amplifyframework.storage.s3.request.AWSS3StoragePathUploadRequest
import com.amplifyframework.storage.s3.service.StorageService
import com.google.common.util.concurrent.MoreExecutors
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import org.junit.Before
import org.junit.Test

class AWSS3StoragePathUploadFileOperationTest {

    private lateinit var awsS3StorageUploadFileOperation: AWSS3StoragePathUploadFileOperation
    private lateinit var storageService: StorageService
    private lateinit var authCredentialsProvider: AuthCredentialsProvider

    @Before
    fun setup() {
        storageService = mockk<StorageService>(relaxed = true)
        authCredentialsProvider = mockk()
    }

    @Test
    fun `success string storage path`() {
        // GIVEN
        val path = StoragePath.fromString("public/123")
        val tempFile = File.createTempFile("new", "file.tmp")
        val expectedServiceKey = "public/123"
        val request = AWSS3StoragePathUploadRequest(
            path,
            tempFile,
            "/image",
            ServerSideEncryption.NONE,
            emptyMap(),
            false
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        awsS3StorageUploadFileOperation = AWSS3StoragePathUploadFileOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            {},
            {},
            onError
        )

        // WHEN
        awsS3StorageUploadFileOperation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify {
            storageService.uploadFile(
                awsS3StorageUploadFileOperation.transferId,
                expectedServiceKey,
                tempFile,
                any(),
                false
            )
        }
    }

    @Test
    fun `success identityId storage path`() {
        // GIVEN
        coEvery { authCredentialsProvider.getIdentityId() } returns "123"
        val path = StoragePath.fromIdentityId { "protected/$it/picture.jpg" }
        val tempFile = File.createTempFile("new", "file.tmp")
        val expectedServiceKey = "protected/123/picture.jpg"
        val request = AWSS3StoragePathUploadRequest(
            path,
            tempFile,
            "/image",
            ServerSideEncryption.NONE,
            emptyMap(),
            false
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        awsS3StorageUploadFileOperation = AWSS3StoragePathUploadFileOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            {},
            {},
            onError
        )

        // WHEN
        awsS3StorageUploadFileOperation.start()

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify {
            storageService.uploadFile(
                awsS3StorageUploadFileOperation.transferId,
                expectedServiceKey,
                tempFile,
                any(),
                false
            )
        }
    }

    @Test
    fun `invalid storage path fails with invalid path`() {
        // GIVEN
        coEvery { authCredentialsProvider.getIdentityId() } returns "123"
        val path = StoragePath.fromIdentityId { "/protected/$it/picture.jpg" }
        val tempFile = File.createTempFile("new", "file.tmp")
        val request = AWSS3StoragePathUploadRequest(
            path,
            tempFile,
            "/image",
            ServerSideEncryption.NONE,
            emptyMap(),
            false
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        awsS3StorageUploadFileOperation = AWSS3StoragePathUploadFileOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            {},
            {},
            onError
        )

        // WHEN
        awsS3StorageUploadFileOperation.start()

        // THEN
        verify { onError.accept(StoragePathValidationException.invalidStoragePathException()) }
        verify(exactly = 0) {
            storageService.uploadFile(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `invalid storage path fails with failed identityId resolution`() {
        // GIVEN
        val expectedException = Exception("test")
        coEvery { authCredentialsProvider.getIdentityId() } throws expectedException
        val path = StoragePath.fromIdentityId { "protected/$it/picture.jpg" }
        val tempFile = File.createTempFile("new", "file.tmp")
        val request = AWSS3StoragePathUploadRequest(
            path,
            tempFile,
            "/image",
            ServerSideEncryption.NONE,
            emptyMap(),
            false
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        awsS3StorageUploadFileOperation = AWSS3StoragePathUploadFileOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            {},
            {},
            onError
        )

        // WHEN
        awsS3StorageUploadFileOperation.start()

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
            storageService.uploadFile(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `invalid storage path fails with unsupported storage path type`() {
        // GIVEN
        val path = UnsupportedStoragePath()
        val tempFile = File.createTempFile("new", "file.tmp")
        val request = AWSS3StoragePathUploadRequest(
            path,
            tempFile,
            "/image",
            ServerSideEncryption.NONE,
            emptyMap(),
            false
        )
        val onError = mockk<Consumer<StorageException>>(relaxed = true)
        awsS3StorageUploadFileOperation = AWSS3StoragePathUploadFileOperation(
            request = request,
            storageService = storageService,
            executorService = MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider = authCredentialsProvider,
            {},
            {},
            onError
        )

        // WHEN
        awsS3StorageUploadFileOperation.start()

        // THEN
        verify { onError.accept(StoragePathValidationException.unsupportedStoragePathException()) }
        verify(exactly = 0) {
            storageService.downloadToFile(any(), any(), any(), any())
        }
    }

    class UnsupportedStoragePath : StoragePath()
}
