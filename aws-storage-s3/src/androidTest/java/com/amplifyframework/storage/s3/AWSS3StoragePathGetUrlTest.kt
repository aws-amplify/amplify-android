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
import com.amplifyframework.storage.options.StorageGetUrlOptions
import com.amplifyframework.storage.options.StorageUploadFileOptions
import com.amplifyframework.storage.s3.options.AWSS3StorageGetPresignedUrlOptions
import com.amplifyframework.storage.s3.test.R
import com.amplifyframework.storage.s3.util.WorkmanagerTestUtils.initializeWorkmanagerTestUtil
import com.amplifyframework.testutils.random.RandomTempFile
import com.amplifyframework.testutils.sync.SynchronousAuth
import com.amplifyframework.testutils.sync.SynchronousStorage
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * Instrumentation test for operational work on download.
 */
class AWSS3StoragePathGetUrlTest {
    // Create a file to download to
    private val downloadFile: File = RandomTempFile()

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
    }

    @Test
    fun testGetUrl() {
        val result = synchronousStorage.getUrl(
            SMALL_FILE_PATH,
            StorageGetUrlOptions.builder().expires(30).build()
        )

        assertEquals("/public/$SMALL_FILE_NAME", result.url.path)
        assertTrue(result.url.query.contains("X-Amz-Expires=30"))
    }

    @Test
    fun testGetUrlWithObjectExistenceValidationEnabled() {
        val result = synchronousStorage.getUrl(
            SMALL_FILE_PATH,
            AWSS3StorageGetPresignedUrlOptions.builder().setValidateObjectExistence(true).expires(30).build()
        )

        assertEquals("/public/$SMALL_FILE_NAME", result.url.path)
        assertTrue(result.url.query.contains("X-Amz-Expires=30"))
    }

    @Test
    fun testGetUrlWithStorageExceptionObjectNotFoundThrown() {
        assertThrows(StorageException::class.java) {
            synchronousStorage.getUrl(
                StoragePath.fromString("/public/SOME_UNKNOWN_FILE"),
                AWSS3StorageGetPresignedUrlOptions.builder().setValidateObjectExistence(true).expires(30).build()
            )
        }
    }
}
