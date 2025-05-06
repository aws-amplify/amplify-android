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

import com.amazonaws.sdk.appsync.core.AppSyncRequest
import com.amazonaws.sdk.appsync.events.data.ConnectException
import com.amazonaws.sdk.appsync.events.data.WebSocketMessage
import com.amazonaws.sdk.appsync.events.mocks.TestAuthorizer
import com.amazonaws.sdk.appsync.events.utils.JsonUtils
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.net.UnknownHostException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class EventsWebSocketTest {
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
    private lateinit var messageCollector: EventsWebSocketMessageCollector

    @Before
    fun setUp() {
        messageCollector = EventsWebSocketMessageCollector(eventsWebSocket)
    }

    @After
    fun tearDown() {
        messageCollector.stop()
    }

    @Test
    fun `test ConnectAppSyncRequest values`() {
        // Setup test data
        val request = Request.Builder()
            .url("https://11111111111111111111111111.appsync-realtime-api.us-east-1.amazonaws.com/event/realtime")
            .header("key", "value")
            .build()

        val connectRequest = ConnectAppSyncRequest(eventsEndpoints, request)

        connectRequest.method shouldBe AppSyncRequest.HttpMethod.POST
        connectRequest.body shouldBe "{}"
        connectRequest.headers shouldBe mapOf("key" to "value")
        connectRequest.url shouldBe eventsEndpoints.websocketBaseEndpoint.toString()
    }

    @Test
    fun `test Websocket request values`(): Unit = runBlocking {
        async(Dispatchers.IO) { getConnectedWebSocket() }.await()
        val requestSlot = slot<Request>()
        verify {
            okHttpClient.newWebSocket(capture(requestSlot), eventsWebSocket)
        }

        val capturedRequest = requestSlot.captured
        capturedRequest.url shouldBe
            "https://11111111111111111111111111.appsync-realtime-api.us-east-1.amazonaws.com/event/realtime".toHttpUrl()
        capturedRequest.headers.size shouldBe 4
        capturedRequest.headers["Sec-WebSocket-Protocol"] shouldBe "aws-appsync-event-ws"
        capturedRequest.headers["host"] shouldBe "11111111111111111111111111.appsync-api.us-east-1.amazonaws.com"
        capturedRequest.headers["x-amz-user-agent"] shouldBe "aws-appsync-events-android#1.0.0"
        capturedRequest.headers["testKey"] shouldBe "default"
        capturedRequest.body shouldBe null
    }

    @Test
    fun `on open send connection init`(): Unit = runBlocking {
        val expectedInit = """{"type":"connection_init"}"""
        async(Dispatchers.IO) { getConnectedWebSocket() }.await()

        eventsWebSocket.onOpen(websocket, mockk())

        verify { websocket.send(expectedInit) }
        eventsWebSocket.isClosed shouldBe false
    }

    @Test
    fun `connect fails if websocket reports closed`(): Unit = runBlocking {
        var caughtException: Exception? = null
        async(Dispatchers.IO) {
            try {
                getFailedWebSocket()
            } catch (e: Exception) {
                caughtException = e
            }
        }.await()

        caughtException shouldBe ConnectException(null)
        verify { websocket.cancel() }
    }

    @Test
    fun `test disconnect with flush`(): Unit = runBlocking {
        val expectedTimeout = 10000L
        val expectedInit = """{"type":"connection_init"}"""
        async(Dispatchers.IO) { getConnectedWebSocket(expectedTimeout) }.await()
        eventsWebSocket.onOpen(websocket, mockk())
        verify { websocket.send(expectedInit) }
        eventsWebSocket.isClosed shouldBe false
        every { websocket.close(any(), any()) } answers {
            true.also {
                this@runBlocking.launch(Dispatchers.IO) {
                    eventsWebSocket.onClosed(websocket, 1000, "User initiated disconnect")
                }
            }
        }

        async(Dispatchers.IO) { eventsWebSocket.disconnect(true) }.await()

        awaitExpectedMessageCount(2, messageCollector)
        messageCollector.messages shouldBe listOf(
            WebSocketMessage.Received.ConnectionAck(expectedTimeout),
            WebSocketMessage.Closed(WebSocketDisconnectReason.UserInitiated)
        )
        eventsWebSocket.isClosed shouldBe true
    }

    @Test
    fun `test disconnect without flush`(): Unit = runBlocking {
        val expectedTimeout = 10000L
        val expectedInit = """{"type":"connection_init"}"""
        async(Dispatchers.IO) { getConnectedWebSocket(expectedTimeout) }.await()
        eventsWebSocket.onOpen(websocket, mockk())
        verify { websocket.send(expectedInit) }
        eventsWebSocket.isClosed shouldBe false
        every { websocket.cancel() } answers {
            true.also {
                this@runBlocking.launch(Dispatchers.IO) {
                    eventsWebSocket.onClosed(websocket, 1000, "User initiated disconnect")
                }
            }
        }

        async(Dispatchers.IO) { eventsWebSocket.disconnect(false) }.await()

        awaitExpectedMessageCount(2, messageCollector)
        messageCollector.messages shouldBe listOf(
            WebSocketMessage.Received.ConnectionAck(expectedTimeout),
            WebSocketMessage.Closed(WebSocketDisconnectReason.UserInitiated)
        )
        eventsWebSocket.isClosed shouldBe true
    }

    @Test
    fun `on unexpected failure send close with service reason`(): Unit = runBlocking {
        val expectedTimeout = 10000L
        val expectedInit = """{"type":"connection_init"}"""
        async(Dispatchers.IO) { getConnectedWebSocket(expectedTimeout) }.await()
        eventsWebSocket.onOpen(websocket, mockk())
        verify { websocket.send(expectedInit) }

        eventsWebSocket.onFailure(websocket, mockk(), mockk())

        awaitExpectedMessageCount(2, messageCollector)
        messageCollector.messages shouldBe listOf(
            WebSocketMessage.Received.ConnectionAck(expectedTimeout),
            WebSocketMessage.Closed(WebSocketDisconnectReason.Service())
        )
        eventsWebSocket.isClosed shouldBe true
    }

    @Test
    fun `on timeout failure send close with timeout reason`(): Unit = runBlocking {
        val expectedTimeout = 1L
        val expectedInit = """{"type":"connection_init"}"""
        async(Dispatchers.IO) { getConnectedWebSocket(expectedTimeout) }.await()
        eventsWebSocket.onOpen(websocket, mockk())
        verify { websocket.send(expectedInit) }

        eventsWebSocket.onFailure(websocket, mockk(), mockk())

        awaitExpectedMessageCount(2, messageCollector)
        messageCollector.messages shouldBe listOf(
            WebSocketMessage.Received.ConnectionAck(expectedTimeout),
            WebSocketMessage.Closed(WebSocketDisconnectReason.Timeout)
        )
        eventsWebSocket.isClosed shouldBe true
    }

    @Test
    fun `send without authorizers`(): Unit = runBlocking {
        val expectedTimeout = 1L
        val expectedInit = """{"type":"connection_init"}"""
        async(Dispatchers.IO) { getConnectedWebSocket(expectedTimeout) }.await()
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
    fun `send with authorizers appends auth values`(): Unit = runBlocking {
        val expectedTimeout = 1L
        val expectedInit = """{"type":"connection_init"}"""
        async(Dispatchers.IO) { getConnectedWebSocket(expectedTimeout) }.await()
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

    private suspend fun getConnectedWebSocket(timeout: Long = 10000) {
        val ack = """
            {
                "type": "connection_ack",
                "connectionTimeoutMs": $timeout
            }
        """

        every { okHttpClient.newWebSocket(any(), any()) } answers {
            websocket.also {
                eventsWebSocket.onMessage(websocket, ack)
            }
        }

        eventsWebSocket.connect()
    }

    private suspend fun getFailedWebSocket() {
        every { okHttpClient.newWebSocket(any(), any()) } answers {
            websocket.also {
                eventsWebSocket.onFailure(websocket, UnknownHostException(""), null)
            }
        }

        eventsWebSocket.connect()
    }
}

internal class EventsWebSocketMessageCollector(eventsWebSocket: EventsWebSocket) {

    val messages = mutableListOf<WebSocketMessage>()
    val job = Job()

    init {
        CoroutineScope(job + Dispatchers.IO).launch {
            eventsWebSocket.events.collect {
                messages.add(it)
            }
        }
    }

    fun stop() {
        job.complete()
    }
}

internal suspend fun awaitExpectedMessageCount(count: Int, collector: EventsWebSocketMessageCollector) {
    withTimeout(5000) {
        while (collector.messages.size < count) {
            delay(10)
        }
    }
}
