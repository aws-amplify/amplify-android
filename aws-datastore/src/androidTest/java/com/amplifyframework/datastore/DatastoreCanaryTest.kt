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
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider
import com.amplifyframework.testmodels.commentsblog.Post
import com.amplifyframework.testmodels.commentsblog.PostStatus
import com.amplifyframework.testutils.HubAccumulator
import com.amplifyframework.testutils.sync.SynchronousDataStore
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.junit.After
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

    val syncDatastore = SynchronousDataStore.delegatingTo(Amplify.DataStore)

    @After
    fun teardown() {
        syncDatastore.clear()
    }

    @Test
    fun save() {
        val post = Post.builder()
            .title("Post" + UUID.randomUUID().toString())
            .status(PostStatus.ACTIVE)
            .rating(3)
            .build()
        syncDatastore.save(post)
    }

    @Test
    fun query() {
        syncDatastore.query(Post::class.java)
    }

    @Test
    fun delete() {
        val post = Post.builder()
            .title("Post" + UUID.randomUUID().toString())
            .status(PostStatus.ACTIVE)
            .rating(3)
            .id(UUID.randomUUID().toString())
            .build()

        val createHub = HubAccumulator.create(
            HubChannel.DATASTORE,
            DataStoreHubEventFilters.enqueueOf(Post::class.simpleName, post.id),
            1
        ).start()

        syncDatastore.save(post)
        createHub.await(TIMEOUT_S.toInt(), TimeUnit.SECONDS)

        syncDatastore.delete(post)

        // Temporarily prevent https://github.com/aws-amplify/amplify-android/issues/2617
        Thread.sleep(1000)
    }

    @Test
    fun start() {
        syncDatastore.start()
    }

    @Test
    fun stop() {
        syncDatastore.stop()
    }

    @Test
    fun clear() {
        syncDatastore.clear()
    }
}
