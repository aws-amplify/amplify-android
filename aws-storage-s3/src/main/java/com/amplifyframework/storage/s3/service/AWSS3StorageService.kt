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
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.listObjectsV2
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.HeadObjectRequest
import aws.sdk.kotlin.services.s3.model.NotFound
import aws.sdk.kotlin.services.s3.paginators.listObjectsV2Paginated
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import aws.sdk.kotlin.services.s3.withConfig
import com.amplifyframework.auth.AuthCredentialsProvider
import com.amplifyframework.storage.ObjectMetadata
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.StorageItem
import com.amplifyframework.storage.options.SubpathStrategy
import com.amplifyframework.storage.options.SubpathStrategy.Exclude
import com.amplifyframework.storage.result.StorageListResult
import com.amplifyframework.storage.s3.transfer.S3StorageTransferClientProvider
import com.amplifyframework.storage.s3.transfer.StorageTransferClientProvider
import com.amplifyframework.storage.s3.transfer.TransferManager
import com.amplifyframework.storage.s3.transfer.TransferObserver
import com.amplifyframework.storage.s3.transfer.TransferRecord
import com.amplifyframework.storage.s3.transfer.UploadOptions
import com.amplifyframework.storage.s3.utils.S3Keys
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.time.Instant
import java.util.Date
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.runBlocking
/**
 * A representation of an S3 backend service endpoint.
 */
