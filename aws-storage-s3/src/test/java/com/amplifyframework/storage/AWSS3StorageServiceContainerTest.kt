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

package com.amplifyframework.storage.s3

import android.content.Context
import com.amplifyframework.storage.BucketInfo
import com.amplifyframework.storage.ResolvedStorageBucket
import com.amplifyframework.storage.StorageBucket
import com.amplifyframework.storage.s3.service.AWSS3StorageService
import com.amplifyframework.storage.s3.service.AWSS3StorageServiceContainer
import com.amplifyframework.storage.s3.transfer.StorageTransferClientProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.ConcurrentHashMap
import org.junit.Before
import org.junit.Test

class AWSS3StorageServiceContainerTest {

    private val storageServiceFactory = mockk<AWSS3StorageService.Factory> {
        every { create(any(), any(), any(), any()) } returns mockk<AWSS3StorageService>()
    }
    private val context = mockk<Context>()
    private val clientProvider = mockk<StorageTransferClientProvider>()
    private val bucketName = "testBucket"
    private val region = "us-east-1"

    private lateinit var serviceContainerHashMap: ConcurrentHashMap<String, AWSS3StorageService>
    private lateinit var serviceContainer: AWSS3StorageServiceContainer
    @Before
    fun setUp() {
        serviceContainerHashMap = ConcurrentHashMap()
        serviceContainer = AWSS3StorageServiceContainer(
            context,
            storageServiceFactory,
            clientProvider,
            serviceContainerHashMap
        )
    }

    @Test
    fun `put default AWSS3Service in container`() {
        val service = storageServiceFactory.create(context, region, bucketName, clientProvider)
        serviceContainer.put(bucketName, service)

        serviceContainerHashMap.size shouldBe 1
        serviceContainerHashMap[bucketName] shouldNotBe null
    }

    @Test
    fun `get non-existent AWSS3Service in container with ResolvedStorageBucket creates new AWSService`() {
        val bucketInfo = BucketInfo(bucketName, region)
        val bucket: ResolvedStorageBucket = StorageBucket.fromBucketInfo(bucketInfo) as ResolvedStorageBucket

        val service = serviceContainer.get(bucket)

        service shouldNotBe null
        serviceContainerHashMap.size shouldBe 1
        serviceContainerHashMap[bucketName] shouldNotBe null
        serviceContainerHashMap[bucketName] shouldBe service
    }

    @Test
    fun `get WSS3Service in container multiple times with ResolvedStorageBucket creates only one service`() {
        val bucketInfo = BucketInfo(bucketName, region)
        val bucket: ResolvedStorageBucket = StorageBucket.fromBucketInfo(bucketInfo) as ResolvedStorageBucket

        val service = serviceContainer.get(bucket)
        val service2 = serviceContainer.get(bucket)

        service shouldNotBe null
        service2 shouldNotBe null
        service shouldBe service2

        serviceContainerHashMap.size shouldBe 1
        serviceContainerHashMap[bucketName] shouldNotBe null
        serviceContainerHashMap[bucketName] shouldBe service
        serviceContainerHashMap[bucketName] shouldBe service2
    }

    @Test
    fun `get non-existent AWSS3Service in container with bucket name and region creates new AWSService`() {
        val service = serviceContainer.get(bucketName, region)

        service shouldNotBe null
        serviceContainerHashMap.size shouldBe 1
        serviceContainerHashMap[bucketName] shouldNotBe null
        serviceContainerHashMap[bucketName] shouldBe service
    }

    @Test
    fun `get WSS3Service in container multiple times with bucket name and region creates only one service`() {

        val service = serviceContainer.get(bucketName, region)
        val service2 = serviceContainer.get(bucketName, region)

        service shouldNotBe null
        service2 shouldNotBe null
        service shouldBe service2

        serviceContainerHashMap.size shouldBe 1
        serviceContainerHashMap[bucketName] shouldNotBe null
        serviceContainerHashMap[bucketName] shouldBe service
        serviceContainerHashMap[bucketName] shouldBe service2
    }
}
