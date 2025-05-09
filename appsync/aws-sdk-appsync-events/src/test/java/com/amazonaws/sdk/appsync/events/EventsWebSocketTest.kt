/*
* Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.sdk.appsync.events

import app.cash.turbine.test
import com.amazonaws.sdk.appsync.core.AppSyncRequest
import com.amazonaws.sdk.appsync.events.data.ConnectException
import com.amazonaws.sdk.appsync.events.data.WebSocketMessage
import com.amazonaws.sdk.appsync.events.mocks.TestAuthorizer
import com.amazonaws.sdk.appsync.events.utils.JsonUtils
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.net.UnknownHostException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.Test

internal class EventsWebSocketTest {
    private val url = "https://11111111111111111111111111.appsync-api.us-east-1.amazonaws.com/event"
    private val eventsEndpoints = EventsEndpoints(
        "https://11111111111111111111111111.appsync-api.us-east-1.amazonaws.com/event"
    )
    private val connectAuthorizer = TestAuthorizer()
    private val okHttpClient = mockk<OkHttpClient>(relaxed = true)
    val websocket = mockk<WebSocket>(relaxed = true)
    private val json = JsonUtils.createJsonForLibrary()
    val eventsWebSocket = EventsWebSocket(
        eventsEndpoints,
        connectAuthorizer,
        okHttpClient,
        json,
        null
    )

    @Test
    fun `test ConnectAppSyncRequest values`() {
        // Setup test data
        val request = Request.Builder()
            .url(url)
            .header("key", "value")
            .build()

        val connectRequest = ConnectAppSyncRequest(eventsEndpoints, request)

        connectRequest.method shouldBe AppSyncRequest.HttpMethod.POST
        connectRequest.body shouldBe "{}"
        connectRequest.headers shouldBe mapOf("key" to "value")
        connectRequest.url shouldBe eventsEndpoints.websocketBaseEndpoint.toString()
    }

    @Test
    fun `test Websocket request values`() = runTest {
        val requestSlot = slot<Request>()
        val ack = """ { "type": "connection_ack", "connectionTimeoutMs": 10000 } """
        every { okHttpClient.newWebSocket(capture(requestSlot), any()) } answers {
            val listener = arg<WebSocketListener>(1)

            launch {
                delay(1) // on virtual timer, just moves to back of queue
                listener.onMessage(websocket, ack)
            }
            websocket
        }

        eventsWebSocket.connect()

        val capturedRequest = requestSlot.captured
        capturedRequest.url.toString() shouldBe
            "https://11111111111111111111111111.appsync-realtime-api.us-east-1.amazonaws.com/event/realtime"
        capturedRequest.headers.size shouldBe 4
        capturedRequest.headers["Sec-WebSocket-Protocol"] shouldBe "aws-appsync-event-ws"
        capturedRequest.headers["host"] shouldBe "11111111111111111111111111.appsync-api.us-east-1.amazonaws.com"
        capturedRequest.headers["x-amz-user-agent"] shouldBe "aws-appsync-events-android#1.0.0"
        capturedRequest.headers["testKey"] shouldBe "default"
        capturedRequest.body shouldBe null
    }

    @Test
    fun `on open send connection init`() = runTest {
        val expectedInit = """{"type":"connection_init"}"""
        getConnectedWebSocket()

        eventsWebSocket.onOpen(websocket, mockk())

        verify { websocket.send(expectedInit) }
        eventsWebSocket.isClosed shouldBe false
    }

    @Test
    fun `connect fails if websocket reports closed`() = runTest {
        shouldThrow<ConnectException> {
            getFailedWebSocket()
        }

        verify { websocket.cancel() }
    }

    @Test
    fun `test disconnect with flush`() = runTest {
        val expectedTimeout = 10000L
        val expectedInit = """{"type":"connection_init"}"""

        every { websocket.close(any(), any()) } answers {
            launch {
                delay(1)
                eventsWebSocket.onClosed(websocket, 1000, "User initiated disconnect")
            }
            true
        }

        getConnectedWebSocket(expectedTimeout)
        eventsWebSocket.onOpen(websocket, mockk())
        verify { websocket.send(expectedInit) }
        eventsWebSocket.isClosed shouldBe false

        eventsWebSocket.events.test {
            eventsWebSocket.disconnect(true)

            val event = awaitItem()
            event shouldBe WebSocketMessage.Closed(WebSocketDisconnectReason.UserInitiated)
            eventsWebSocket.isClosed shouldBe true
        }
    }

    @Test
    fun `test disconnect without flush`() = runTest {
        val expectedTimeout = 10000L
        val expectedInit = """{"type":"connection_init"}"""

        every { websocket.cancel() } answers {
            launch {
                delay(1)
                eventsWebSocket.onClosed(websocket, 1000, "User initiated disconnect")
            }
        }

        getConnectedWebSocket(expectedTimeout)
        eventsWebSocket.onOpen(websocket, mockk())
        verify { websocket.send(expectedInit) }
        eventsWebSocket.isClosed shouldBe false

        eventsWebSocket.events.test {
            eventsWebSocket.disconnect(false)

            val event = awaitItem()
            event shouldBe WebSocketMessage.Closed(WebSocketDisconnectReason.UserInitiated)
            eventsWebSocket.isClosed shouldBe true
        }
    }

    @Test
    fun `on unexpected failure send close with service reason`() = runTest {
        val expectedTimeout = 10000L
        val expectedInit = """{"type":"connection_init"}"""
        getConnectedWebSocket(expectedTimeout)
        eventsWebSocket.onOpen(websocket, mockk())
        verify { websocket.send(expectedInit) }

        eventsWebSocket.events.test {
            eventsWebSocket.onFailure(websocket, mockk(), mockk())

            val event = awaitItem()
            event shouldBe WebSocketMessage.Closed(WebSocketDisconnectReason.Service())
            eventsWebSocket.isClosed shouldBe true
        }
    }

    @Test
    fun `on timeout failure send close with timeout reason`() = runTest {
        val expectedTimeout = 1L
        val expectedInit = """{"type":"connection_init"}"""

        getConnectedWebSocket(expectedTimeout)
        eventsWebSocket.onOpen(websocket, mockk())
        verify { websocket.send(expectedInit) }

        eventsWebSocket.events.test {
            eventsWebSocket.onFailure(websocket, mockk(), mockk())

            val event = awaitItem()
            event shouldBe WebSocketMessage.Closed(WebSocketDisconnectReason.Timeout)
            eventsWebSocket.isClosed shouldBe true
        }
    }

    @Test
    fun `send without authorizers`() = runTest {
        val expectedTimeout = 1000L
        val expectedInit = """{"type":"connection_init"}"""
        getConnectedWebSocket(expectedTimeout)
        eventsWebSocket.onOpen(websocket, mockk())
        verify { websocket.send(expectedInit) }
        val unsubscribeMessage = WebSocketMessage.Send.Subscription.Unsubscribe("abc-123")
        val expectedUnsubscribe = """
            {
               "id":"abc-123",
               "type":"unsubscribe"
            }""".replace("\\s+".toRegex(), "")

        eventsWebSocket.send(unsubscribeMessage)

        verify { websocket.send(expectedUnsubscribe) }
    }

    @Test
    fun `send with authorizers appends auth values`() = runTest {
        val expectedTimeout = 1L
        val expectedInit = """{"type":"connection_init"}"""
        getConnectedWebSocket(expectedTimeout)
        eventsWebSocket.onOpen(websocket, mockk())
        verify { websocket.send(expectedInit) }

        val publishMessage = WebSocketMessage.Send.Publish(
            id = "123",
            channel = "default/channel",
            events = JsonArray(listOf(JsonPrimitive(true)))
        )

        val expectedPublish = """
            {
                "channel":"default/channel",
                "events":[true],
                "id":"123",
                "type":"publish",
                "authorization": {
                    "host":"11111111111111111111111111.appsync-api.us-east-1.amazonaws.com",
                    "testKey":"default"
                }
            }""".replace("\\s+".toRegex(), "")

        eventsWebSocket.sendWithAuthorizer(publishMessage, TestAuthorizer())

        verify { websocket.send(expectedPublish) }
    }

    private suspend fun getConnectedWebSocket(timeout: Long = 10000) = coroutineScope {
        val ack = """ { "type": "connection_ack", "connectionTimeoutMs": $timeout } """
        every { okHttpClient.newWebSocket(any(), any()) } answers {
            val listener = arg<WebSocketListener>(1)
            launch {
                delay(1) // on virtual timer, just moves to back of queue
                listener.onMessage(websocket, ack)
            }
            websocket
        }

        eventsWebSocket.connect()
    }

    private suspend fun getFailedWebSocket(cause: Throwable = UnknownHostException()) = coroutineScope {
        every { okHttpClient.newWebSocket(any(), any()) } answers {
            val listener = arg<WebSocketListener>(1)
            launch {
                delay(1) // on virtual timer, just moves to back of queue
                listener.onFailure(websocket, cause, null)
            }
            websocket
        }

        eventsWebSocket.connect()
    }
}
