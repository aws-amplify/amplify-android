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

package com.amplifyframework.api.aws

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.api.aws.test.R
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.api.graphql.model.ModelSubscription
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.core.model.LazyModelReference
import com.amplifyframework.core.model.LoadedModelReference
import com.amplifyframework.core.model.includes
import com.amplifyframework.datastore.generated.model.HasOneChild
import com.amplifyframework.datastore.generated.model.Parent
import com.amplifyframework.datastore.generated.model.ParentPath
import com.amplifyframework.kotlin.core.Amplify
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test

class GraphQLLazySubscribeInstrumentationTest {

    companion object {

        val instrumentationRunId by lazy {
            UUID.randomUUID().toString()
        }

        @JvmStatic
        @BeforeClass
        fun setUp() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val config = AmplifyConfiguration.fromConfigFile(context, R.raw.amplifyconfigurationlazy)
            Amplify.addPlugin(AWSApiPlugin())
            Amplify.configure(config, context)
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

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    @Test
    fun subscribe_with_no_includes_create() = runTest {
        // GIVEN
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
        val subscription = Amplify.API.subscribe(ModelSubscription.onCreate(Parent::class.java))
        CoroutineScope(Dispatchers.IO).launch {
            subscription.collect {
                val returnedParent = it.data
                if (modelCreatedByDevice(returnedParent.id)) {
                    assertEquals(parent.id, returnedParent.id)
                    capturedParent = returnedParent
                    capturedChild = (returnedParent.child as LazyModelReference).fetchModel()!!
                    latch.countDown()
                }
            }
            collectRunningLatch.countDown()
        }
        collectRunningLatch.await(1, TimeUnit.SECONDS)

        // WHEN
        Amplify.API.mutate(ModelMutation.create(hasOneChild))
        Amplify.API.mutate(ModelMutation.create(parent))

        // THEN
        withContext(this.coroutineContext) {
            latch.await(10, TimeUnit.SECONDS)
        }

        assertEquals(parent.id, capturedParent!!.id)
        assertEquals(hasOneChild.content, capturedChild!!.content)
        assertEquals(hasOneChild.id, capturedChild!!.id)

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasOneChild))
        Amplify.API.mutate(ModelMutation.delete(parent))
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    @Test
    fun subscribe_with_includes_create() = runTest {
        // GIVEN
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
        val subscription = Amplify.API.subscribe(request)

        var capturedParent: Parent? = null
        var capturedChild: HasOneChild? = null
        CoroutineScope(Dispatchers.IO).launch {
            subscription.collect {
                val returnedParent = it.data
                if (modelCreatedByDevice(returnedParent.id)) {
                    assertEquals(parent.id, returnedParent.id)
                    capturedParent = returnedParent
                    capturedChild = (returnedParent.child as LoadedModelReference).value
                    latch.countDown()
                }
            }
            collectRunningLatch.countDown()
        }
        collectRunningLatch.await(1, TimeUnit.SECONDS)

        // WHEN
        Amplify.API.mutate(ModelMutation.create(hasOneChild))
        val createRequest = ModelMutation.create<Parent, ParentPath>(parent) {
            includes(it.child)
        }
        Amplify.API.mutate(createRequest)

        // THEN
        withContext(this.coroutineContext) {
            latch.await(10, TimeUnit.SECONDS)
        }

        assertEquals(parent.id, capturedParent!!.id)
        assertEquals(hasOneChild.content, capturedChild!!.content)
        assertEquals(hasOneChild.id, capturedChild!!.id)

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasOneChild))
        Amplify.API.mutate(ModelMutation.delete(parent))
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    @Test
    fun subscribe_with_no_includes_update() = runTest {
        // GIVEN
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

        val subscription = Amplify.API.subscribe(ModelSubscription.onUpdate(Parent::class.java))

        val latch = CountDownLatch(1)
        var capturedParent: Parent? = null
        var capturedChild: HasOneChild? = null
        CoroutineScope(Dispatchers.IO).launch {
            subscription.collect {
                val returnedParent = it.data
                if (modelCreatedByDevice(returnedParent.id)) {
                    assertEquals(parent.id, returnedParent.id)
                    capturedParent = returnedParent
                    capturedChild = (returnedParent.child as LazyModelReference).fetchModel()!!
                    latch.countDown()
                }
            }
        }

        Amplify.API.mutate(ModelMutation.create(hasOneChild))
        Amplify.API.mutate(ModelMutation.create(hasOneChild2))
        val parentFromResponse = Amplify.API.mutate(ModelMutation.create(parent)).data
        assertEquals(hasOneChild.id, parentFromResponse.parentChildId)

        // WHEN
        val updateParent = parent.copyOfBuilder().parentChildId(hasOneChild2.id).build()
        Amplify.API.mutate(ModelMutation.update(updateParent))

        // THEN
        withContext(this.coroutineContext) {
            latch.await(10, TimeUnit.SECONDS)
        }

        assertEquals(parent.id, capturedParent!!.id)
        assertEquals(capturedParent!!.parentChildId, capturedChild!!.id)
        assertEquals(hasOneChild2.content, capturedChild!!.content)
        assertEquals(hasOneChild2.id, capturedChild!!.id)

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasOneChild))
        Amplify.API.mutate(ModelMutation.delete(hasOneChild2))
        Amplify.API.mutate(ModelMutation.delete(parent))
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    @Test
    fun subscribe_with_includes_update() = runTest {
        // GIVEN
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
        val subscription = Amplify.API.subscribe(request)

        val latch = CountDownLatch(1)
        var capturedParent: Parent? = null
        var capturedChild: HasOneChild? = null
        CoroutineScope(Dispatchers.IO).launch {
            subscription.collect {
                val returnedParent = it.data
                if (modelCreatedByDevice(returnedParent.id)) {
                    assertEquals(parent.id, returnedParent.id)
                    capturedParent = returnedParent
                    capturedChild = (returnedParent.child as LoadedModelReference).value
                    latch.countDown()
                }
            }
        }

        Amplify.API.mutate(ModelMutation.create(hasOneChild))
        Amplify.API.mutate(ModelMutation.create(hasOneChild2))
        Amplify.API.mutate(ModelMutation.create(parent))

        // WHEN
        val updateParent = parent.copyOfBuilder().parentChildId(hasOneChild2.id).build()
        val updateRequest = ModelMutation.update<Parent, ParentPath>(updateParent) {
            includes(it.child)
        }
        Amplify.API.mutate(updateRequest)

        // THEN
        withContext(this.coroutineContext) {
            latch.await(10, TimeUnit.SECONDS)
        }

        assertEquals(parent.id, capturedParent!!.id)
        assertEquals(capturedParent!!.parentChildId, capturedChild!!.id)
        assertEquals(hasOneChild2.content, capturedChild!!.content)
        assertEquals(hasOneChild2.id, capturedChild!!.id)

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasOneChild))
        Amplify.API.mutate(ModelMutation.delete(hasOneChild2))
        Amplify.API.mutate(ModelMutation.delete(parent))
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    @Test
    fun subscribe_with_no_includes_delete() = runTest {
        // GIVEN
        val hasOneChild = HasOneChild.builder()
            .content("Child1")
            .build()
        val parent = Parent.builder()
            .id(createRandomIdWithRunIdPrefix())
            .parentChildId(hasOneChild.id)
            .build()

        val subscription = Amplify.API.subscribe(ModelSubscription.onDelete(Parent::class.java))

        val latch = CountDownLatch(1)
        var capturedParent: Parent? = null
        var capturedChild: HasOneChild? = null
        CoroutineScope(Dispatchers.IO).launch {
            subscription.collect {
                val returnedParent = it.data
                if (modelCreatedByDevice(returnedParent.id)) {
                    assertEquals(parent.id, returnedParent.id)
                    capturedParent = returnedParent
                    capturedChild = (returnedParent.child as LazyModelReference).fetchModel()!!
                    latch.countDown()
                }
            }
        }

        Amplify.API.mutate(ModelMutation.create(hasOneChild))
        val parentFromResponse = Amplify.API.mutate(ModelMutation.create(parent)).data
        assertEquals(hasOneChild.id, parentFromResponse.parentChildId)

        // WHEN
        Amplify.API.mutate(ModelMutation.delete(parent))

        // THEN
        withContext(this.coroutineContext) {
            latch.await(10, TimeUnit.SECONDS)
        }

        assertEquals(parent.id, capturedParent!!.id)
        assertEquals(hasOneChild.content, capturedChild!!.content)
        assertEquals(hasOneChild.id, capturedChild!!.id)

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasOneChild))
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    @Test
    fun subscribe_with_includes_delete() = runTest {
        // GIVEN
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
        val subscription = Amplify.API.subscribe(request)

        val latch = CountDownLatch(1)
        var capturedParent: Parent? = null
        var capturedChild: HasOneChild? = null
        CoroutineScope(Dispatchers.IO).launch {
            subscription.collect {
                val returnedParent = it.data
                if (modelCreatedByDevice(returnedParent.id)) {
                    assertEquals(parent.id, returnedParent.id)
                    capturedParent = returnedParent
                    capturedChild = (returnedParent.child as LoadedModelReference).value
                    latch.countDown()
                }
            }
        }

        Amplify.API.mutate(ModelMutation.create(hasOneChild))
        Amplify.API.mutate(ModelMutation.create(parent))

        // WHEN
        val deleteRequest = ModelMutation.delete<Parent, ParentPath>(parent) {
            includes(it.child)
        }
        Amplify.API.mutate(deleteRequest)

        // THEN
        withContext(this.coroutineContext) {
            latch.await(10, TimeUnit.SECONDS)
        }

        assertEquals(parent.id, capturedParent!!.id)
        assertEquals(hasOneChild.content, capturedChild!!.content)
        assertEquals(hasOneChild.id, capturedChild!!.id)

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasOneChild))
    }
}
