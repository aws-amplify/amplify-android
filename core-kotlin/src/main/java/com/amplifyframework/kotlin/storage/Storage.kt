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

import com.amplifyframework.core.async.Cancelable
import com.amplifyframework.core.async.Resumable
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

interface Storage {
    @Throws(StorageException::class)
    suspend fun getUrl(
        key: String,
        options: StorageGetUrlOptions = StorageGetUrlOptions.defaultInstance()
    ): StorageGetUrlResult

    @ExperimentalCoroutinesApi
    @FlowPreview
    fun downloadFile(
        key: String,
        local: File,
        options: StorageDownloadFileOptions = StorageDownloadFileOptions.defaultInstance()
    ): InProgressStorageOperation<StorageDownloadFileResult>

    @ExperimentalCoroutinesApi
    @FlowPreview
    fun uploadFile(
        key: String,
        local: File,
        options: StorageUploadFileOptions = StorageUploadFileOptions.defaultInstance()
    ): InProgressStorageOperation<StorageUploadFileResult>

    @FlowPreview
    @ExperimentalCoroutinesApi
    fun uploadInputStream(
        key: String,
        local: InputStream,
        options: StorageUploadInputStreamOptions = StorageUploadInputStreamOptions.defaultInstance()
    ): InProgressStorageOperation<StorageUploadInputStreamResult>

    @Throws(StorageException::class)
    suspend fun remove(
        key: String,
        options: StorageRemoveOptions = StorageRemoveOptions.defaultInstance()
    ): StorageRemoveResult

    @Deprecated("use the paged list api instead.", replaceWith = ReplaceWith("list(String, StoragePagedListOptions)"))
    @Throws(StorageException::class)
    suspend fun list(
        path: String,
        options: StorageListOptions = StorageListOptions.defaultInstance()
    ): StorageListResult

    @Throws(StorageException::class)
    suspend fun list(
        path: String,
        options: StoragePagedListOptions
    ): StorageListResult

    @Throws(StorageException::class)
    suspend fun getTransfer(transferId: String): StorageTransferOperation<*, StorageTransferResult>

    @FlowPreview
    data class InProgressStorageOperation<T>(
        val transferId: String,
        private val results: Flow<T>,
        private val progress: Flow<StorageTransferProgress>,
        private val errors: Flow<StorageException>,
        private val delegate: StorageTransferOperation<*, *>?
    ) : Cancelable, Resumable {

        override fun cancel() {
            delegate?.cancel()
        }

        fun progress(): Flow<StorageTransferProgress> {
            return progress
        }

        @Suppress("UNCHECKED_CAST")
        suspend fun result(): T {
            // We want to take the first item from either one,
            // without waiting for the other.
            // Maybe there's a cleaner way to achieve this.
            return flowOf(errors, results)
                .flattenMerge()
                .onEach { emission ->
                    if (emission is StorageException) {
                        throw emission
                    }
                }
                .map { it as T }
                .first()
        }

        override fun pause() {
            delegate?.pause()
        }

        override fun resume() {
            delegate?.resume()
        }
    }
}
