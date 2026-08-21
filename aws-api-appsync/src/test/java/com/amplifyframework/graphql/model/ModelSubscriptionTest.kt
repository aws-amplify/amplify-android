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
import com.amplifyframework.api.graphql.SubscriptionType
import com.amplifyframework.api.graphql.model.ModelSubscription
import com.amplifyframework.core.model.includes
import com.amplifyframework.testmodels.lazy.Post
import com.amplifyframework.testmodels.lazy.PostPath
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelSubscriptionTest {

    @Test
    fun of() {
        val expectedClass = Post::class.java
        val expectedType = SubscriptionType.ON_CREATE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory.buildSubscription(
            expectedClass,
            expectedType
        )

        val actualRequest = ModelSubscription.of(expectedClass, expectedType)

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun of_with_includes() {
        val expectedClass = Post::class.java
        val expectedType = SubscriptionType.ON_CREATE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildSubscription<Post, Post, PostPath>(
                expectedClass,
                expectedType
            ) {
                includes(it.comments, it.blog)
            }

        val actualRequest = ModelSubscription.of<Post, PostPath>(expectedClass, expectedType) {
            includes(it.comments, it.blog)
        }

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun create() {
        val expectedClass = Post::class.java
        val expectedType = SubscriptionType.ON_CREATE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildSubscription(
                expectedClass,
                expectedType
            )

        val actualRequest = ModelSubscription.onCreate(expectedClass)

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun create_with_includes() {
        val expectedClass = Post::class.java
        val expectedType = SubscriptionType.ON_CREATE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildSubscription<Post, Post, PostPath>(
                expectedClass,
                expectedType
            ) {
                includes(it.comments, it.blog)
            }

        val actualRequest = ModelSubscription.onCreate<Post, PostPath>(expectedClass) {
            includes(it.comments, it.blog)
        }

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun delete() {
        val expectedClass = Post::class.java
        val expectedType = SubscriptionType.ON_DELETE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildSubscription(
                expectedClass,
                expectedType
            )

        val actualRequest = ModelSubscription.onDelete(expectedClass)

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun delete_with_includes() {
        val expectedClass = Post::class.java
        val expectedType = SubscriptionType.ON_DELETE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildSubscription<Post, Post, PostPath>(
                expectedClass,
                expectedType
            ) {
                includes(it.comments, it.blog)
            }

        val actualRequest = ModelSubscription.onDelete<Post, PostPath>(expectedClass) {
            includes(it.comments, it.blog)
        }

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun update() {
        val expectedClass = Post::class.java
        val expectedType = SubscriptionType.ON_UPDATE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildSubscription(
                expectedClass,
                expectedType
            )

        val actualRequest = ModelSubscription.onUpdate(expectedClass)

        assertEquals(expectedRequest, actualRequest)
    }

    @Test
    fun update_with_includes() {
        val expectedClass = Post::class.java
        val expectedType = SubscriptionType.ON_UPDATE

        val expectedRequest: GraphQLRequest<Post> = AppSyncGraphQLRequestFactory
            .buildSubscription<Post, Post, PostPath>(
                expectedClass,
                expectedType
            ) {
                includes(it.comments, it.blog)
            }

        val actualRequest = ModelSubscription.onUpdate<Post, PostPath>(expectedClass) {
            includes(it.comments, it.blog)
        }

        assertEquals(expectedRequest, actualRequest)
    }
}
