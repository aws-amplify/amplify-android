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
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.core.model.LazyModelList
import com.amplifyframework.core.model.LazyModelReference
import com.amplifyframework.core.model.LoadedModelList
import com.amplifyframework.core.model.LoadedModelReference
import com.amplifyframework.core.model.PaginationToken
import com.amplifyframework.core.model.includes
import com.amplifyframework.kotlin.core.Amplify
import com.amplifyframework.testmodels.lazyinstrumented.Parent
import com.amplifyframework.testmodels.lazyinstrumented.ParentPath
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore("Waiting to add test config")
class GraphQLLazyQueryInstrumentationTest {

    companion object {
        const val PARENT1_ID = "GraphQLLazyQueryInstrumentationTest-Parent"
        const val PARENT2_ID = "GraphQLLazyQueryInstrumentationTest-Parent2"
        const val HAS_ONE_CHILD1_ID = "GraphQLLazyQueryInstrumentationTest-HasOneChild1"
        const val HAS_ONE_CHILD2_ID = "GraphQLLazyQueryInstrumentationTest-HasOneChild2"
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = AmplifyConfiguration.fromConfigFile(context, R.raw.amplifyconfigurationlazy)
        Amplify.addPlugin(AWSApiPlugin())
        Amplify.configure(config, context)
    }

//    private suspend fun populate() {
//        val hasOneChild = HasOneChild.builder()
//            .content("Child1")
//            .id("GraphQLLazyQueryInstrumentationTest-HasOneChild1")
//            .build()
//        Amplify.API.mutate(ModelMutation.create(hasOneChild))
//
//        val parent = Parent.builder().parentChildId(hasOneChild.id).id("GraphQLLazyQueryInstrumentationTest-Parent").build()
//        Amplify.API.mutate(ModelMutation.create(parent))
//
//        val hasOneChild2 = HasOneChild.builder()
//            .content("Child2")
//            .id("GraphQLLazyQueryInstrumentationTest-HasOneChild2")
//            .build()
//        Amplify.API.mutate(ModelMutation.create(hasOneChild2))
//
//        val parent2 = Parent.builder().parentChildId(hasOneChild2.id).id("GraphQLLazyQueryInstrumentationTest-Parent2").build()
//        Amplify.API.mutate(ModelMutation.create(parent2))
//
//        for(i in 0 until 1001) {
//            val hasManyChild = HasManyChild.builder()
//                .content("Child$i")
//                .id("GraphQLLazyQueryInstrumentationTest-HasManyChild$i")
//                .parent(parent)
//                .build()
//            Amplify.API.mutate(ModelMutation.create(hasManyChild))
//        }
//    }

    @Test
    fun query_parent_no_includes() = runBlocking {
        // GIVEN
        val request = ModelQuery[Parent::class.java, Parent.ParentIdentifier(PARENT1_ID)]

        // WHEN
        val responseParent = Amplify.API.query(request).data

        // THEN
        assertEquals(HAS_ONE_CHILD1_ID, responseParent.parentChildId)
        (responseParent.child as? LazyModelReference)?.fetchModel()?.let { child ->
            assertEquals(HAS_ONE_CHILD1_ID, child.id)
            assertEquals("Child1", child.content)
        } ?: fail("Response child was null or not a LazyModelReference")

        val children = responseParent.children as LazyModelList
        var children1HasNextPage = true
        var children1NextToken: PaginationToken? = null
        var children1Count = 0
        while (children1HasNextPage) {
            val page = children.fetchPage(children1NextToken)
            children1HasNextPage = page.hasNextPage
            children1NextToken = page.nextToken
            children1Count += page.items.size
        }
        assertEquals(1001, children1Count)
        return@runBlocking
    }

    @Test
    fun query_parent_with_includes() = runBlocking {
        // GIVEN
        val request = ModelQuery.get<Parent, ParentPath>(
            Parent::class.java, Parent.ParentIdentifier(PARENT1_ID)
        ) {
            includes(it.child, it.children)
        }

        // WHEN
        val responseParent = Amplify.API.query(request).data

        // THEN
        assertEquals(HAS_ONE_CHILD1_ID, responseParent.parentChildId)
        (responseParent.child as? LoadedModelReference)?.let { childRef ->
            val child = childRef.value!!
            assertEquals(HAS_ONE_CHILD1_ID, child.id)
            assertEquals("Child1", child.content)
        } ?: fail("Response child was null or not a LoadedModelReference")

        val children = responseParent.children as LoadedModelList
        assertEquals(100, children.items.size)
        return@runBlocking
    }

