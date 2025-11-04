/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.configuration.AmplifyOutputs
import com.amplifyframework.storage.StorageAccessLevel
import com.amplifyframework.storage.operation.StorageUploadFileOperation
import com.amplifyframework.storage.options.StorageDownloadFileOptions
import com.amplifyframework.storage.options.StorageGetUrlOptions
import com.amplifyframework.storage.options.StoragePagedListOptions
import com.amplifyframework.storage.options.StorageRemoveOptions
import com.amplifyframework.storage.options.StorageUploadFileOptions
import com.amplifyframework.storage.options.StorageUploadInputStreamOptions
import com.amplifyframework.storage.s3.test.R
import com.amplifyframework.testutils.sync.SynchronousStorage
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.junit.BeforeClass
import org.junit.Test

class StorageCanaryTestGen2 {
    companion object {
        private const val TIMEOUT_S = 20L
        private val TAG = StorageCanaryTestGen2::class.simpleName
        private const val TEMP_DIR_PROPERTY = "java.io.tmpdir"
        private val TEMP_DIR = System.getProperty(TEMP_DIR_PROPERTY)

        @BeforeClass
        @JvmStatic
        fun setup() {
            try {
                Amplify.addPlugin(AWSCognitoAuthPlugin())
                Amplify.addPlugin(AWSS3StoragePlugin())
                Amplify.configure(AmplifyOutputs(R.raw.amplify_outputs), ApplicationProvider.getApplicationContext())
            } catch (error: AmplifyException) {
                Log.e(TAG, "Could not initialize Amplify", error)
            }
        }
    }

    private val syncStorage = SynchronousStorage.delegatingToAmplify()

    @Test
    fun uploadInputStream() {
        val raf = createFile(1)
        val stream = FileInputStream(raf)
        val fileKey = "ExampleKey"
        syncStorage.uploadInputStream(fileKey, stream, StorageUploadInputStreamOptions.defaultInstance())
        removeFile(fileKey)
    }

    @Test
    fun uploadFile() {
        val file = createFile(1)
        val fileKey = UUID.randomUUID().toString()
        syncStorage.uploadFile(fileKey, file, StorageUploadFileOptions.defaultInstance())
        removeFile(fileKey)
    }

    @Test
    fun downloadFile() {
        val file = createFile(1)
        val fileName = "ExampleKey${UUID.randomUUID()}"
        syncStorage.uploadFile(fileName, file, StorageUploadFileOptions.defaultInstance())
        syncStorage.downloadFile(fileName, file, StorageDownloadFileOptions.defaultInstance())
    }

    @Test
    fun getUrl() {
        val result = syncStorage.getUrl("ExampleKey", StorageGetUrlOptions.defaultInstance())
        Log.i(TAG, "Successfully generated: ${result.url}")
    }

    @Test
    fun getTransfer() {
        val file = createFile(100)
        val fileName = "ExampleKey${UUID.randomUUID()}"

        val opFuture = CompletableFuture<StorageUploadFileOperation<*>>()
        val uploadComplete = CompletableFuture<Boolean>()
        val paused = CompletableFuture<Boolean>()

        val op = Amplify.Storage.uploadFile(
            fileName,
            file,
            StorageUploadFileOptions.builder().accessLevel(StorageAccessLevel.PUBLIC).build(),
            { progress ->
                if (progress.currentBytes > 0) {
                    // Block until operation is available
                    opFuture.get(TIMEOUT_S, TimeUnit.SECONDS).pause()
                }
                paused.complete(true)
            },
            {
                uploadComplete.complete(true)
            },
            {
                paused.completeExceptionally(it)
                uploadComplete.complete(false)
            }
        )
        opFuture.complete(op)
        paused.get(TIMEOUT_S, TimeUnit.SECONDS)

        val operation = syncStorage.getTransfer(op.transferId)
        Log.i(TAG, "Current State" + operation.transferState)

        // Ensure the transfer finishes. We don't particularly care if it's successful or not at this point.
        // We just don't want it still going to potentially impact other tests.
        uploadComplete.get(TIMEOUT_S, TimeUnit.SECONDS)
    }

    @Test
    fun list() {
        val options = StoragePagedListOptions.builder()
            .setPageSize(1000)
            .build()
        val result = syncStorage.list("", options)
        result.items.forEach { item ->
            Log.i(TAG, "Item: ${item.key}")
        }
    }

    @Test
    fun remove() {
        val result = syncStorage.remove("myUploadedFileName.txt", StorageRemoveOptions.defaultInstance())
        Log.i(TAG, "Successfully removed: ${result.key}")
    }

    private fun createFile(size: Int): File {
        val file = File(TEMP_DIR!! + File.separator + "file")
        file.createNewFile()
        val raf = RandomAccessFile(file, "rw")
        raf.setLength((size * 1024 * 1024).toLong())
        raf.close()
        file.deleteOnExit()
        return file
    }

    private fun removeFile(fileKey: String) {
        syncStorage.remove(fileKey, StorageRemoveOptions.defaultInstance())
    }
}
