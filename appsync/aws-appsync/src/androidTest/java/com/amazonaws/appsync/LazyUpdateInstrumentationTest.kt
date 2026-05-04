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
import com.amplifyframework.testmodels.lazycpk.HasManyChildPath
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
 * Integration test that verifies [AmplifyAppSyncClient] can perform lazy-loading update
 * mutation operations using codegen models with relationships. Mirrors the test scenarios from
 * [com.amplifyframework.api.aws.GraphQLLazyUpdateInstrumentationTest] in the :aws-api module.
 */
@OptIn(ExperimentalAmplifyApi::class, InternalAmplifyApi::class)
class LazyUpdateInstrumentationTest : DeviceFarmTestBase() {

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
    fun update_with_no_includes() = runTest {
        val hasOneChild = HasOneChild.builder().content("Child1").build()
        client.mutate(ModelMutation.create(hasOneChild)).getOrThrow()

        val hasOneChild2 = HasOneChild.builder().content("Child2").build()
        client.mutate(ModelMutation.create(hasOneChild2)).getOrThrow()

        val parent = Parent.builder().parentChildId(hasOneChild.id).build()
        val parentResponse = client.mutate(ModelMutation.create(parent)).getOrThrow().data

        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        client.mutate(ModelMutation.create(hasManyChild)).getOrThrow()

        val newParent = parentResponse.copyOfBuilder().parentChildId(hasOneChild2.id).build()
        val request = ModelMutation.update(newParent)
        val updatedParent = client.mutate(request).getOrThrow().data

        assertEquals(parent.id, updatedParent.id)
        assertEquals(hasOneChild2.id, updatedParent.parentChildId)
        (updatedParent.child as? LazyModelReference)?.fetchModel()?.let {
            assertEquals(hasOneChild2.id, it.id)
            assertEquals(hasOneChild2.content, it.content)
        } ?: fail("Response child was null or not a LazyModelReference")
        (updatedParent.children as? LazyModelList)?.fetchAllPages()?.let {
            assertEquals(1, it.size)
        } ?: fail("Response child was null or not a LazyModelList")

        // CLEANUP
        client.mutate(ModelMutation.delete(hasOneChild)).getOrThrow()
        client.mutate(ModelMutation.delete(hasOneChild2)).getOrThrow()
        client.mutate(ModelMutation.delete(hasManyChild)).getOrThrow()
        client.mutate(ModelMutation.delete(parent)).getOrThrow()
    }

    @Test
    fun update_with_includes() = runTest {
        val hasOneChild = HasOneChild.builder().content("Child1").build()
        client.mutate(ModelMutation.create(hasOneChild)).getOrThrow()

        val hasOneChild2 = HasOneChild.builder().content("Child2").build()
        client.mutate(ModelMutation.create(hasOneChild2)).getOrThrow()

        val parent = Parent.builder().parentChildId(hasOneChild.id).build()
        val parentResponse = client.mutate(ModelMutation.create(parent)).getOrThrow().data

        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        client.mutate(ModelMutation.create(hasManyChild)).getOrThrow()

        val newParent = parentResponse.copyOfBuilder().parentChildId(hasOneChild2.id).build()
        val request = ModelMutation.update<Parent, ParentPath>(newParent) {
            includes(it.child, it.children)
        }
        val updatedParent = client.mutate(request).getOrThrow().data

        assertEquals(parent.id, updatedParent.id)
        assertEquals(hasOneChild2.id, updatedParent.parentChildId)
        (updatedParent.child as? LoadedModelReference)?.value?.let {
            assertEquals(hasOneChild2.id, it.id)
            assertEquals(hasOneChild2.content, it.content)
        } ?: fail("Response child was null or not a LoadedModelReference")
        (updatedParent.children as? LoadedModelList)?.let {
            assertEquals(1, it.items.size)
        } ?: fail("Response child was null or not a LoadedModelList")

        // CLEANUP
        client.mutate(ModelMutation.delete(hasOneChild)).getOrThrow()
        client.mutate(ModelMutation.delete(hasOneChild2)).getOrThrow()
        client.mutate(ModelMutation.delete(hasManyChild)).getOrThrow()
        client.mutate(ModelMutation.delete(parent)).getOrThrow()
    }

