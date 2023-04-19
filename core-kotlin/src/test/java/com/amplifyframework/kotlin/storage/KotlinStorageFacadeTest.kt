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

import com.amplifyframework.core.Consumer
import com.amplifyframework.storage.StorageCategoryBehavior
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.StorageItem
import com.amplifyframework.storage.operation.StorageDownloadFileOperation
import com.amplifyframework.storage.operation.StorageTransferOperation
import com.amplifyframework.storage.operation.StorageUploadFileOperation
import com.amplifyframework.storage.operation.StorageUploadInputStreamOperation
import com.amplifyframework.storage.options.StoragePagedListOptions
import com.amplifyframework.storage.result.StorageDownloadFileResult
import com.amplifyframework.storage.result.StorageGetUrlResult
import com.amplifyframework.storage.result.StorageListResult
import com.amplifyframework.storage.result.StorageRemoveResult
import com.amplifyframework.storage.result.StorageTransferProgress
import com.amplifyframework.storage.result.StorageTransferResult
import com.amplifyframework.storage.result.StorageUploadFileResult
import com.amplifyframework.storage.result.StorageUploadInputStreamResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@FlowPreview
@ExperimentalCoroutinesApi
@Suppress("UNCHECKED_CAST")
class KotlinStorageFacadeTest {
    private val delegate = mockk<StorageCategoryBehavior>()
    private val storage = KotlinStorageFacade(delegate)

    /**
     * When the getUrl() delegate emits a value, it should be returned
     * via the coroutine API.
     */
    @Test
    fun getUrlSucceeds() = runBlocking {
        val forRemoteKey = "delete_me.png"
        val result = StorageGetUrlResult.fromUrl(URL("https://s3.amazon.biz/file.png"))
        every {
            delegate.getUrl(eq(forRemoteKey), any(), any(), any())
        } answers {
            val onResultArg = it.invocation.args[/* index of result consumer = */ 2]
            val onResult = onResultArg as Consumer<StorageGetUrlResult>
            onResult.accept(result)
            mockk()
        }
        assertEquals(result, storage.getUrl(forRemoteKey))
    }

