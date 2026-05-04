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
import com.amazonaws.appsync.extensions.fetchAllPages
import com.amazonaws.appsync.test.R
import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.core.model.LazyModelList
import com.amplifyframework.core.model.LazyModelReference
import com.amplifyframework.core.model.LoadedModelList
import com.amplifyframework.core.model.LoadedModelReference
import com.amplifyframework.core.model.includes
import com.amplifyframework.foundation.result.getOrThrow
import com.amplifyframework.testmodels.lazycpk.HasManyChild
import com.amplifyframework.testmodels.lazycpk.HasOneChild
import com.amplifyframework.testmodels.lazycpk.Parent
import com.amplifyframework.testmodels.lazycpk.ParentPath
import com.amplifyframework.testutils.DeviceFarmTestBase
import com.amplifyframework.testutils.Resources
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test

/**
 * Integration test that verifies [AmplifyAppSyncClient] can perform lazy-loading create
 * mutation operations using codegen models with relationships. Mirrors the test scenarios from
 * [com.amplifyframework.api.aws.GraphQLLazyCreateInstrumentationTest] in the :aws-api module.
 */
@OptIn(ExperimentalAmplifyApi::class, InternalAmplifyApi::class)
class LazyCreateInstrumentationTest : DeviceFarmTestBase() {

    companion object {
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

    @Test
    fun create_with_no_includes() = runTest {
        val hasOneChild = HasOneChild.builder().content("Child1").build()
        client.mutate(ModelMutation.create(hasOneChild)).getOrThrow()
        val parent = Parent.builder().parentChildId(hasOneChild.id).build()
        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        val request = ModelMutation.create(parent)

        val responseParent = client.mutate(request).getOrThrow().data
        client.mutate(ModelMutation.create(hasManyChild)).getOrThrow()

        assertEquals(hasOneChild.id, responseParent.parentChildId)
        (responseParent.child as? LazyModelReference)?.fetchModel()?.let {
            assertEquals(hasOneChild.id, it.id)
            assertEquals(hasOneChild.content, it.content)
        } ?: fail("Response child was null or not a LazyModelReference")

        val children = responseParent.children as LazyModelList
        val hasManyChildren = children.fetchAllPages()
        assertEquals(1, hasManyChildren.size)

        // CLEANUP
        client.mutate(ModelMutation.delete(hasOneChild)).getOrThrow()
        client.mutate(ModelMutation.delete(hasManyChild)).getOrThrow()
        client.mutate(ModelMutation.delete(parent)).getOrThrow()
    }

    @Test
    fun create_with_includes() = runTest {
        val hasOneChild = HasOneChild.builder().content("Child1").build()
        client.mutate(ModelMutation.create(hasOneChild)).getOrThrow()
        val parent = Parent.builder().parentChildId(hasOneChild.id).build()
        val request = ModelMutation.create<Parent, ParentPath>(parent) {
            includes(it.child, it.children)
        }

        val responseParent = client.mutate(request).getOrThrow().data

        assertEquals(parent.id, responseParent.id)
        assertEquals(hasOneChild.id, responseParent.parentChildId)
        (responseParent.child as? LoadedModelReference)?.value?.let {
            assertEquals(hasOneChild.id, it.id)
            assertEquals(hasOneChild.content, it.content)
        } ?: fail("Response child was null or not a LoadedModelReference")
        (responseParent.children as? LoadedModelList)?.let {
            assertEquals(0, it.items.size)
        } ?: fail("Response child was null or not a LoadedModelList")

        // CLEANUP
        client.mutate(ModelMutation.delete(hasOneChild)).getOrThrow()
        client.mutate(ModelMutation.delete(parent)).getOrThrow()
    }

    @Test
    fun create_with_no_includes_null_optional_relationship() = runTest {
        val hasManyChild = HasManyChild.builder().content("Child1").parent(null).build()
        val request = ModelMutation.create(hasManyChild)

        val responseChild = client.mutate(request).getOrThrow().data

        assertEquals(hasManyChild.id, responseChild.id)
        assertEquals("Child1", responseChild.content)
        assertNull((responseChild.parent as LoadedModelReference).value)

        // CLEANUP
        client.mutate(ModelMutation.delete(hasManyChild)).getOrThrow()
    }

    @Test
    fun create_with_no_includes_missing_optional_relationship() = runTest {
        val hasManyChild = HasManyChild.builder().content("Child1").build()
        val request = ModelMutation.create(hasManyChild)

        val responseChild = client.mutate(request).getOrThrow().data

        assertEquals(hasManyChild.id, responseChild.id)
        assertEquals("Child1", responseChild.content)
        assertNull((responseChild.parent as LoadedModelReference).value)

        // CLEANUP
        client.mutate(ModelMutation.delete(hasManyChild)).getOrThrow()
    }
}
