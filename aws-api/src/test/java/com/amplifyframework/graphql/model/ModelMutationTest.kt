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

package com.amplifyframework.graphql.model

import com.amplifyframework.api.aws.AppSyncGraphQLRequestFactory
import com.amplifyframework.api.aws.AuthorizationType
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.MutationType
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.core.model.includes
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.testmodels.lazy.Blog
import com.amplifyframework.testmodels.lazy.Post
import com.amplifyframework.testmodels.lazy.PostPath
import io.kotest.matchers.shouldBe
import org.junit.Test

class ModelMutationTest {

    @Test
    fun create() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.CREATE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation(
                expectedClass,
                QueryPredicates.all(),
                expectedType
            )

        val actualRequest = ModelMutation.create(expectedClass)

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun `create with auth mode`() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.CREATE
        val expectedAuthMode = AuthorizationType.OPENID_CONNECT

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation(
                expectedClass,
                QueryPredicates.all(),
                expectedType,
                expectedAuthMode
            )

        val actualRequest = ModelMutation.create(expectedClass, expectedAuthMode)

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun create_with_includes() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.CREATE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation<Post, Post, PostPath>(
                expectedClass,
                QueryPredicates.all(),
                expectedType
            ) {
                includes(it.comments, it.blog)
            }

        val actualRequest = ModelMutation.create<Post, PostPath>(expectedClass) {
            includes(it.comments, it.blog)
        }

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun `create with includes and authMode`() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.CREATE
        val expectedAuthMode = AuthorizationType.OPENID_CONNECT

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation<Post, Post, PostPath>(
                expectedClass,
                QueryPredicates.all(),
                expectedType,
                expectedAuthMode
            ) {
                includes(it.comments, it.blog)
            }

        val actualRequest = ModelMutation.create<Post, PostPath>(expectedClass, expectedAuthMode) {
            includes(it.comments, it.blog)
        }

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun delete() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.DELETE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation(
                expectedClass,
                QueryPredicates.all(),
                expectedType
            )

        val actualRequest = ModelMutation.delete(expectedClass)

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun `delete with authMode`() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.DELETE
        val expectedAuthMode = AuthorizationType.OPENID_CONNECT

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation(
                expectedClass,
                QueryPredicates.all(),
                expectedType,
                expectedAuthMode
            )

        val actualRequest = ModelMutation.delete(expectedClass, authMode = expectedAuthMode)

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun delete_with_predicate() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.DELETE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation(
                expectedClass,
                QueryPredicates.all(),
                expectedType
            )

        val actualRequest = ModelMutation.delete(expectedClass, QueryPredicates.all())

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun `delete with predicate and authMode`() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.DELETE
        val expectedAuthMode = AuthorizationType.OPENID_CONNECT

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation(
                expectedClass,
                QueryPredicates.all(),
                expectedType,
                expectedAuthMode
            )

        val actualRequest = ModelMutation.delete(expectedClass, QueryPredicates.all(), expectedAuthMode)

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun delete_with_includes() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.DELETE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation<Post, Post, PostPath>(
                expectedClass,
                QueryPredicates.all(),
                expectedType
            ) {
                includes(it.comments, it.blog)
            }

        val actualRequest = ModelMutation.delete<Post, PostPath>(expectedClass) {
            includes(it.comments, it.blog)
        }

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun `delete with includes and authMode`() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.DELETE
        val expectedAuthMode = AuthorizationType.OPENID_CONNECT

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation<Post, Post, PostPath>(
                expectedClass,
                QueryPredicates.all(),
                expectedType,
                expectedAuthMode
            ) {
                includes(it.comments, it.blog)
            }

        val actualRequest = ModelMutation.delete<Post, PostPath>(expectedClass, expectedAuthMode) {
            includes(it.comments, it.blog)
        }

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun delete_with_predicate_with_includes() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.DELETE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation<Post, Post, PostPath>(
                expectedClass,
                QueryPredicates.all(),
                expectedType
            ) {
                includes(it.comments, it.blog)
            }

