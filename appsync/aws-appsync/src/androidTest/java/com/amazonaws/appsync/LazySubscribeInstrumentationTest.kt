/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amazonaws.appsync

import androidx.test.core.app.ApplicationProvider
import com.amazonaws.appsync.test.R
import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.api.graphql.model.ModelSubscription
import com.amplifyframework.core.model.LazyModelReference
import com.amplifyframework.core.model.LoadedModelReference
import com.amplifyframework.core.model.includes
import com.amplifyframework.foundation.result.getOrThrow
import com.amplifyframework.testmodels.lazycpk.HasOneChild
import com.amplifyframework.testmodels.lazycpk.Parent
import com.amplifyframework.testmodels.lazycpk.ParentPath
import com.amplifyframework.testutils.DeviceFarmTestBase
import com.amplifyframework.testutils.Resources
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test

/**
 * Integration test that verifies [AmplifyAppSyncClient] can perform lazy-loading subscription
 * operations using codegen models with relationships. Mirrors the test scenarios from
 * [com.amplifyframework.api.aws.GraphQLLazySubscribeInstrumentationTest] in the :aws-api module.
 */
@OptIn(ExperimentalAmplifyApi::class, InternalAmplifyApi::class)
class LazySubscribeInstrumentationTest : DeviceFarmTestBase() {

    companion object {
        val instrumentationRunId by lazy {
            UUID.randomUUID().toString()
        }

        private lateinit var client: AmplifyAppSyncClient

        @JvmStatic
        @BeforeClass
        fun setUp() {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val config = Resources.readAsJson(context, R.raw.appsync_client_config)
            client = AmplifyAppSyncClient(
                AmplifyAppSyncClient.Configuration {
                    endpoint = config.getString("lazyEndpoint")
                    authorization = AppSyncAuthorization.Single(
                        AppSyncClientAuthorizer.ApiKey(config.getString("lazyApiKey"))
                    )
                }
            )
        }

        @JvmStatic
        @AfterClass
        fun tearDown() {
            client.close()
        }
    }

    /**
     * Some of our test suites run at the same time on multiple devices.
     * This can result in failures if 1 device creates a model that the other receives in its subscription.
     * This helps check that the model was created by this instrumentation test run and not another device.
     */
    private fun modelCreatedByDevice(modelId: String) = modelId.startsWith(instrumentationRunId)

    /**
     * Use this method for any models that will be subscribed to for the tests in this class.
     * This allows us to verify that the models in this test were created by a specific device
     */
    private fun createRandomIdWithRunIdPrefix() = "$instrumentationRunId-${UUID.randomUUID()}"

    @Test
    fun subscribe_with_no_includes_create() = runTest {
        val hasOneChild = HasOneChild.builder()
            .content("Child1")
            .build()
        val parent = Parent.builder()
            .id(createRandomIdWithRunIdPrefix())
            .parentChildId(hasOneChild.id)
            .build()

        val latch = CountDownLatch(1)
        val collectRunningLatch = CountDownLatch(1)

        var capturedParent: Parent? = null
        var capturedChild: HasOneChild? = null
        val subscription = client.subscribe(ModelSubscription.onCreate(Parent::class.java))
        CoroutineScope(Dispatchers.IO).launch {
            subscription.collect { event ->
                if (event is SubscriptionEvent.Data) {
                    val returnedParent = event.response.data
                    if (modelCreatedByDevice(returnedParent.id)) {
                        assertEquals(parent.id, returnedParent.id)
                        capturedParent = returnedParent
                        capturedChild = (returnedParent.child as LazyModelReference).fetchModel()!!
                        latch.countDown()
                    }
                }
            }
            collectRunningLatch.countDown()
        }
        collectRunningLatch.await(1, TimeUnit.SECONDS)

        client.mutate(ModelMutation.create(hasOneChild)).getOrThrow()
        client.mutate(ModelMutation.create(parent)).getOrThrow()

        withContext(this.coroutineContext) {
            latch.await(10, TimeUnit.SECONDS)
        }

        assertEquals(parent.id, capturedParent!!.id)
        assertEquals(hasOneChild.content, capturedChild!!.content)
        assertEquals(hasOneChild.id, capturedChild!!.id)

        // CLEANUP
        client.mutate(ModelMutation.delete(hasOneChild)).getOrThrow()
        client.mutate(ModelMutation.delete(parent)).getOrThrow()
    }

