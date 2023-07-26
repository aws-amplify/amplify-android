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
import com.google.common.util.concurrent.MoreExecutors
import java.io.File
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.eq
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AWSS3StorageUploadFileOperationTest {

    private lateinit var awsS3StorageUploadFileOperation: AWSS3StorageUploadFileOperation
    private lateinit var storageService: StorageService
    private lateinit var authCredentialsProvider: AuthCredentialsProvider

    @Before
    fun setup() {
        storageService = Mockito.spy(StorageService::class.java)
        authCredentialsProvider = Mockito.mock(AuthCredentialsProvider::class.java)
    }

    @Test
    fun defaultPrefixResolverAWSS3PluginConfigTest() {
        val key = "123"
        val expectedKey = "public/123"
        val tempFile = File.createTempFile("new", "file.tmp")
        val request = AWSS3StorageUploadRequest<File>(
            key,
            tempFile,
            StorageAccessLevel.PUBLIC,
            "",
            "/image",
            ServerSideEncryption.NONE,
            mutableMapOf(),
            false
        )
        awsS3StorageUploadFileOperation = AWSS3StorageUploadFileOperation(
            storageService,
            MoreExecutors.newDirectExecutorService(),
            authCredentialsProvider,
            request,
            AWSS3StoragePluginConfiguration {},
            {},
            {},
            {}
        )
        awsS3StorageUploadFileOperation.start()
        Mockito.verify(storageService).uploadFile(
            eq(awsS3StorageUploadFileOperation.transferId),
            eq(expectedKey),
            eq(tempFile),
            any(ObjectMetadata::class.java),
            eq(false)
        )
    }

    @Test
    fun customEmptyPrefixResolverAWSS3PluginConfigTest() {
        val key = "123"
        val expectedKey = "123"
        val tempFile = File.createTempFile("new", "file.tmp")
        val request = AWSS3StorageUploadRequest<File>(
            key,
            tempFile,
            StorageAccessLevel.PUBLIC,
            "",
            "/image",
            ServerSideEncryption.NONE,
            mutableMapOf(),
            false
        )
        awsS3StorageUploadFileOperation = AWSS3StorageUploadFileOperation(
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
        awsS3StorageUploadFileOperation.start()
        Mockito.verify(storageService).uploadFile(
            eq(awsS3StorageUploadFileOperation.transferId),
            eq(expectedKey),
            eq(tempFile),
            any(ObjectMetadata::class.java),
            eq(false)
        )
    }

    @Test
    fun customPrefixResolverAWSS3PluginConfigTest() {
        val key = "123"
        val expectedKey = "publicCustom/123"
        val tempFile = File.createTempFile("new", "file.tmp")
        val request = AWSS3StorageUploadRequest<File>(
            key,
            tempFile,
            StorageAccessLevel.PUBLIC,
            "",
            "/image",
            ServerSideEncryption.NONE,
            mutableMapOf(),
            false
        )
        awsS3StorageUploadFileOperation = AWSS3StorageUploadFileOperation(
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
                        onSuccess.accept("publicCustom/")
                    }
                }
            },
            {},
            {},
            {}
        )
        awsS3StorageUploadFileOperation.start()
        Mockito.verify(storageService).uploadFile(
            eq(awsS3StorageUploadFileOperation.transferId),
            eq(expectedKey),
            eq(tempFile),
            any(ObjectMetadata::class.java),
            eq(false)
        )
    }
}
