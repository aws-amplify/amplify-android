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

package com.amplifyframework.storage.s3

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.amplifyframework.auth.AuthPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.storage.StorageAccessLevel
import com.amplifyframework.storage.StorageCategory
import com.amplifyframework.storage.options.StorageDownloadFileOptions
import com.amplifyframework.storage.options.StorageUploadFileOptions
import com.amplifyframework.storage.s3.test.R
import com.amplifyframework.storage.s3.util.WorkmanagerTestUtils.initializeWorkmanagerTestUtil
import com.amplifyframework.testutils.FileAssert
import com.amplifyframework.testutils.Sleep
import com.amplifyframework.testutils.random.RandomTempFile
import com.amplifyframework.testutils.sync.SynchronousAuth
import com.amplifyframework.testutils.sync.SynchronousStorage
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.BeforeClass
import org.junit.Test

class StorageStressTest {
    companion object {
        private lateinit var storageCategory: StorageCategory
        lateinit var synchronousStorage: SynchronousStorage
        lateinit var smallFiles: MutableList<File>
        lateinit var largeFile: File
        private val TESTING_ACCESS_LEVEL = StorageAccessLevel.PUBLIC
        private const val LARGE_FILE_SIZE = 100 * 1024 * 1024L // 100MB
        private const val SMALL_FILE_SIZE = 1024 * 1024L // 1MB
        const val LARGE_FILE_NAME = "large-"
        const val SMALL_FILE_NAME = "small-"
        private val uploadOptions = StorageUploadFileOptions.builder().accessLevel(TESTING_ACCESS_LEVEL).build()
        private val downloadOptions = StorageDownloadFileOptions.builder().accessLevel(TESTING_ACCESS_LEVEL).build()
        private const val TRANSFER_TIMEOUT = 10 * 60_000L // 10 minutes
        private const val STRESS_TEST_TIMEOUT = 10 * 60_000L

        /**
         * Initialize mobile client and configure the storage.
         * Upload the test files ahead of time.
         *
         * @throws Exception if mobile client initialization fails
         */
        @BeforeClass
        @JvmStatic
        @Throws(Exception::class)
        fun setUpOnce() {
            val context = getApplicationContext<Context>()
            initializeWorkmanagerTestUtil(context)
            SynchronousAuth.delegatingToCognito(context, AWSCognitoAuthPlugin() as AuthPlugin<*>)

            // Get a handle to storage
            storageCategory = TestStorageCategory.create(context, R.raw.amplifyconfiguration)
            synchronousStorage = SynchronousStorage.delegatingTo(storageCategory)
        }
    }

    /**
     * Calls Storage.downloadFile with random temporary files of size 1MB 50 times
     */
    @Test
    fun testDownloadManyFiles() {
        // Upload to PUBLIC for consistency
        val uploadOptions = StorageUploadFileOptions.builder()
            .accessLevel(TESTING_ACCESS_LEVEL)
            .build()

        // Upload 25 small test files
        var key: String
        smallFiles = mutableListOf()
        val uploadLatch = CountDownLatch(50)
        repeat(50) {
            key = "${SMALL_FILE_NAME}${UUID.randomUUID()}"
            val smallFile = RandomTempFile(key, SMALL_FILE_SIZE)
            synchronousStorage.uploadFile(key, smallFile, uploadOptions, TRANSFER_TIMEOUT)
            uploadLatch.countDown()
            Log.i("STORAGE_STRESS_TEST", "@BeforeClass Small Uploads Left: ${uploadLatch.count}")
            smallFiles.add(smallFile)
        }
        uploadLatch.await(STRESS_TEST_TIMEOUT, TimeUnit.MILLISECONDS)

        Sleep.milliseconds(1000)
        // Upload 1 large test file
        Log.i("STORAGE_STRESS_TEST", "@BeforeClass Large Upload Started")
        val downloadLatch = CountDownLatch(50)
        smallFiles.forEach {
            Thread {
                val downloadFile = RandomTempFile()
                synchronousStorage.downloadFile(it.name, downloadFile, downloadOptions, TRANSFER_TIMEOUT)
                FileAssert.assertEquals(it, downloadFile)

                downloadLatch.countDown()
                Log.i("STORAGE_STRESS_TEST", "Downloads Left: ${downloadLatch.count}")
            }.start()
        }
        downloadLatch.await(STRESS_TEST_TIMEOUT, TimeUnit.MILLISECONDS)
    }

    /**
     * Calls Storage.uploadFile with random temporary files of size 1MB 50 times
     */
    @Test
    fun testUploadManyFiles() {
        val uploadLatch = CountDownLatch(50)
        repeat(50) {
            val key = "${SMALL_FILE_NAME}${UUID.randomUUID()}"
            val smallFile = RandomTempFile(key, SMALL_FILE_SIZE)
            synchronousStorage.uploadFile(key, smallFile, uploadOptions, TRANSFER_TIMEOUT)
            uploadLatch.countDown()
            Log.i("STORAGE_STRESS_TEST", "Small Uploads Left: ${uploadLatch.count}")
        }
        uploadLatch.await(STRESS_TEST_TIMEOUT, TimeUnit.MILLISECONDS)
    }

    /**
     * Calls Storage.uploadFile with a random temporary file of size .5GB
     */
    @Test
    fun testUploadLargeFile() {
        val uploadLatch = CountDownLatch(1)
        val fileName = LARGE_FILE_NAME + UUID.randomUUID().toString()
        val largeFile = RandomTempFile(fileName, LARGE_FILE_SIZE)
        synchronousStorage.uploadFile(fileName, largeFile, uploadOptions, TRANSFER_TIMEOUT)
        uploadLatch.countDown()
        uploadLatch.await(STRESS_TEST_TIMEOUT, TimeUnit.MILLISECONDS)
    }

    /**
     * Calls Storage.downloadFile with a random temporary file of size .5GB
     */
    @Test
    fun testDownloadLargeFile() {
        val uploadOptions = StorageUploadFileOptions.builder()
            .accessLevel(TESTING_ACCESS_LEVEL)
            .build()
        val key = "${LARGE_FILE_NAME}${UUID.randomUUID()}"
        largeFile = RandomTempFile(key, LARGE_FILE_SIZE)
        synchronousStorage.uploadFile(key, largeFile, uploadOptions, TRANSFER_TIMEOUT)
        Log.i("STORAGE_STRESS_TEST", "@BeforeClass Large Upload Complete")
        val downloadLatch = CountDownLatch(1)
        val downloadFile = RandomTempFile()
        synchronousStorage.downloadFile(largeFile.name, downloadFile, downloadOptions, TRANSFER_TIMEOUT)
        FileAssert.assertEquals(largeFile, downloadFile)
        downloadLatch.countDown()
        downloadLatch.await(STRESS_TEST_TIMEOUT, TimeUnit.MILLISECONDS)
    }
}
