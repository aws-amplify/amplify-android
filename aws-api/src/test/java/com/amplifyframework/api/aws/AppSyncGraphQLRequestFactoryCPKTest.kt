/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.api.graphql.MutationType
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.core.model.temporal.Temporal
import com.amplifyframework.testmodels.cpk.Blog
import com.amplifyframework.testmodels.cpk.Blog.BlogIdentifier
import com.amplifyframework.testmodels.cpk.Comment
import com.amplifyframework.testmodels.cpk.Item
import com.amplifyframework.testmodels.cpk.Post
import com.amplifyframework.testmodels.cpk.Post.PostIdentifier
import com.amplifyframework.testutils.Resources
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

/**
 * Tests the [AppSyncGraphQLRequestFactory].
 */
@RunWith(RobolectricTestRunner::class)
class AppSyncGraphQLRequestFactoryCPKTest {

    @Test
    fun create_with_cpk() {
        val item = Item.builder()
            .customKey("ck1")
            .name("name1")
            .build()

        val request: GraphQLRequest<Comment> =
            AppSyncGraphQLRequestFactory.buildMutation(item, QueryPredicates.all(), MutationType.CREATE)

        JSONAssert.assertEquals(Resources.readAsString("cpk_create.json"), request.content, true)
    }

    @Test
    fun create_with_cpk_with_sk() {
        val blog = Blog.builder()
            .blogId("b1")
            .siteId("s1")
            .name("name1")
            .blogAuthorId("a1")
            .build()

        val request: GraphQLRequest<Blog> =
            AppSyncGraphQLRequestFactory.buildMutation(blog, QueryPredicates.all(), MutationType.CREATE)

        JSONAssert.assertEquals(Resources.readAsString("cpk_create_with_sk.json"), request.content, true)
    }

    // Also tests creating item with belongsTo Parent CPK
    @Test
    fun create_with_cpk_with_multiple_sk() {
        val blog = Blog.builder()
            .blogId("b1")
            .siteId("s1")
            .name("name1")
            .blogAuthorId("a1")
            .build()

        val post = Post.builder()
            .postId("p1")
            .title("t1")
            .createdAt(Temporal.DateTime("2023-06-09T16:22:30.48Z"))
            .rating(3.4)
            .blog(blog)
            .build()

        val request: GraphQLRequest<Post> =
            AppSyncGraphQLRequestFactory.buildMutation(post, QueryPredicates.all(), MutationType.CREATE)

        JSONAssert.assertEquals(
            Resources.readAsString("cpk_create_with_multiple_sk.json"),
            request.content,
            true
        )
    }

    @Test
    fun create_with_cpk_with_multiple_sk_null_parent() {

        val post = Post.builder()
            .postId("p1")
            .title("t1")
            .createdAt(Temporal.DateTime("2023-06-09T16:22:30.48Z"))
            .rating(3.4)
            .build()

        val request: GraphQLRequest<Post> =
            AppSyncGraphQLRequestFactory.buildMutation(post, QueryPredicates.all(), MutationType.CREATE)

        JSONAssert.assertEquals(
            Resources.readAsString("cpk_create_with_multiple_sk_null_parent.json"),
            request.content,
            true
        )
    }

    @Test
    fun create_with_cpk_with_multiple_sk_parent() {

        val post = Post.builder()
            .postId("p1")
            .title("t1")
            .createdAt(Temporal.DateTime("2023-06-09T16:22:30.48Z"))
            .rating(3.4)
            .build()

        val comment = Comment.builder()
            .commentId("c1")
            .post(post)
            .content("content1")
            .build()

        val request: GraphQLRequest<Comment> =
            AppSyncGraphQLRequestFactory.buildMutation(comment, QueryPredicates.all(), MutationType.CREATE)

        JSONAssert.assertEquals(
            Resources.readAsString("cpk_create_with_parent_with_multiple_sk.json"),
            request.content,
            true
        )
    }

    @Test
    fun query_by_cpk() {
        val request: GraphQLRequest<Comment> =
            AppSyncGraphQLRequestFactory.buildQuery(Comment::class.java, "c1")

        JSONAssert.assertEquals(Resources.readAsString("cpk_query.json"), request.content, true)
    }

    @Test
    fun query_by_cpk_with_sk() {
        val request: GraphQLRequest<Blog> =
            AppSyncGraphQLRequestFactory.buildQuery(Blog::class.java, BlogIdentifier("b1", "s1"))

        JSONAssert.assertEquals(Resources.readAsString("cpk_query_with_sk.json"), request.content, true)
    }

    @Test
    fun query_by_cpk_with_multiple_sk() {
        val identifier = PostIdentifier(
            "p1",
            "t1",
            Temporal.DateTime("2023-06-09T16:22:30.48Z"),
            4.5
        )
        val request: GraphQLRequest<Post> =
            AppSyncGraphQLRequestFactory.buildQuery(Post::class.java, identifier)

        JSONAssert.assertEquals(
            Resources.readAsString("cpk_query_with_multiple_sk.json"),
            request.content,
            true
        )
    }

    @Test
    fun query_model_list_with_cpk() {
        val request = ModelQuery.list(Blog::class.java)

        JSONAssert.assertEquals(Resources.readAsString("cpk_list_query.json"), request.content, true)
    }

    @Test
    fun delete_model_with_cpk() {
        val post = Post.builder()
            .postId("p1")
            .title("t1")
            .createdAt(Temporal.DateTime("2023-06-09T16:22:30.48Z"))
            .rating(3.4)
            .build()

        val request = ModelMutation.delete(post)

        JSONAssert.assertEquals(Resources.readAsString("cpk_delete.json"), request.content, true)
    }

    @Test
    fun update_model_with_cpk() {
        val post = Post.builder()
            .postId("p1")
            .title("t1")
            .createdAt(Temporal.DateTime("2023-06-09T16:22:30.48Z"))
            .rating(3.4)
            .build()

        val request = ModelMutation.update(post)

        JSONAssert.assertEquals(Resources.readAsString("cpk_update.json"), request.content, true)
    }
}
