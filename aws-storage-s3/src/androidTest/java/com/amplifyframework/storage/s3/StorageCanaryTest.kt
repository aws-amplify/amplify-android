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
import com.amplifyframework.logging.AndroidLoggingPlugin
import com.amplifyframework.logging.LogLevel
import com.amplifyframework.storage.StorageAccessLevel
import com.amplifyframework.storage.operation.StorageUploadFileOperation
import com.amplifyframework.storage.options.StoragePagedListOptions
import com.amplifyframework.storage.options.StorageUploadFileOptions
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test

class StorageCanaryTest {
    companion object {
        private const val TIMEOUT_S = 20L
        private val TAG = StorageCanaryTest::class.simpleName
        private const val TEMP_DIR_PROPERTY = "java.io.tmpdir"
        private val TEMP_DIR = System.getProperty(TEMP_DIR_PROPERTY)

        @BeforeClass
        @JvmStatic
        fun setup() {
            try {
                Amplify.addPlugin(AWSCognitoAuthPlugin())
                Amplify.addPlugin(AWSS3StoragePlugin())
                Amplify.addPlugin(AndroidLoggingPlugin(LogLevel.VERBOSE))
                Amplify.configure(ApplicationProvider.getApplicationContext())
                Log.i(TAG, "Initialized Amplify")
            } catch (error: AmplifyException) {
                Log.e(TAG, "Could not initialize Amplify", error)
            }
        }
    }

    @Test
    fun uploadInputStream() {
        val latch = CountDownLatch(1)
        val raf = createFile(1)
        val stream = FileInputStream(raf)
        try {
            Amplify.Storage.uploadInputStream(
                "ExampleKey",
                stream,
                {
                    Log.i(TAG, "Successfully uploaded: ${it.key}")
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Upload failed", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun uploadFile() {
        val latch = CountDownLatch(1)
        val file = createFile(1)
        try {
            Amplify.Storage.uploadFile(
                "ExampleKey",
                file,
                {
                    Log.i(TAG, "Successfully uploaded: ${it.key}")
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Upload failed", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun downloadFile() {
        val latch = CountDownLatch(1)
        val uploadLatch = CountDownLatch(1)
        val file = createFile(1)
        val fileName = "ExampleKey${UUID.randomUUID()}"
        Amplify.Storage.uploadFile(
            fileName,
            file,
            {
                Log.i(TAG, "Successfully uploaded: ${it.key}")
                uploadLatch.countDown()
            },
            { Log.e(TAG, "Upload failed", it) }
        )
        latch.await(TIMEOUT_S, TimeUnit.SECONDS)
        try {
            Amplify.Storage.downloadFile(
                fileName,
                file,
                {
                    Log.i(TAG, "Successfully downloaded: ${it.file.name}")
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Download Failure", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun getUrl() {
        val latch = CountDownLatch(1)
        try {
            Amplify.Storage.getUrl(
                "ExampleKey",
                {
                    Log.i(TAG, "Successfully generated: ${it.url}")
                    latch.countDown()
                },
                {
                    Log.e(TAG, "URL generation failure", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun getTransfer() {
        val uploadLatch = CountDownLatch(1)
        val latch = CountDownLatch(1)
        val file = createFile(100)
        val fileName = "ExampleKey${UUID.randomUUID()}"
        val transferId = AtomicReference<String>()
        val opContainer = AtomicReference<StorageUploadFileOperation<*>>()
        val op = Amplify.Storage.uploadFile(
            fileName,
            file,
            StorageUploadFileOptions.builder().accessLevel(StorageAccessLevel.PUBLIC).build(),
            { progress ->
                if (progress.currentBytes > 0) {
                    opContainer.get().pause()
                }
                uploadLatch.countDown()
            },
            { Log.i(TAG, "Successfully uploaded: ${it.key}") },
            { Log.e(TAG, "Upload failed", it) }
        )
        opContainer.set(op)
        transferId.set(op.transferId)
        uploadLatch.await(TIMEOUT_S, TimeUnit.SECONDS)

        try {
            Amplify.Storage.getTransfer(
                transferId.get(),
                { operation ->
                    Log.i(TAG, "Current State" + operation.transferState)
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Failed to query transfer", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun list() {
        val options = StoragePagedListOptions.builder()
            .setPageSize(1000)
            .build()

        val latch = CountDownLatch(1)
        try {
            Amplify.Storage.list(
                "",
                options,
                { result ->
                    result.items.forEach { item ->
                        Log.i(TAG, "Item: ${item.key}")
                    }
                    latch.countDown()
                },
                {
                    Log.e(TAG, "List failure", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun remove() {
        val latch = CountDownLatch(1)
        try {
            Amplify.Storage.remove(
                "myUploadedFileName.txt",
                {
                    Log.i(TAG, "Successfully removed: ${it.key}")
                    latch.countDown()
                },
                {
                    Log.e(TAG, "Remove failure", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
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
}
