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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

/**
 * Tests [RawAppSyncGraphQLRequest], which carries a raw GraphQL document plus the
 * [AuthorizationType] that should be used to authorize it.
 */
class RawAppSyncGraphQLRequestTest {

    private val serializer = GraphQLRequest.VariablesSerializer { it.toString() }
    private val document = "query GetByUser { getByUser }"

    @Test
    fun `exposes the document and authorization type it was built with`() {
        val request = RawAppSyncGraphQLRequest<String>(
            document,
            String::class.java,
            serializer,
            AuthorizationType.AMAZON_COGNITO_USER_POOLS
        )

        request.query shouldBe document
        request.variables shouldBe emptyMap()
        request.authorizationType shouldBe AuthorizationType.AMAZON_COGNITO_USER_POOLS
        request.responseType shouldBe String::class.java
    }

    @Test
    fun `exposes the variables it was built with`() {
        val variables = mapOf("id" to "id-123", "limit" to 10)
        val request = RawAppSyncGraphQLRequest<String>(
            document,
            variables,
            String::class.java,
            serializer,
            AuthorizationType.API_KEY
        )

        request.variables shouldBe variables
        request.authorizationType shouldBe AuthorizationType.API_KEY
    }

    @Test
    fun `permits a null authorization type to fall back to the API default`() {
        val request = RawAppSyncGraphQLRequest<String>(
            document,
            String::class.java,
            serializer,
            null
        )

        request.authorizationType shouldBe null
    }

    @Test
    fun `is an AuthorizedGraphQLRequest so the plugin can read its authorization type`() {
        val request: GraphQLRequest<String> = RawAppSyncGraphQLRequest<String>(
            document,
            String::class.java,
            serializer,
            AuthorizationType.AWS_IAM
        )

        request.shouldBeInstanceOf<AuthorizedGraphQLRequest>()
        (request as AuthorizedGraphQLRequest).authorizationType shouldBe AuthorizationType.AWS_IAM
    }

    @Test
    fun `requests with equal contents are equal`() {
        val one = RawAppSyncGraphQLRequest<String>(
            document,
            mapOf("id" to "id-123"),
            String::class.java,
            serializer,
            AuthorizationType.API_KEY
        )
        val two = RawAppSyncGraphQLRequest<String>(
            document,
            mapOf("id" to "id-123"),
            String::class.java,
            serializer,
            AuthorizationType.API_KEY
        )

        one shouldBe two
        one.hashCode() shouldBe two.hashCode()
    }

    @Test
    fun `authorization type participates in equality`() {
        val apiKey = RawAppSyncGraphQLRequest<String>(
            document,
            String::class.java,
            serializer,
            AuthorizationType.API_KEY
        )
        val userPools = RawAppSyncGraphQLRequest<String>(
            document,
            String::class.java,
            serializer,
            AuthorizationType.AMAZON_COGNITO_USER_POOLS
        )

        apiKey shouldNotBe userPools
    }
}
