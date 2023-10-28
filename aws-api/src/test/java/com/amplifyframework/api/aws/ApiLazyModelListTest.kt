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
import com.amplifyframework.core.model.ModelPage
import com.amplifyframework.testmodels.lazy.Blog
import com.amplifyframework.testmodels.lazy.Post
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApiLazyModelListTest {

    private val apiCategory = mockk<ApiCategory>()
    private val expectedApiName = "myApi"
    private val expectedQuery = "query ListPosts(\$filter: ModelPostFilterInput, \$limit: Int) {\n" +
        "  listPosts(filter: \$filter, limit: \$limit) {\n" +
        "    items {\n" +
        "      blog {\n" +
        "        id\n" +
        "      }\n" +
        "      createdAt\n" +
        "      id\n" +
        "      name\n" +
        "      updatedAt\n" +
        "    }\n" +
        "    nextToken\n" +
        "  }\n" +
        "}\n"
    private val expectedContent = "{\"query\": \"query ListPosts(\$filter: ModelPostFilterInput, \$limit: Int) " +
        "{\\n  listPosts(filter: \$filter, limit: \$limit) {\\n    items {\\n      blog {\\n        id\\n      }" +
        "\\n      createdAt\\n      id\\n      name\\n      updatedAt\\n    }\\n    nextToken\\n  }\\n}\\n\", " +
        "\"variables\": {\"filter\":{\"blogPostsId\":{\"eq\":\"b1\"}},\"limit\":1000}}"
    private val expectedVariables = "{filter={blogPostsId={eq=b1}}, limit=1000}"

    private val expectedNextToken = ApiPaginationToken("456")
    private val expectedContentWithToken = "{\"query\": \"query ListPosts(\$filter: ModelPostFilterInput, " +
        "\$limit: Int, \$nextToken: String) {\\n  listPosts(filter: \$filter, limit: \$limit, " +
        "nextToken: \$nextToken) {\\n    items {\\n      blog {\\n        id\\n      }\\n      " +
        "createdAt\\n      id\\n      name\\n      updatedAt\\n    }\\n    nextToken\\n  }\\n}\\n\", \"" +
        "variables\": {\"filter\":{\"blogPostsId\":{\"eq\":\"b1\"}},\"limit\":1000,\"nextToken\":\"123\"}}"

    private val expectedQueryWithToken =
        "query ListPosts(\$filter: ModelPostFilterInput, \$limit: Int, \$nextToken: String) {\n" +
            "  listPosts(filter: \$filter, limit: \$limit, nextToken: \$nextToken) {\n" +
            "    items {\n" +
            "      blog {\n" +
            "        id\n" +
            "      }\n" +
            "      createdAt\n" +
            "      id\n" +
            "      name\n" +
            "      updatedAt\n" +
            "    }\n" +
            "    nextToken\n" +
            "  }\n" +
            "}\n"
    private val expectedVariablesWithToken = "{filter={blogPostsId={eq=b1}}, limit=1000, nextToken=123}"

    private val items = listOf<Post>(
        Post.builder().name("p1").blog(Blog.justId("b1")).build(),
        Post.builder().name("p2").blog(Blog.justId("b1")).build(),
    )

    @Test
    fun fetch_with_provided_api_success() = runTest {
        val lazyPostList = ApiLazyModelList(
            Post::class.java,
            mapOf(Pair("blogPostsId", "b1")),
            expectedApiName,
            apiCategory
        )
        val requestSlot = slot<GraphQLRequest<Post>>()

        every {
            apiCategory.query(any(), capture(requestSlot), any(), any())
        } answers {
            thirdArg<Consumer<GraphQLResponse<ModelPage<Post>>>>().accept(
                GraphQLResponse(ApiModelPage(items, null), null)
            )
            mockk()
        }

        val page = lazyPostList.fetchPage()

        assertEquals(items, page.items)
        assertFalse(page.hasNextPage)
        assertNull(page.nextToken)
        assertEquals(expectedContent, requestSlot.captured.content)
        assertEquals(expectedQuery, requestSlot.captured.query)
        assertEquals(expectedVariables, requestSlot.captured.variables.toString())
    }

    @Test
    fun fetch_with_no_provided_api_success() = runTest {
        val lazyPostList = ApiLazyModelList(
            Post::class.java,
            mapOf(Pair("blogPostsId", "b1")),
            null,
            apiCategory
        )
        val requestSlot = slot<GraphQLRequest<Post>>()

        every {
            apiCategory.query(capture(requestSlot), any(), any())
        } answers {
            secondArg<Consumer<GraphQLResponse<ModelPage<Post>>>>().accept(
                GraphQLResponse(ApiModelPage(items, null), null)
            )
            mockk()
        }

        val page = lazyPostList.fetchPage()

        assertEquals(items, page.items)
        assertFalse(page.hasNextPage)
        assertNull(page.nextToken)
        assertEquals(expectedContent, requestSlot.captured.content)
        assertEquals(expectedQuery, requestSlot.captured.query)
        assertEquals(expectedVariables, requestSlot.captured.variables.toString())
    }

    @Test
    fun fetch_with_provided_api_and_token_success() = runTest {
        val lazyPostList = ApiLazyModelList(
            Post::class.java,
            mapOf(Pair("blogPostsId", "b1")),
            expectedApiName,
            apiCategory
        )
        val expectedToken = ApiPaginationToken("456")
        val requestSlot = slot<GraphQLRequest<Post>>()
        every {
            apiCategory.query(any(), capture(requestSlot), any(), any())
        } answers {
            thirdArg<Consumer<GraphQLResponse<ModelPage<Post>>>>().accept(
                GraphQLResponse(ApiModelPage(items, expectedToken), null)
            )
            mockk()
        }

        val page = lazyPostList.fetchPage(ApiPaginationToken("123"))

        assertEquals(items, page.items)
        assertTrue(page.hasNextPage)
        assertNotNull(page.nextToken)
        assertEquals(expectedToken, page.nextToken)
        assertEquals(expectedContentWithToken, requestSlot.captured.content)
        assertEquals(expectedQueryWithToken, requestSlot.captured.query)
        assertEquals(expectedVariablesWithToken, requestSlot.captured.variables.toString())
    }

    @Test
    fun fetch_with_provided_api_failure() = runTest {
        val lazyPostList = ApiLazyModelList(
            Post::class.java,
            mapOf(Pair("blogPostsId", "b1")),
            expectedApiName,
            apiCategory
        )
        val requestSlot = slot<GraphQLRequest<Post>>()
        val apiException = ApiException("fail", "fail")
        val expectedException = AmplifyException("Error lazy loading the model list.", apiException, "fail")

        every {
            apiCategory.query(any(), capture(requestSlot), any(), any())
        } answers {
            lastArg<Consumer<ApiException>>().accept(apiException)
            mockk()
        }

        var page: ModelPage<Post>? = null
        var capturedException: AmplifyException? = null
        try {
            page = lazyPostList.fetchPage()
        } catch (e: AmplifyException) {
            capturedException = e
        }

        assertNull(page)
        assertEquals(expectedException, capturedException)
    }

    @Test
    fun fetch_by_callback_with_provided_api_success() = runTest {
        val lazyPostList = ApiLazyModelList(
            Post::class.java,
            mapOf(Pair("blogPostsId", "b1")),
            expectedApiName,
            apiCategory
        )
        val requestSlot = slot<GraphQLRequest<Post>>()
        val latch = CountDownLatch(1)

        every {
            apiCategory.query(any(), capture(requestSlot), any(), any())
        } answers {
            thirdArg<Consumer<GraphQLResponse<ModelPage<Post>>>>().accept(
                GraphQLResponse(ApiModelPage(items, null), null)
            )
            mockk()
        }

        var page: ModelPage<Post>? = null
        var capturedException: AmplifyException? = null
        lazyPostList.fetchPage(
            onSuccess = {
                page = it
                latch.countDown()
            },
            onError = {
                capturedException = it
                latch.countDown()
            }
        )

        latch.await(2, TimeUnit.SECONDS)
        assertNotNull(page)
        assertNull(capturedException)

        assertEquals(items, page!!.items)
        assertFalse(page!!.hasNextPage)
        assertNull(page!!.nextToken)
        assertEquals(expectedContent, requestSlot.captured.content)
        assertEquals(expectedQuery, requestSlot.captured.query)
        assertEquals(expectedVariables, requestSlot.captured.variables.toString())
    }

    @Test
    fun fetch_by_callback_with_token_provided_api_success() = runTest {
        val lazyPostList = ApiLazyModelList(
            Post::class.java,
            mapOf(Pair("blogPostsId", "b1")),
            expectedApiName,
            apiCategory
        )

        val requestSlot = slot<GraphQLRequest<Post>>()
        val latch = CountDownLatch(1)

        every {
            apiCategory.query(any(), capture(requestSlot), any(), any())
        } answers {
            thirdArg<Consumer<GraphQLResponse<ModelPage<Post>>>>().accept(
                GraphQLResponse(ApiModelPage(items, expectedNextToken), null)
            )
            mockk()
        }

        var page: ModelPage<Post>? = null
        var capturedException: AmplifyException? = null
        lazyPostList.fetchPage(
            ApiPaginationToken("123"),
            onSuccess = {
                page = it
                latch.countDown()
            },
            onError = {
                capturedException = it
                latch.countDown()
            }
        )

        latch.await(2, TimeUnit.SECONDS)
        assertNotNull(page)
        assertNull(capturedException)

        assertEquals(items, page!!.items)
        assertTrue(page!!.hasNextPage)
        assertEquals(expectedNextToken, page!!.nextToken)
        assertEquals(expectedContentWithToken, requestSlot.captured.content)
        assertEquals(expectedQueryWithToken, requestSlot.captured.query)
        assertEquals(expectedVariablesWithToken, requestSlot.captured.variables.toString())
    }

    @Test
    fun fetch_by_callback_with_provided_api_failure() = runTest {
        val lazyPostList = ApiLazyModelList(
            Post::class.java,
            mapOf(Pair("blogPostsId", "b1")),
            expectedApiName,
            apiCategory
        )
        val requestSlot = slot<GraphQLRequest<Post>>()
        val apiException = ApiException("fail", "fail")
        val expectedException = AmplifyException("Error lazy loading the model list.", apiException, "fail")
        val latch = CountDownLatch(1)

        every {
            apiCategory.query(any(), capture(requestSlot), any(), any())
        } answers {
            lastArg<Consumer<ApiException>>().accept(apiException)
            mockk()
        }

        var page: ModelPage<Post>? = null
        var capturedException: AmplifyException? = null
        lazyPostList.fetchPage(
            onSuccess = {
                page = it
                latch.countDown()
            },
            onError = {
                capturedException = it
                latch.countDown()
            }
        )

        latch.await(2, TimeUnit.SECONDS)
        assertNull(page)
        assertEquals(expectedException, capturedException)
    }

    @Test
    fun fetch_by_callback_with_no_provided_api_failure() = runTest {
        val lazyPostList = ApiLazyModelList(
            Post::class.java,
            mapOf(Pair("blogPostsId", "b1")),
            null,
            apiCategory
        )
        val requestSlot = slot<GraphQLRequest<Post>>()
        val apiException = ApiException("fail", "fail")
        val expectedException = AmplifyException("Error lazy loading the model list.", apiException, "fail")
        val latch = CountDownLatch(1)

        every {
            apiCategory.query(capture(requestSlot), any(), any())
        } answers {
            lastArg<Consumer<ApiException>>().accept(apiException)
            mockk()
        }

        var page: ModelPage<Post>? = null
        var capturedException: AmplifyException? = null
        lazyPostList.fetchPage(
            onSuccess = {
                page = it
                latch.countDown()
            },
            onError = {
                capturedException = it
                latch.countDown()
            }
        )

        latch.await(2, TimeUnit.SECONDS)
        assertNull(page)
        assertEquals(expectedException, capturedException)
    }
}
