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
import com.amplifyframework.storage.BucketInfo
import com.amplifyframework.storage.StorageBucket
import com.amplifyframework.storage.StorageCategory
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.options.StorageDownloadFileOptions
import com.amplifyframework.storage.options.StorageRemoveOptions
import com.amplifyframework.storage.options.StorageUploadFileOptions
import com.amplifyframework.storage.s3.test.R
import com.amplifyframework.storage.s3.util.WorkmanagerTestUtils.initializeWorkmanagerTestUtil
import com.amplifyframework.testutils.random.RandomTempFile
import com.amplifyframework.testutils.sync.SynchronousAuth
import com.amplifyframework.testutils.sync.SynchronousStorage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.io.File
import org.junit.BeforeClass
import org.junit.Test

/**
 * Instrumentation test for operational work on remove.
 */
class AWSS3StorageMultiBucketRemoveTest {
    // Create a file to download to
    private val downloadFile: File = RandomTempFile()

    private companion object {
        const val SMALL_FILE_SIZE = 100L
        val SMALL_FILE_NAME = "small-${System.currentTimeMillis()}"

        lateinit var storageCategory: StorageCategory
        lateinit var synchronousStorage: SynchronousStorage
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

            SynchronousAuth.delegatingToCognito(context, AWSCognitoAuthPlugin())

            storageCategory = TestStorageCategory.create(context, R.raw.amplifyconfiguration)
            synchronousStorage = SynchronousStorage.delegatingTo(storageCategory)

            // Upload small test file
            smallFile = RandomTempFile(SMALL_FILE_NAME, SMALL_FILE_SIZE)
            synchronousStorage.uploadFile(SMALL_FILE_NAME, smallFile, StorageUploadFileOptions.defaultInstance())
        }
    }

    @Test
    fun testRemove() {
        val options = StorageRemoveOptions.builder().bucket(TestStorageCategory.getStorageBucket()).build()
        val result = synchronousStorage.remove(SMALL_FILE_NAME, options)

        result.path shouldBe "public/$SMALL_FILE_NAME"
        shouldThrow<StorageException> {
            synchronousStorage.downloadFile(
                SMALL_FILE_NAME,
                downloadFile,
                StorageDownloadFileOptions.defaultInstance()
            )
        }
    }

    @Test
    fun testRemoveFromInvalidBucket() {
        val bucketName = "amplify-android-storage-integration-test-123xyz"
        val region = "us-east-1"
        val bucketInfo = BucketInfo(bucketName, region)
        val storageBucket = StorageBucket.fromBucketInfo(bucketInfo)
        val option = StorageRemoveOptions.builder().bucket(storageBucket).build()

        shouldThrow<StorageException> {
            synchronousStorage.remove(SMALL_FILE_NAME, option)
        }
    }
}
