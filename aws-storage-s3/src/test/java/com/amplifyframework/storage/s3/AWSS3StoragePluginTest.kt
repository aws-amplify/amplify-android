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

import com.amplifyframework.storage.BucketInfo
import com.amplifyframework.storage.StorageBucket
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.s3.service.AWSS3StorageService
import com.amplifyframework.storage.s3.service.StorageService
import com.amplifyframework.testutils.configuration.amplifyOutputsData
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class AWSS3StoragePluginTest {

    private val storageServiceFactory = mockk<StorageService.Factory> {
        every { create(any(), any(), any()) } returns mockk<AWSS3StorageService>()
    }

    private val plugin = AWSS3StoragePlugin(
        storageServiceFactory,
        mockk(),
        mockk()
    )

    @Test
    fun `configures with AmplifyOutputs`() {
        val data = amplifyOutputsData {
            storage {
                awsRegion = "test-region"
                bucketName = "test-bucket"
            }
        }

        plugin.configure(data, mockk())

        verify {
            storageServiceFactory.create(any(), "test-region", "test-bucket")
        }
    }

    @Test
    fun `throws exception if storage configuration is missing`() {
        val data = amplifyOutputsData {
            // do not add storage config
        }

        shouldThrow<StorageException> {
            plugin.configure(data, mockk())
        }
    }

    @Test
    fun `getStorageService returns default storage service if bucket is null`() {
        val data = amplifyOutputsData {
            storage {
                awsRegion = "test-region"
                bucketName = "test-bucket"
                buckets {
                    awsRegion = "test-region"
                    bucketName = "test-bucket"
                    name = "test=name"
                }
            }
        }

        plugin.configure(data, mockk())
        val service = plugin.getStorageService(null)
        service shouldNotBe null
    }

    @Test
    fun `get AWSS3StorageService from BucketInfo`() {
        val data = amplifyOutputsData {
            storage {
                awsRegion = "test-region"
                bucketName = "test-bucket"
                buckets {
                    awsRegion = "test-region"
                    bucketName = "test-bucket"
                    name = "test=name"
                }
            }
        }

        plugin.configure(data, mockk())
        val bucketInfo = BucketInfo("test-bucket", "test-region")
        val bucket = StorageBucket.fromBucketInfo(bucketInfo)
        val service = plugin.getStorageService(bucket)
        service shouldNotBe null
    }

    @Test
    fun `get AWSS3StorageService from AmplifyOutputs`() {
        val data = amplifyOutputsData {
            storage {
                awsRegion = "test-region"
                bucketName = "test-bucket"
                buckets {
                    awsRegion = "test-region"
                    bucketName = "test-bucket"
                    name = "test=name"
                }
            }
        }

        plugin.configure(data, mockk())
        val bucket = StorageBucket.fromOutputs("test=name")
        val service = plugin.getStorageService(bucket)
        service shouldNotBe null
    }

    @Test
    fun `getStorageService throws StorageException`() {
        val data = amplifyOutputsData {
            storage {
                awsRegion = "test-region"
                bucketName = "test-bucket"
                buckets {
                    awsRegion = "test-region"
                    bucketName = "test-bucket"
                    name = "test=name"
                }
            }
        }

        plugin.configure(data, mockk())
        val bucket = StorageBucket.fromOutputs("myBucket")
        shouldThrow<StorageException> {
            plugin.getStorageService(bucket)
        }
    }
}
