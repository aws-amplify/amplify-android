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
import com.amplifyframework.storage.StoragePath
import com.amplifyframework.storage.options.StoragePagedListOptions
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
import java.util.concurrent.TimeUnit
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

/**
 * Instrumentation test for operational work on download.
 */
class AWSS3StorageMultiBucketListTest {
    companion object {
        private val EXTENDED_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(60)
        private const val LARGE_FILE_SIZE = 10 * 1024 * 1024L // 10 MB
        private const val SMALL_FILE_SIZE = 100L
        private val TEST_DIR_NAME = System.currentTimeMillis().toString()
        private val LARGE_FILE_NAME = "large-${System.currentTimeMillis()}"
        private val LARGE_FILE_STRING_PATH = "public/$TEST_DIR_NAME/$LARGE_FILE_NAME"
        private val LARGE_FILE_PATH = StoragePath.fromString(LARGE_FILE_STRING_PATH)
        private val SMALL_FILE_NAME = "small-${System.currentTimeMillis()}"
        private val SMALL_FILE_STRING_PATH = "public/$TEST_DIR_NAME/$SMALL_FILE_NAME"
        private val SMALL_FILE_PATH = StoragePath.fromString(SMALL_FILE_STRING_PATH)

        lateinit var storageCategory: StorageCategory
        lateinit var synchronousStorage: SynchronousStorage
        lateinit var synchronousAuth: SynchronousAuth
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
            synchronousAuth = SynchronousAuth.delegatingToCognito(context, AWSCognitoAuthPlugin())

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

        @JvmStatic
        @AfterClass
        fun tearDownOnce() {
            synchronousStorage.remove(SMALL_FILE_PATH, StorageRemoveOptions.defaultInstance())
            synchronousStorage.remove(LARGE_FILE_PATH, StorageRemoveOptions.defaultInstance())
        }
    }

    @Test
    fun testListFromBucket() {
        val public = StoragePath.fromString("public/$TEST_DIR_NAME")

        val option = StoragePagedListOptions
            .builder()
            .bucket(TestStorageCategory.getStorageBucket())
            .setPageSize(10)
            .build()

        val result = synchronousStorage.list(public, option)

        result.items.apply {
            size shouldBe 2
            first { it.path == LARGE_FILE_STRING_PATH }.apply {
                path shouldBe LARGE_FILE_STRING_PATH
                size shouldBe LARGE_FILE_SIZE
            }
            first { it.path == SMALL_FILE_STRING_PATH }.apply {
                path shouldBe SMALL_FILE_STRING_PATH
                size shouldBe SMALL_FILE_SIZE
            }
        }
    }

    @Test
    fun testListFromInvalidBucket() {
        val bucketName = "amplify-android-storage-integration-test-123xyz"
        val region = "us-east-1"
        val bucketInfo = BucketInfo(bucketName, region)
        val public = StoragePath.fromString("public/$TEST_DIR_NAME")
        val storageBucket = StorageBucket.fromBucketInfo(bucketInfo)
        val option = StoragePagedListOptions
            .builder()
            .bucket(storageBucket)
            .setPageSize(10)
            .build()

        shouldThrow<StorageException> {
            synchronousStorage.list(public, option)
        }
    }
}