    @Test
    fun subscribe_with_includes_create() = runTest {
        val hasOneChild = HasOneChild.builder()
            .content("Child1")
            .build()
        val parent = Parent.builder()
            .id(createRandomIdWithRunIdPrefix())
            .parentChildId(hasOneChild.id)
            .build()

        val latch = CountDownLatch(1)
        val collectRunningLatch = CountDownLatch(1)

        val request = ModelSubscription.onCreate<Parent, ParentPath>(Parent::class.java) {
            includes(it.child)
        }
        val subscription = client.subscribe(request)

        var capturedParent: Parent? = null
        var capturedChild: HasOneChild? = null
        CoroutineScope(Dispatchers.IO).launch {
            subscription.collect { event ->
                if (event is SubscriptionEvent.Data) {
                    val returnedParent = event.response.data
                    if (modelCreatedByDevice(returnedParent.id)) {
                        assertEquals(parent.id, returnedParent.id)
                        capturedParent = returnedParent
                        capturedChild = (returnedParent.child as LoadedModelReference).value
                        latch.countDown()
                    }
                }
            }
            collectRunningLatch.countDown()
        }
        collectRunningLatch.await(1, TimeUnit.SECONDS)

        client.mutate(ModelMutation.create(hasOneChild)).getOrThrow()
        val createRequest = ModelMutation.create<Parent, ParentPath>(parent) {
            includes(it.child)
        }
        client.mutate(createRequest).getOrThrow()

        withContext(this.coroutineContext) {
            latch.await(10, TimeUnit.SECONDS)
        }

        assertEquals(parent.id, capturedParent!!.id)
        assertEquals(hasOneChild.content, capturedChild!!.content)
        assertEquals(hasOneChild.id, capturedChild!!.id)

        // CLEANUP
        client.mutate(ModelMutation.delete(hasOneChild)).getOrThrow()
        client.mutate(ModelMutation.delete(parent)).getOrThrow()
    }

    @Test
    fun subscribe_with_no_includes_update() = runTest {
        val hasOneChild = HasOneChild.builder()
            .content("Child1")
            .build()
        val hasOneChild2 = HasOneChild.builder()
            .content("Child2")
            .build()
        val parent = Parent.builder()
            .id(createRandomIdWithRunIdPrefix())
            .parentChildId(hasOneChild.id)
            .build()

        val subscription = client.subscribe(ModelSubscription.onUpdate(Parent::class.java))

        val latch = CountDownLatch(1)
        var capturedParent: Parent? = null
        var capturedChild: HasOneChild? = null
        CoroutineScope(Dispatchers.IO).launch {
            subscription.collect { event ->
                if (event is SubscriptionEvent.Data) {
                    val returnedParent = event.response.data
                    if (modelCreatedByDevice(returnedParent.id)) {
                        assertEquals(parent.id, returnedParent.id)
                        capturedParent = returnedParent
                        capturedChild = (returnedParent.child as LazyModelReference).fetchModel()!!
                        latch.countDown()
                    }
                }
            }
        }

        client.mutate(ModelMutation.create(hasOneChild)).getOrThrow()
        client.mutate(ModelMutation.create(hasOneChild2)).getOrThrow()
        val parentFromResponse = client.mutate(ModelMutation.create(parent)).getOrThrow().data
        assertEquals(hasOneChild.id, parentFromResponse.parentChildId)

        val updateParent = parent.copyOfBuilder().parentChildId(hasOneChild2.id).build()
        client.mutate(ModelMutation.update(updateParent)).getOrThrow()

        withContext(this.coroutineContext) {
            latch.await(10, TimeUnit.SECONDS)
        }

        assertEquals(parent.id, capturedParent!!.id)
        assertEquals(capturedParent!!.parentChildId, capturedChild!!.id)
        assertEquals(hasOneChild2.content, capturedChild!!.content)
        assertEquals(hasOneChild2.id, capturedChild!!.id)

        // CLEANUP
        client.mutate(ModelMutation.delete(hasOneChild)).getOrThrow()
        client.mutate(ModelMutation.delete(hasOneChild2)).getOrThrow()
        client.mutate(ModelMutation.delete(parent)).getOrThrow()
    }

