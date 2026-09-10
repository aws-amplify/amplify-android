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

import com.amplifyframework.core.model.LoadedModelReference
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.temporal.Temporal
import com.amplifyframework.testmodels.lazy.AmplifyModelProvider
import com.amplifyframework.testmodels.lazy.Blog
import com.amplifyframework.testmodels.lazy.Comment
import com.amplifyframework.testmodels.lazy.Post
import com.google.gson.reflect.TypeToken
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests the relationship fields a response leaves out, through [AppSyncGson] rather than in isolation, so
 * that the registration is covered too — a pass that is written correctly but never registered would pass
 * any test that invoked it directly.
 */
@RunWith(RobolectricTestRunner::class)
class AppSyncRelationshipTypeAdapterFactoryTest {

    private val loader = RecordingModelLoader { AppSyncModelPage(emptyList<Model>(), null) }

    private val gson = gsonFor(AmplifyModelProvider.getInstance())

    private fun gsonFor(provider: ModelProvider) = AppSyncGson(
        loader = loader,
        schemaRegistry = AppSyncSchemaRegistry { provider }
    ).gson

    /** The filter the lazy list built for itself, as the request carries it. */
    private fun sentFilter() = gson.toJson(loader.requests.single().variables["filter"])

    @Test
    fun `a list the response left out becomes one that fetches it, keyed by the parent`() = runTest {
        val blog: Blog = gson.fromJson("""{"id":"b1","name":"My Blog"}""", Blog::class.java)

        val posts = blog.posts.shouldBeInstanceOf<AppSyncLazyModelList<Post>>()
        // Nothing is fetched until the caller asks: the point of the field is that it defers.
        loader.calls shouldBe 0

        posts.fetchPage()

        loader.requests.single().query shouldContain "listPosts"
        sentFilter() shouldContain """"blogPostsId":{"eq":"b1"}"""
    }

    @Test
    fun `a related model the response left out is already loaded, holding null`() = runTest {
        val post: Post = gson.fromJson("""{"id":"p1","name":"My Post"}""", Post::class.java)

        // Loaded rather than lazy: there is no key to fetch by, so the absence is the answer.
        post.blog.shouldBeInstanceOf<LoadedModelReference<Blog>>().value.shouldBeNull()
        loader.calls shouldBe 0
    }

    @Test
    fun `a relationship the response carried is left as it arrived`() {
        val post: Post = gson.fromJson(
            """
            {"id":"p1","name":"My Post","blog":{"id":"b1"},"comments":{"items":[{"id":"c1","text":"hi"}]}}
            """.trimIndent(),
            Post::class.java
        )

        post.comments.shouldBeInstanceOf<AppSyncLoadedModelList<Comment>>().items.single().text shouldBe "hi"
        post.blog.shouldBeInstanceOf<AppSyncLazyModelReference<Blog>>()
            .getIdentifier() shouldContainExactly mapOf("id" to "b1")
    }

    @Test
    fun `a composite key parent keys its list by every identifying value, in order`() = runTest {
        val gson = gsonFor(FixtureModelProvider(setOf(Order::class.java, OrderItem::class.java)))

        val order: Order = gson.fromJson("""{"customerId":"c1","orderNumber":"o9"}""", Order::class.java)
        order.items.shouldBeInstanceOf<AppSyncLazyModelList<OrderItem>>().fetchPage()

        val filter = gson.toJson(loader.requests.single().variables["filter"])
        filter shouldContain """"orderCustomerId":{"eq":"c1"}"""
        filter shouldContain """"orderOrderNumber":{"eq":"o9"}"""
    }

    @Test
    fun `a parent whose key the response left out keeps a null list, rather than querying for every child`() {
        val blog: Blog = gson.fromJson("""{"name":"My Blog"}""", Blog::class.java)

        blog.posts.shouldBeNull()
        loader.calls shouldBe 0
    }

    @Test
    fun `a child the models do not describe is reported as a configuration error`() {
        val gson = gsonFor(FixtureModelProvider(setOf(Blog::class.java)))

        val error = shouldThrow<AppSyncInvalidConfigException> {
            gson.fromJson("""{"id":"b1","name":"My Blog"}""", Blog::class.java)
        }

        error.message shouldContain "Post"
    }

    @Test
    fun `a relationship only the parent declares is reported as a configuration error`() {
        val gson = gsonFor(FixtureModelProvider(setOf(Tag::class.java, Entry::class.java)))

        val error = shouldThrow<AppSyncInvalidConfigException> {
            gson.fromJson("""{"id":"t1"}""", Tag::class.java)
        }

        error.message shouldContain "Entry"
        error.message shouldContain "Tag"
    }

    // ── Types with no relationships to fill ─────────────────────────────

    @Test
    fun `a type that is not a model is read as it would be without the pass`() {
        val value = gson.fromJson<Map<String, Any>>(
            """{"name":"x","count":2}""",
            object : TypeToken<Map<String, Any>>() {}.type
        )

        value shouldContainExactly mapOf("name" to "x", "count" to 2.0)
    }

    @Test
    fun `a type with an adapter of its own still reaches that adapter`() {
        gson.fromJson(""""2026-01-02"""", Temporal.Date::class.java) shouldBe Temporal.Date("2026-01-02")
    }
}
