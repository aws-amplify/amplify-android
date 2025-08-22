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
package com.amplifyframework.storage.s3

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.async.Cancelable
import com.amplifyframework.core.async.Resumable
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import com.amplifyframework.hub.SubscriptionToken
import com.amplifyframework.storage.BucketInfo
import com.amplifyframework.storage.StorageBucket
import com.amplifyframework.storage.StorageCategory
import com.amplifyframework.storage.StorageChannelEventName
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.StoragePath
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.TransferState.Companion.getState
import com.amplifyframework.storage.operation.StorageUploadFileOperation
import com.amplifyframework.storage.options.StorageRemoveOptions
import com.amplifyframework.storage.options.StorageUploadFileOptions
import com.amplifyframework.storage.s3.options.AWSS3StorageUploadFileOptions
import com.amplifyframework.storage.s3.test.R
import com.amplifyframework.storage.s3.util.WorkmanagerTestUtils.initializeWorkmanagerTestUtil
import com.amplifyframework.testutils.random.RandomTempFile
import com.amplifyframework.testutils.sync.SynchronousAuth
import com.amplifyframework.testutils.sync.SynchronousStorage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test

/**
 * Instrumentation test for operational work on upload.
 */
class AWSS3StorageMultiBucketUploadTest {
    private val defaultFileOptions = StorageUploadFileOptions
        .builder()
        .bucket(TestStorageCategory.getStorageBucket())
        .build()

