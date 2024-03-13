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
import com.amplifyframework.storage.StoragePath
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.operation.StorageDownloadFileOperation
import com.amplifyframework.storage.result.StorageDownloadFileResult
import com.amplifyframework.storage.result.StorageTransferProgress
import com.amplifyframework.storage.s3.request.AWSS3StorageDownloadFileRequestV2
import com.amplifyframework.storage.s3.service.StorageService
import com.amplifyframework.storage.s3.transfer.TransferListener
import com.amplifyframework.storage.s3.transfer.TransferObserver
import java.io.File
import java.util.UUID
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.runBlocking

/**
 * An operation to download a file from AWS S3.
 */
internal class AWSS3StorageDownloadFileOperationV2(
    private val transferId: String = UUID.randomUUID().toString(),
    private val request: AWSS3StorageDownloadFileRequestV2?,
    private var file: File,
    private val storageService: StorageService,
    private val executorService: ExecutorService,
    private val authCredentialsProvider: AuthCredentialsProvider,
    private var transferObserver: TransferObserver? = null,
    onProgress: Consumer<StorageTransferProgress>? = null,
    onSuccess: Consumer<StorageDownloadFileResult>? = null,
    onError: Consumer<StorageException>? = null
) :
    StorageDownloadFileOperation<AWSS3StorageDownloadFileRequestV2>(
        request,
        transferId,
        onProgress,
        onSuccess,
        onError
    ) {

    constructor(
        request: AWSS3StorageDownloadFileRequestV2,
        storageService: StorageService,
        executorService: ExecutorService,
        authCredentialsProvider: AuthCredentialsProvider,
        onProgress: Consumer<StorageTransferProgress>? = null,
        onSuccess: Consumer<StorageDownloadFileResult>? = null,
        onError: Consumer<StorageException>? = null
    ) : this(
        UUID.randomUUID().toString(),
        request,
        request.local,
        storageService,
        executorService,
        authCredentialsProvider,
        null,
        onProgress,
        onSuccess,
        onError
    )

    init {
        transferObserver?.setTransferListener(DownloadTransferListener())
    }

    override fun start() {
        // Only start if it hasn't already been started
        if (transferObserver != null) {
            return
        }
        val downloadRequest = request ?: return
        executorService.submit {

            try {

                val path = when (val storagePath = downloadRequest.path) {
                    is StoragePath.StringStoragePath -> {
                        storagePath.resolvePath()
                    }
                    is StoragePath.IdentityIdProvidedStoragePath -> {
                        val identityId = try {
                            runBlocking {
                                authCredentialsProvider.getIdentityId()
                            }
                        } catch (e: Exception) {
                            onError?.accept(
                                StorageException(
                                    "Failed to fetch identity ID",
                                    e,
                                    "See included exception for more details and suggestions to fix."
                                )
                            )
                            return@submit
                        }
                        storagePath.resolvePath(identityId)
                    }

                    else -> {
                        onError?.accept(
                            StorageException(
                                "Unsupported StoragePath type provided",
                                "Use an Amplify supported StoragePath type"
                            )
                        )
                        return@submit
                    }
                }

                if (!path.startsWith("/")) {
                    onError?.accept(
                        StorageException(
                            "Invalid path provided",
                            "Update path to start with a /."
                        )
                    )
                    return@submit
                }

                transferObserver = storageService.downloadToFile(
                    transferId,
                    path,
                    downloadRequest.local,
                    downloadRequest.useAccelerateEndpoint
                )
                transferObserver?.setTransferListener(DownloadTransferListener())
            } catch (e: Exception) {
                onError?.accept(
                    StorageException(
                        "Issue downloading file",
                        e,
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
        override fun onStateChanged(id: Int, state: TransferState, key: String) {
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

        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
            onProgress?.accept(StorageTransferProgress(bytesCurrent, bytesTotal))
        }

        override fun onError(id: Int, ex: java.lang.Exception) {
            Amplify.Hub.publish(
                HubChannel.STORAGE,
                HubEvent.create(StorageChannelEventName.DOWNLOAD_ERROR, ex)
            )
            onError?.accept(
                StorageException(
                    "Something went wrong with your AWS S3 Storage download file operation",
                    ex,
                    "See attached exception for more information and suggestions"
                )
            )
        }
    }
}
