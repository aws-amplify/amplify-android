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

import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.core.model.temporal.Temporal
import com.amplifyframework.testmodels.cpk.Post
import com.amplifyframework.testmodels.lazy.Blog
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests [AppSyncGraphQLRequestFactory.buildQueryFromKeyMap], the request-building path used by
 * lazy loading. Variable names and their GraphQL types are resolved from the model schema, so these
 * tests pin that resolution for both a single-field and a composite primary key.
 */
@RunWith(RobolectricTestRunner::class)
class AppSyncGraphQLRequestFactoryKeyMapTest {

    @Test
    fun `resolves a single primary key field from the model schema`() {
        val request: GraphQLRequest<Blog> = AppSyncGraphQLRequestFactory.buildQueryFromKeyMap(
            Blog::class.java,
            mapOf("id" to "blog-1")
        )

        request.variables shouldBe mapOf("id" to "blog-1")
        request.query shouldContain "\$id: ID!"
        request.responseType shouldBe Blog::class.java
    }

    @Test
    fun `resolves every field of a composite primary key with its schema type`() {
        val createdAt = Temporal.DateTime("2026-01-01T00:00:00.000Z")
        val keyMap = mapOf<String, Any>(
            "postId" to "post-1",
            "title" to "a title",
            "createdAt" to createdAt,
            "rating" to 4.5
        )

        val request: GraphQLRequest<Post> =
            AppSyncGraphQLRequestFactory.buildQueryFromKeyMap(Post::class.java, keyMap)

        request.variables shouldBe keyMap
        // Types come from @ModelField targetType, and every key field on this model is required.
        request.query shouldContain "\$postId: ID!"
        request.query shouldContain "\$title: String!"
        request.query shouldContain "\$createdAt: AWSDateTime!"
        request.query shouldContain "\$rating: Float!"
        request.responseType shouldBe Post::class.java
    }

    @Test
    fun `builds the same request as the ModelIdentifier overload for a single key`() {
        val fromKeyMap: GraphQLRequest<Blog> = AppSyncGraphQLRequestFactory.buildQueryFromKeyMap(
            Blog::class.java,
            mapOf("id" to "blog-1")
        )
        val fromObjectId: GraphQLRequest<Blog> =
            AppSyncGraphQLRequestFactory.buildQuery(Blog::class.java, "blog-1")

        fromKeyMap.query shouldBe fromObjectId.query
        fromKeyMap.variables shouldBe fromObjectId.variables
    }
}
