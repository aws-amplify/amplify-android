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
package com.amplifyframework.storage.s3

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.auth.AuthPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.async.Cancelable
import com.amplifyframework.core.async.Resumable
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import com.amplifyframework.hub.SubscriptionToken
import com.amplifyframework.storage.StorageCategory
import com.amplifyframework.storage.StorageChannelEventName
import com.amplifyframework.storage.StoragePath
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.TransferState.Companion.getState
import com.amplifyframework.storage.operation.StorageDownloadFileOperation
import com.amplifyframework.storage.options.StorageDownloadFileOptions
import com.amplifyframework.storage.options.StorageUploadFileOptions
import com.amplifyframework.storage.s3.options.AWSS3StorageDownloadFileOptions
import com.amplifyframework.storage.s3.test.R
import com.amplifyframework.storage.s3.util.WorkmanagerTestUtils.initializeWorkmanagerTestUtil
import com.amplifyframework.testutils.FileAssert
import com.amplifyframework.testutils.random.RandomTempFile
import com.amplifyframework.testutils.sync.SynchronousAuth
import com.amplifyframework.testutils.sync.SynchronousStorage
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * Instrumentation test for operational work on download.
 */
class AWSS3StoragePathDownloadTest {
    // Create a file to download to
    private val downloadFile: File = RandomTempFile()
    private val options = StorageDownloadFileOptions.defaultInstance()
    // Create a set to remember all the subscriptions
    private val subscriptions = mutableSetOf<SubscriptionToken>()

