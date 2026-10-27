/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.AmplifyException
import com.amplifyframework.api.ApiCategory
import com.amplifyframework.api.ApiCategoryConfiguration
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.ModelPage
import com.amplifyframework.core.model.PaginationToken
import com.amplifyframework.testmodels.lazy.Post
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApiLazyModelListNullDataTest {
    private val apiCategory = mockk<ApiCategory>()
    private val factory = GsonGraphQLResponseFactory()
    private val deniedJson = """{"data":{"listPosts":null},"errors":[{"message":"Not Authorized"}]}"""
    private val emptyJson = """{"data":{"listPosts":{"items":[],"nextToken":null}},"errors":[]}"""

    @Test
    fun `real response factory accepts GraphQL errors with null data`() {
        val response = factory.buildResponse(request(), deniedJson, null)
        response.hasData() shouldBe false
        response.hasErrors() shouldBe true
        response.errors.single().message shouldBe "Not Authorized"
    }

    @Test
    fun `suspend fetch without api name reports AmplifyException for GraphQL null data`() = runTest {
        respondWith(deniedJson)
        shouldThrow<AmplifyException> { lazyList(null).fetchPage() }
    }

    @Test
    fun `suspend fetch with api name reports AmplifyException for GraphQL null data`() = runTest {
        respondWith(deniedJson)
        shouldThrow<AmplifyException> { lazyList("myApi").fetchPage() }
    }

    @Test
    fun `suspend fetch with pagination token reports AmplifyException for GraphQL null data`() = runTest {
        respondWith(deniedJson)
        shouldThrow<AmplifyException> { lazyList("myApi").fetchPage(ApiPaginationToken("next")) }
    }

    @Test
    fun `suspend fetch reports AmplifyException when both data and errors are absent`() = runTest {
        respondWith("""{"data":{"listPosts":null},"errors":null}""")
        shouldThrow<AmplifyException> { lazyList(null).fetchPage() }
    }

    @Test
    fun `an empty real page remains a successful page`() = runTest {
        respondWith(emptyJson)
        val page = lazyList(null).fetchPage()
        page.items.shouldBeEmpty()
        page.hasNextPage shouldBe false
    }

    @Test
    fun `real HTTP GraphQL error reaches lazy fetch through configured API plugin`() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().code(200).body(deniedJson).build())
            val configuration = ApiCategoryConfiguration().apply {
                populateFromJSON(
                    JSONObject().put(
                        "plugins",
                        JSONObject().put(
                            "awsAPIPlugin",
                            JSONObject().put(
                                "graphQlApi",
                                JSONObject()
                                    .put("endpointType", "GraphQL")
                                    .put("endpoint", server.url("/").toString())
                                    .put("region", "us-east-1")
                                    .put("authorizationType", "API_KEY")
                                    .put("apiKey", "FAKE-API-KEY")
                            )
                        )
                    )
                )
            }
            val category = ApiCategory().apply {
                addPlugin(AWSApiPlugin())
                configure(configuration, ApplicationProvider.getApplicationContext())
            }
            val list = ApiLazyModelList(
                Post::class.java,
                mapOf("blogPostsId" to "b1"),
                "graphQlApi",
                category
            )
            shouldThrow<AmplifyException> { list.fetchPage() }
            server.requestCount shouldBe 1
        }
    }

    @Test
    fun `callback overload without token routes null data to onError`() {
        assertCallbackRoutesError(useTokenOverload = false)
    }

    @Test
    fun `callback overload with token routes null data to onError`() {
        assertCallbackRoutesError(useTokenOverload = true)
    }

    @Test
    fun `transport failures retain the existing AmplifyException wrapping`() = runTest {
        val transportError = ApiException("Network failed", "Retry")
        every {
            apiCategory.query(any<GraphQLRequest<ModelPage<Post>>>(), any(), any())
        } answers {
            lastArg<Consumer<ApiException>>().accept(transportError)
            mockk()
        }
        val error = shouldThrow<AmplifyException> { lazyList(null).fetchPage() }
        error.cause shouldBe transportError
    }

    private fun request(): GraphQLRequest<ModelPage<Post>> = AppSyncGraphQLRequestFactory.buildModelPageQuery(
        Post::class.java,
        ApiLazyModelList.createPredicate(Post::class.java, mapOf("blogPostsId" to "b1")),
        null
    )

    private fun lazyList(apiName: String?) = ApiLazyModelList(
        Post::class.java,
        mapOf("blogPostsId" to "b1"),
        apiName,
        apiCategory
    )

    private fun respondWith(json: String) {
        every {
            apiCategory.query(any<GraphQLRequest<ModelPage<Post>>>(), any(), any())
        } answers {
            secondArg<Consumer<GraphQLResponse<ModelPage<Post>>>>().accept(
                factory.buildResponse(firstArg(), json, null)
            )
            mockk()
        }
        every {
            apiCategory.query(any<String>(), any<GraphQLRequest<ModelPage<Post>>>(), any(), any())
        } answers {
            thirdArg<Consumer<GraphQLResponse<ModelPage<Post>>>>().accept(
                factory.buildResponse(secondArg(), json, firstArg())
            )
            mockk()
        }
    }

    private fun assertCallbackRoutesError(useTokenOverload: Boolean) {
        respondWith(deniedJson)
        val list = lazyList("myApi")
        val completed = CountDownLatch(1)
        val successes = AtomicInteger()
        val errors = AtomicInteger()
        val observed = AtomicReference<Throwable?>()
        val uncaught = AtomicReference<Throwable?>()
        val handler = CoroutineExceptionHandler { _, error ->
            uncaught.set(error)
            completed.countDown()
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + handler)
        // Observe uncaught coroutine errors without changing the process-wide exception handler.
        // The production query, response factory, fetch method and callback dispatch all remain real.
        val field = ApiLazyModelList::class.java.getDeclaredField("callbackScope")
        field.isAccessible = true
        val originalScope = field.get(list) as CoroutineScope
        field.set(list, scope)
        originalScope.cancel()
        try {
            val onSuccess = Consumer<ModelPage<Post>> {
                successes.incrementAndGet()
                completed.countDown()
            }
            val onError = Consumer<AmplifyException> {
                observed.set(it)
                errors.incrementAndGet()
                completed.countDown()
            }
            if (useTokenOverload) {
                list.fetchPage(ApiPaginationToken("next") as PaginationToken, onSuccess, onError)
            } else {
                list.fetchPage(onSuccess, onError)
            }
            completed.await(5, TimeUnit.SECONDS) shouldBe true
            withClue("Uncaught coroutine error: ${uncaught.get()}") {
                uncaught.get() shouldBe null
                successes.get() shouldBe 0
                errors.get() shouldBe 1
                observed.get().shouldBeInstanceOf<AmplifyException>()
            }
        } finally {
            scope.cancel()
        }
    }
}
