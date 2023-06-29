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

import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
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
import com.amplifyframework.util.GsonFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

/**
 * Tests the [AppSyncGraphQLRequestFactory].
 */
@RunWith(RobolectricTestRunner::class)
class AppSyncGraphQLRequestAndResponseCPKTest {

    lateinit var responseFactory: GraphQLResponse.Factory

    @Before
    fun setup() {
        val gson = GsonFactory.instance()
        responseFactory = GsonGraphQLResponseFactory(gson)
    }

    @Test
    fun create_with_cpk() {
        // GIVEN
        val item = Item.builder()
            .customKey("ck1")
            .name("name1")
            .build()
        val requestJson = Resources.readAsString("cpk_create.json")
        val responseJson = Resources.readAsString("cpk_create_response.json")

        // WHEN
        val request: GraphQLRequest<Item> =
            AppSyncGraphQLRequestFactory.buildMutation(item, QueryPredicates.all(), MutationType.CREATE)
        val response = responseFactory.buildResponse(request, responseJson)

        // THEN
        JSONAssert.assertEquals(requestJson, request.content, true)
        assertFalse(response.hasErrors())
        assertEquals(item.customKey, response.data.customKey)
        assertEquals(item.name, response.data.name)
    }

    @Test
    fun create_with_cpk_with_sk() {
        // GIVEN
        val blog = Blog.builder()
            .blogId("b1")
            .siteId("s1")
            .name("name1")
            .blogAuthorId("a1")
            .build()
        val requestJson = Resources.readAsString("cpk_create_with_sk.json")
        val responseJson = Resources.readAsString("cpk_create_with_sk_response.json")

        // WHEN
        val request: GraphQLRequest<Blog> =
            AppSyncGraphQLRequestFactory.buildMutation(blog, QueryPredicates.all(), MutationType.CREATE)
        val response = responseFactory.buildResponse(request, responseJson)

        // THEN
        JSONAssert.assertEquals(requestJson, request.content, true)
        assertFalse(response.hasErrors())
        assertEquals(blog.blogId, response.data.blogId)
        assertEquals(blog.siteId, response.data.siteId)
        assertEquals(blog.name, response.data.name)
        assertEquals(blog.blogAuthorId, response.data.blogAuthorId)
        assertEquals(blog.blogAuthorId, response.data.author.id)
        assertTrue(response.data.posts.isEmpty())
    }

    // Also tests creating item with belongsTo Parent CPK
    @Test
    fun create_with_cpk_with_multiple_sk() {
        // GIVEN
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
        val requestJson = Resources.readAsString("cpk_create_with_multiple_sk.json")
        val responseJson = Resources.readAsString("cpk_create_with_multiple_sk_response.json")

        // WHEN
        val request: GraphQLRequest<Post> =
            AppSyncGraphQLRequestFactory.buildMutation(post, QueryPredicates.all(), MutationType.CREATE)
        val response = responseFactory.buildResponse(request, responseJson)

        // THEN
        JSONAssert.assertEquals(requestJson, request.content, true)
        assertFalse(response.hasErrors())
        assertEquals(post.postId, response.data.postId)
        assertEquals(post.title, response.data.title)
        assertEquals(post.createdAt, response.data.createdAt)
        assertEquals(post.rating, response.data.rating, 0.0)
        assertEquals(post.blog.blogId, response.data.blog.blogId)
    }

    @Test
    fun create_with_cpk_with_multiple_sk_null_parent() {
        // GIVEN
        val post = Post.builder()
            .postId("detachedPostId")
            .title("Detached Post")
            .createdAt(Temporal.DateTime("2023-06-10T16:22:30.48Z"))
            .rating(4.1)
            .build()
        val requestJson = Resources.readAsString("cpk_create_with_multiple_sk_null_parent.json")
        val responseJson = Resources.readAsString("cpk_create_with_multiple_sk_null_parent_response.json")

        // WHEN
        val request: GraphQLRequest<Post> =
            AppSyncGraphQLRequestFactory.buildMutation(post, QueryPredicates.all(), MutationType.CREATE)
        val response = responseFactory.buildResponse(request, responseJson)

        // THEN
        JSONAssert.assertEquals(requestJson, request.content, true)
        assertFalse(response.hasErrors())
        assertEquals(post.postId, response.data.postId)
        assertEquals(post.title, response.data.title)
        assertEquals(post.createdAt, response.data.createdAt)
        assertEquals(post.rating, response.data.rating, 0.0)
        assertNull(response.data.blog)
    }

