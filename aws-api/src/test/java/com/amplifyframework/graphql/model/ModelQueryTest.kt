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
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.api.graphql.model.ModelPagination
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.model.includes
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.testmodels.lazy.Post
import com.amplifyframework.testmodels.lazy.PostPath
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelQueryTest {

    @Test
    fun get_string_id() {
        val expectedClass = Post::class.java
        val expectedId = "p1"

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory.buildQuery(
            expectedClass,
            expectedId
        )

        val actualRequest = ModelQuery[expectedClass, expectedId]

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun get_string_id_passes_includes() {
        val expectedClass = Post::class.java
        val expectedId = "p1"

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory.buildQuery<Post, Post, PostPath>(
            expectedClass,
            expectedId
        ) {
            includes(it.comments, it.blog)
        }

        val actualRequest = ModelQuery.get<Post, PostPath>(expectedClass, expectedId) {
            includes(it.comments, it.blog)
        }

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun get_model_identifier() {
        val expectedClass = Post::class.java
        val expectedId = Post.PostIdentifier("p1")

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory.buildQuery(
            expectedClass,
            expectedId
        )

        val actualRequest = ModelQuery[expectedClass, expectedId]

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun get_model_identifier_passes_includes() {
        val expectedClass = Post::class.java
        val expectedId = Post.PostIdentifier("p1")

        val expectedRequest = AppSyncGraphQLRequestFactory.buildQuery<Post, Post, PostPath>(
            expectedClass,
            expectedId
        ) {
            includes(it.comments, it.blog)
        }

        val actualRequest = ModelQuery.get<Post, PostPath>(expectedClass, expectedId) {
            includes(it.comments, it.blog)
        }

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun list_with_default_query_predicate() {
        val expectedClass = Post::class.java

        val expectedRequest = AppSyncGraphQLRequestFactory.buildQuery<PaginatedResult<Post>, Post>(
            expectedClass,
            QueryPredicates.all()
        )

        val actualRequest = ModelQuery.list<Post, PostPath>(expectedClass) {
            includes(it.comments, it.blog)
        }

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun list_with_query_predicate() {
        val expectedClass = Post::class.java

        val expectedRequest = AppSyncGraphQLRequestFactory.buildQuery<PaginatedResult<Post>, Post>(
            expectedClass,
            QueryPredicates.all()
        )

        val actualRequest = ModelQuery.list<Post, PostPath>(expectedClass, QueryPredicates.all()) {
            includes(it.comments, it.blog)
        }

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun list_with_default_query_predicate_passes_includes() {
        val expectedClass = Post::class.java

        val expectedRequest = AppSyncGraphQLRequestFactory.buildQuery<PaginatedResult<Post>, Post, PostPath>(
            expectedClass,
            QueryPredicates.all()
        ) {
            includes(it.comments, it.blog)
        }

        val actualRequest = ModelQuery.list<Post, PostPath>(expectedClass) {
            includes(it.comments, it.blog)
        }

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun list_with_query_predicate_passes_includes() {
        val expectedClass = Post::class.java

        val expectedRequest = AppSyncGraphQLRequestFactory.buildQuery<PaginatedResult<Post>, Post, PostPath>(
            expectedClass,
            QueryPredicates.all()
        ) {
            includes(it.comments, it.blog)
        }

        val actualRequest = ModelQuery.list<Post, PostPath>(expectedClass, QueryPredicates.all()) {
            includes(it.comments, it.blog)
        }

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun list_with_query_predicate_and_pagination() {
        val expectedClass = Post::class.java

        val expectedRequest = AppSyncGraphQLRequestFactory.buildPaginatedResultQuery<PaginatedResult<Post>, Post>(
            expectedClass,
            QueryPredicates.all(),
            10
        )

        val actualRequest = ModelQuery.list(
            expectedClass,
            QueryPredicates.all(),
            ModelPagination.limit(10)
        )

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun list_with_query_predicate_and_pagination_and_includes() {
        val expectedClass = Post::class.java

        val expectedRequest = AppSyncGraphQLRequestFactory
            .buildPaginatedResultQuery<PaginatedResult<Post>, Post, PostPath>(
                expectedClass,
                QueryPredicates.all(),
                10
            ) {
                includes(it.comments, it.blog)
            }

        val actualRequest = ModelQuery.list<Post, PostPath>(
            expectedClass,
            QueryPredicates.all(),
            ModelPagination.limit(10)
        ) {
            includes(it.comments, it.blog)
        }

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun list_with_model_pagination() {
        val expectedClass = Post::class.java
        val expectedPagination = ModelPagination.limit(10)

        val expectedRequest = AppSyncGraphQLRequestFactory
            .buildPaginatedResultQuery<PaginatedResult<Post>, Post>(
                expectedClass, QueryPredicates.all(), 10
            )

        val actualRequest = ModelQuery.list(expectedClass, expectedPagination)

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun list_with_model_pagination_passes_includes() {
        val expectedClass = Post::class.java
        val expectedPagination = ModelPagination.limit(10)

        val expectedRequest = AppSyncGraphQLRequestFactory
            .buildPaginatedResultQuery<PaginatedResult<Post>, Post, PostPath>(
                expectedClass, QueryPredicates.all(), 10
            ) {
                includes(it.comments, it.blog)
            }

        val actualRequest = ModelQuery.list<Post, PostPath>(expectedClass, expectedPagination) {
            includes(it.comments, it.blog)
        }

        assertEquals(expectedRequest, actualRequest)
    }
}
