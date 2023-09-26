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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Ignore("Waiting to add test config")
class GraphQLLazySubscribeInstrumentationTest {

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = AmplifyConfiguration.fromConfigFile(context, R.raw.amplifyconfigurationlazy)
        Amplify.addPlugin(AWSApiPlugin())
        Amplify.configure(config, context)
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    @Test
    fun subscribe_with_no_includes_crate() = runTest {
        // GIVEN
        val hasOneChild = HasOneChild.builder()
            .content("Child1")
            .build()
        val parent = Parent.builder().parentChildId(hasOneChild.id).build()


        val latch = CountDownLatch(1)

        var capturedParent: Parent? = null
        var capturedChild: HasOneChild? = null
        CoroutineScope(Dispatchers.IO).launch {
            Amplify.API.subscribe(ModelSubscription.onCreate(Parent::class.java)).collect {
                assertEquals(parent.id, it.data.id)
                capturedParent = it.data
                capturedChild = (it.data.child as LazyModelReference).fetchModel()!!
                latch.countDown()
            }
        }

        // WHEN
        Amplify.API.mutate(ModelMutation.create(hasOneChild))
        Amplify.API.mutate(ModelMutation.create(parent))

        // THEN
        withContext(this.coroutineContext) {
            latch.await(10, TimeUnit.SECONDS)
        }

        assertEquals(parent.id, capturedParent!!.id)
        assertEquals(hasOneChild.content, capturedChild!!.content)
        assertEquals(hasOneChild.content, capturedChild!!.content)

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
        val parent = Parent.builder().parentChildId(hasOneChild.id).build()


        val latch = CountDownLatch(1)

        var capturedParent: Parent? = null
        var capturedChild: HasOneChild? = null
        CoroutineScope(Dispatchers.IO).launch {
            val request = ModelSubscription.onCreate<Parent, ParentPath>(Parent::class.java) {
                includes(it.child)
            }
            Amplify.API.subscribe(request).collect {
                assertEquals(parent.id, it.data.id)
                capturedParent = it.data
                capturedChild = (it.data.child as LoadedModelReference).value
                latch.countDown()
            }
        }

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
        assertEquals(hasOneChild.content, capturedChild!!.content)

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasOneChild))
        Amplify.API.mutate(ModelMutation.delete(parent))
    }
}
