/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.kotlin.storage

import com.amplifyframework.core.Amplify
import com.amplifyframework.kotlin.storage.Storage.InProgressStorageOperation
import com.amplifyframework.storage.StorageCategoryBehavior as Delegate
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.operation.StorageTransferOperation
import com.amplifyframework.storage.options.StorageDownloadFileOptions
import com.amplifyframework.storage.options.StorageGetUrlOptions
import com.amplifyframework.storage.options.StorageListOptions
import com.amplifyframework.storage.options.StoragePagedListOptions
import com.amplifyframework.storage.options.StorageRemoveOptions
import com.amplifyframework.storage.options.StorageUploadFileOptions
import com.amplifyframework.storage.options.StorageUploadInputStreamOptions
import com.amplifyframework.storage.result.StorageDownloadFileResult
import com.amplifyframework.storage.result.StorageGetUrlResult
import com.amplifyframework.storage.result.StorageListResult
import com.amplifyframework.storage.result.StorageRemoveResult
import com.amplifyframework.storage.result.StorageTransferProgress
import com.amplifyframework.storage.result.StorageTransferResult
import com.amplifyframework.storage.result.StorageUploadFileResult
import com.amplifyframework.storage.result.StorageUploadInputStreamResult
import java.io.File
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class KotlinStorageFacade(private val delegate: Delegate = Amplify.Storage) : Storage {
    @Throws(StorageException::class)
    override suspend fun getUrl(key: String, options: StorageGetUrlOptions):
        StorageGetUrlResult {
        return suspendCoroutine { continuation ->
            delegate.getUrl(
                key,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    @ExperimentalCoroutinesApi
    @FlowPreview
    override fun downloadFile(key: String, local: File, options: StorageDownloadFileOptions):
        InProgressStorageOperation<StorageDownloadFileResult> {
        val progress = MutableSharedFlow<StorageTransferProgress>(replay = 1)
        val results = MutableSharedFlow<StorageDownloadFileResult>(replay = 1)
        val errors = MutableSharedFlow<StorageException>(replay = 1)
        val operation = delegate.downloadFile(
            key,
            local,
            options,
            { progress.tryEmit(it) },
            { results.tryEmit(it) },
            { errors.tryEmit(it) }
        )
        return InProgressStorageOperation(
            operation.transferId,
            results.asSharedFlow(),
            progress.asSharedFlow(),
            errors.asSharedFlow(),
            operation
        )
    }

    @ExperimentalCoroutinesApi
    @FlowPreview
    override fun uploadFile(key: String, local: File, options: StorageUploadFileOptions):
        InProgressStorageOperation<StorageUploadFileResult> {
        val progress = MutableSharedFlow<StorageTransferProgress>(replay = 1)
        val results = MutableSharedFlow<StorageUploadFileResult>(replay = 1)
        val errors = MutableSharedFlow<StorageException>(replay = 1)
        val operation = delegate.uploadFile(
            key,
            local,
            options,
            { progress.tryEmit(it) },
            { results.tryEmit(it) },
            { errors.tryEmit(it) }
        )
        return InProgressStorageOperation(
            operation.transferId,
            results.asSharedFlow(),
            progress.asSharedFlow(),
            errors.asSharedFlow(),
            operation
        )
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun uploadInputStream(
        key: String,
        local: InputStream,
        options: StorageUploadInputStreamOptions
    ):
        InProgressStorageOperation<StorageUploadInputStreamResult> {
        val progress = MutableSharedFlow<StorageTransferProgress>(replay = 1)
        val results = MutableSharedFlow<StorageUploadInputStreamResult>(replay = 1)
        val errors = MutableSharedFlow<StorageException>(replay = 1)
        val cancelable = delegate.uploadInputStream(
            key,
            local,
            options,
            { progress.tryEmit(it) },
            { results.tryEmit(it) },
            { errors.tryEmit(it) }
        )
        return InProgressStorageOperation(
            cancelable.transferId,
            results.asSharedFlow(),
            progress.asSharedFlow(),
            errors.asSharedFlow(),
            cancelable
        )
    }

    @Throws(StorageException::class)
    override suspend fun remove(key: String, options: StorageRemoveOptions): StorageRemoveResult {
        return suspendCoroutine { continuation ->
            delegate.remove(
                key,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    @Throws(StorageException::class)
    @Deprecated("use the paged list api instead.", replaceWith = ReplaceWith("list(String, StoragePagedListOptions)"))
    override suspend fun list(path: String, options: StorageListOptions): StorageListResult {
        return suspendCoroutine { continuation ->
            delegate.list(
                path,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    @Throws(StorageException::class)
    override suspend fun list(path: String, options: StoragePagedListOptions): StorageListResult {
        return suspendCoroutine { continuation ->
            delegate.list(
                path,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    @Throws(StorageException::class)
    override suspend fun getTransfer(transferId: String): StorageTransferOperation<*, StorageTransferResult> {
        return suspendCoroutine { continuation ->
            delegate.getTransfer(
                transferId,
                {
                    continuation.resume(it as StorageTransferOperation<*, StorageTransferResult>)
                },
                {
                    continuation.resumeWithException(it)
                }
            )
        }
    }
}
