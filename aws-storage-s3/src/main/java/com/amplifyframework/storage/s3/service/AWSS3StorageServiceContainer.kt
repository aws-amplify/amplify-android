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
package com.amplifyframework.storage.s3.service

import android.content.Context
import com.amplifyframework.storage.ResolvedStorageBucket
import com.amplifyframework.storage.s3.transfer.S3StorageTransferClientProvider
import com.amplifyframework.storage.s3.transfer.StorageTransferClientProvider
import java.util.concurrent.ConcurrentHashMap

/**
 * A container that stores a list of AWSS3StorageService based on the bucket name associated with the service.
 * repository.
 */
internal class AWSS3StorageServiceContainer(
    private val context: Context,
    private val storageServiceFactory: AWSS3StorageService.Factory,
    private val clientProvider: StorageTransferClientProvider,
    private val awsS3StorageServicesByBucketName: ConcurrentHashMap<String, AWSS3StorageService>
) {
    constructor(
        context: Context,
        storageServiceFactory: AWSS3StorageService.Factory,
        clientProvider: S3StorageTransferClientProvider
    ) : this(context, storageServiceFactory, clientProvider, ConcurrentHashMap())

    private val lock = Any()

    /**
     * Stores a instance of AWSS3StorageService
     *
     * @param bucketName the bucket name
     * @param service the AWSS3StorageService instance
     */
    fun put(bucketName: String, service: AWSS3StorageService) {
        synchronized(lock) {
            awsS3StorageServicesByBucketName.put(bucketName, service)
        }
    }

    /**
     * Get an AWSS3StorageSErvice instance based on a ResolvedStorageBucket
     * @param resolvedStorageBucket An instance of ResolvedStorageBucket with bucket info
     * @return An AWSS3StorageService instance associated with the ResolvedStorageBucket
     */
    fun get(resolvedStorageBucket: ResolvedStorageBucket): AWSS3StorageService {
        synchronized(lock) {
            val bucketName: String = resolvedStorageBucket.bucketInfo.bucketName
            var service = awsS3StorageServicesByBucketName.get(bucketName)
            if (service == null) {
                val region: String = resolvedStorageBucket.bucketInfo.region
                service = storageServiceFactory.create(context, region, bucketName, clientProvider)
                awsS3StorageServicesByBucketName[bucketName] = service
            }
            return service
        }
    }

    /**
     * Get an AWSS3StorageSErvice instance based on a bucket name and region
     * @param bucketName the bucket name associated with the AWSS3StorageService
     * @param bucketName the region to associate with a new AWSS3StorageService instance if one doesn't exist
     * @return An AWSS3StorageService instance associated with the ResolvedStorageBucket
     */
    fun get(bucketName: String, region: String): AWSS3StorageService {
        synchronized(lock) {
            var service = awsS3StorageServicesByBucketName[bucketName]
            if (service == null) {
                service = storageServiceFactory.create(context, region, bucketName, clientProvider)
                awsS3StorageServicesByBucketName[bucketName] = service
            }

            return service
        }
    }
}
