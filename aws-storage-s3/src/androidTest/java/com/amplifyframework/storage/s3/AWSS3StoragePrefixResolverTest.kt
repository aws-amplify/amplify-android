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

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.storage.StorageAccessLevel
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.s3.configuration.AWSS3PluginPrefixResolver
import com.amplifyframework.storage.s3.configuration.AWSS3StoragePluginConfiguration
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test

class AWSS3StoragePrefixResolverTest {
    companion object {
        private const val TIMEOUT_S = 20L
        private val TAG = AWSS3StoragePrefixResolverTest::class.simpleName

        @BeforeClass
        @JvmStatic
        fun setup() {
            try {
                Amplify.addPlugin(AWSCognitoAuthPlugin())
                Amplify.addPlugin(AWSS3StoragePlugin(
                    AWSS3StoragePluginConfiguration.Builder().apply {
                        awsS3PluginPrefixResolver = object : AWSS3PluginPrefixResolver {
                            override fun resolvePrefix(
                                accessLevel: StorageAccessLevel,
                                targetIdentity: String?,
                                onSuccess: Consumer<String>,
                                onError: Consumer<StorageException>?
                            ) {
                                onSuccess.accept("custom/")
                            }
                        }
                    }.build()
                ))
                Amplify.configure(ApplicationProvider.getApplicationContext())
            } catch (error: AmplifyException) {
                Log.e(TAG, "Could not initialize Amplify", error)
            }
        }
    }

    @Test
    fun uploadInputStreamWithInvalidPathCallsOnErrorOnce() {
        // Given
        var timesOnErrorCalled = 0
        val latch = CountDownLatch(1)
        val file = File("${System.getProperty("java.io.tmpdir")}/${System.currentTimeMillis()}")
        RandomAccessFile(file, "rw").apply {
            setLength((1024 * 1024).toLong())
            close()
        }
        file.deleteOnExit()
        val stream = FileInputStream(file)
        val fileKey = "ExampleKey"

        // WHEN
        Amplify.Storage.uploadInputStream(
            fileKey,
            stream,
            { fail("Upload unexpectedly succeeded") },
            {
                timesOnErrorCalled += 1
                latch.countDown()
            }
        )

        // THEN
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))

        // Not ideal to add a wait, but here we want to assert we don't call onError a second time
        // This was happening immediately after first callback due to bug
        Thread.sleep(1000)

        assertEquals(1, timesOnErrorCalled)
    }
}