        val actualRequest = ModelMutation.delete<Post, PostPath>(expectedClass, QueryPredicates.all()) {
            includes(it.comments, it.blog)
        }

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun `delete with predicates with includes and authMode`() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.DELETE
        val expectedAuthMode = AuthorizationType.OPENID_CONNECT

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation<Post, Post, PostPath>(
                expectedClass,
                QueryPredicates.all(),
                expectedType,
                expectedAuthMode
            ) {
                includes(it.comments, it.blog)
            }

        val actualRequest = ModelMutation.delete<Post, PostPath>(
            expectedClass,
            QueryPredicates.all(),
            expectedAuthMode
        ) {
            includes(it.comments, it.blog)
        }

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun update() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.UPDATE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation(
                expectedClass,
                QueryPredicates.all(),
                expectedType
            )

        val actualRequest = ModelMutation.update(expectedClass)

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun `update with authMode`() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.UPDATE
        val expectedAuthMode = AuthorizationType.OPENID_CONNECT

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation(
                expectedClass,
                QueryPredicates.all(),
                expectedType,
                expectedAuthMode
            )

        val actualRequest = ModelMutation.update(expectedClass, authMode = expectedAuthMode)

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun update_with_predicate() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.UPDATE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation(
                expectedClass,
                QueryPredicates.all(),
                expectedType
            )

        val actualRequest = ModelMutation.update(expectedClass, QueryPredicates.all())

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun `update with predicate with authMode`() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.UPDATE
        val expectedAuthMode = AuthorizationType.OPENID_CONNECT

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation(
                expectedClass,
                QueryPredicates.all(),
                expectedType,
                expectedAuthMode
            )

        val actualRequest = ModelMutation.update(expectedClass, QueryPredicates.all(), expectedAuthMode)

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun update_with_includes() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.UPDATE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation<Post, Post, PostPath>(
                expectedClass,
                QueryPredicates.all(),
                expectedType
            ) {
                includes(it.comments, it.blog)
            }

        val actualRequest = ModelMutation.update<Post, PostPath>(expectedClass) {
            includes(it.comments, it.blog)
        }

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun `update with includes with authMode`() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.UPDATE
        val expectedAuthMode = AuthorizationType.OPENID_CONNECT

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation<Post, Post, PostPath>(
                expectedClass,
                QueryPredicates.all(),
                expectedType,
                expectedAuthMode
            ) {
                includes(it.comments, it.blog)
            }

        val actualRequest = ModelMutation.update<Post, PostPath>(expectedClass, expectedAuthMode) {
            includes(it.comments, it.blog)
        }

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun update_with_includes_with_predicate() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.UPDATE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation<Post, Post, PostPath>(
                expectedClass,
                QueryPredicates.all(),
                expectedType
            ) {
                includes(it.comments, it.blog)
            }

        val actualRequest = ModelMutation.update<Post, PostPath>(expectedClass, QueryPredicates.all()) {
            includes(it.comments, it.blog)
        }

        actualRequest shouldBe expectedRequest
    }

    @Test
    fun `update with includes with predicate with authMode`() {
        val expectedClass = Post.builder().name("Post").blog(Blog.builder().name("Blog").build()).build()
        val expectedType = MutationType.UPDATE
        val expectedAuthMode = AuthorizationType.OPENID_CONNECT

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildMutation<Post, Post, PostPath>(
                expectedClass,
                QueryPredicates.all(),
                expectedType,
                expectedAuthMode
            ) {
                includes(it.comments, it.blog)
            }

        val actualRequest = ModelMutation.update<Post, PostPath>(
            expectedClass,
            QueryPredicates.all(),
            expectedAuthMode
        ) {
            includes(it.comments, it.blog)
        }

        actualRequest shouldBe expectedRequest
    }
}
