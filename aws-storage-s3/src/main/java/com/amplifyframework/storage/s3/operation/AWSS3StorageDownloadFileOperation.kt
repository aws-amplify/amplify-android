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
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import com.amplifyframework.storage.StorageChannelEventName
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.operation.StorageDownloadFileOperation
import com.amplifyframework.storage.result.StorageDownloadFileResult
import com.amplifyframework.storage.result.StorageTransferProgress
import com.amplifyframework.storage.s3.configuration.AWSS3StoragePluginConfiguration
import com.amplifyframework.storage.s3.request.AWSS3StorageDownloadFileRequest
import com.amplifyframework.storage.s3.service.StorageService
import com.amplifyframework.storage.s3.transfer.TransferListener
import com.amplifyframework.storage.s3.transfer.TransferObserver
import java.io.File
import java.util.UUID
import java.util.concurrent.ExecutorService

/**
 * An operation to download a file from AWS S3.
 */
class AWSS3StorageDownloadFileOperation @JvmOverloads internal constructor(
    transferId: String,
    private var file: File,
    private val storageService: StorageService,
    private val executorService: ExecutorService,
    private val authCredentialsProvider: AuthCredentialsProvider,
    private val awsS3StoragePluginConfiguration: AWSS3StoragePluginConfiguration,
    request: AWSS3StorageDownloadFileRequest? = null,
    private var transferObserver: TransferObserver? = null,
    onProgress: Consumer<StorageTransferProgress>? = null,
    onSuccess: Consumer<StorageDownloadFileResult>? = null,
    onError: Consumer<StorageException>? = null
) : StorageDownloadFileOperation<AWSS3StorageDownloadFileRequest>(request, transferId, onProgress, onSuccess, onError) {

    init {
        transferObserver?.setTransferListener(DownloadTransferListener())
    }

    constructor(
        storageService: StorageService,
        executorService: ExecutorService,
        authCredentialsProvider: AuthCredentialsProvider,
        request: AWSS3StorageDownloadFileRequest,
        awsS3StoragePluginConfiguration: AWSS3StoragePluginConfiguration,
        onProgress: Consumer<StorageTransferProgress>,
        onSuccess: Consumer<StorageDownloadFileResult>,
        onError: Consumer<StorageException>
    ) : this(
        UUID.randomUUID().toString(),
        request.local,
        storageService,
        executorService,
        authCredentialsProvider,
        awsS3StoragePluginConfiguration,
        request,
        null,
        onProgress,
        onSuccess,
        onError
    )

    override fun start() {
        // Only start if it hasn't already been started
        if (transferObserver != null) {
            return
        }
        val downloadRequest = request ?: return
        executorService.submit(
            Runnable {
                awsS3StoragePluginConfiguration.getAWSS3PluginPrefixResolver(authCredentialsProvider).resolvePrefix(
                    downloadRequest.accessLevel,
                    downloadRequest.targetIdentityId,
                    Consumer { prefix: String ->
                        try {
                            val serviceKey = prefix + downloadRequest.key
                            this.file = downloadRequest.local
                            transferObserver = storageService.downloadToFile(
                                transferId,
                                serviceKey,
                                file,
                                downloadRequest.useAccelerateEndpoint()
                            )
                            transferObserver?.setTransferListener(DownloadTransferListener())
                        } catch (exception: Exception) {
                            onError?.accept(
                                StorageException(
                                    "Issue downloading file",
                                    exception,
                                    "See included exception for more details and suggestions to fix."
                                )
                            )
                        }
                    },
                    onError
                )
            }
        )
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
                                "AWS S3 Storage download file operation",
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
                            "Something went wrong while attempting to " +
                                "resume your AWS S3 Storage download file operation",
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
                                "AWS S3 Storage download file operation",
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

    override fun setOnSuccess(onSuccess: Consumer<StorageDownloadFileResult>?) {
        super.setOnSuccess(onSuccess)
        // replay the onSuccess if transfer is already completed.
        if (transferState == TransferState.COMPLETED) {
            onSuccess?.accept(StorageDownloadFileResult.fromFile(file))
        }
    }

    inner class DownloadTransferListener : TransferListener {
        override fun onStateChanged(transferId: Int, state: TransferState, key: String) {
            Amplify.Hub.publish(
                HubChannel.STORAGE,
                HubEvent.create(StorageChannelEventName.DOWNLOAD_STATE, state.name)
            )
            when (state) {
                TransferState.COMPLETED -> {
                    onSuccess?.accept(StorageDownloadFileResult.fromFile(file))
                    return
                }
                TransferState.FAILED -> {}
                else -> {}
            }
        }

        override fun onProgressChanged(transferId: Int, bytesCurrent: Long, bytesTotal: Long) {
            onProgress?.accept(StorageTransferProgress(bytesCurrent, bytesTotal))
        }

        override fun onError(transferId: Int, exception: java.lang.Exception) {
            Amplify.Hub.publish(
                HubChannel.STORAGE,
                HubEvent.create(StorageChannelEventName.DOWNLOAD_ERROR, exception)
            )
            onError?.accept(
                StorageException(
                    "Something went wrong with your AWS S3 Storage download file operation",
                    exception,
                    "See attached exception for more information and suggestions"
                )
            )
        }
    }
}
