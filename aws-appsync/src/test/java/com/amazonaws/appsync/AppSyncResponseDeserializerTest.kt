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

import com.amplifyframework.api.aws.GsonVariablesSerializer
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.api.graphql.SimpleGraphQLRequest
import com.amplifyframework.util.TypeMaker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.Test

/**
 * Tests [AppSyncResponseDeserializer].
 *
 * The list cases are the substance here. AppSync returns a list as an object wrapping an `items`
 * array, and the same JSON has to become a `PaginatedResult` at the root of a query but a plain
 * `List` below it — because that is what codegen emits for a one-to-many relationship.
 */
class AppSyncResponseDeserializerTest {

    // ── Scalars and errors ──────────────────────────────────────────────

    @Test
    fun `deserializes data`() {
        val response = AppSyncResponseDeserializer.deserialize(stringRequest(), """{"data":"hello"}""")

        response.data shouldBe "hello"
        response.hasErrors() shouldBe false
    }

    @Test
    fun `deserializes errors`() {
        val response = AppSyncResponseDeserializer.deserialize(
            stringRequest(),
            """{"errors":[{"message":"first"},{"message":"second"}]}"""
        )

        response.errors shouldHaveSize 2
        response.errors[0].message shouldBe "first"
        response.data.shouldBeNull()
    }

    @Test
    fun `deserializes data and errors together`() {
        val response = AppSyncResponseDeserializer.deserialize(
            stringRequest(),
            """{"data":"partial","errors":[{"message":"a field failed"}]}"""
        )

        response.data shouldBe "partial"
        response.errors shouldHaveSize 1
    }

    @Test
    fun `preserves error locations and extensions`() {
        val response = AppSyncResponseDeserializer.deserialize(
            stringRequest(),
            """
            {"errors":[{
                "message":"Unauthorized",
                "locations":[{"line":2,"column":5}],
                "path":["getTodo","secret"],
                "extensions":{"errorType":"Unauthorized"}
            }]}
            """.trimIndent()
        )

        val error = response.errors[0]
        error.locations.shouldNotBeNull() shouldHaveSize 1
        error.path.shouldNotBeNull() shouldHaveSize 2
        error.extensions.shouldNotBeNull()["errorType"] shouldBe "Unauthorized"
    }

    // ── Failures ────────────────────────────────────────────────────────

    @Test
    fun `an empty body fails rather than returning null`() {
        // Gson returns null instead of throwing for an empty string, so this is guarded explicitly.
        // See https://github.com/google/gson/issues/457
        shouldThrow<AppSyncDeserializationException> {
            AppSyncResponseDeserializer.deserialize(stringRequest(), "")
        }.message shouldContain "empty"
    }

    @Test
    fun `a null body fails`() {
        shouldThrow<AppSyncDeserializationException> {
            AppSyncResponseDeserializer.deserialize(stringRequest(), null)
        }
    }

    @Test
    fun `a malformed body fails with the response type in the message`() {
        shouldThrow<AppSyncDeserializationException> {
            AppSyncResponseDeserializer.deserialize(stringRequest(), "{not json")
        }.message shouldContain "String"
    }

    // ── Lists ───────────────────────────────────────────────────────────
    //
    // For an Iterable response type the response deserializer strips the query-name level first, so
    // `data` must be an object with exactly one field — the query name. What is left below it is what
    // the Iterable deserializer sees: either AppSync's `{ items: [...] }` wrapper, or a bare array for
    // a nested field.

    @Test
    fun `deserializes a list from the items wrapper`() {
        val response = AppSyncResponseDeserializer.deserialize(
            listRequest(),
            """{"data":{"listTodos":{"items":["a","b","c"]}}}"""
        )

        response.data shouldHaveSize 3
        response.data shouldBe listOf("a", "b", "c")
    }

    @Test
    fun `deserializes a bare json array under the query field`() {
        val response = AppSyncResponseDeserializer.deserialize(
            listRequest(),
            """{"data":{"listTodos":["a","b"]}}"""
        )

        response.data shouldBe listOf("a", "b")
    }

    @Test
    fun `a nextToken below the root is ignored, because codegen models the field as a plain List`() {
        val response = AppSyncResponseDeserializer.deserialize(
            listRequest(),
            """{"data":{"listTodos":{"items":["a"],"nextToken":"tok"}}}"""
        )

        response.data shouldBe listOf("a")
    }

    @Test
    fun `an object that is neither an array nor an items wrapper fails`() {
        shouldThrow<AppSyncDeserializationException> {
            AppSyncResponseDeserializer.deserialize(
                listRequest(),
                """{"data":{"listTodos":{"unexpected":"shape"}}}"""
            )
        }
    }

    @Test
    fun `a query with more than one top level field fails`() {
        shouldThrow<AppSyncDeserializationException> {
            AppSyncResponseDeserializer.deserialize(
                listRequest(),
                """{"data":{"listTodos":["a"],"listOther":["b"]}}"""
            )
        }
    }

    // ── Pagination ──────────────────────────────────────────────────────

    @Test
    fun `deserializes a PaginatedResult and its items`() {
        val response = AppSyncResponseDeserializer.deserialize(
            paginatedRequest(),
            """{"data":{"listTodos":{"items":["a","b"],"nextToken":"tok"}}}"""
        )

        response.data.items shouldBe listOf("a", "b")
    }

    @Test
    fun `a PaginatedResult has no next page when the request is not an AppSyncGraphQLRequest`() {
        // Only an AppSyncGraphQLRequest can be rebuilt with a nextToken variable, so a raw request
        // yields items without a follow-up request rather than failing.
        val response = AppSyncResponseDeserializer.deserialize(
            paginatedRequest(),
            """{"data":{"listTodos":{"items":["a"],"nextToken":"tok"}}}"""
        )

        response.data.hasNextResult() shouldBe false
    }

    @Test
    fun `a PaginatedResult with no nextToken has no next page`() {
        val response = AppSyncResponseDeserializer.deserialize(
            paginatedRequest(),
            """{"data":{"listTodos":{"items":["a"]}}}"""
        )

        response.data.items shouldBe listOf("a")
        response.data.hasNextResult() shouldBe false
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun stringRequest(): GraphQLRequest<String> = SimpleGraphQLRequest(
        "query { getTodo { id } }",
        emptyMap(),
        String::class.java,
        GsonVariablesSerializer()
    )

    private fun listRequest(): GraphQLRequest<List<String>> = SimpleGraphQLRequest(
        "query { listTodos { items { id } } }",
        emptyMap(),
        TypeMaker.getParameterizedType(List::class.java, String::class.java),
        GsonVariablesSerializer()
    )

    private fun paginatedRequest(): GraphQLRequest<PaginatedResult<String>> = SimpleGraphQLRequest(
        "query { listTodos { items { id } nextToken } }",
        emptyMap(),
        TypeMaker.getParameterizedType(PaginatedResult::class.java, String::class.java),
        GsonVariablesSerializer()
    )
}
