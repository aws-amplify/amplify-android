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
package com.amplifyframework.datastore

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.AmplifyException
import com.amplifyframework.core.Amplify
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider
import com.amplifyframework.testmodels.commentsblog.Post
import com.amplifyframework.testmodels.commentsblog.PostStatus
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test

class DatastoreCanaryTest {
    companion object {
        private const val TIMEOUT_S = 20L
        private val TAG = DatastoreCanaryTest::class.simpleName

        @BeforeClass
        @JvmStatic
        fun setup() {
            try {
                Amplify.addPlugin(
                    AWSDataStorePlugin.builder()
                        .modelProvider(AmplifyModelProvider.getInstance())
                        .build()
                )
                Amplify.configure(ApplicationProvider.getApplicationContext())
            } catch (error: AmplifyException) {
                Log.e(TAG, "Could not initialize Amplify", error)
            }
        }
    }

    @After
    fun teardown() {
        val latch = CountDownLatch(1)
        Amplify.DataStore.clear(
            { latch.countDown() },
            {
                latch.countDown()
                Log.e(TAG, "Error clearing DataStore", it)
            }
        )
        latch.await(TIMEOUT_S, TimeUnit.SECONDS)
    }

    @Test
    fun save() {
        val latch = CountDownLatch(1)
        val post = Post.builder()
            .title("Post" + UUID.randomUUID().toString())
            .status(PostStatus.ACTIVE)
            .rating(3)
            .build()

        Amplify.DataStore.save(
            post,
            { latch.countDown() },
            { fail("Error creating post: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun query() {
        val latch = CountDownLatch(1)
        Amplify.DataStore.query(
            Post::class.java,
            { latch.countDown() },
            { fail("Error retrieving posts: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun delete() {
        val post = Post.builder()
            .title("Post" + UUID.randomUUID().toString())
            .status(PostStatus.ACTIVE)
            .rating(3)
            .build()

        val saveLatch = CountDownLatch(1)
        Amplify.DataStore.save(
            post,
            { saveLatch.countDown() },
            { fail("Error creating post: $it") }
        )
        saveLatch.await(TIMEOUT_S, TimeUnit.SECONDS)
        val deleteLatch = CountDownLatch(1)
        Amplify.DataStore.delete(
            post,
            { deleteLatch.countDown() },
            { fail("Failed to delete post: $it") }
        )
        assertTrue(deleteLatch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun start() {
        val latch = CountDownLatch(1)
        Amplify.DataStore.start(
            { latch.countDown() },
            { fail("Error starting DataStore: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun observe() {
        val latch = CountDownLatch(1)
        Amplify.DataStore.observe(
            Post::class.java,
            { latch.countDown() },
            {
                val post = it.item()
                Log.i(TAG, "Post: $post")
            },
            { fail("Observation failed: $it") },
            { Log.i(TAG, "Observation complete") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun stop() {
        val latch = CountDownLatch(1)
        Amplify.DataStore.stop(
            { latch.countDown() },
            { fail("Error stopping DataStore: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun clear() {
        val latch = CountDownLatch(1)
        Amplify.DataStore.clear(
            { latch.countDown() },
            { fail("Error clearing DataStore: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }
}
