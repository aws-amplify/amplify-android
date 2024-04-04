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
package com.amplifyframework.storage.s3.operation

import com.amplifyframework.auth.AuthCredentialsProvider
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import com.amplifyframework.storage.ObjectMetadata
import com.amplifyframework.storage.StorageChannelEventName
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.operation.StorageUploadInputStreamOperation
import com.amplifyframework.storage.result.StorageTransferProgress
import com.amplifyframework.storage.result.StorageUploadInputStreamResult
import com.amplifyframework.storage.s3.ServerSideEncryption
import com.amplifyframework.storage.s3.extensions.toS3ServiceKey
import com.amplifyframework.storage.s3.request.AWSS3StoragePathUploadRequest
import com.amplifyframework.storage.s3.service.StorageService
import com.amplifyframework.storage.s3.transfer.TransferListener
import com.amplifyframework.storage.s3.transfer.TransferObserver
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.runBlocking

/**
 * An operation to upload an InputStream to AWS S3.
 */
internal class AWSS3StoragePathUploadInputStreamOperation internal constructor(
    transferId: String,
    private val request: AWSS3StoragePathUploadRequest<InputStream>,
    private val storageService: StorageService,
    private val executorService: ExecutorService,
    private val authCredentialsProvider: AuthCredentialsProvider,
    private var transferObserver: TransferObserver? = null,
    onProgress: Consumer<StorageTransferProgress>? = null,
    onSuccess: Consumer<StorageUploadInputStreamResult>? = null,
    onError: Consumer<StorageException>? = null
) : StorageUploadInputStreamOperation<AWSS3StoragePathUploadRequest<InputStream>>(
    request,
    transferId,
    onProgress,
    onSuccess,
    onError
) {

    constructor(
        request: AWSS3StoragePathUploadRequest<InputStream>,
        storageService: StorageService,
        executorService: ExecutorService,
        authCredentialsProvider: AuthCredentialsProvider,
        onProgress: Consumer<StorageTransferProgress>,
        onSuccess: Consumer<StorageUploadInputStreamResult>,
        onError: Consumer<StorageException>
    ) : this(
        UUID.randomUUID().toString(),
        request,
        storageService,
        executorService,
        authCredentialsProvider,
        null,
        onProgress,
        onSuccess,
        onError
    )

    init {
        transferObserver?.setTransferListener(UploadTransferListener())
    }

    override fun start() {
        // Only start if it hasn't already been started
        if (transferObserver != null) {
            return
        }

        executorService.submit {
            val serviceKey = try {
                runBlocking {
                    request.path.toS3ServiceKey(authCredentialsProvider)
                }
            } catch (se: StorageException) {
                onError.accept(se)
                return@submit
            }

            try {
                val inputStream = request.local
                // Set up the metadata
                val objectMetadata = ObjectMetadata()
                objectMetadata.userMetadata = request.metadata
                objectMetadata.metaData[ObjectMetadata.CONTENT_TYPE] = request.contentType
                val storageServerSideEncryption =
                    request.serverSideEncryption
                if (ServerSideEncryption.NONE != storageServerSideEncryption) {
                    objectMetadata.metaData[ObjectMetadata.SERVER_SIDE_ENCRYPTION] =
                        storageServerSideEncryption.getName()
                }
                transferObserver = storageService.uploadInputStream(
                    transferId,
                    serviceKey,
                    inputStream,
                    objectMetadata,
                    request.useAccelerateEndpoint
                )
                transferObserver?.setTransferListener(UploadTransferListener())
            } catch (exception: Exception) {
                onError?.accept(
                    StorageException(
                        "Issue uploading InputStream.",
                        exception,
                        "See included exception for more details and suggestions to fix."
                    )
                )
            }
        }
    }

    override fun pause() {
        executorService.submit {
            transferObserver?.let {
                try {
                    storageService.pauseTransfer(it)
                } catch (exception: java.lang.Exception) {
                    onError?.accept(
                        StorageException(
                            "Something went wrong while attempting to pause your " +
                                "AWS S3 Storage upload InputStream operation",
                            exception,
                            "See attached exception for more information and suggestions"
                        )
                    )
                }
            }
        }
    }

    override fun resume() {
        executorService.submit {
            transferObserver?.let {
                try {
                    storageService.resumeTransfer(it)
                } catch (exception: java.lang.Exception) {
                    onError?.accept(
                        StorageException(
                            "Something went wrong while attempting to resume your " +
                                "AWS S3 Storage upload InputStream operation",
                            exception,
                            "See attached exception for more information and suggestions"
                        )
                    )
                }
            }
        }
    }

    override fun cancel() {
        executorService.submit {
            transferObserver?.let {
                try {
                    storageService.cancelTransfer(it)
                } catch (exception: java.lang.Exception) {
                    onError?.accept(
                        StorageException(
                            "Something went wrong while attempting to cancel your " +
                                "AWS S3 Storage upload InputStream operation",
                            exception,
                            "See attached exception for more information and suggestions"
                        )
                    )
                }
            }
        }
    }

    override fun getTransferState(): TransferState {
        return transferObserver?.transferState ?: TransferState.UNKNOWN
    }

    override fun setOnSuccess(onSuccess: Consumer<StorageUploadInputStreamResult>?) {
        super.setOnSuccess(onSuccess)
        val serviceKey = transferObserver?.key
        if (transferState == TransferState.COMPLETED && serviceKey != null) {
            onSuccess?.accept(StorageUploadInputStreamResult(serviceKey, serviceKey))
        }
    }

    private inner class UploadTransferListener : TransferListener {
        override fun onStateChanged(id: Int, state: TransferState, key: String) {
            Amplify.Hub.publish(
                HubChannel.STORAGE,
                HubEvent.create(StorageChannelEventName.UPLOAD_STATE, state.name)
            )
            when (state) {
                TransferState.COMPLETED -> {
                    onSuccess?.accept(StorageUploadInputStreamResult(key, key))
                    return
                }
                else -> {}
            }
        }

        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
            onProgress?.accept(StorageTransferProgress(bytesCurrent, bytesTotal))
        }

        override fun onError(id: Int, ex: Exception) {
            Amplify.Hub.publish(
                HubChannel.STORAGE,
                HubEvent.create(StorageChannelEventName.UPLOAD_ERROR, ex)
            )
            onError?.accept(
                StorageException(
                    "Something went wrong with your AWS S3 Storage upload InputStream operation",
                    ex,
                    "See attached exception for more information and suggestions"
                )
            )
        }
    }
}
