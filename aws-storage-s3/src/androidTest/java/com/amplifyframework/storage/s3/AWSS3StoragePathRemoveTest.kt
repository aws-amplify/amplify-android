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
import com.amplifyframework.storage.StorageCategory
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.StoragePath
import com.amplifyframework.storage.options.StorageDownloadFileOptions
import com.amplifyframework.storage.options.StorageRemoveOptions
import com.amplifyframework.storage.options.StorageUploadFileOptions
import com.amplifyframework.storage.s3.test.R
import com.amplifyframework.storage.s3.util.WorkmanagerTestUtils.initializeWorkmanagerTestUtil
import com.amplifyframework.testutils.random.RandomTempFile
import com.amplifyframework.testutils.sync.SynchronousAuth
import com.amplifyframework.testutils.sync.SynchronousStorage
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.BeforeClass
import org.junit.Test

/**
 * Instrumentation test for operational work on remove with StoragePath.
 */
class AWSS3StoragePathRemoveTest {
    // Create a file to download to
    private val downloadFile: File = RandomTempFile()

    private companion object {
        const val SMALL_FILE_SIZE = 100L
        val SMALL_FILE_NAME = "small-${System.currentTimeMillis()}"
        val SMALL_FILE_STRING_PATH = "public/$SMALL_FILE_NAME"
        val SMALL_FILE_PATH = StoragePath.fromString(SMALL_FILE_STRING_PATH)

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
    }

    @Test
    fun testRemove() {
        val result = synchronousStorage.remove(SMALL_FILE_PATH, StorageRemoveOptions.defaultInstance())

        assertEquals(SMALL_FILE_STRING_PATH, result.path)
        assertEquals(SMALL_FILE_STRING_PATH, result.key)

        // download will fail if file no longer exists
        assertThrows(StorageException::class.java) {
            synchronousStorage.downloadFile(
                SMALL_FILE_PATH,
                downloadFile,
                StorageDownloadFileOptions.defaultInstance()
            )
        }
    }
}