    companion object {
        private val EXTENDED_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(60)
        private const val LARGE_FILE_SIZE = 10 * 1024 * 1024L // 10 MB
        private const val SMALL_FILE_SIZE = 100L
        private val LARGE_FILE_NAME = "large-${System.currentTimeMillis()}"
        private val LARGE_FILE_PATH = StoragePath.fromString("public/$LARGE_FILE_NAME")
        private val SMALL_FILE_NAME = "small-${System.currentTimeMillis()}"
        private val SMALL_FILE_PATH = StoragePath.fromString("public/$SMALL_FILE_NAME")

        lateinit var storageCategory: StorageCategory
        lateinit var synchronousStorage: SynchronousStorage
        lateinit var largeFile: File
        lateinit var smallFile: File

        /**
         * Initialize mobile client and configure the storage.
         * Upload the test files ahead of time.
         */
        @JvmStatic
        @BeforeClass
        fun setUpOnce() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            initializeWorkmanagerTestUtil(context)
            SynchronousAuth.delegatingToCognito(context, AWSCognitoAuthPlugin() as AuthPlugin<*>)

            // Get a handle to storage
            storageCategory = TestStorageCategory.create(context, R.raw.amplifyconfiguration)
            synchronousStorage = SynchronousStorage.delegatingTo(storageCategory)

            val uploadOptions = StorageUploadFileOptions.defaultInstance()

            // Upload large test file
            largeFile = RandomTempFile(LARGE_FILE_NAME, LARGE_FILE_SIZE)
            synchronousStorage.uploadFile(LARGE_FILE_PATH, largeFile, uploadOptions, EXTENDED_TIMEOUT_MS)

            // Upload small test file
            smallFile = RandomTempFile(SMALL_FILE_NAME, SMALL_FILE_SIZE)
            synchronousStorage.uploadFile(SMALL_FILE_PATH, smallFile, uploadOptions)
        }
    }

    /**
     * Unsubscribe from everything after each test.
     */
    @After
    fun tearDown() {
        // Unsubscribe from everything
        for (token in subscriptions) {
            Amplify.Hub.unsubscribe(token)
        }
    }

    @Test
    fun testDownloadSmallFile() {
        synchronousStorage.downloadFile(SMALL_FILE_PATH, downloadFile, options)
        FileAssert.assertEquals(smallFile, downloadFile)
    }

    @Test
    fun testDownloadLargeFile() {
        synchronousStorage.downloadFile(
            LARGE_FILE_PATH,
            downloadFile,
            options,
            EXTENDED_TIMEOUT_MS
        )
        FileAssert.assertEquals(largeFile, downloadFile)
    }

    @Test
    fun testDownloadFileIsCancelable() {
        val canceled = CountDownLatch(1)
        val opContainer = AtomicReference<Cancelable>()
        val errorContainer = AtomicReference<Throwable>()

        // Listen to Hub events for cancel
        val cancelToken = Amplify.Hub.subscribe(HubChannel.STORAGE) { hubEvent: HubEvent<*> ->
            if (StorageChannelEventName.DOWNLOAD_STATE.toString() == hubEvent.name) {
                val state = getState(hubEvent.data as String)
                if (TransferState.CANCELED == state) {
                    canceled.countDown()
                }
            }
        }
        subscriptions.add(cancelToken)

        // Begin downloading a large file
        val op = storageCategory.downloadFile(
            LARGE_FILE_PATH,
            downloadFile,
            options,
            {
                if (it.currentBytes > 0 && canceled.count > 0) {
                    opContainer.get().cancel()
                }
            },
            { errorContainer.set(RuntimeException("Download completed without canceling.")) },
            { newValue -> errorContainer.set(newValue) }
        )
        opContainer.set(op)

        // Assert that the required conditions have been met
        assertTrue(canceled.await(EXTENDED_TIMEOUT_MS, TimeUnit.MILLISECONDS))
        assertNull(errorContainer.get())
    }

    @Test
    fun testDownloadFileIsResumable() {
        val completed = CountDownLatch(1)
        val resumed = CountDownLatch(1)
        val opContainer = AtomicReference<Resumable>()
        val errorContainer = AtomicReference<Throwable>()

        // Listen to Hub events to resume when operation has been paused
        val resumeToken = Amplify.Hub.subscribe(HubChannel.STORAGE) { hubEvent: HubEvent<*> ->
            if (StorageChannelEventName.DOWNLOAD_STATE.toString() == hubEvent.name) {
                val state = getState(hubEvent.data as String)
                if (TransferState.PAUSED == state) {
                    opContainer.get().resume()
                    resumed.countDown()
                }
            }
        }
        subscriptions.add(resumeToken)

        // Begin downloading a large file
        val op = storageCategory.downloadFile(
            LARGE_FILE_PATH,
            downloadFile,
            options,
            {
                if (it.currentBytes > 0 && resumed.count > 0) {
                    opContainer.get().pause()
                }
            },
            { completed.countDown() },
            { errorContainer.set(it) }
        )
        opContainer.set(op)

        // Assert that all the required conditions have been met
        assertTrue(resumed.await(EXTENDED_TIMEOUT_MS, TimeUnit.MILLISECONDS))
        assertTrue(completed.await(EXTENDED_TIMEOUT_MS, TimeUnit.MILLISECONDS))
        assertNull(errorContainer.get())
        FileAssert.assertEquals(largeFile, downloadFile)
    }

    @Test
    fun testGetTransferOnPause() {
        val completed = CountDownLatch(1)
        val resumed = CountDownLatch(1)
        val opContainer = AtomicReference<StorageDownloadFileOperation<*>>()
        val transferId = AtomicReference<String>()
        val errorContainer = AtomicReference<Throwable>()
        // Listen to Hub events to resume when operation has been paused
        val resumeToken = Amplify.Hub.subscribe(HubChannel.STORAGE) { hubEvent: HubEvent<*> ->
            if (StorageChannelEventName.DOWNLOAD_STATE.toString() == hubEvent.name) {
                val state = getState(hubEvent.data as String)
                if (TransferState.PAUSED == state) {
                    opContainer.get().clearAllListeners()
                    storageCategory.getTransfer(
                        transferId.get(),
                        {
                            val getOp = it as StorageDownloadFileOperation<*>
                            getOp.resume()
                            resumed.countDown()
                            getOp.setOnSuccess { completed.countDown() }
                        },
                        { errorContainer.set(it) }
                    )
                }
            }
        }
        subscriptions.add(resumeToken)

        // Begin downloading a large file
        val op = storageCategory.downloadFile(
            LARGE_FILE_PATH,
            downloadFile,
            options,
            {
                if (it.currentBytes > 0 && resumed.count > 0) {
                    opContainer.get().pause()
                }
            },
            { },
            { errorContainer.set(it) }
        )

        opContainer.set(op)
        transferId.set(op.transferId)

        // Assert that all the required conditions have been met
        assertTrue(resumed.await(EXTENDED_TIMEOUT_MS, TimeUnit.MILLISECONDS))
        assertTrue(completed.await(EXTENDED_TIMEOUT_MS, TimeUnit.MILLISECONDS))
        assertNull(errorContainer.get())
        FileAssert.assertEquals(largeFile, downloadFile)
    }

    @Test
    fun testDownloadLargeFileWithAccelerationEnabled() {
        val awsS3Options = AWSS3StorageDownloadFileOptions.builder().setUseAccelerateEndpoint(true).build()
        synchronousStorage.downloadFile(
            LARGE_FILE_PATH,
            downloadFile,
            awsS3Options,
            EXTENDED_TIMEOUT_MS
        )
    }
}
