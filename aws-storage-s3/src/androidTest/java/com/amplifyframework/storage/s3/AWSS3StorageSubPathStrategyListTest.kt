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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.storage.StorageCategory
import com.amplifyframework.storage.StoragePath
import com.amplifyframework.storage.options.StorageRemoveOptions
import com.amplifyframework.storage.options.StorageUploadFileOptions
import com.amplifyframework.storage.options.SubpathStrategy
import com.amplifyframework.storage.s3.options.AWSS3StoragePagedListOptions
import com.amplifyframework.storage.s3.test.R
import com.amplifyframework.storage.s3.util.WorkmanagerTestUtils
import com.amplifyframework.testutils.random.RandomTempFile
import com.amplifyframework.testutils.sync.SynchronousAuth
import com.amplifyframework.testutils.sync.SynchronousStorage
import java.io.File
import org.junit.After
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

/**
 * Integration tests for using SubpathStrategy with Storage List API
 */
class AWSS3StorageSubPathStrategyListTest {
    companion object {
        private const val SMALL_FILE_SIZE = 100L
        private const val FIRST_FILE_NAME = "01"
        private const val SECOND_FILE_NAME = "02"
        private const val THIRD_FILE_NAME = "03"
        private const val FOURTH_FILE_NAME = "04"
        private const val FIFTH_FILE_NAME = "05"
        private const val FIRST_FILE_STRING_PATH = "public/photos/2023/$FIRST_FILE_NAME"
        private val FIRST_FILE_PATH = StoragePath.fromString(FIRST_FILE_STRING_PATH)
        private const val SECOND_FILE_STRING_PATH = "public/photos/2023/$SECOND_FILE_NAME"
        private val SECOND_FILE_PATH = StoragePath.fromString(SECOND_FILE_STRING_PATH)
        private const val THIRD_FILE_STRING_PATH = "public/photos/2024/$THIRD_FILE_NAME"
        private val THIRD_FILE_PATH = StoragePath.fromString(THIRD_FILE_STRING_PATH)
        private const val FOURTH_FILE_STRING_PATH = "public/photos/2024/$FOURTH_FILE_NAME"
        private val FOURTH_FILE_PATH = StoragePath.fromString(FOURTH_FILE_STRING_PATH)
        private const val FIFTH_FILE_STRING_PATH = "public/photos/$FIFTH_FILE_NAME"
        private val FIFTH_FILE_PATH = StoragePath.fromString(FIFTH_FILE_STRING_PATH)

        lateinit var storageCategory: StorageCategory
        lateinit var synchronousStorage: SynchronousStorage
        lateinit var synchronousAuth: SynchronousAuth
        private lateinit var first: File
        private lateinit var second: File
        private lateinit var third: File
        private lateinit var fourth: File
        private lateinit var fifth: File

        /**
         * Initialize mobile client and configure the storage.
         * Upload the test files ahead of time.
         */
        @JvmStatic
        @BeforeClass
        fun setUpOnce() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            WorkmanagerTestUtils.initializeWorkmanagerTestUtil(context)

            synchronousAuth = SynchronousAuth.delegatingToCognito(context, AWSCognitoAuthPlugin())

            // Get a handle to storage
            storageCategory = TestStorageCategory.create(context, R.raw.amplifyconfiguration)
            synchronousStorage = SynchronousStorage.delegatingTo(storageCategory)

            // Upload test files
            first = RandomTempFile(FIRST_FILE_NAME, SMALL_FILE_SIZE)
            synchronousStorage.uploadFile(FIRST_FILE_PATH, first, StorageUploadFileOptions.defaultInstance())
            second = RandomTempFile(SECOND_FILE_NAME, SMALL_FILE_SIZE)
            synchronousStorage.uploadFile(SECOND_FILE_PATH, second, StorageUploadFileOptions.defaultInstance())
            third = RandomTempFile(THIRD_FILE_NAME, SMALL_FILE_SIZE)
            synchronousStorage.uploadFile(THIRD_FILE_PATH, third, StorageUploadFileOptions.defaultInstance())
            fourth = RandomTempFile(FOURTH_FILE_NAME, SMALL_FILE_SIZE)
            synchronousStorage.uploadFile(FOURTH_FILE_PATH, fourth, StorageUploadFileOptions.defaultInstance())
            fifth = RandomTempFile(FIFTH_FILE_NAME, SMALL_FILE_SIZE)
            synchronousStorage.uploadFile(FIFTH_FILE_PATH, fifth, StorageUploadFileOptions.defaultInstance())
        }
    }

    @After
    fun tearDown() {
        synchronousStorage.remove("photos/2023/$FIRST_FILE_NAME", StorageRemoveOptions.defaultInstance())
        synchronousStorage.remove("photos/2023/$SECOND_FILE_NAME", StorageRemoveOptions.defaultInstance())
        synchronousStorage.remove("photos/2024/$THIRD_FILE_NAME", StorageRemoveOptions.defaultInstance())
        synchronousStorage.remove("photos/2024/$FOURTH_FILE_NAME", StorageRemoveOptions.defaultInstance())
        synchronousStorage.remove("photos/$FIFTH_FILE_NAME", StorageRemoveOptions.defaultInstance())
    }

    @Test
    fun testListWithIncludeStrategyAndStoragePath() {
        val path = StoragePath.fromString("public/photos/")
        val options = AWSS3StoragePagedListOptions
            .builder()
            .setPageSize(10)
            .setSubpathStrategy(SubpathStrategy.Include)
            .build()

        val result = synchronousStorage.list(path, options)

        result.items.apply {
            Assert.assertEquals(5, size)
        }
    }

    @Test
    fun testListWithExcludeStrategyAndStoragePath() {
        val options = AWSS3StoragePagedListOptions
            .builder()
            .setPageSize(10)
            .setSubpathStrategy(SubpathStrategy.Exclude())
            .build()

        var result = synchronousStorage.list(StoragePath.fromString("public/photos/"), options)

        result.items.apply {
            Assert.assertEquals(1, size)
        }

        result.excludedSubpaths.apply {
            Assert.assertEquals(2, size)
        }

        result = synchronousStorage.list(StoragePath.fromString("public/photos/2023/"), options)

        result.items.apply {
            Assert.assertEquals(2, size)
        }

        Assert.assertNull(result.excludedSubpaths)
    }

    @Test
    fun testListWithExcludeCustomDelimiterStrategyAndStoragePath() {
        val options = AWSS3StoragePagedListOptions
            .builder()
            .setPageSize(10)
            .setSubpathStrategy(SubpathStrategy.Exclude("$"))
            .build()

        var result = synchronousStorage.list(StoragePath.fromString("public/photos/"), options)

        result.items.apply {
            Assert.assertEquals(5, size)
        }

        Assert.assertNull(result.excludedSubpaths)

        result = synchronousStorage.list(StoragePath.fromString("public/photos/2023/"), options)

        result.items.apply {
            Assert.assertEquals(2, size)
        }

        Assert.assertNull(result.excludedSubpaths)
    }
}
