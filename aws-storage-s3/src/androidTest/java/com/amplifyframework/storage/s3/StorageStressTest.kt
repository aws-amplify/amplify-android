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
import android.os.Handler
import android.os.Looper
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
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class StorageStressTest {
    companion object {
        private lateinit var storageCategory: StorageCategory
        lateinit var synchronousStorage: SynchronousStorage
        lateinit var filesToDownload: MutableList<File>
        private val TESTING_ACCESS_LEVEL = StorageAccessLevel.PUBLIC
        private const val LARGE_FILE_SIZE = 50 * 1024 * 1024L // 100 MB
        private const val SMALL_FILE_SIZE = 10 * 1024 * 1024L // 1MB
        const val LARGE_FILE_NAME = "large-"
        const val SMALL_FILE_NAME = "small-"
        lateinit var largeFile: File
        private val uploadOptions = StorageUploadFileOptions.builder().accessLevel(TESTING_ACCESS_LEVEL).build()
        private val downloadOptions = StorageDownloadFileOptions.builder().accessLevel(TESTING_ACCESS_LEVEL).build()
        private const val UPLOAD_TIMEOUT = 3 * 100_000L // 5 minutes
        private const val STRESS_TEST_TIMEOUT = 60_000L


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

            // Upload to PUBLIC for consistency
            val uploadOptions = StorageUploadFileOptions.builder()
                .accessLevel(TESTING_ACCESS_LEVEL)
                .build()

            // Upload 25 small test files
            var key = ""
//            filesToDownload = mutableListOf()
//            val uploadLatch = CountDownLatch(15)
//            for (i in 1..15) {
//                Sleep.milliseconds(500)
//                key = "${SMALL_FILE_NAME}${System.currentTimeMillis()}"
//                val smallFile = RandomTempFile(key, SMALL_FILE_SIZE)
//                Thread {
//                    synchronousStorage.uploadFile(key, smallFile, uploadOptions, UPLOAD_TIMEOUT)
//                    uploadLatch.countDown()
//                    Log.i("STORAGE_STRESS_TEST","@BeforeClass Small Uploads Left: ${uploadLatch.count}")
//                }.start()
//                filesToDownload.add(smallFile)
//            }
//            uploadLatch.await(STRESS_TEST_TIMEOUT, TimeUnit.MILLISECONDS)

            // Upload large test file
            Log.i("STORAGE_STRESS_TEST","@BeforeClass Large Upload Started")
            key = "${LARGE_FILE_NAME}${System.currentTimeMillis()}"
            largeFile = RandomTempFile(key, LARGE_FILE_SIZE)
            synchronousStorage.uploadFile(key, largeFile, uploadOptions, UPLOAD_TIMEOUT)
            Log.i("STORAGE_STRESS_TEST","@BeforeClass Large Upload Complete")
        }
    }

    /**
     * Calls Storage.downloadFile with random temporary files of size 1MB 50 times
     */
    @Test
    fun testDownloadManyFiles() {
        Log.i("STORAGE_STRESS_TEST","Here0")
        val transfersLatch = CountDownLatch(15)
        filesToDownload.forEach {
            Log.i("STORAGE_STRESS_TEST","Here1")
//            Handler(Looper.getMainLooper()).postDelayed(
//                {
                    Thread {
                        Log.i("STORAGE_STRESS_TEST","Here2")
                        val downloadFile = RandomTempFile()
                        synchronousStorage.downloadFile(it.name, downloadFile, downloadOptions, STRESS_TEST_TIMEOUT)
                        FileAssert.assertEquals(it, downloadFile)
                        transfersLatch.countDown()
                        Log.i("STORAGE_STRESS_TEST","Downloads Left: ${transfersLatch.count}")
                    }.start()
//                },
//                2000
//            )
        }
    }

    /**
     * Calls Storage.uploadFile with random temporary files of size 1MB 50 times
     */
    @Test
    fun testUploadManyFiles() {
        val uploadLatch = CountDownLatch(15)
        for (i in 1..15) {
            val fileName = "${SMALL_FILE_NAME}$i"
            val smallFile = RandomTempFile(fileName, SMALL_FILE_SIZE)
            Thread {
                synchronousStorage.uploadFile(fileName, smallFile, uploadOptions, STRESS_TEST_TIMEOUT)
                uploadLatch.countDown()
                Log.i("STORAGE_STRESS_TEST","Uploads Left: ${uploadLatch.count}")
            }.start()
            filesToDownload.add(smallFile)
        }
        uploadLatch.await(STRESS_TEST_TIMEOUT, TimeUnit.MILLISECONDS)
    }

    /**
     * Calls Storage.uploadFile with a random temporary file of size 1GB
     */
    @Test
    fun testUploadLargeFile() {
        val fileName = LARGE_FILE_NAME
        val largeFile = RandomTempFile(fileName, LARGE_FILE_SIZE)
        synchronousStorage.uploadFile(fileName, largeFile, uploadOptions, STRESS_TEST_TIMEOUT)
    }

    /**
     * Calls Storage.downloadFile with a random temporary file of size 1GB
     */
    @Test
    fun testDownloadLargeFile() {
        val downloadFile = RandomTempFile()
        synchronousStorage.downloadFile(largeFile.name, downloadFile, downloadOptions, UPLOAD_TIMEOUT)
        FileAssert.assertEquals(largeFile, downloadFile)
        Log.i("STORAGE_STRESS_TEST", "Download large file complete")
    }
}