    @Test
    fun create_with_cpk_with_multiple_sk_parent() {
        // GIVEN
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
        val requestJson = Resources.readAsString("cpk_create_with_parent_with_multiple_sk.json")
        val responseJson = Resources.readAsString("cpk_create_with_parent_with_multiple_sk_response.json")

        // WHEN
        val request: GraphQLRequest<Comment> =
            AppSyncGraphQLRequestFactory.buildMutation(comment, QueryPredicates.all(), MutationType.CREATE)
        val response = responseFactory.buildResponse(request, responseJson)

        // THEN
        JSONAssert.assertEquals(requestJson, request.content, true)
        assertFalse(response.hasErrors())
        assertEquals(comment.commentId, response.data.commentId)
        assertEquals(comment.content, response.data.content)
        assertEquals(post.postId, response.data.post.postId)
    }

    @Test
    fun query_by_single_cpk() {
        // GIVEN
        val requestJson = Resources.readAsString("cpk_query.json")
        val responseJson = Resources.readAsString("cpk_query_response.json")

        // WHEN
        val request: GraphQLRequest<Comment> =
            AppSyncGraphQLRequestFactory.buildQuery(Comment::class.java, "c1")
        val response = responseFactory.buildResponse(request, responseJson)

        // THEN
        JSONAssert.assertEquals(requestJson, request.content, true)
        assertFalse(response.hasErrors())
        assertEquals("c1", response.data.commentId)
        assertEquals("content1", response.data.content)
        assertEquals("p1", response.data.post.postId)
        assertEquals("b1", response.data.post.blog.blogId)
        assertEquals("a1", response.data.post.blog.blogAuthorId)
    }

    @Test
    fun query_by_cpk_with_custom_identifier() {
        // GIVEN
        val requestJson = Resources.readAsString("cpk_query.json")
        val responseJson = Resources.readAsString("cpk_query_response.json")

        // WHEN
        val request: GraphQLRequest<Comment> =
            AppSyncGraphQLRequestFactory.buildQuery(Comment::class.java, Comment.CommentIdentifier("c1"))
        val response = responseFactory.buildResponse(request, responseJson)

        // THEN
        JSONAssert.assertEquals(requestJson, request.content, true)
        assertFalse(response.hasErrors())
        assertEquals("c1", response.data.commentId)
        assertEquals("content1", response.data.content)
        assertEquals("p1", response.data.post.postId)
        assertEquals("b1", response.data.post.blog.blogId)
        assertEquals("a1", response.data.post.blog.blogAuthorId)
    }

    @Test
    fun query_by_cpk_with_sk() {
        // GIVEN
        val requestJson = Resources.readAsString("cpk_query_with_sk.json")
        val responseJson = Resources.readAsString("cpk_query_with_sk_response.json")

        // WHEN
        val request: GraphQLRequest<Blog> =
            AppSyncGraphQLRequestFactory.buildQuery(Blog::class.java, BlogIdentifier("b1", "s1"))
        val response = responseFactory.buildResponse(request, responseJson)

        // THEN
        JSONAssert.assertEquals(requestJson, request.content, true)
        assertFalse(response.hasErrors())
        assertEquals("b1", response.data.blogId)
        assertEquals("s1", response.data.siteId)
        assertEquals("name1", response.data.name)
        assertEquals("a1", response.data.blogAuthorId)
        assertEquals("a1", response.data.author.id)
        assertEquals(1, response.data.posts.size)
        assertEquals("p1", response.data.posts[0].postId)
    }

