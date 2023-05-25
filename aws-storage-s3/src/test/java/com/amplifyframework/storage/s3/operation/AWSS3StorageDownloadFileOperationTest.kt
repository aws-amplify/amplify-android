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
import com.amplifyframework.storage.StorageAccessLevel
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.s3.configuration.AWSS3PluginPrefixResolver
import com.amplifyframework.storage.s3.configuration.AWSS3StoragePluginConfiguration
import com.amplifyframework.storage.s3.request.AWSS3StorageDownloadFileRequest
import com.amplifyframework.storage.s3.service.StorageService
import com.google.common.util.concurrent.MoreExecutors
import io.mockk.coEvery
import io.mockk.mockk
import java.io.File
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AWSS3StorageDownloadFileOperationTest {

    private lateinit var awsS3StorageDownloadFileOperation: AWSS3StorageDownloadFileOperation
    private lateinit var storageService: StorageService
    private lateinit var authCredentialsProvider: AuthCredentialsProvider

    @Before
    fun setup() {
        storageService = Mockito.spy(StorageService::class.java)
        authCredentialsProvider = mockk()
    }

    @Test
    fun defaultPrefixResolverAWSS3PluginConfigTest() {
        val key = "123"
        val tempFile = File.createTempFile("new", "file.tmp")
        val expectedKey = "public/123"
        val request: AWSS3StorageDownloadFileRequest = AWSS3StorageDownloadFileRequest(
            key,
            tempFile,
            StorageAccessLevel.PUBLIC,
            null,
            false
        )
        coEvery { authCredentialsProvider.getIdentityId() } returns "abc"
        awsS3StorageDownloadFileOperation = AWSS3StorageDownloadFileOperation(
            storageService,
            MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider,
            request,
            AWSS3StoragePluginConfiguration {},
            {},
            {},
            {}
        )
        awsS3StorageDownloadFileOperation.start()
        Mockito.verify(storageService).downloadToFile(
            awsS3StorageDownloadFileOperation.transferId,
            expectedKey,
            tempFile,
            false
        )
    }

    @Test
    fun customEmptyPrefixResolverAWSS3PluginConfigTest() {
        val key = "123"
        val tempFile = File.createTempFile("new", "file.tmp")
        val expectedKey = "123"
        val request: AWSS3StorageDownloadFileRequest = AWSS3StorageDownloadFileRequest(
            key,
            tempFile,
            StorageAccessLevel.PUBLIC,
            null,
            false
        )
        coEvery { authCredentialsProvider.getIdentityId() } returns "abc"
        awsS3StorageDownloadFileOperation = AWSS3StorageDownloadFileOperation(
            storageService,
            MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider,
            request,
            AWSS3StoragePluginConfiguration {
                awsS3PluginPrefixResolver = object : AWSS3PluginPrefixResolver {
                    override fun resolvePrefix(
                        accessLevel: StorageAccessLevel,
                        targetIdentity: String?,
                        onSuccess: Consumer<String>,
                        onError: Consumer<StorageException>?
                    ) {
                        onSuccess.accept("")
                    }
                }
            },
            {},
            {},
            {}
        )
        awsS3StorageDownloadFileOperation.start()
        Mockito.verify(storageService).downloadToFile(
            awsS3StorageDownloadFileOperation.transferId,
            expectedKey,
            tempFile,
            false
        )
    }

    @Test
    fun customPrefixResolverAWSS3PluginConfigTest() {
        val key = "123"
        val tempFile = File.createTempFile("new", "file.tmp")
        val expectedKey = "customPublic/123"
        val request: AWSS3StorageDownloadFileRequest = AWSS3StorageDownloadFileRequest(
            key,
            tempFile,
            StorageAccessLevel.PUBLIC,
            null,
            false
        )
        coEvery { authCredentialsProvider.getIdentityId() } returns "abc"
        awsS3StorageDownloadFileOperation = AWSS3StorageDownloadFileOperation(
            storageService,
            MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider,
            request,
            AWSS3StoragePluginConfiguration {
                awsS3PluginPrefixResolver = object : AWSS3PluginPrefixResolver {
                    override fun resolvePrefix(
                        accessLevel: StorageAccessLevel,
                        targetIdentity: String?,
                        onSuccess: Consumer<String>,
                        onError: Consumer<StorageException>?
                    ) {
                        onSuccess.accept("customPublic/")
                    }
                }
            },
            {},
            {},
            {}
        )
        awsS3StorageDownloadFileOperation.start()
        Mockito.verify(storageService).downloadToFile(
            awsS3StorageDownloadFileOperation.transferId,
            expectedKey,
            tempFile,
            false
        )
    }
}