    @Test
    fun subscribe_with_includes_update() = runTest {
        val hasOneChild = HasOneChild.builder()
            .content("Child1")
            .build()
        val hasOneChild2 = HasOneChild.builder()
            .content("Child2")
            .build()
        val parent = Parent.builder()
            .id(createRandomIdWithRunIdPrefix())
            .parentChildId(hasOneChild.id)
            .build()

        val request = ModelSubscription.onUpdate<Parent, ParentPath>(Parent::class.java) {
            includes(it.child)
        }
        val subscription = client.subscribe(request)

        val latch = CountDownLatch(1)
        var capturedParent: Parent? = null
        var capturedChild: HasOneChild? = null
        CoroutineScope(Dispatchers.IO).launch {
            subscription.collect { event ->
                if (event is SubscriptionEvent.Data) {
                    val returnedParent = event.response.data
                    if (modelCreatedByDevice(returnedParent.id)) {
                        assertEquals(parent.id, returnedParent.id)
                        capturedParent = returnedParent
                        capturedChild = (returnedParent.child as LoadedModelReference).value
                        latch.countDown()
                    }
                }
            }
        }

        client.mutate(ModelMutation.create(hasOneChild)).getOrThrow()
        client.mutate(ModelMutation.create(hasOneChild2)).getOrThrow()
        client.mutate(ModelMutation.create(parent)).getOrThrow()

        val updateParent = parent.copyOfBuilder().parentChildId(hasOneChild2.id).build()
        val updateRequest = ModelMutation.update<Parent, ParentPath>(updateParent) {
            includes(it.child)
        }
        client.mutate(updateRequest).getOrThrow()

        withContext(this.coroutineContext) {
            latch.await(10, TimeUnit.SECONDS)
        }

        assertEquals(parent.id, capturedParent!!.id)
        assertEquals(capturedParent!!.parentChildId, capturedChild!!.id)
        assertEquals(hasOneChild2.content, capturedChild!!.content)
        assertEquals(hasOneChild2.id, capturedChild!!.id)

        // CLEANUP
        client.mutate(ModelMutation.delete(hasOneChild)).getOrThrow()
        client.mutate(ModelMutation.delete(hasOneChild2)).getOrThrow()
        client.mutate(ModelMutation.delete(parent)).getOrThrow()
    }

    @Test
    fun subscribe_with_no_includes_delete() = runTest {
        val hasOneChild = HasOneChild.builder()
            .content("Child1")
            .build()
        val parent = Parent.builder()
            .id(createRandomIdWithRunIdPrefix())
            .parentChildId(hasOneChild.id)
            .build()

        val subscription = client.subscribe(ModelSubscription.onDelete(Parent::class.java))

        val latch = CountDownLatch(1)
        var capturedParent: Parent? = null
        var capturedChild: HasOneChild? = null
        CoroutineScope(Dispatchers.IO).launch {
            subscription.collect { event ->
                if (event is SubscriptionEvent.Data) {
                    val returnedParent = event.response.data
                    if (modelCreatedByDevice(returnedParent.id)) {
                        assertEquals(parent.id, returnedParent.id)
                        capturedParent = returnedParent
                        capturedChild = (returnedParent.child as LazyModelReference).fetchModel()!!
                        latch.countDown()
                    }
                }
            }
        }

        client.mutate(ModelMutation.create(hasOneChild)).getOrThrow()
        val parentFromResponse = client.mutate(ModelMutation.create(parent)).getOrThrow().data
        assertEquals(hasOneChild.id, parentFromResponse.parentChildId)

        client.mutate(ModelMutation.delete(parent)).getOrThrow()

        withContext(this.coroutineContext) {
            latch.await(10, TimeUnit.SECONDS)
        }

        assertEquals(parent.id, capturedParent!!.id)
        assertEquals(hasOneChild.content, capturedChild!!.content)
        assertEquals(hasOneChild.id, capturedChild!!.id)

        // CLEANUP
        client.mutate(ModelMutation.delete(hasOneChild)).getOrThrow()
    }

    @Test
    fun subscribe_with_includes_delete() = runTest {
        val hasOneChild = HasOneChild.builder()
            .content("Child1")
            .build()
        val parent = Parent.builder()
            .id(createRandomIdWithRunIdPrefix())
            .parentChildId(hasOneChild.id)
            .build()

        val request = ModelSubscription.onDelete<Parent, ParentPath>(Parent::class.java) {
            includes(it.child)
        }
        val subscription = client.subscribe(request)

        val latch = CountDownLatch(1)
        var capturedParent: Parent? = null
        var capturedChild: HasOneChild? = null
        CoroutineScope(Dispatchers.IO).launch {
            subscription.collect { event ->
                if (event is SubscriptionEvent.Data) {
                    val returnedParent = event.response.data
                    if (modelCreatedByDevice(returnedParent.id)) {
                        assertEquals(parent.id, returnedParent.id)
                        capturedParent = returnedParent
                        capturedChild = (returnedParent.child as LoadedModelReference).value
                        latch.countDown()
                    }
                }
            }
        }

        client.mutate(ModelMutation.create(hasOneChild)).getOrThrow()
        client.mutate(ModelMutation.create(parent)).getOrThrow()

        val deleteRequest = ModelMutation.delete<Parent, ParentPath>(parent) {
            includes(it.child)
        }
        client.mutate(deleteRequest).getOrThrow()

        withContext(this.coroutineContext) {
            latch.await(10, TimeUnit.SECONDS)
        }

        assertEquals(parent.id, capturedParent!!.id)
        assertEquals(hasOneChild.content, capturedChild!!.content)
        assertEquals(hasOneChild.id, capturedChild!!.id)

        // CLEANUP
        client.mutate(ModelMutation.delete(hasOneChild)).getOrThrow()
    }
}
