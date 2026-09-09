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
package com.amazonaws.appsync

import com.amplifyframework.AmplifyException
import com.amplifyframework.core.model.ModelPage
import com.amplifyframework.testmodels.lazy.Blog
import com.amplifyframework.testmodels.lazy.Post
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests a related list that is fetched a page at a time.
 */
@RunWith(RobolectricTestRunner::class)
class AppSyncLazyModelListTest {

    // The foreign key on Post that points back at its Blog, which is what the parent's response carries.
    private val keyMap = mapOf("blogPostsId" to "b1")

    private val posts = listOf(
        Post.builder().name("first").blog(Blog.justId("b1")).build(),
        Post.builder().name("second").blog(Blog.justId("b1")).build()
    )

    private fun pageLoader(page: ModelPage<Post>?) = RecordingModelLoader { page }

    @Test
    fun `fetching builds a list query filtered by the parent's key`() = runTest {
        val loader = pageLoader(AppSyncModelPage(posts, null))
        val list = AppSyncLazyModelList(Post::class.java, keyMap, loader)

        val page = list.fetchPage()

        page.items shouldContainExactly posts
        page.hasNextPage shouldBe false
        val request = loader.requests.single()
        request.query shouldContain "listPosts"
        request.variables["filter"].toString() shouldContain "blogPostsId"
    }

    @Test
    fun `a page carries the token for the next one`() = runTest {
        val loader = pageLoader(AppSyncModelPage(posts, AppSyncPaginationToken("cursor-abc")))
        val list = AppSyncLazyModelList(Post::class.java, keyMap, loader)

        val page = list.fetchPage()

        page.hasNextPage shouldBe true
        page.nextToken.shouldBeInstanceOf<AppSyncPaginationToken>().nextToken shouldBe "cursor-abc"
    }

    @Test
    fun `a supplied token is sent as the cursor for the requested page`() = runTest {
        val loader = pageLoader(AppSyncModelPage(posts, null))
        val list = AppSyncLazyModelList(Post::class.java, keyMap, loader)

        list.fetchPage(AppSyncPaginationToken("cursor-abc"))

        loader.requests.single().variables["nextToken"] shouldBe "cursor-abc"
    }

    @Test
    fun `every fetch queries, because the caller chooses the page`() = runTest {
        val loader = pageLoader(AppSyncModelPage(posts, null))
        val list = AppSyncLazyModelList(Post::class.java, keyMap, loader)

        list.fetchPage()
        list.fetchPage()

        loader.calls shouldBe 2
    }

    @Test
    fun `a failure is reported as an AmplifyException carrying the client's own exception`() = runTest {
        val cause = AppSyncNetworkException(message = "offline", recoverySuggestion = "reconnect")
        val list = AppSyncLazyModelList(Post::class.java, keyMap, failingLoader(cause))

        val error = shouldThrow<AmplifyException> { list.fetchPage() }

        error.cause shouldBe cause
        error.recoverySuggestion shouldBe "reconnect"
        error.message shouldContain "Post"
    }

    @Test
    fun `a response with no page is a failure rather than an empty page`() = runTest {
        // An empty page and a missing one mean different things: reporting the second as the first would
        // silently end a caller's pagination loop.
        val list = AppSyncLazyModelList(Post::class.java, keyMap, pageLoader(null))

        val error = shouldThrow<AmplifyException> { list.fetchPage() }

        error.cause.shouldBeInstanceOf<AppSyncException>()
    }

    // ── Callback overloads ──────────────────────────────────────────────

    @Test
    fun `the callback overload delivers a page`() = runTest {
        val loader = pageLoader(AppSyncModelPage(posts, null))
        val list = AppSyncLazyModelList(Post::class.java, keyMap, loader, this)

        var delivered: ModelPage<Post>? = null
        list.fetchPage({ delivered = it }, { })
        advanceUntilIdle()

        delivered?.items shouldContainExactly posts
        loader.requests.single().variables["nextToken"].shouldBeNull()
    }

    @Test
    fun `the callback overload with a token requests that page`() = runTest {
        val loader = pageLoader(AppSyncModelPage(posts, null))
        val list = AppSyncLazyModelList(Post::class.java, keyMap, loader, this)

        var delivered: ModelPage<Post>? = null
        list.fetchPage(AppSyncPaginationToken("cursor-abc"), { delivered = it }, { })
        advanceUntilIdle()

        delivered?.items shouldContainExactly posts
        loader.requests.single().variables["nextToken"] shouldBe "cursor-abc"
    }

    @Test
    fun `the callback overload reports a failure on onError`() = runTest {
        val cause = AppSyncNetworkException(message = "offline", recoverySuggestion = "reconnect")
        val list = AppSyncLazyModelList(Post::class.java, keyMap, failingLoader(cause), this)

        var reported: AmplifyException? = null
        var delivered = 0
        list.fetchPage({ delivered++ }, { reported = it })
        advanceUntilIdle()

        delivered shouldBe 0
        reported?.cause shouldBe cause
    }
}
