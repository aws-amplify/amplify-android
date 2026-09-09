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

import com.amplifyframework.core.model.LoadedModelList
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelList
import com.amplifyframework.core.model.ModelPage
import com.google.gson.reflect.TypeToken
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

/**
 * Tests the related-list and page deserializers, through [AppSyncGson] rather than in isolation, so the
 * registration is covered too — an adapter that is written correctly but never registered would pass any
 * test that instantiated it directly.
 */
class AppSyncModelListDeserializerTest {

    private val gson = AppSyncGson.instance

    // ── Loaded lists ────────────────────────────────────────────────────

    @Test
    fun `a list that arrived in full is loaded, not lazy`() {
        val list: ModelList<Todo> = gson.fromJson(
            """{"items":[{"id":"1","name":"first"},{"id":"2","name":"second"}]}""",
            object : TypeToken<ModelList<Todo>>() {}.type
        )

        val loaded = list.shouldBeInstanceOf<LoadedModelList<Todo>>()
        loaded.items shouldHaveSize 2
        loaded.items[0].name shouldBe "first"
    }

    @Test
    fun `an empty items array is a loaded list with no items, not a failure`() {
        val list: ModelList<Todo> = gson.fromJson(
            """{"items":[]}""",
            object : TypeToken<ModelList<Todo>>() {}.type
        )

        list.shouldBeInstanceOf<LoadedModelList<Todo>>().items.shouldHaveSize(0)
    }

    // ── Pages ───────────────────────────────────────────────────────────

    @Test
    fun `a page carries its next token`() {
        val page: ModelPage<Todo> = gson.fromJson(
            """{"items":[{"id":"1","name":"first"}],"nextToken":"cursor-abc"}""",
            object : TypeToken<ModelPage<Todo>>() {}.type
        )

        page.items shouldHaveSize 1
        page.hasNextPage shouldBe true
        page.nextToken.shouldBeInstanceOf<AppSyncPaginationToken>().nextToken shouldBe "cursor-abc"
    }

    @Test
    fun `a final page has no next token, which is what ends pagination`() {
        val page: ModelPage<Todo> = gson.fromJson(
            """{"items":[{"id":"1","name":"first"}]}""",
            object : TypeToken<ModelPage<Todo>>() {}.type
        )

        page.nextToken.shouldBeNull()
        // hasNextPage is derived, so a caller looping on it terminates without inspecting the token.
        page.hasNextPage shouldBe false
    }

    @Test
    fun `a null next token is treated as absent rather than as a cursor`() {
        val page: ModelPage<Todo> = gson.fromJson(
            """{"items":[],"nextToken":null}""",
            object : TypeToken<ModelPage<Todo>>() {}.type
        )

        page.nextToken.shouldBeNull()
        page.hasNextPage shouldBe false
    }

    // ── Malformed input ─────────────────────────────────────────────────

    @Test
    fun `a payload with no items array is reported as a deserialization failure`() {
        val error = shouldThrow<AppSyncDeserializationException> {
            gson.fromJson<ModelList<Todo>>(
                """{"unexpected":"shape"}""",
                object : TypeToken<ModelList<Todo>>() {}.type
            )
        }

        error.message shouldContain "items"
    }

    @Test
    fun `a list requested without an element type is reported rather than silently empty`() {
        // The element type is what tells the deserializer how to read each item, so a raw ModelList
        // cannot be honoured — failing loudly beats returning items deserialized as the wrong type.
        val error = shouldThrow<AppSyncDeserializationException> {
            gson.fromJson<ModelList<*>>("""{"items":[]}""", ModelList::class.java)
        }

        error.message shouldContain "element type"
    }

    /** A minimal model: these tests exercise the list wrapper, not model deserialization. */
    private data class Todo(val id: String, val name: String) : Model
}