    @Test
    fun query_by_cpk_with_multiple_sk() {
        // GIVEN
        val identifier = PostIdentifier(
            "p1",
            "t1",
            Temporal.DateTime("2023-06-09T16:22:30.48Z"),
            4.5
        )
        val requestJson = Resources.readAsString("cpk_query_with_multiple_sk.json")
        val responseJson = Resources.readAsString("cpk_query_with_multiple_sk_response.json")

        // WHEN
        val request: GraphQLRequest<Post> =
            AppSyncGraphQLRequestFactory.buildQuery(Post::class.java, identifier)
        val response = responseFactory.buildResponse(request, responseJson)

        // THEN
        JSONAssert.assertEquals(requestJson, request.content, true)
        assertFalse(response.hasErrors())
        assertEquals("p1", response.data.postId)
        assertEquals("t1", response.data.title)
        assertEquals(4.5, response.data.rating, 0.0)
        assertEquals(Temporal.DateTime("2023-06-09T16:22:30.48Z"), response.data.createdAt)
        assertEquals("b1", response.data.blog.blogId)
        assertEquals("a1", response.data.blog.author.id)
        assertEquals("p1", response.data.blog.posts[0].postId)
    }

    @Test
    fun query_model_list_with_cpk() {
        // GIVEN
        val requestJson = Resources.readAsString("cpk_list_query.json")
        val responseJson = Resources.readAsString("cpk_list_query_response.json")

        // WHEN
        val request = ModelQuery.list(Blog::class.java)
        val response = responseFactory.buildResponse(request, responseJson)

        // THEN
        JSONAssert.assertEquals(requestJson, request.content, true)
        assertFalse(response.hasErrors())
        assertEquals(1, response.data.items.count())
        val firstItem = response.data.items.first()
        assertEquals("b1", firstItem.blogId)
    }

    @Test
    fun delete_model_with_cpk() {
        // GIVEN
        val post = Post.builder()
            .postId("p1")
            .title("t1")
            .createdAt(Temporal.DateTime("2023-06-09T16:22:30.48Z"))
            .rating(3.4)
            .build()
        val requestJson = Resources.readAsString("cpk_delete.json")
        val responseJson = Resources.readAsString("cpk_delete_response.json")

        // WHEN
        val request = ModelMutation.delete(post)
        val response = responseFactory.buildResponse(request, responseJson)

        // THEN
        JSONAssert.assertEquals(requestJson, request.content, true)
        assertFalse(response.hasErrors())
        assertEquals(post.postId, response.data.postId)
    }

    @Test
    fun update_model_with_cpk() {
        // GIVEN
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
        val comment = Comment.builder()
            .commentId("c1")
            .post(post)
            .content("Updated Comment")
            .build()
        val requestJson = Resources.readAsString("cpk_update.json")
        val responseJson = Resources.readAsString("cpk_update_response.json")

        // WHEN
        val request = ModelMutation.update(comment)
        val response = responseFactory.buildResponse(request, responseJson)

        // THEN
        JSONAssert.assertEquals(requestJson, request.content, true)
        assertFalse(response.hasErrors())
        assertEquals(comment.commentId, response.data.commentId)
        assertEquals(comment.content, response.data.content)
        assertEquals(post.postId, response.data.post.postId)
        assertEquals(post.title, response.data.post.title)
        assertEquals(post.createdAt, response.data.post.createdAt)
        assertEquals(post.rating, response.data.post.rating, 0.0)
        assertEquals(blog.blogId, response.data.post.blog.blogId)
    }

    @Test
    fun update_model_with_cpk_remove_association() {
        // GIVEN
        val post = Post.builder()
            .postId("p1")
            .title("t1")
            .createdAt(Temporal.DateTime("2023-06-09T16:22:30.48Z"))
            .rating(3.4)
            .build()
        val requestJson = Resources.readAsString("cpk_update_remove_association.json")
        val responseJson = Resources.readAsString("cpk_update_remove_association_response.json")

        // WHEN
        val request = ModelMutation.update(post)
        val response = responseFactory.buildResponse(request, responseJson)

        // THEN
        JSONAssert.assertEquals(requestJson, request.content, true)
        assertFalse(response.hasErrors())
        assertEquals(post.postId, response.data.postId)
        assertEquals(post.title, response.data.title)
        assertEquals(post.createdAt, response.data.createdAt)
        assertEquals(post.rating, response.data.rating, 0.0)
        assertNull(response.data.blog)
    }
}
