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
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.amplifyframework.AmplifyException
import com.amplifyframework.core.Amplify
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider
import com.amplifyframework.testmodels.commentsblog.Post
import com.amplifyframework.testmodels.commentsblog.PostStatus

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test
import java.util.UUID

class DatastoreCanaryTest {
    companion object {
        private const val TIMEOUT_S = 20L

        @BeforeClass
        @JvmStatic
        fun setup() {
            try {
                Amplify.addPlugin(
                    AWSDataStorePlugin.builder()
                        .modelProvider(AmplifyModelProvider.getInstance())
                        .build()
                )
                Amplify.configure(getApplicationContext())
                Log.i("DatastoreCanaryTest", "Initialized Amplify")
            } catch (error: AmplifyException) {
                Log.e("DatastoreCanaryTest", "Could not initialize Amplify", error)
            }
        }
    }

    @After
    @Throws(DataStoreException::class)
    fun teardown() {
        try {
            val latch = CountDownLatch(1)
            Amplify.DataStore.clear(
                {
                    latch.countDown()
                    Log.i("DataStoreStressTest", "DataStore cleared")
                },
                {
                    latch.countDown()
                    Log.e("DataStoreStressTest", "Error clearing DataStore", it)
                }
            )
            latch.await(TIMEOUT_S.toLong(), TimeUnit.SECONDS)
        } catch (error: Exception) {
            // ok to ignore since problem encountered during tear down of the test.
        }
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
            {
                Log.i("DatastoreCanaryTest", "Created a new post successfully")
                latch.countDown()
            },
            {
                Log.e("DatastoreCanaryTest", "Error creating post", it)
                fail()
            }
        )
        Thread.sleep(500)
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun query() {
        val latch = CountDownLatch(1)
        Amplify.DataStore.query(
            Post::class.java,
            {
                Log.i("DatastoreCanaryTest", "Successful query")
                latch.countDown()
            },
            {
                Log.e("DatastoreCanaryTest", "Error retrieving posts", it)
                fail()
            }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun delete() {
        val latch = CountDownLatch(1)
        val post = Post.builder()
            .title("Post" + UUID.randomUUID().toString())
            .status(PostStatus.ACTIVE)
            .rating(3)
            .build()

        Amplify.DataStore.save(
            post,
            { Log.i("DatastoreCanaryTest", "Created a new post successfully") },
            { Log.e("DatastoreCanaryTest", "Error creating post", it) }
        )
        Thread.sleep(500)
        Amplify.DataStore.delete(
            post,
            {
                Log.i("DatastoreCanaryTest", "Deleted a post.")
                latch.countDown()
            },
            { Log.e("DatastoreCanaryTest", "Delete failed.", it) }
        )
        Thread.sleep(500)
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun start() {
        val latch = CountDownLatch(1)
        Amplify.DataStore.start(
            {
                Log.i("DatastoreCanaryTest", "DataStore started")
                latch.countDown()
            },
            {
                Log.e("DatastoreCanaryTest", "Error starting DataStore", it)
                fail()
            }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun observe() {
        val latch = CountDownLatch(1)
        Amplify.DataStore.observe(
            Post::class.java,
            {
                Log.i("DatastoreCanaryTest", "Observation began")
                latch.countDown()
            },
            {
                val post = it.item()
                Log.i("DatastoreCanaryTest", "Post: $post")
            },
            {
                Log.e("DatastoreCanaryTest", "Observation failed", it)
                fail()
            },
            {
                Log.i("DatastoreCanaryTest", "Observation complete")
            }
        )
        Amplify.DataStore.stop(
            { Log.i("DatastoreCanaryTest", "DataStore stopped") },
            { Log.e("DatastoreCanaryTest", "Error stopped DataStore", it) }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun stop() {
        val latch = CountDownLatch(1)
        Amplify.DataStore.stop(
            {
                Log.i("DatastoreCanaryTest", "DataStore stopped")
                latch.countDown()
            },
            {
                Log.e("DatastoreCanaryTest", "Error stopped DataStore", it)
                fail()
            }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun clear() {
        val latch = CountDownLatch(1)
        Amplify.DataStore.clear(
            {
                Log.i("DatastoreCanaryTest", "DataStore cleared")
                latch.countDown()
            },
            {
                Log.e("DatastoreCanaryTest", "Error clearing DataStore", it)
                fail()
            }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }
}
