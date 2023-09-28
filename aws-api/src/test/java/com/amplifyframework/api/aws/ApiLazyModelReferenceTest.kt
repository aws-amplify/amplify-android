/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.api.aws

import com.amplifyframework.AmplifyException
import com.amplifyframework.api.ApiCategory
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.core.Consumer
import com.amplifyframework.testmodels.lazy.Blog
import com.amplifyframework.testmodels.lazy.Post
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApiLazyModelReferenceTest {

    private val apiCategory = mockk<ApiCategory>()

    private val expectedQuery = "query GetPost(\$id: ID!) {\n" +
        "  getPost(id: \$id) {\n" +
        "    blog {\n" +
        "      id\n" +
        "    }\n" +
        "    createdAt\n" +
        "    id\n" +
        "    name\n" +
        "    updatedAt\n" +
        "  }\n" +
        "}\n"
    private val expectedContent = "{\"query\": \"query GetPost(\$id: ID!) {\\n  getPost(id: \$id) {\\n    " +
        "blog {\\n      id\\n    }\\n    createdAt\\n    id\\n    name\\n    updatedAt\\n  }" +
        "\\n}\\n\", \"variables\": {\"id\":\"p1\"}}"
    private val expectedVariables = mapOf(Pair("id", "p1"))
    private val expectedPost = Post.builder().name("My Post").blog(Blog.justId("b1")).build()

    @Test
    fun fetch_with_provided_api_success_uses_cache_after() = runTest {
        // GIVEN
        val expectedApi = "myApi"
        val requestSlot = slot<GraphQLRequest<Post>>()

        every {
            apiCategory.query(any(), capture(requestSlot), any<Consumer<GraphQLResponse<Post>>>(), any())
        } answers {
            thirdArg<Consumer<GraphQLResponse<Post>>>().accept(GraphQLResponse(expectedPost, null))
            mockk()
        }
        val postReference = ApiLazyModelReference(Post::class.java, expectedVariables, expectedApi, apiCategory)

        // WHEN
        val fetchedPost1 = postReference.fetchModel()
        val fetchedPost2 = postReference.fetchModel()

        // THEN
        verify(exactly = 1) { apiCategory.query(expectedApi, any(), any<Consumer<GraphQLResponse<Post>>>(), any()) }
        assertEquals(expectedPost, fetchedPost1)
        assertEquals(fetchedPost1, fetchedPost2)
        assertEquals(expectedQuery, requestSlot.captured.query)
        assertEquals(expectedContent, requestSlot.captured.content)
        assertEquals(expectedVariables, requestSlot.captured.variables)
    }

    @Test
    fun fetch_default_api_success_uses_cache_after() = runTest {
        // GIVEN
        val requestSlot = slot<GraphQLRequest<Post>>()

        every { apiCategory.query(capture(requestSlot), any<Consumer<GraphQLResponse<Post>>>(), any()) } answers {
            secondArg<Consumer<GraphQLResponse<Post>>>().accept(GraphQLResponse(expectedPost, null))
            mockk()
        }
        val postReference = ApiLazyModelReference(Post::class.java, mapOf(Pair("id", "p1")), apiCategory = apiCategory)

        // WHEN
        val fetchedPost1 = postReference.fetchModel()
        val fetchedPost2 = postReference.fetchModel()

        // THEN
        verify(exactly = 1) { apiCategory.query(any(), any<Consumer<GraphQLResponse<Post>>>(), any()) }
        assertEquals(expectedPost, fetchedPost1)
        assertEquals(fetchedPost1, fetchedPost2)
        assertEquals(expectedQuery, requestSlot.captured.query)
        assertEquals(expectedContent, requestSlot.captured.content)
        assertEquals(expectedVariables, requestSlot.captured.variables)
    }

    @Test
    fun fetch_failure_tries_again() = runTest {
        val expectedPost = Post.builder().name("My Post").blog(Blog.justId("b1")).build()
        val expectedApi = "myApi"
        val apiException = ApiException("fail", "fail")
        val expectedException = AmplifyException("Error lazy loading the model.", apiException, "fail")
        val postReference = ApiLazyModelReference(Post::class.java, mapOf(Pair("id", "p1")), expectedApi, apiCategory)

        // fail first time
        every { apiCategory.query(any(), any(), any<Consumer<GraphQLResponse<Post>>>(), any()) } answers {
            lastArg<Consumer<ApiException>>().accept(apiException)
            mockk()
        }

        var fetchedPost1: Post? = null
        var capturedException: AmplifyException? = null
        try {
            fetchedPost1 = postReference.fetchModel()
        } catch (e: AmplifyException) {
            capturedException = e
        }

        assertNull(fetchedPost1)
        assertEquals(expectedException, capturedException)

        // success second time
        every { apiCategory.query(any(), any(), any<Consumer<GraphQLResponse<Post>>>(), any()) } answers {
            thirdArg<Consumer<GraphQLResponse<Post>>>().accept(GraphQLResponse(expectedPost, null))
            mockk()
        }

        val fetchedPost2 = postReference.fetchModel()

        verify(exactly = 2) { apiCategory.query(expectedApi, any(), any<Consumer<GraphQLResponse<Post>>>(), any()) }
        assertNull(fetchedPost1)
        assertEquals(expectedPost, fetchedPost2)
    }

    @Test
    fun empty_map_returns_null_model() = runTest {
        // GIVEN
        val postReference = ApiLazyModelReference(Post::class.java, emptyMap(), apiCategory = apiCategory)

        // WHEN
        val fetchedPost1 = postReference.fetchModel()

        // THEN
        verify(exactly = 0) { apiCategory.query(any(), any<Consumer<GraphQLResponse<Post>>>(), any()) }
        assertNull(fetchedPost1)
    }

    @Test
    fun fetch_with_callbacks_with_provided_api_success_uses_cache_after() = runTest {
        // GIVEN
        val expectedApi = "myApi"
        val requestSlot = slot<GraphQLRequest<Post>>()

        every {
            apiCategory.query(any(), capture(requestSlot), any<Consumer<GraphQLResponse<Post>>>(), any())
        } answers {
            thirdArg<Consumer<GraphQLResponse<Post>>>().accept(GraphQLResponse(expectedPost, null))
            mockk()
        }
        val postReference = ApiLazyModelReference(Post::class.java, expectedVariables, expectedApi, apiCategory)
        var fetchedPost1: Post? = null
        var fetchedPost2: Post? = null

        // WHEN
        var latch = CountDownLatch(1)
        postReference.fetchModel({ fetchedPost1 = it; latch.countDown() }, {})
        latch.await(2, TimeUnit.SECONDS)
        latch = CountDownLatch(1)
        postReference.fetchModel({ fetchedPost2 = it; latch.countDown() }, {})
        latch.await(2, TimeUnit.SECONDS)

        // THEN
        verify(exactly = 1) { apiCategory.query(expectedApi, any(), any<Consumer<GraphQLResponse<Post>>>(), any()) }
        assertEquals(expectedPost, fetchedPost1)
        assertEquals(fetchedPost1, fetchedPost2)
        assertEquals(expectedQuery, requestSlot.captured.query)
        assertEquals(expectedContent, requestSlot.captured.content)
        assertEquals(expectedVariables, requestSlot.captured.variables)
    }

    @Test
    fun fetch_with_callbacks_failure_tries_again() = runTest {
        val expectedPost = Post.builder().name("My Post").blog(Blog.justId("b1")).build()
        val expectedApi = "myApi"
        val apiException = ApiException("fail", "fail")
        val expectedException = AmplifyException("Error lazy loading the model.", apiException, "fail")
        val postReference = ApiLazyModelReference(Post::class.java, mapOf(Pair("id", "p1")), expectedApi, apiCategory)
        var latch = CountDownLatch(1)
        var fetchedPost1: Post? = null
        var fetchedPost2: Post? = null
        var capturedException1: AmplifyException? = null
        var capturedException2: AmplifyException? = null

        // fail first time
        every { apiCategory.query(any(), any(), any<Consumer<GraphQLResponse<Post>>>(), any()) } answers {
            lastArg<Consumer<ApiException>>().accept(apiException)
            mockk()
        }

        postReference.fetchModel(
            {
                fetchedPost1 = it
                latch.countDown()
            },
            {
                capturedException1 = it
                latch.countDown()
            }
        )

        latch.await(2, TimeUnit.SECONDS)
        latch = CountDownLatch(1)

        // success second time
        every { apiCategory.query(any(), any(), any<Consumer<GraphQLResponse<Post>>>(), any()) } answers {
            thirdArg<Consumer<GraphQLResponse<Post>>>().accept(GraphQLResponse(expectedPost, null))
            mockk()
        }

        postReference.fetchModel(
            {
                fetchedPost2 = it
                latch.countDown()
            },
            {
                capturedException2 = it
                latch.countDown()
            }
        )

        latch.await(2, TimeUnit.SECONDS)
        verify(exactly = 2) { apiCategory.query(expectedApi, any(), any<Consumer<GraphQLResponse<Post>>>(), any()) }
        assertNull(fetchedPost1)
        assertEquals(expectedException, capturedException1)
        assertNull(capturedException2)
        assertEquals(expectedPost, fetchedPost2)
    }
}
