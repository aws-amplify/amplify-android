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
import com.amplifyframework.storage.ObjectMetadata
import com.amplifyframework.storage.StorageAccessLevel
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.s3.ServerSideEncryption
import com.amplifyframework.storage.s3.configuration.AWSS3PluginPrefixResolver
import com.amplifyframework.storage.s3.configuration.AWSS3StoragePluginConfiguration
import com.amplifyframework.storage.s3.request.AWSS3StorageUploadRequest
import com.amplifyframework.storage.s3.service.StorageService
import com.amplifyframework.storage.s3.transfer.TransferObserver
import com.google.common.util.concurrent.MoreExecutors
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.File
import java.io.InputStream

class AWSS3StorageUploadInputStreamOperationTest {

    private lateinit var inputStreamOperation: AWSS3StorageUploadInputStreamOperation
    private lateinit var storageService: StorageService
    private lateinit var authCredentialsProvider: AuthCredentialsProvider

    @Before
    fun setup() {
        storageService = Mockito.spy(StorageService::class.java)
        authCredentialsProvider = mockk<AuthCredentialsProvider>()
    }

    @Test
    fun defaultPrefixResolverAWSS3PluginConfigTest() {
        val key = "123"
        val expectedKey = "public/123"
        val tempInputStream = File.createTempFile("new", "file.tmp").inputStream()
        coEvery { authCredentialsProvider.getIdentityId() } returns "abc"
        Mockito.`when`(
            storageService.uploadInputStream(
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
            )
        ).thenReturn(Mockito.mock(TransferObserver::class.java))
        val request = AWSS3StorageUploadRequest<InputStream>(
            key,
            tempInputStream,
            StorageAccessLevel.PUBLIC,
            "",
            "/image",
            ServerSideEncryption.NONE,
            mutableMapOf()
        )
        inputStreamOperation = AWSS3StorageUploadInputStreamOperation(
            storageService,
            MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider,
            request,
            AWSS3StoragePluginConfiguration {},
            {},
            {},
            {}
        )
        inputStreamOperation.start()
        Mockito.verify(storageService).uploadInputStream(
            Mockito.eq(expectedKey),
            Mockito.eq(tempInputStream),
            Mockito.any(ObjectMetadata::class.java)
        )
    }

    @Test
    fun customEmptyPrefixResolverAWSS3PluginConfigTest() {
        val key = "123"
        val expectedKey = "123"
        val tempInputStream = File.createTempFile("new", "file.tmp").inputStream()
        coEvery { authCredentialsProvider.getIdentityId() } returns "abc"
        Mockito.`when`(
            storageService.uploadInputStream(
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
            )
        )
            .thenReturn(Mockito.mock(TransferObserver::class.java))
        val request = AWSS3StorageUploadRequest<InputStream>(
            key,
            tempInputStream,
            StorageAccessLevel.PUBLIC,
            "",
            "/image",
            ServerSideEncryption.NONE,
            mutableMapOf()
        )
        inputStreamOperation = AWSS3StorageUploadInputStreamOperation(
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
                        onError: Consumer<StorageException>
                    ) {
                        onSuccess.accept("")
                    }
                }
            },
            {},
            {},
            {}
        )
        inputStreamOperation.start()
        Mockito.verify(storageService).uploadInputStream(
            Mockito.eq(expectedKey),
            Mockito.eq(tempInputStream),
            Mockito.any(ObjectMetadata::class.java)
        )
    }

    @Test
    fun customPrefixResolverAWSS3PluginConfigTest() {
        val key = "123"
        val expectedKey = "publicCustom/123"
        val tempInputStream = File.createTempFile("new", "file.tmp").inputStream()
        coEvery { authCredentialsProvider.getIdentityId() } returns "abc"
        Mockito.`when`(
            storageService.uploadInputStream(
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
            )
        ).thenReturn(Mockito.mock(TransferObserver::class.java))
        val request = AWSS3StorageUploadRequest<InputStream>(
            key,
            tempInputStream,
            StorageAccessLevel.PUBLIC,
            "",
            "/image",
            ServerSideEncryption.NONE,
            mutableMapOf()
        )
        inputStreamOperation = AWSS3StorageUploadInputStreamOperation(
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
                        onError: Consumer<StorageException>
                    ) {
                        onSuccess.accept("publicCustom/")
                    }
                }
            },
            {},
            {},
            {}
        )
        inputStreamOperation.start()
        Mockito.verify(storageService).uploadInputStream(
            Mockito.eq(expectedKey),
            Mockito.eq(tempInputStream),
            Mockito.any(ObjectMetadata::class.java)
        )
    }
}
