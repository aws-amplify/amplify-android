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
import com.amplifyframework.storage.StoragePath
import com.amplifyframework.storage.options.StorageGetUrlOptions
import com.amplifyframework.storage.options.StorageRemoveOptions
import com.amplifyframework.storage.options.StorageUploadFileOptions
import com.amplifyframework.storage.s3.test.R
import com.amplifyframework.storage.s3.util.WorkmanagerTestUtils.initializeWorkmanagerTestUtil
import com.amplifyframework.testutils.random.RandomTempFile
import com.amplifyframework.testutils.sync.SynchronousAuth
import com.amplifyframework.testutils.sync.SynchronousStorage
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

/**
 * Instrumentation test for operational work on download.
 */
class AWSS3StorageMultiBucketGetUrlTest {
    private companion object {
        const val SMALL_FILE_SIZE = 100L
        val SMALL_FILE_NAME = "small-${System.currentTimeMillis()}"
        val SMALL_FILE_PATH = StoragePath.fromString("public/$SMALL_FILE_NAME")

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
            synchronousStorage.uploadFile(SMALL_FILE_PATH, smallFile, StorageUploadFileOptions.defaultInstance())
        }
        @JvmStatic
        @AfterClass
        fun tearDownOnce() {
            synchronousStorage.remove(SMALL_FILE_PATH, StorageRemoveOptions.defaultInstance())
        }
    }

    @Test
    fun testGetUrl() {
        val result = synchronousStorage.getUrl(
            SMALL_FILE_PATH,
            StorageGetUrlOptions.builder().expires(30).bucket(TestStorageCategory.getStorageBucket()).build()
        )

        result.url.path shouldBe "/public/$SMALL_FILE_NAME"
        result.url.query shouldContain "X-Amz-Expires=30"
    }

    @Test
    fun testGetUrlFromInvalidBucket() {
        val bucketName = "amplify-android-storage-integration-test-123xyz"
        val region = "us-east-1"
        val bucketInfo = BucketInfo(bucketName, region)
        val invalidBucket = StorageBucket.fromBucketInfo(bucketInfo)
        val result = synchronousStorage.getUrl(
            SMALL_FILE_PATH,
            StorageGetUrlOptions.builder().expires(30).bucket(invalidBucket).build()
        )

        result shouldNotBe null
        result.url.host shouldContain bucketName
        result.url.host shouldContain region
    }
}