    /**
     * When the getUrl() delegate emits an error, it should be thrown from the coroutine API.
     */
    @Test(expected = StorageException::class)
    fun getUrlThrows(): Unit = runBlocking {
        val forRemoteKey = "delete_me.png"
        val error = StorageException("uh", "oh")
        every {
            delegate.getUrl(eq(forRemoteKey), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<StorageException>
            onError.accept(error)
            mockk()
        }
        storage.getUrl(forRemoteKey)
    }

    /**
     * When the downloadFile() delegate emits a result, it should
     * be returned from the coroutine API.
     */
    @Test
    fun downloadFileSucceeds(): Unit = runBlocking {
        val fromRemoteKey = "kool-pic.png"
        val toLocalFile = File("/local/path/kool-pic.png")
        val transferId = UUID.randomUUID().toString()
        val progressEvents = (0L until 101 step 50)
            .map { amount -> StorageTransferProgress(amount, 100) }

        val cancelable = mockk<StorageDownloadFileOperation<*>>()
        every { cancelable.cancel() } answers {}
        every { cancelable.transferId } answers { transferId }

        every {
            delegate.downloadFile(eq(fromRemoteKey), eq(toLocalFile), any(), any(), any(), any())
        } answers {
            val onProgressArg = it.invocation.args[/* index of progress consumer = */ 3]
            val onProgress = onProgressArg as Consumer<StorageTransferProgress>
            val onResultArg = it.invocation.args[/* index of result consumer = */ 4]
            val onResult = onResultArg as Consumer<StorageDownloadFileResult>
            GlobalScope.launch {
                progressEvents.forEach { progressEvent ->
                    delay(200)
                    onProgress.accept(progressEvent)
                }
                onResult.accept(StorageDownloadFileResult.fromFile(toLocalFile))
            }
            cancelable
        }

        val download = storage.downloadFile(fromRemoteKey, toLocalFile)
        assertEquals(transferId, download.transferId)
        val actualProgressEvents = download.progress().take(progressEvents.size).toList()
        assertEquals(progressEvents, actualProgressEvents)
        assertEquals(toLocalFile, download.result().file)
    }

    /**
     * When the downloadFile() kotlin operation invokes pause, resume & cancel operation then corresponding
     * delegate apis are invoked.
     */
    @Test
    fun performActionsOnDownloadFile(): Unit = runBlocking {
        val fromRemoteKey = "kool-pic.png"
        val toLocalFile = File("/local/path/kool-pic.png")
        val transferId = UUID.randomUUID().toString()

        val cancelable = mockk<StorageDownloadFileOperation<*>>()
        every { cancelable.cancel() } answers {}
        every { cancelable.pause() } answers {}
        every { cancelable.resume() } answers {}
        every { cancelable.transferId } answers { transferId }

        every {
            delegate.downloadFile(eq(fromRemoteKey), eq(toLocalFile), any(), any(), any(), any())
        } answers {
            cancelable
        }

        val download = storage.downloadFile(fromRemoteKey, toLocalFile)
        download.pause()
        download.resume()
        download.cancel()
        verifyOrder {
            cancelable.pause()
            cancelable.resume()
            cancelable.cancel()
        }
    }

    /**
     * When the downloadFile() API emits an error, it should be thrown from
     * the coroutine API.
     */
    @Test(expected = StorageException::class)
    fun downloadFileThrows(): Unit = runBlocking {
        val fromRemoteKey = "kool-pic.png"
        val toLocalFile = File("/local/path/kool-pic.png")
        val error = StorageException("uh", "oh")
        val transferId = UUID.randomUUID().toString()
        val cancelable = mockk<StorageDownloadFileOperation<*>>()
        every { cancelable.cancel() } answers {}
        every { cancelable.transferId } answers { transferId }
        every {
            delegate.downloadFile(eq(fromRemoteKey), eq(toLocalFile), any(), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 5
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<StorageException>
            onError.accept(error)
            cancelable
        }

        storage.downloadFile(fromRemoteKey, toLocalFile)
            .result()
    }

    /**
     * When the uploadFile() delegate emits a result, it should be
     * returned via the coroutine API.
     */
    @Test
    fun uploadFileSucceeds() = runBlocking {
        val toRemoteKey = "kool-pic.png"
        val fromLocalFile = File("/local/path/kool-pic.png")

        val progressEvents = (0L until 101 step 50)
            .map { amount -> StorageTransferProgress(amount, 100) }
        val transferId = UUID.randomUUID().toString()
        val cancelable = mockk<StorageUploadFileOperation<*>>()
        every { cancelable.cancel() } answers {}
        every { cancelable.transferId } answers { transferId }
        every {
            delegate.uploadFile(eq(toRemoteKey), eq(fromLocalFile), any(), any(), any(), any())
        } answers {
            val onProgressArg = it.invocation.args[/* index of progress consumer = */ 3]
            val onProgress = onProgressArg as Consumer<StorageTransferProgress>
            val onResultArg = it.invocation.args[/* index of result consumer = */ 4]
            val onResult = onResultArg as Consumer<StorageUploadFileResult>
            GlobalScope.launch {
                progressEvents.forEach { progressEvent ->
                    delay(200)
                    onProgress.accept(progressEvent)
                }
                onResult.accept(StorageUploadFileResult.fromKey(toRemoteKey))
            }
            cancelable
        }

        val upload = storage.uploadFile(toRemoteKey, fromLocalFile)
        assertEquals(transferId, upload.transferId)
        val receivedProgressEvents = upload.progress().take(3).toList()
        assertEquals(transferId, upload.transferId)
        assertEquals(progressEvents, receivedProgressEvents)
        assertEquals(toRemoteKey, upload.result().key)
    }

    /**
     * When the underlying uploadFile() emits an error,
     * it should be thrown from the coroutine API.
     */
    @Test(expected = StorageException::class)
    fun uploadFileThrows(): Unit = runBlocking {
        val toRemoteKey = "kool-pic.png"
        val fromLocalFile = File("/local/path/kool-pic.png")
        val error = StorageException("uh", "oh")
        val transferId = UUID.randomUUID().toString()
        val cancelable = mockk<StorageUploadFileOperation<*>>()
        every { cancelable.cancel() } answers {}
        every { cancelable.transferId } answers { transferId }
        every {
            delegate.uploadFile(eq(toRemoteKey), eq(fromLocalFile), any(), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 5
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<StorageException>
            onError.accept(error)
            cancelable
        }

        storage.uploadFile(toRemoteKey, fromLocalFile)
            .result()
    }

    /**
     * When the uploadFile() kotlin operation invokes pause, resume & cancel operation then corresponding
     * delegate apis are invoked.
     */
    @Test
    fun performActionOnUploadFileSucceeds() = runBlocking {
        val toRemoteKey = "kool-pic.png"
        val fromLocalFile = File("/local/path/kool-pic.png")
        val transferId = UUID.randomUUID().toString()
        val cancelable = mockk<StorageUploadFileOperation<*>>()
        every { cancelable.cancel() } answers {}
        every { cancelable.pause() } answers {}
        every { cancelable.resume() } answers {}
        every { cancelable.transferId } answers { transferId }
        every {
            delegate.uploadFile(eq(toRemoteKey), eq(fromLocalFile), any(), any(), any(), any())
        } answers {
            cancelable
        }

        val upload = storage.uploadFile(toRemoteKey, fromLocalFile)
        upload.pause()
        upload.resume()
        upload.cancel()
        verifyOrder {
            cancelable.pause()
            cancelable.resume()
            cancelable.cancel()
        }
    }

    /**
     * When the underlying uploadInputStream() delegate emits a result,
     * it should be returned from the coroutine API.
     */
    @Test
    fun uploadInputStreamSucceeds() = runBlocking {
        val toRemoteKey = "kool-pic.png"
        val fromStream = mockk<InputStream>()
        val transferId = UUID.randomUUID().toString()
        val progressEvents = (0L until 101 step 50)
            .map { amount -> StorageTransferProgress(amount, 100) }

        val cancelable = mockk<StorageUploadInputStreamOperation<*>>()
        every { cancelable.cancel() } answers {}
        every { cancelable.transferId } answers { transferId }
        every {
            delegate.uploadInputStream(eq(toRemoteKey), eq(fromStream), any(), any(), any(), any())
        } answers {
            val onProgressArg = it.invocation.args[/* index of progress consumer = */ 3]
            val onProgress = onProgressArg as Consumer<StorageTransferProgress>
            val onResultArg = it.invocation.args[/* index of result consumer = */ 4]
            val onResult = onResultArg as Consumer<StorageUploadInputStreamResult>
            GlobalScope.launch {
                progressEvents.forEach { progressEvent ->
                    delay(200)
                    onProgress.accept(progressEvent)
                }
                onResult.accept(StorageUploadInputStreamResult.fromKey(toRemoteKey))
            }
            cancelable
        }

        val upload = storage.uploadInputStream(toRemoteKey, fromStream)
        assertEquals(transferId, upload.transferId)
        val receivedProgressEvents = upload.progress().take(3).toList()
        assertEquals(progressEvents, receivedProgressEvents)
        assertEquals(toRemoteKey, upload.result().key)
    }

    /**
     * When the underlying uploadInputStream() emits an error,
     * it should be thrown from the coroutine API.
     */
    @Test(expected = StorageException::class)
    fun uploadInputStreamThrows(): Unit = runBlocking {
        val toRemoteKey = "kool-pic.png"
        val fromStream = mockk<InputStream>()
        val error = StorageException("uh", "oh")
        val transferId = UUID.randomUUID().toString()
        val cancelable = mockk<StorageUploadInputStreamOperation<*>>()
        every { cancelable.cancel() } answers {}
        every { cancelable.transferId } answers { transferId }

        every {
            delegate.uploadInputStream(eq(toRemoteKey), eq(fromStream), any(), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 5
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<StorageException>
            onError.accept(error)
            cancelable
        }

        storage.uploadInputStream(toRemoteKey, fromStream)
            .result()
    }

    /**
     * When the underlying uploadInputStream() kotlin operation invokes pause, resume & cancel operation then the
     * corresponding delegate apis are invoked.
     */
    @Test
    fun performActionOnUploadInputStream() = runBlocking {
        val toRemoteKey = "kool-pic.png"
        val fromStream = mockk<InputStream>()
        val transferId = UUID.randomUUID().toString()
        val cancelable = mockk<StorageUploadInputStreamOperation<*>>()
        every { cancelable.cancel() } answers {}
        every { cancelable.pause() } answers {}
        every { cancelable.resume() } answers {}
        every { cancelable.transferId } answers { transferId }

        every {
            delegate.uploadInputStream(eq(toRemoteKey), eq(fromStream), any(), any(), any(), any())
        } answers {
            cancelable
        }

        val upload = storage.uploadInputStream(toRemoteKey, fromStream)
        upload.pause()
        upload.resume()
        upload.cancel()
        verifyOrder {
            cancelable.pause()
            cancelable.resume()
            cancelable.cancel()
        }
    }

    /**
     * When the remove() delegate emits a result, it should be returned
     * from the coroutine API.
     */
    @Test
    fun removeSucceeds() = runBlocking {
        val key = "delete_me.png"
        val result = StorageRemoveResult.fromKey(key)
        every {
            delegate.remove(eq(key), any(), any(), any())
        } answers {
            val indexOfResultConsumer = 2
            val arg = it.invocation.args[indexOfResultConsumer]
            val onResult = arg as Consumer<StorageRemoveResult>
            onResult.accept(result)
            mockk()
        }
        assertEquals(result, storage.remove(key))
    }

    /**
     * When the remove() delegate emits an error, it should be thrown
     * from the coroutine API.
     */
    @Test(expected = StorageException::class)
    fun removeThrows(): Unit = runBlocking {
        val key = "delete_me.png"
        val error = StorageException("uh", "oh")
        every {
            delegate.remove(eq(key), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<StorageException>
            onError.accept(error)
            mockk()
        }
        storage.remove(key)
    }

    /**
     * When the list() delegate emits a result, it should be returned through the
     * coroutine API.
     */
    @Test
    fun listSucceeds() = runBlocking {
        val path = "/beach/photos"
        val item = StorageItem("me_at_beach.png", 100L, Date(), "eTag", "props")
        val result = StorageListResult.fromItems(listOf(item), null)
        every {
            delegate.list(eq(path), any<StoragePagedListOptions>(), any(), any())
        } answers {
            val indexOfResultConsumer = 2
            val arg = it.invocation.args[indexOfResultConsumer]
            val onResult = arg as Consumer<StorageListResult>
            onResult.accept(result)
            mockk()
        }
        assertEquals(result, storage.list(path, StoragePagedListOptions.builder().build()))
    }

    /**
     * When the list() delegate emits an error, it should be thrown from the coroutine
     * API.
     */
    @Test(expected = StorageException::class)
    fun listThrows(): Unit = runBlocking {
        val path = "/beach/photos"
        val error = StorageException("uh", "oh")
        every {
            delegate.list(eq(path), any<StoragePagedListOptions>(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<StorageException>
            onError.accept(error)
            mockk()
        }
        storage.list(path, StoragePagedListOptions.builder().build())
    }

    @Test
    fun getTransferSucceeds() = runTest {
        val transferId = UUID.randomUUID().toString()
        val result = mockk<StorageTransferOperation<*, StorageTransferResult>>()
        every {
            delegate.getTransfer(transferId, any(), any())
        } answers {
            val indexOfResultConsumer = 1
            val arg = it.invocation.args[indexOfResultConsumer]
            val onResult = arg as Consumer<StorageTransferOperation<*, StorageTransferResult>>
            onResult.accept(result)
            mockk()
        }
        assertEquals(result, storage.getTransfer(transferId))
    }

    @Test(expected = StorageException::class)
    fun getTransferErrors() = runTest {
        val transferId = UUID.randomUUID().toString()
        val error = StorageException("Transfer invalid", "Suggestion")
        every {
            delegate.getTransfer(transferId, any(), any())
        } answers {
            val indexOfResultConsumer = 2
            val arg = it.invocation.args[indexOfResultConsumer]
            val onError = arg as Consumer<StorageException>
            onError.accept(error)
            mockk()
        }
        storage.getTransfer(transferId)
    }
}
