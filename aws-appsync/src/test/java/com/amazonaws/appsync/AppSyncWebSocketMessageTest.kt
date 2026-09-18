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

import com.google.gson.JsonParser
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

/**
 * Tests [AppSyncWebSocketMessage] — the `graphql-ws` wire format.
 *
 * The parsing side is the substance: a subscription connection is shared, so one malformed frame must
 * not be able to take down the other subscriptions riding on it. Every unparseable input is expected
 * to become [AppSyncWebSocketMessage.Unknown] rather than throw.
 */
class AppSyncWebSocketMessageTest {

    // ── Outbound ────────────────────────────────────────────────────────

    @Test
    fun `connection init has just a type`() {
        val json = JsonParser.parseString(AppSyncWebSocketMessage.ConnectionInit.toJson()).asJsonObject

        json.get("type").asString shouldBe "connection_init"
    }

    @Test
    fun `start carries the document and its auth headers under payload extensions`() {
        val message = AppSyncWebSocketMessage.Start(
            id = "sub-1",
            query = """{"query":"subscription { onCreateTodo { id } }"}""",
            authorizationHeaders = mapOf("x-api-key" to "da2-fakekey", "host" to "example.com")
        )

        val json = JsonParser.parseString(message.toJson()).asJsonObject

        json.get("id").asString shouldBe "sub-1"
        json.get("type").asString shouldBe "start"
        val payload = json.getAsJsonObject("payload")
        payload.get("data").asString shouldContain "onCreateTodo"
        val authorization = payload.getAsJsonObject("extensions").getAsJsonObject("authorization")
        authorization.get("x-api-key").asString shouldBe "da2-fakekey"
        authorization.get("host").asString shouldBe "example.com"
    }

    @Test
    fun `start with no auth headers still produces a valid authorization object`() {
        val message = AppSyncWebSocketMessage.Start("sub-1", "{}", emptyMap())

        val payload = JsonParser.parseString(message.toJson()).asJsonObject.getAsJsonObject("payload")

        payload.getAsJsonObject("extensions").getAsJsonObject("authorization").size() shouldBe 0
    }

    @Test
    fun `stop carries the id being cancelled`() {
        val json = JsonParser.parseString(AppSyncWebSocketMessage.Stop("sub-7").toJson()).asJsonObject

        json.get("id").asString shouldBe "sub-7"
        json.get("type").asString shouldBe "stop"
    }

    // ── Inbound ─────────────────────────────────────────────────────────

    @Test
    fun `parses connection_ack and its timeout`() {
        val message = AppSyncWebSocketMessage.parse(
            """{"type":"connection_ack","payload":{"connectionTimeoutMs":300000}}"""
        )

        message.shouldBeInstanceOf<AppSyncWebSocketMessage.ConnectionAck>()
        message.connectionTimeoutMs shouldBe 300_000L
    }

    @Test
    fun `connection_ack without a timeout falls back to a default rather than failing`() {
        val message = AppSyncWebSocketMessage.parse("""{"type":"connection_ack"}""")

        message.shouldBeInstanceOf<AppSyncWebSocketMessage.ConnectionAck>()
        message.connectionTimeoutMs shouldBe 300_000L
    }

    @Test
    fun `parses keep-alive`() {
        AppSyncWebSocketMessage.parse("""{"type":"ka"}""")
            .shouldBeInstanceOf<AppSyncWebSocketMessage.KeepAlive>()
    }

    @Test
    fun `parses start_ack with its id`() {
        val message = AppSyncWebSocketMessage.parse("""{"type":"start_ack","id":"sub-1"}""")

        message.shouldBeInstanceOf<AppSyncWebSocketMessage.StartAck>()
        message.id shouldBe "sub-1"
    }

    @Test
    fun `parses data and passes the payload through as raw json`() {
        // The payload is not deserialized here: only the subscriber knows the response type.
        val message = AppSyncWebSocketMessage.parse(
            """{"type":"data","id":"sub-1","payload":{"data":{"onCreateTodo":{"id":"1"}}}}"""
        )

        message.shouldBeInstanceOf<AppSyncWebSocketMessage.Data>()
        message.id shouldBe "sub-1"
        message.payload shouldContain "onCreateTodo"
    }