    // Create a set to remember all the subscriptions
    private val subscriptions = mutableSetOf<SubscriptionToken>()
    private lateinit var storagePath: StoragePath
    companion object {
        private const val LARGE_FILE_SIZE = 10 * 1024 * 1024L // 10 MB
        private const val SMALL_FILE_SIZE = 100L
        private val EXTENDED_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(60)

        lateinit var storageCategory: StorageCategory
        lateinit var synchronousStorage: SynchronousStorage
        lateinit var synchronousAuth: SynchronousAuth

        /**
         * Initialize mobile client and configure the storage.
         *
         */
        @JvmStatic
        @BeforeClass
        fun setUpOnce() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            initializeWorkmanagerTestUtil(context)
            synchronousAuth = SynchronousAuth.delegatingToCognito(context, AWSCognitoAuthPlugin())

            // Get a handle to storage
            storageCategory = TestStorageCategory.create(context, R.raw.amplifyconfiguration)
            synchronousStorage = SynchronousStorage.delegatingTo(storageCategory)
        }
    }

    /**
     * Unsubscribe from everything after each test.
     * Remove test file
     */
    @After
    fun tearDown() {
        // Unsubscribe from everything
        for (token in subscriptions) {
            Amplify.Hub.unsubscribe(token)
        }
        try {
            synchronousStorage.remove(storagePath, StorageRemoveOptions.defaultInstance())
        } catch (ex: StorageException) {
            // in some cases, access denied exception made occur here
        }
    }

    @Test
    fun testUploadSmallFile() {
        val uploadFile: File = RandomTempFile(SMALL_FILE_SIZE)
        storagePath = StoragePath.fromString("public/${uploadFile.name}")
        synchronousStorage.uploadFile(storagePath, uploadFile, defaultFileOptions)
    }

    @Test
    fun testUploadLargeFile() {
        val uploadFile: File = RandomTempFile(LARGE_FILE_SIZE)
        storagePath = StoragePath.fromString("public/${uploadFile.name}")
        val options = AWSS3StorageUploadFileOptions.builder().setUseAccelerateEndpoint(true).build()
        synchronousStorage.uploadFile(storagePath, uploadFile, options, EXTENDED_TIMEOUT_MS)
    }

    @Test
    fun testUploadFileIsCancelable() {
        val canceled = CountDownLatch(1)
        val opContainer = AtomicReference<Cancelable>()
        val errorContainer = AtomicReference<Throwable>()

        // Create a file large enough that transfer won't finish before being canceled
        val uploadFile: File = RandomTempFile(LARGE_FILE_SIZE)
        storagePath = StoragePath.fromString("public/${uploadFile.name}")

        // Listen to Hub events for cancel
        val cancelToken = Amplify.Hub.subscribe(HubChannel.STORAGE) { hubEvent: HubEvent<*> ->
            if (StorageChannelEventName.UPLOAD_STATE.toString() == hubEvent.name) {
                val state = getState(hubEvent.data as String)
                if (TransferState.CANCELED == state) {
                    canceled.countDown()
                }
            }
        }
        subscriptions.add(cancelToken)

        // Begin uploading a large file
        val op = storageCategory.uploadFile(
            storagePath,
            uploadFile,
            defaultFileOptions,
            {
                if (it.currentBytes > 0) {
                    opContainer.get().cancel()
                }
            },
            { errorContainer.set(RuntimeException("Upload completed without canceling.")) },
            { errorContainer.set(it) }
        )

        opContainer.set(op)

        // Assert that the required conditions have been met
        canceled.await(EXTENDED_TIMEOUT_MS, TimeUnit.MILLISECONDS) shouldBe true
        errorContainer.get() shouldBe null
    }

    @Test
    fun testUploadFileIsResumable() {
        val completed = CountDownLatch(1)
        val resumed = CountDownLatch(1)
        val opContainer = AtomicReference<Resumable>()
        val errorContainer = AtomicReference<Throwable>()

        // Create a file large enough that transfer won't finish before being paused
        val uploadFile: File = RandomTempFile(LARGE_FILE_SIZE)
        storagePath = StoragePath.fromString("public/${uploadFile.name}")

        // Listen to Hub events to resume when operation has been paused
        val resumeToken = Amplify.Hub.subscribe(HubChannel.STORAGE) { hubEvent: HubEvent<*> ->
            if (StorageChannelEventName.UPLOAD_STATE.toString() == hubEvent.name) {
                val state = getState(hubEvent.data as String)
                if (TransferState.PAUSED == state) {
                    opContainer.get().resume()
                    resumed.countDown()
                }
            }
        }
        subscriptions.add(resumeToken)

        // Begin uploading a large file
        val op = storageCategory.uploadFile(
            storagePath,
            uploadFile,
            defaultFileOptions,
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
        resumed.await(EXTENDED_TIMEOUT_MS, TimeUnit.MILLISECONDS) shouldBe true
        completed.await(EXTENDED_TIMEOUT_MS, TimeUnit.MILLISECONDS) shouldBe true
        errorContainer.get() shouldBe null
    }

    @Test
    fun testUploadFileGetTransferOnPause() {
        val completed = CountDownLatch(1)
        val resumed = CountDownLatch(1)
        val transferId = AtomicReference<String>()
        val opContainer = AtomicReference<StorageUploadFileOperation<*>>()
        val errorContainer = AtomicReference<Throwable>()

        // Create a file large enough that transfer won't finish before being paused
        val uploadFile: File = RandomTempFile(LARGE_FILE_SIZE)
        storagePath = StoragePath.fromString("public/${uploadFile.name}")

        // Listen to Hub events to resume when operation has been paused
        val resumeToken = Amplify.Hub.subscribe(HubChannel.STORAGE) { hubEvent: HubEvent<*> ->
            if (StorageChannelEventName.UPLOAD_STATE.toString() == hubEvent.name) {
                val state = getState(hubEvent.data as String)
                if (TransferState.PAUSED == state) {
                    opContainer.get().clearAllListeners()
                    storageCategory.getTransfer(
                        transferId.get(),
                        {
                            val getOp = it as StorageUploadFileOperation
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

        // Begin uploading a large file
        val op = storageCategory.uploadFile(
            storagePath,
            uploadFile,
            defaultFileOptions,
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
        resumed.await(EXTENDED_TIMEOUT_MS, TimeUnit.MILLISECONDS) shouldBe true
        completed.await(EXTENDED_TIMEOUT_MS, TimeUnit.MILLISECONDS) shouldBe true
        errorContainer.get() shouldBe null
    }

    @Test
    fun testUploadFromInvalidBucket() {
        val bucketName = "amplify-android-storage-integration-test-123xyz"
        val region = "us-east-1"
        val bucketInfo = BucketInfo(bucketName, region)
        val storageBucket = StorageBucket.fromBucketInfo(bucketInfo)
        val option = StorageUploadFileOptions.builder().bucket(storageBucket).build()
        val uploadFile: File = RandomTempFile(SMALL_FILE_SIZE)
        storagePath = StoragePath.fromString("public/${uploadFile.name}")

        shouldThrow<StorageException> {
            synchronousStorage.uploadFile(storagePath, uploadFile, option)
        }
    }
}
