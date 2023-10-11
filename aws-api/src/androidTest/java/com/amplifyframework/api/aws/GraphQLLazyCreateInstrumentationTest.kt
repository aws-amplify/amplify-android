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
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.core.model.LazyModelList
import com.amplifyframework.core.model.LazyModelReference
import com.amplifyframework.core.model.LoadedModelList
import com.amplifyframework.core.model.LoadedModelReference
import com.amplifyframework.core.model.PaginationToken
import com.amplifyframework.core.model.includes
import com.amplifyframework.datastore.generated.model.HasManyChild
import com.amplifyframework.datastore.generated.model.HasOneChild
import com.amplifyframework.datastore.generated.model.Parent
import com.amplifyframework.datastore.generated.model.ParentPath
import com.amplifyframework.kotlin.core.Amplify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test

class GraphQLLazyCreateInstrumentationTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUp() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val config = AmplifyConfiguration.fromConfigFile(context, R.raw.amplifyconfigurationlazy)
            Amplify.addPlugin(AWSApiPlugin())
            Amplify.configure(config, context)
        }
    }

    @Test
    fun create_with_no_includes() = runTest {
        // GIVEN
        val hasOneChild = HasOneChild.builder().content("Child1").build()
        Amplify.API.mutate(ModelMutation.create(hasOneChild))
        val parent = Parent.builder().parentChildId(hasOneChild.id).build()
        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        val request = ModelMutation.create(parent)

        // WHEN
        val responseParent = Amplify.API.mutate(request).data
        Amplify.API.mutate(ModelMutation.create(hasManyChild))

        // THEN
        assertEquals(hasOneChild.id, responseParent.parentChildId)
        (responseParent.child as? LazyModelReference)?.fetchModel()?.let {
            assertEquals(hasOneChild.id, it.id)
            assertEquals(hasOneChild.content, it.content)
        } ?: fail("Response child was null or not a LazyModelReference")

        val children = responseParent.children as LazyModelList
        var children1HasNextPage = true
        var children1NextToken: PaginationToken? = null
        var hasManyChildren = mutableListOf<HasManyChild>()
        while (children1HasNextPage) {
            val page = children.fetchPage(children1NextToken)
            children1HasNextPage = page.hasNextPage
            children1NextToken = page.nextToken
            hasManyChildren.addAll(page.items)
        }
        assertEquals(1, hasManyChildren.size)

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasOneChild))
        Amplify.API.mutate(ModelMutation.delete(hasManyChild))
        Amplify.API.mutate(ModelMutation.delete(parent))
    }

    @Test
    fun create_with_includes() = runTest {
        // GIVEN
        val hasOneChild = HasOneChild.builder().content("Child1").build()
        Amplify.API.mutate(ModelMutation.create(hasOneChild))
        val parent = Parent.builder().parentChildId(hasOneChild.id).build()
        val request = ModelMutation.create<Parent, ParentPath>(parent) {
            includes(it.child, it.children)
        }

        // WHEN
        val responseParent = Amplify.API.mutate(request).data

        // THEN
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
        Amplify.API.mutate(ModelMutation.delete(hasOneChild))
        Amplify.API.mutate(ModelMutation.delete(parent))
    }

    @Test
    fun create_with_no_includes_null_optional_relationship() = runTest {
        // GIVEN
        val hasManyChild = HasManyChild.builder().content("Child1").parent(null).build()
        val request = ModelMutation.create(hasManyChild)
        // WHEN
        val responseChild = Amplify.API.mutate(request).data

        // THEN
        assertEquals(hasManyChild.id, responseChild.id)
        assertEquals("Child1", responseChild.content)
        assertNull((responseChild.parent as LoadedModelReference).value)

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasManyChild))
    }

    @Test
    fun create_with_no_includes_missing_optional_relationship() = runTest {
        // GIVEN
        val hasManyChild = HasManyChild.builder().content("Child1").build()
        val request = ModelMutation.create(hasManyChild)
        // WHEN
        val responseChild = Amplify.API.mutate(request).data

        // THEN
        assertEquals(hasManyChild.id, responseChild.id)
        assertEquals("Child1", responseChild.content)
        assertNull((responseChild.parent as LoadedModelReference).value)

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasManyChild))
    }
}
