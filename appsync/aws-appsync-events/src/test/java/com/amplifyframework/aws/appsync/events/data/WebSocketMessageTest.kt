/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.amplifyframework.aws.appsync.events.data

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test

class WebSocketMessageTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `test ConnectionInit serialization`() {
        val connectionInit = WebSocketMessage.Send.ConnectionInit()
        val serialized = json.encodeToString(connectionInit)

        // ConnectionInit should only contain type field when serialized
        serialized shouldEqualJson """
            {
                "type": "connection_init"
            }
        """
    }

    @Test
    fun `test Subscribe message serialization`() {
        val subscriptionMessage = WebSocketMessage.Send.Subscription.Subscribe(
            channel = "my/channel"
        )

        val serialized = json.encodeToString(subscriptionMessage)

        serialized shouldEqualJson """
            {
                "channel": "my/channel"
            }
        """
    }

    @Test
    fun `test Unsubscribe message serialization`() {
        val unsubscribeMessage = WebSocketMessage.Send.Subscription.Subscribe(
            channel = "my/channel"
        )

        val serialized = json.encodeToString(unsubscribeMessage)

        serialized shouldEqualJson """
            {
                "channel": "my/channel"
            }
        """
    }

    @Test
    fun `test Connection Ack deserialization`() {
        val jsonString = """
            {
                "type": "connection_ack",
                "connectionTimeoutMs": 1000
            }
        """

        val deserialized = json.decodeFromString<WebSocketMessage.Received>(jsonString)
        deserialized shouldBe WebSocketMessage.Received.ConnectionAck(1000L)
    }

    @Test
    fun `test KeepAlive deserialization`() {
        val jsonString = """
            {
                "type": "ka"
            }
        """

        val deserialized = json.decodeFromString<WebSocketMessage.Received>(jsonString)
        deserialized shouldBe WebSocketMessage.Received.KeepAlive
    }

    @Test
    fun `test ConnectionError message deserialization`() {
        val jsonString = """
            {
                "type": "connection_error",
                "errors": [
                    {
                        "errorType": "UnauthorizedException",
                        "message": "Test error message"
                    }
                ]
            }
        """

        val deserialized = json.decodeFromString<WebSocketMessage.Received>(jsonString)
        deserialized shouldBe WebSocketMessage.Received.ConnectionError(
            listOf(
                EventsError("UnauthorizedException", "Test error message")
            )
        )
    }

    @Test
    fun `test Data deserialization`() {
        val jsonString = """
            {
                "id": "123",
                "type": "data",
                "event": "{\"event_1\":\"data_1\"}"
        }
        """

        val deserialized = json.decodeFromString<WebSocketMessage.Received>(jsonString)
        deserialized shouldBe WebSocketMessage.Received.Subscription.Data(
            id = "123",
            event = JsonObject(mapOf("event_1" to JsonPrimitive("data_1")))
        )
    }

    @Test
    fun `test SubscribeSuccess deserialization`() {
        val jsonString = """
            {
                "type": "subscribe_success",
                "id": "123"
            }
        """

        val deserialized = json.decodeFromString<WebSocketMessage.Received>(jsonString)
        deserialized shouldBe WebSocketMessage.Received.Subscription.SubscribeSuccess(
            id = "123"
        )
    }

    @Test
    fun `test UnsubscribeSuccess deserialization`() {
        val jsonString = """
            {
                "type": "unsubscribe_success",
                "id": "123"
            }
        """

        val deserialized = json.decodeFromString<WebSocketMessage.Received>(jsonString)
        deserialized shouldBe WebSocketMessage.Received.Subscription.UnsubscribeSuccess(
            id = "123"
        )
    }

    @Test
    fun `test SubscribeError message deserialization`() {
        val jsonString = """
            {
                "type": "subscribe_error",
                "id": "123",
                "errors": [
                    {
                        "errorType": "UnauthorizedException",
                        "message": "Test error message"
                    }
                ]
            }
        """

        val deserialized = json.decodeFromString<WebSocketMessage.Received>(jsonString)
        deserialized shouldBe WebSocketMessage.Received.Subscription.SubscribeError(
            id = "123",
            errors = listOf(
                EventsError("UnauthorizedException", "Test error message")
            )
        )
    }

    @Test
    fun `test UnsubscribeError message deserialization`() {
        val jsonString = """
            {
                "type": "unsubscribe_error",
                "id": "123",
                "errors": [
                    {
                        "errorType": "UnauthorizedException",
                        "message": "Test error message"
                    }
                ]
            }
        """

        val deserialized = json.decodeFromString<WebSocketMessage.Received>(jsonString)
        deserialized shouldBe WebSocketMessage.Received.Subscription.UnsubscribeError(
            id = "123",
            errors = listOf(
                EventsError("UnauthorizedException", "Test error message")
            )
        )
    }

    @Test
    fun `test PublishSuccess message deserialization`() {
        val jsonString = """
            {
                "type": "publish_success",
                "id": "123",
                "failed": [
                    {
                        "identifier": "f1",
                        "index": 1,
                        "code": 401,
                        "message": "error"
                    }
                ],
                "successful": [
                    {
                        "identifier": "s1",
                        "index": 0
                    }
                ]
            }
        """

        val deserialized = json.decodeFromString<WebSocketMessage.Received>(jsonString)
        deserialized shouldBe WebSocketMessage.Received.PublishSuccess(
            id = "123",
            successfulEvents = listOf(
                SuccessfulEvent(
                    identifier = "s1",
                    index = 0
                )
            ),
            failedEvents = listOf(
                FailedEvent(
                    identifier = "f1",
                    index = 1,
                    errorCode = 401,
                    errorMessage = "error"
                )
            )
        )
    }

    @Test
    fun `test PublishError message deserialization`() {
        val jsonString = """
            {
                "type": "publish_error",
                "id": "123",
                "errors": [
                    {
                        "errorType": "UnauthorizedException",
                        "message": "Test error message"
                    }
                ]
            }
        """

        val deserialized = json.decodeFromString<WebSocketMessage.Received>(jsonString)
        deserialized shouldBe WebSocketMessage.Received.PublishError(
            id = "123",
            errors = listOf(
                EventsError("UnauthorizedException", "Test error message")
            )
        )
    }

    @Test
    fun `test Error message deserialization`() {
        val jsonString = """
            {
                "type": "error",
                "id": "123",
                "errors": [
                    {
                        "errorType": "UnauthorizedException",
                        "message": "Test error message"
                    }
                ]
            }
        """

        val deserialized = json.decodeFromString<WebSocketMessage.Received>(jsonString)
        deserialized shouldBe WebSocketMessage.Received.Error(
            id = "123",
            errors = listOf(
                EventsError("UnauthorizedException", "Test error message")
            )
        )
    }
}
