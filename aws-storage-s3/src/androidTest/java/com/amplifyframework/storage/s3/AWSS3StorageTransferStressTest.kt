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
import com.amplifyframework.storage.StorageAccessLevel
import com.amplifyframework.storage.StorageCategory
import com.amplifyframework.storage.options.StorageDownloadFileOptions
import com.amplifyframework.storage.options.StorageUploadFileOptions
import com.amplifyframework.storage.s3.helper.AmplifyTransferServiceTestHelper
import com.amplifyframework.storage.s3.test.R
import com.amplifyframework.testutils.FileAssert
import com.amplifyframework.testutils.random.RandomTempFile
import com.amplifyframework.testutils.sync.SynchronousMobileClient
import com.amplifyframework.testutils.sync.SynchronousStorage
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import kotlin.random.Random
import org.junit.After
import org.junit.Before
import org.junit.Test

class AWSS3StorageTransferStressTest {

    private lateinit var storageCategory: StorageCategory
    private lateinit var synchronousStorage: SynchronousStorage
    private lateinit var filesToDownload: MutableList<File>

    private val uploadOptions = StorageUploadFileOptions.builder().accessLevel(TESTING_ACCESS_LEVEL).build()
    private val downloadOptions = StorageDownloadFileOptions.builder().accessLevel(TESTING_ACCESS_LEVEL).build()

    @Before
    fun setUp() {
        val context = getApplicationContext<Context>()

        AmplifyTransferServiceTestHelper.stopForegroundAndUnbind(getApplicationContext())

        // Init auth stuff
        SynchronousMobileClient.instance().initialize()

        // Get a handle to storage
        storageCategory = TestStorageCategory.create(context, R.raw.amplifyconfiguration)
        synchronousStorage = SynchronousStorage.delegatingTo(storageCategory)

        // Upload 25 files to later download
        filesToDownload = mutableListOf()
        val uploadLatch = CountDownLatch(15)
        for (i in 1..15) {
            val fileName = "${FILE_PREFIX}$i"
            // random file size from 1-2MB
            val randomFile = RandomTempFile(fileName, 1024L * 1024 * Random.nextInt(1, 3))
            Thread {
                synchronousStorage.uploadFile(fileName, randomFile, uploadOptions, STRESS_TEST_TIMEOUT)
                uploadLatch.countDown()
                Log.e("UPLOADS_LEFT", "${uploadLatch.count}")
            }.start()
            filesToDownload.add(randomFile)
        }

        uploadLatch.await(STRESS_TEST_TIMEOUT, TimeUnit.MILLISECONDS)
    }

    @After
    fun tearDown() {
        AmplifyTransferServiceTestHelper.stopForegroundAndUnbind(getApplicationContext())
    }

    @Test
    fun stressTestTransfersManyTransfers() {
        val transfersLatch = CountDownLatch(30)

        // start 25 downloads in random order
        filesToDownload.forEach {
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    Thread {
                        val downloadFile = RandomTempFile()
                        synchronousStorage.downloadFile(it.name, downloadFile, downloadOptions, STRESS_TEST_TIMEOUT)
                        FileAssert.assertEquals(it, downloadFile)
                        transfersLatch.countDown()
                        Log.e("TRANSFERS_LEFT", "${transfersLatch.count}")
                    }.start()
                },
                Random.nextLong(0, 5_000)
            )
        }

        // start 25 uploads in random order
        for (i in 16..30) {
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    Thread {
                        val fileName = "${FILE_PREFIX}$i"
// random file size from 1-2MB
                        val randomFile = RandomTempFile(fileName, 1024L * 1024 * Random.nextInt(1, 3))
                        synchronousStorage.uploadFile(fileName, randomFile, uploadOptions, STRESS_TEST_TIMEOUT)
                        transfersLatch.countDown()
                        Log.e("TRANSFERS_LEFT", "${transfersLatch.count}")
                    }.start()
                },
                Random.nextLong(0, 5_000)
            )
        }

        // make sure transfer service notification shows once transfer begins
        Handler(Looper.getMainLooper()).postDelayed(
            { assertTrue(AmplifyTransferServiceTestHelper.isNotificationShowing()) },
            4_000
        )

        // If latch completes, test passes
        transfersLatch.await(STRESS_TEST_TIMEOUT, TimeUnit.MILLISECONDS)

        // Ensure AmplifyTransferService Foreground is removed within 10 seconds of transfers completing
        Thread.sleep(10_000)
        assertFalse(AmplifyTransferServiceTestHelper.isNotificationShowing())
    }

    companion object {
        const val STRESS_TEST_TIMEOUT = 60_000L
        val TESTING_ACCESS_LEVEL = StorageAccessLevel.PUBLIC
        const val FILE_PREFIX = "integration_test_file_"
    }
}