internal class AWSS3StorageService(
    private val context: Context,
    private val awsRegion: String,
    private val s3BucketName: String,
    private val authCredentialsProvider: AuthCredentialsProvider,
    private val awsS3StoragePluginKey: String,
    private val clientProvider: StorageTransferClientProvider
) : StorageService {

    private var s3Client: S3Client = S3StorageTransferClientProvider.getS3Client(awsRegion, authCredentialsProvider)

    val transferManager: TransferManager =
        TransferManager(context, clientProvider, awsS3StoragePluginKey)

    /**
     * Generate pre-signed URL for an object.
     * @param serviceKey S3 service key
     * @param expires Number of seconds before URL expires
     * @return A pre-signed URL
     */
    @OptIn(ExperimentalTime::class)
    override fun getPresignedUrl(serviceKey: String, expires: Int, useAccelerateEndpoint: Boolean): URL {
        val presignUrlRequest = s3Client.withConfig {
            enableAccelerate = useAccelerateEndpoint
        }.use {
            runBlocking {
                it.presignGetObject(
                    GetObjectRequest {
                        bucket = s3BucketName
                        key = serviceKey
                    },
                    expires.seconds
                )
            }
        }
        return URL(presignUrlRequest.url.toString())
    }

    /**
     * Validate if S3 object exists for the given key.
     * Throws StorageException if NoSuchKey S3 client exception is caught.
     * @param serviceKey S3 service key
     */
    override fun validateObjectExists(serviceKey: String) {
        try {
            runBlocking {
                s3Client.headObject(
                    HeadObjectRequest {
                        bucket = s3BucketName
                        key = serviceKey
                    }
                )
            }
        } catch (ex: NotFound) {
            throw StorageException(
                "Unable to generate URL for non-existent path: $serviceKey",
                ex,
                "Please ensure the path is valid or the object has been uploaded"
            )
        }
    }

    /**
     * Begin downloading a file.
     * @param serviceKey S3 service key
     * @param file Target file
     * @param useAccelerateEndpoint Flag to use accelerate endpoint
     * @return A transfer observer
     */
    override fun downloadToFile(
        transferId: String,
        serviceKey: String,
        file: File,
        useAccelerateEndpoint: Boolean
    ): TransferObserver {
        return transferManager.download(
            transferId,
            s3BucketName,
            awsRegion,
            serviceKey,
            file,
            useAccelerateEndpoint = useAccelerateEndpoint
        )
    }

    /**
     * Begin uploading a file.
     * @param serviceKey S3 service key
     * @param file Target file
     * @param metadata Object metadata to associate with upload
     * @return A transfer observer
     */
    override fun uploadFile(
        transferId: String,
        serviceKey: String,
        file: File,
        metadata: ObjectMetadata,
        useAccelerateEndpoint: Boolean
    ): TransferObserver {
        return transferManager.upload(
            transferId,
            s3BucketName,
            awsRegion,
            serviceKey,
            file,
            metadata,
            useAccelerateEndpoint = useAccelerateEndpoint
        )
    }

    /**
     * Begin uploading an inputStream.
     * @param serviceKey S3 service key
     * @param inputStream Target InputStream
     * @param metadata Object metadata to associate with upload
     * @return A transfer observer
     * @throws IOException An IOException thrown during the process writing an InputStream into a file
     */
    override fun uploadInputStream(
        transferId: String,
        serviceKey: String,
        inputStream: InputStream,
        metadata: ObjectMetadata,
        useAccelerateEndpoint: Boolean
    ): TransferObserver {
        val uploadOptions = UploadOptions(s3BucketName, awsRegion, metadata)
        return transferManager.upload(transferId, serviceKey, inputStream, uploadOptions, useAccelerateEndpoint)
    }

    /**
     * List items inside an S3 path.
     * @param path The path to list items from
     * @return A list of parsed items
     */
    override fun listFiles(path: String, prefix: String): MutableList<StorageItem>? {
        val items = mutableListOf<StorageItem>()
        runBlocking {
            val result = s3Client.listObjectsV2Paginated {
                this.bucket = s3BucketName
                this.prefix = path
            }
            result.collect {
                it.contents?.forEach { value ->
                    val serviceKey = value.key
                    val lastModified = value.lastModified
                    val eTag = value.eTag
                    if (serviceKey != null && lastModified != null && eTag != null) {
                        items += StorageItem(
                            serviceKey,
                            S3Keys.extractAmplifyKey(serviceKey, prefix),
                            value.size ?: 0,
                            Date.from(Instant.ofEpochMilli(lastModified.epochSeconds)),
                            eTag,
                            null
                        )
                    }
                }
            }
        }
        return items
    }

    override fun listFiles(path: String, prefix: String, pageSize: Int, nextToken: String?): StorageListResult {
        return runBlocking {
            val result = s3Client.listObjectsV2 {
                this.bucket = s3BucketName
                this.prefix = path
                this.maxKeys = pageSize
                this.continuationToken = nextToken
            }
            val items = result.contents?.mapNotNull { value ->
                val serviceKey = value.key
                val lastModified = value.lastModified
                val eTag = value.eTag
                if (serviceKey != null && lastModified != null && eTag != null) {
                    StorageItem(
                        serviceKey,
                        S3Keys.extractAmplifyKey(serviceKey, prefix),
                        value.size ?: 0,
                        Date.from(Instant.ofEpochMilli(lastModified.epochSeconds)),
                        eTag,
                        null
                    )
                } else {
                    null
                }
            }
            StorageListResult.fromItems(items, result.nextContinuationToken)
        }
    }

    /**
     * List items inside an S3 path.
     * @param path The path to list items from
     * @param prefix The prefix to the path
     * @param subPathStrategy The SubpathStrategy to include/exclude sub-paths
     * @return A list of parsed items
     */
    override fun listFiles(path: String, prefix: String, subPathStrategy: SubpathStrategy?): StorageListResult {
        return runBlocking {
            val items = mutableListOf<StorageItem>()
            val excludedSubPaths = mutableListOf<String>()
            val delimiter: String? = (subPathStrategy as? Exclude)?.delimiter
            val result = s3Client.listObjectsV2Paginated {
                this.bucket = s3BucketName
                this.prefix = path
                this.delimiter = delimiter
            }
            result.collect {
                it.contents?.forEach { value ->
                    val serviceKey = value.key
                    val lastModified = value.lastModified
                    val eTag = value.eTag
                    if (serviceKey != null && lastModified != null && eTag != null) {
                        items += StorageItem(
                            serviceKey,
                            S3Keys.extractAmplifyKey(serviceKey, prefix),
                            value.size ?: 0,
                            Date.from(Instant.ofEpochMilli(lastModified.epochSeconds)),
                            eTag,
                            null
                        )
                    }
                }
                it.commonPrefixes?.forEach {
                    val subpath = it.prefix
                    if (subpath != null) {
                        excludedSubPaths.add(subpath)
                    }
                }
            }

            StorageListResult.fromItems(items, null, excludedSubPaths)
        }
    }

    override fun listFiles(
        path: String,
        prefix: String,
        pageSize: Int,
        nextToken: String?,
        subPathStrategy: SubpathStrategy?
    ): StorageListResult {
        return runBlocking {
            val delimiter = (subPathStrategy as? Exclude)?.delimiter
            val result = s3Client.listObjectsV2 {
                this.bucket = s3BucketName
                this.prefix = path
                this.maxKeys = pageSize
                this.continuationToken = nextToken
                this.delimiter = delimiter
            }
            val items = result.contents?.mapNotNull { value ->
                val serviceKey = value.key
                val lastModified = value.lastModified
                val eTag = value.eTag
                if (serviceKey != null && lastModified != null && eTag != null) {
                    StorageItem(
                        serviceKey,
                        S3Keys.extractAmplifyKey(serviceKey, prefix),
                        value.size ?: 0,
                        Date.from(Instant.ofEpochMilli(lastModified.epochSeconds)),
                        eTag,
                        null
                    )
                } else {
                    null
                }
            }

            val subPaths = result.commonPrefixes?.mapNotNull { it.prefix }
            StorageListResult.fromItems(items, result.nextContinuationToken, subPaths)
        }
    }

    /**
     * This method is used to list files when StoragePath was used.
     * When StoragePath is used, we provide the full serviceKey for both StorageItem.key and StorageItem.path
     */
    fun listFiles(
        path: String,
        pageSize: Int,
        nextToken: String?,
        subPathStrategy: SubpathStrategy?
    ): StorageListResult {
        return runBlocking {
            val delimiter = (subPathStrategy as? Exclude)?.delimiter
            val result = s3Client.listObjectsV2 {
                this.bucket = s3BucketName
                this.prefix = path
                this.maxKeys = pageSize
                this.continuationToken = nextToken
                this.delimiter = delimiter
            }
            val items = result.contents?.mapNotNull { value ->
                val serviceKey = value.key
                val lastModified = value.lastModified
                val eTag = value.eTag
                if (serviceKey != null && lastModified != null && eTag != null) {
                    StorageItem(
                        serviceKey,
                        serviceKey,
                        value.size ?: 0,
                        Date.from(Instant.ofEpochMilli(lastModified.epochSeconds)),
                        eTag,
                        null
                    )
                } else {
                    null
                }
            }
            val subPaths = result.commonPrefixes?.mapNotNull { it.prefix }
            StorageListResult.fromItems(items, result.nextContinuationToken, subPaths)
        }
    }

    /**
     * Synchronous operation to delete a file in s3.
     * @param serviceKey Fully specified path to file to delete (including public/private/protected folder)
     */
    override fun deleteObject(serviceKey: String) {
        runBlocking {
            s3Client.deleteObject {
                bucket = s3BucketName
                key = serviceKey
            }
        }
    }

    /**
     * Pause a file transfer operation.
     * @param transferObserver an in-progress transfer
     */
    override fun pauseTransfer(transferObserver: TransferObserver) {
        transferManager.pause(transferObserver.id)
    }

    /**
     * Resume a file transfer.
     * @param transferObserver A transfer to be resumed
     */
    override fun resumeTransfer(transferObserver: TransferObserver) {
        transferManager.resume(transferObserver.id)
    }

    /**
     * Cancel a file transfer.
     * @param transferObserver A file transfer to cancel
     */
    override fun cancelTransfer(transferObserver: TransferObserver) {
        transferManager.cancel(transferObserver.id)
    }

    /**
     * Gets an existing transfer in the local device queue.
     * Register consumer to observe result of transfer lookup.
     * @param transferId the unique identifier of the object in storage
     * @return transfer record matching the transfer id
     */
    override fun getTransfer(transferId: String): TransferRecord? {
        return transferManager.getTransferOperationById(transferId)
    }

    /**
     * Gets a handle of S3 client underlying this service.
     * @return S3 client instance
     */
    fun getClient(): S3Client {
        return s3Client
    }

    interface Factory {
        /**
         * Factory interface to instantiate [StorageService] object.
         *
         * @param context    Android context
         * @param region     S3 bucket region
         * @param bucketName Name of the bucket where the items are stored
         * @return An instantiated storage service instance
         */
        fun create(
            context: Context,
            region: String,
            bucketName: String,
            clientProvider: StorageTransferClientProvider
        ): AWSS3StorageService
    }
}