    @Test
    fun `parses complete`() {
        val message = AppSyncWebSocketMessage.parse("""{"type":"complete","id":"sub-1"}""")

        message.shouldBeInstanceOf<AppSyncWebSocketMessage.Complete>()
        message.id shouldBe "sub-1"
    }

    // ── Error shapes ────────────────────────────────────────────────────

    @Test
    fun `parses connection_error messages from payload errors`() {
        val message = AppSyncWebSocketMessage.parse(
            """{"type":"connection_error","payload":{"errors":[{"message":"Unauthorized"}]}}"""
        )

        message.shouldBeInstanceOf<AppSyncWebSocketMessage.ConnectionError>()
        message.errors.map { it.message } shouldContainExactly listOf("Unauthorized")
    }

    @Test
    fun `parses subscription error from a top-level errors array too`() {
        // AppSync uses both shapes depending on the failure, so both are read.
        val message = AppSyncWebSocketMessage.parse(
            """{"type":"error","id":"sub-1","errors":[{"message":"MaxSubscriptionsReachedError"}]}"""
        )

        message.shouldBeInstanceOf<AppSyncWebSocketMessage.Error>()
        message.id shouldBe "sub-1"
        message.errors.map { it.message } shouldContainExactly listOf("MaxSubscriptionsReachedError")
    }

    @Test
    fun `parses errors given as bare strings`() {
        val message = AppSyncWebSocketMessage.parse(
            """{"type":"error","id":"sub-1","payload":{"errors":["something broke"]}}"""
        )

        message.shouldBeInstanceOf<AppSyncWebSocketMessage.Error>()
        message.errors.map { it.message } shouldContainExactly listOf("something broke")
        // A bare string carries no classification, so the error type is unknown rather than assumed.
        message.errors.single().errorType shouldBe null
    }

    @Test
    fun `an error with no id is still an Error, since a connection-level error has none`() {
        val message = AppSyncWebSocketMessage.parse("""{"type":"error","payload":{"errors":[]}}""")

        message.shouldBeInstanceOf<AppSyncWebSocketMessage.Error>()
        message.id shouldBe null
    }

    @Test
    fun `an errorType on the error object itself is read`() {
        // The realtime protocol puts the classification directly on the error, not under extensions.
        val message = AppSyncWebSocketMessage.parse(
            """{"type":"error","id":"sub-1","payload":{"errors":[
               {"errorType":"MaxSubscriptionsReachedError","message":"limit reached"}]}}"""
        )

        message.shouldBeInstanceOf<AppSyncWebSocketMessage.Error>()
        message.errors.single().errorType shouldBe "MaxSubscriptionsReachedError"
    }

    @Test
    fun `an errorType under extensions is still read`() {
        val message = AppSyncWebSocketMessage.parse(
            """{"type":"error","id":"sub-1","payload":{"errors":[
               {"message":"nope","extensions":{"errorType":"Unauthorized"}}]}}"""
        )

        message.shouldBeInstanceOf<AppSyncWebSocketMessage.Error>()
        message.errors.single().errorType shouldBe "Unauthorized"
    }

    // ── Robustness: nothing may throw ───────────────────────────────────

    @Test
    fun `an unrecognised type becomes Unknown rather than throwing`() {
        val message = AppSyncWebSocketMessage.parse("""{"type":"something_new","id":"x"}""")

        message.shouldBeInstanceOf<AppSyncWebSocketMessage.Unknown>()
        message.type shouldBe "something_new"
    }

    @Test
    fun `malformed json becomes Unknown rather than throwing`() {
        // One bad frame must not take down a shared connection carrying healthy subscriptions.
        listOf("", "not json", "{", "[]", """{"type":}""").forEach { bad ->
            AppSyncWebSocketMessage.parse(bad)
                .shouldBeInstanceOf<AppSyncWebSocketMessage.Unknown>()
        }
    }

    @Test
    fun `a message whose type requires an id but has none becomes Unknown`() {
        // Without an id these cannot be routed to a subscriber, so they must not masquerade as valid.
        listOf("start_ack", "data", "complete").forEach { type ->
            AppSyncWebSocketMessage.parse("""{"type":"$type"}""")
                .shouldBeInstanceOf<AppSyncWebSocketMessage.Unknown>()
        }
    }

    @Test
    fun `a null type becomes Unknown`() {
        AppSyncWebSocketMessage.parse("""{"type":null}""")
            .shouldBeInstanceOf<AppSyncWebSocketMessage.Unknown>()
    }
}
