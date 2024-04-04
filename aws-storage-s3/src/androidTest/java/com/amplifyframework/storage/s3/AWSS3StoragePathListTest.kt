/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.storage.options.StorageUploadFileOptions
import com.amplifyframework.storage.s3.options.AWSS3StoragePagedListOptions
import com.amplifyframework.storage.s3.test.R
import com.amplifyframework.storage.s3.util.WorkmanagerTestUtils.initializeWorkmanagerTestUtil
import com.amplifyframework.testutils.random.RandomTempFile
import com.amplifyframework.testutils.sync.SynchronousAuth
import com.amplifyframework.testutils.sync.SynchronousStorage
import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * Instrumentation test for operational work on download.
 */
class AWSS3StoragePathListTest {
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
        private val USER_ONE_PRIVATE_FILE_NAME = "user1Private-${System.currentTimeMillis()}"

        lateinit var storageCategory: StorageCategory
        lateinit var synchronousStorage: SynchronousStorage
        lateinit var synchronousAuth: SynchronousAuth
        lateinit var largeFile: File
        lateinit var smallFile: File
        lateinit var userOnePrivateFile: File
        lateinit var userOnePrivateFileStringPath: String
        lateinit var userOnePrivateFileStoragePath: StoragePath
        internal lateinit var userOne: UserCredentials.Credential
        internal lateinit var userTwo: UserCredentials.Credential

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
            val identityIdSource = MobileClientIdentityIdSource.create(synchronousAuth)
            val userCredentials = UserCredentials.create(context, identityIdSource)
            val iterator = userCredentials.iterator()
            userOne = iterator.next()

            // Get a handle to storage
            storageCategory = TestStorageCategory.create(context, R.raw.amplifyconfiguration)
            synchronousStorage = SynchronousStorage.delegatingTo(storageCategory)

            val uploadOptions = StorageUploadFileOptions.defaultInstance()

            synchronousAuth.signOut()

            // Upload large test file
            largeFile = RandomTempFile(LARGE_FILE_NAME, LARGE_FILE_SIZE)
            synchronousStorage.uploadFile(LARGE_FILE_PATH, largeFile, uploadOptions, EXTENDED_TIMEOUT_MS)

            // Upload small test file
            smallFile = RandomTempFile(SMALL_FILE_NAME, SMALL_FILE_SIZE)
            synchronousStorage.uploadFile(SMALL_FILE_PATH, smallFile, uploadOptions)

            synchronousAuth.signIn(userOne.username, userOne.password)

            userOnePrivateFile = RandomTempFile(USER_ONE_PRIVATE_FILE_NAME, SMALL_FILE_SIZE)
            userOnePrivateFileStringPath = "private/${userOne.identityId}/$USER_ONE_PRIVATE_FILE_NAME"
            userOnePrivateFileStoragePath = StoragePath.fromString(userOnePrivateFileStringPath)
            synchronousStorage.uploadFile(
                userOnePrivateFileStoragePath,
                userOnePrivateFile,
                uploadOptions
            )

            synchronousAuth.signOut()
        }
    }

    /**
     * Unsubscribe from everything after each test.
     */
    @After
    fun tearDown() {
        synchronousAuth.signOut()
    }

    @Test
    fun testListPublic() {
        val public = StoragePath.fromString("public/$TEST_DIR_NAME")

        val result = synchronousStorage.list(public, AWSS3StoragePagedListOptions.defaultInstance())

        result.items.apply {
            assertEquals(2, size)
            first { it.path == LARGE_FILE_STRING_PATH }.apply {
                assertEquals(LARGE_FILE_STRING_PATH, key)
                assertEquals(LARGE_FILE_STRING_PATH, path)
                assertEquals(LARGE_FILE_SIZE, size)
            }
            first { it.path == SMALL_FILE_STRING_PATH }.apply {
                assertEquals(SMALL_FILE_STRING_PATH, key)
                assertEquals(SMALL_FILE_STRING_PATH, path)
                assertEquals(SMALL_FILE_SIZE, size)
            }
        }
    }

    @Test
    fun testListPageSize() {
        val public = StoragePath.fromString("public/$TEST_DIR_NAME")

        val result = synchronousStorage.list(public, AWSS3StoragePagedListOptions.builder().setPageSize(1).build())

        result.items.apply {
            assertEquals(1, size)
            assertEquals(
                1,
                filter { it.path == LARGE_FILE_STRING_PATH || it.path == SMALL_FILE_STRING_PATH }.size
            )
        }
    }

    @Test(expected = StorageException::class)
    fun testListFailsAccessDenied() {
        val public = StoragePath.fromString("private/${userOne.identityId}/")

        synchronousStorage.list(public, AWSS3StoragePagedListOptions.defaultInstance())
    }

    @Test
    fun testListSucceedsWhenAuthenticated() {
        synchronousAuth.signIn(userOne.username, userOne.password)
        val path = StoragePath.fromString("private/${userOne.identityId}/")

        val result = synchronousStorage.list(path, AWSS3StoragePagedListOptions.defaultInstance())

        assertTrue(result.items.size > 0)
    }
}
