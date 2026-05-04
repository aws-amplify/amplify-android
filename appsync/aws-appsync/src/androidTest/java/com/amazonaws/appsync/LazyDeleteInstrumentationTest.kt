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
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test

/**
 * Integration test that verifies [AmplifyAppSyncClient] can perform lazy-loading delete
 * mutation operations using codegen models with relationships. Mirrors the test scenarios from
 * [com.amplifyframework.api.aws.GraphQLLazyDeleteInstrumentationTest] in the :aws-api module.
 */
@OptIn(ExperimentalAmplifyApi::class, InternalAmplifyApi::class)
class LazyDeleteInstrumentationTest : DeviceFarmTestBase() {

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
    fun delete_with_no_includes() = runTest {
        val hasOneChild = HasOneChild.builder().content("Child1").build()
        client.mutate(ModelMutation.create(hasOneChild)).getOrThrow()

        val parent = Parent.builder().parentChildId(hasOneChild.id).build()
        client.mutate(ModelMutation.create(parent)).getOrThrow()

        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        client.mutate(ModelMutation.create(hasManyChild)).getOrThrow()

        val request = ModelMutation.delete(parent)
        val updatedParent = client.mutate(request).getOrThrow().data

        assertEquals(parent.id, updatedParent.id)
        assertEquals(hasOneChild.id, updatedParent.parentChildId)
        (updatedParent.child as? LazyModelReference)?.fetchModel()?.let {
            assertEquals(hasOneChild.id, it.id)
            assertEquals(hasOneChild.content, it.content)
        } ?: fail("Response child was null or not a LazyModelReference")
        (updatedParent.children as? LazyModelList)?.fetchAllPages()?.let {
            assertEquals(1, it.size)
        } ?: fail("Response child was null or not a LazyModelList")

        // CLEANUP
        client.mutate(ModelMutation.delete(hasOneChild)).getOrThrow()
        client.mutate(ModelMutation.delete(hasManyChild)).getOrThrow()
    }

    @Test
    fun delete_with_includes() = runTest {
        val hasOneChild = HasOneChild.builder().content("Child1").build()
        client.mutate(ModelMutation.create(hasOneChild)).getOrThrow()

        val parent = Parent.builder().parentChildId(hasOneChild.id).build()
        client.mutate(ModelMutation.create(parent)).getOrThrow()

        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        client.mutate(ModelMutation.create(hasManyChild)).getOrThrow()

        val request = ModelMutation.delete<Parent, ParentPath>(parent) {
            includes(it.child, it.children)
        }
        val updatedParent = client.mutate(request).getOrThrow().data

        assertEquals(parent.id, updatedParent.id)
        assertEquals(hasOneChild.id, updatedParent.parentChildId)
        (updatedParent.child as? LoadedModelReference)?.value?.let {
            assertEquals(hasOneChild.id, it.id)
            assertEquals(hasOneChild.content, it.content)
        } ?: fail("Response child was null or not a LoadedModelReference")
        (updatedParent.children as? LoadedModelList)?.let {
            assertEquals(1, it.items.size)
        } ?: fail("Response child was null or not a LoadedModelList")

        // CLEANUP
        client.mutate(ModelMutation.delete(hasOneChild)).getOrThrow()
        client.mutate(ModelMutation.delete(hasManyChild)).getOrThrow()
    }
}