    @Test
    fun query_list_with_no_includes() = runBlocking {

        val request = ModelQuery.list(
            Parent::class.java,
            Parent.ID.beginsWith("GraphQLLazyQueryInstrumentationTest-Parent")
        )

        // WHEN
        val paginatedResult = Amplify.API.query(request).data

        assertFalse(paginatedResult.hasNextResult())

        // THEN
        val parents = paginatedResult.items.toList()
        assertEquals(2, parents.size)

        val parent2 = parents[0]
        val parent1 = parents[1]

        assertEquals(HAS_ONE_CHILD1_ID, parent1.parentChildId)
        assertEquals(PARENT1_ID, parent1.id)
        (parent1.child as? LazyModelReference)?.fetchModel()?.let { child ->
            assertEquals(HAS_ONE_CHILD1_ID, child.id)
            assertEquals("Child1", child.content)
        } ?: fail("Response child was null or not a LazyModelReference")

        val childrenFromParent1 = parent1.children as LazyModelList
        var children1HasNextPage = true
        var children1NextToken: PaginationToken? = null
        var children1Count = 0
        while (children1HasNextPage) {
            val page = childrenFromParent1.fetchPage(children1NextToken)
            children1HasNextPage = page.hasNextPage
            children1NextToken = page.nextToken
            children1Count += page.items.size
        }
        assertEquals(1001, children1Count)

        assertEquals(HAS_ONE_CHILD2_ID, parent2.parentChildId)
        assertEquals(PARENT2_ID, parent2.id)
        (parent2.child as? LazyModelReference)?.fetchModel()?.let { child ->
            assertEquals(HAS_ONE_CHILD2_ID, child.id)
            assertEquals("Child2", child.content)
        } ?: fail("Response child was null or not a LazyModelReference")

        val childrenFromParent2 = parent2.children as LazyModelList
        var children2HasNextPage = true
        var children2NextToken: PaginationToken? = null
        var children2Count = 0
        while (children2HasNextPage) {
            val page = childrenFromParent2.fetchPage(children2NextToken)
            children2HasNextPage = page.hasNextPage
            children2NextToken = page.nextToken
            children2Count += page.items.size
        }
        assertEquals(0, children2Count)
        return@runBlocking
    }

    @Test
    fun query_list_with_includes() = runBlocking {

        val request = ModelQuery.list<Parent, ParentPath>(
            Parent::class.java,
            Parent.ID.beginsWith("GraphQLLazyQueryInstrumentationTest-Parent")
        ) {
            includes(it.child, it.children)
        }

        // WHEN
        val paginatedResult = Amplify.API.query(request).data

        // THEN
        assertFalse(paginatedResult.hasNextResult())

        val parents = paginatedResult.items.toList()
        assertEquals(2, parents.size)

        val parent2 = parents[0]
        val parent1 = parents[1]

        assertEquals(HAS_ONE_CHILD1_ID, parent1.parentChildId)
        assertEquals(PARENT1_ID, parent1.id)
        (parent1.child as? LoadedModelReference)?.let { childRef ->
            val child = childRef.value!!
            assertEquals(HAS_ONE_CHILD1_ID, child.id)
            assertEquals("Child1", child.content)
        } ?: fail("Response child was null or not a LoadedModelReference")

        assertEquals(HAS_ONE_CHILD2_ID, parent2.parentChildId)
        assertEquals(PARENT2_ID, parent2.id)
        (parent2.child as? LoadedModelReference)?.let { childRef ->
            val child = childRef.value!!
            assertEquals(HAS_ONE_CHILD2_ID, child.id)
            assertEquals("Child2", child.content)
        } ?: fail("Response child was null or not a LoadedModelReference")

        val children = parent2.children as LoadedModelList
        assertEquals(0, children.items.size)
        return@runBlocking
    }
}