    @Test
    fun update_without_includes_does_not_remove_relationship() = runTest {
        val parent = Parent.builder().build()
        client.mutate(ModelMutation.create(parent)).getOrThrow()

        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        client.mutate(ModelMutation.create(hasManyChild)).getOrThrow()

        val hasManyChildToUpdate = hasManyChild.copyOfBuilder().content("Child2-Updated").build()
        val request = ModelMutation.update(hasManyChildToUpdate)
        val updatedHasManyChild = client.mutate(request).getOrThrow().data

        assertEquals(hasManyChild.id, updatedHasManyChild.id)
        assertEquals("Child2-Updated", updatedHasManyChild.content)
        (updatedHasManyChild.parent as? LazyModelReference)?.fetchModel()?.let {
            assertEquals(parent.id, it.id)
        } ?: fail("Response child was null or not a LazyModelReference")

        // CLEANUP
        client.mutate(ModelMutation.delete(hasManyChild)).getOrThrow()
        client.mutate(ModelMutation.delete(parent)).getOrThrow()
    }

    @Test
    fun update_with_includes_does_not_remove_relationship() = runTest {
        val parent = Parent.builder().build()
        client.mutate(ModelMutation.create(parent)).getOrThrow()

        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        client.mutate(ModelMutation.create(hasManyChild)).getOrThrow()

        val hasManyChildToUpdate = hasManyChild.copyOfBuilder().content("Child2-Updated").build()
        val request = ModelMutation.update<HasManyChild, HasManyChildPath>(hasManyChildToUpdate) {
            includes(it.parent)
        }
        val updatedHasManyChild = client.mutate(request).getOrThrow().data

        assertEquals(hasManyChild.id, updatedHasManyChild.id)
        assertEquals("Child2-Updated", updatedHasManyChild.content)
        (updatedHasManyChild.parent as? LoadedModelReference)?.value?.let {
            assertEquals(parent.id, it.id)
        } ?: fail("Response child was null or not a LoadedModelReference")

        // CLEANUP
        client.mutate(ModelMutation.delete(hasManyChild)).getOrThrow()
        client.mutate(ModelMutation.delete(parent)).getOrThrow()
    }

    @Test
    fun update_without_includes_explicit_remove_relationship() = runTest {
        val parent = Parent.builder().build()
        client.mutate(ModelMutation.create(parent)).getOrThrow()

        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        client.mutate(ModelMutation.create(hasManyChild)).getOrThrow()

        val hasManyChildToUpdate = hasManyChild.copyOfBuilder().parent(null).content("Child2-Updated").build()
        val request = ModelMutation.update(hasManyChildToUpdate)
        val updatedHasManyChild = client.mutate(request).getOrThrow().data

        assertEquals(hasManyChild.id, updatedHasManyChild.id)
        assertEquals("Child2-Updated", updatedHasManyChild.content)
        assertNull((updatedHasManyChild.parent as LoadedModelReference).value)

        // CLEANUP
        client.mutate(ModelMutation.delete(hasManyChild)).getOrThrow()
        client.mutate(ModelMutation.delete(parent)).getOrThrow()
    }

    @Test
    fun update_with_includes_explicit_remove_relationship() = runTest {
        val parent = Parent.builder().build()
        client.mutate(ModelMutation.create(parent)).getOrThrow()

        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        client.mutate(ModelMutation.create(hasManyChild)).getOrThrow()

        val hasManyChildToUpdate = hasManyChild.copyOfBuilder().parent(null).content("Child2-Updated").build()
        val request = ModelMutation.update<HasManyChild, HasManyChildPath>(hasManyChildToUpdate) {
            includes(it.parent)
        }
        val updatedHasManyChild = client.mutate(request).getOrThrow().data

        assertEquals(hasManyChild.id, updatedHasManyChild.id)
        assertEquals("Child2-Updated", updatedHasManyChild.content)
        assertNull((updatedHasManyChild.parent as LoadedModelReference).value)

        // CLEANUP
        client.mutate(ModelMutation.delete(hasManyChild)).getOrThrow()
        client.mutate(ModelMutation.delete(parent)).getOrThrow()
    }
}
