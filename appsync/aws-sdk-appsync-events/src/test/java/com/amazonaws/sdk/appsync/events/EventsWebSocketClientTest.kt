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
import app.cash.turbine.turbineScope
import com.amazonaws.sdk.appsync.events.data.BadRequestException
import com.amazonaws.sdk.appsync.events.data.ConnectionClosedException
import com.amazonaws.sdk.appsync.events.data.PublishResult
import com.amazonaws.sdk.appsync.events.mocks.EventsLibraryLogCapture
import com.amazonaws.sdk.appsync.events.mocks.TestAuthorizer
import io.kotest.assertions.fail
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkConstructor
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

internal class EventsWebSocketClientTest {

    @JvmField
    @Rule
    var repeatRule: RepeatRule = RepeatRule()

    private lateinit var webSocketLogCapture: EventsLibraryLogCapture
    private lateinit var eventsEndpoints: EventsEndpoints

    private val connectAuthorizer = TestAuthorizer()
    private val subscribeAuthorizer = TestAuthorizer()
    private val publishAuthorizer = TestAuthorizer()
    private lateinit var websocket: WebSocket
    private lateinit var websocketListenerSlot: CapturingSlot<WebSocketListener>
    private lateinit var options: Events.Options.WebSocket

    @Before
    fun setUp() {
        webSocketLogCapture = EventsLibraryLogCapture()
        eventsEndpoints = EventsEndpoints(
            "https://11111111111111111111111111.appsync-api.us-east-1.amazonaws.com/event"
        )
        websocket = mockk<WebSocket>(relaxed = true)
        websocketListenerSlot = slot<WebSocketListener>()
        options = Events.Options.WebSocket(
            loggerProvider = { _ -> webSocketLogCapture }
        )
        mockkConstructor(OkHttpClient.Builder::class)
    }

    @After
    fun tearDown() {
        unmockkConstructor(OkHttpClient.Builder::class)
    }

    @Test
    fun `successful publish with default authorizer`() = runTest {
        val client = createClient(StandardTestDispatcher(testScheduler))

        setupSendResult { type, id -> successPublishResult(id) }

        val result = client.publish(
            "default/channel",
            JsonPrimitive("test")
        )

        val response = result.shouldBeInstanceOf<PublishResult.Response>()
        response.successfulEvents.shouldHaveSize(1)
        response.failedEvents.shouldBeEmpty()
    }

    @Test
    fun `successful publish with custom authorizer`() = runTest {
        val client = createClient(StandardTestDispatcher(testScheduler))

        val customAuthorizer = TestAuthorizer("c1")

        setupSendResult(authKey = "c1") { type, id -> successPublishResult(id) }

        val result = client.publish(
            "default/channel",
            JsonPrimitive("test"),
            customAuthorizer
        )

        val response = result.shouldBeInstanceOf<PublishResult.Response>()
        response.successfulEvents.shouldHaveSize(1)
        response.failedEvents.shouldBeEmpty()
    }

    @Test
    fun `failed publish with connection closed`() = runTest {
        val client = createClient(StandardTestDispatcher(testScheduler))

        setupSendResult { _, _ ->
            launch {
                delay(1)
                websocketListenerSlot.captured.onClosed(websocket, 1000, "User initiated disconnect")
            }
        }

        val result = client.publish(
            "default/channel",
            JsonPrimitive("test")
        )

        val failure = result.shouldBeInstanceOf<PublishResult.Failure>()
        failure.error.shouldBeInstanceOf<ConnectionClosedException>()
    }

    @Test
    fun `failed publish with bad request error`() = runTest {
        val client = createClient(StandardTestDispatcher(testScheduler))

        setupSendResult(channel = "default/*") { _, id ->
            val failedResult = """
                {
                    "id": $id,
                    "type": "publish_error",
                    "errors": [
                    {
                      "errorType": "BadRequestException",
                      "message": "Invalid Channel Format"
                    }
                    ]
                }
            """.trimIndent()
            backgroundScope.launch {
                delay(1)
                websocketListenerSlot.captured.onMessage(websocket, failedResult)
            }
        }

        val result = client.publish(
            "default/*",
            JsonPrimitive("test")
        )

        val failure = result.shouldBeInstanceOf<PublishResult.Failure>()
        failure.error.shouldBeInstanceOf<BadRequestException>()
    }

    @Test
    fun `successful subscribe with default authorizer`() = runTest {
        val client = createClient(StandardTestDispatcher(testScheduler))
        val channel = "default/channel"
        var receivedSubscriptionResult = false

        setupSendResult { sendType, id ->
            when (sendType) {
                "subscribe" -> {
                    receivedSubscriptionResult = true
                    subscribeSuccessResult(id)
                }
                "publish" -> {
                    if (!receivedSubscriptionResult) {
                        fail("Subscription result not received before publish")
                    }
                    successPublishResult(id)
                }
            }
        }

        client.subscribe(channel).test {
            webSocketLogCapture.messages.filter {
                it == "Successfully subscribed to: $channel"
            }.testIn(backgroundScope).apply {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            val result = client.publish(
                channel,
                JsonPrimitive("test")
            )

            val response = result.shouldBeInstanceOf<PublishResult.Response>()
            response.successfulEvents.shouldHaveSize(1)
            response.failedEvents.shouldBeEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `successful subscribe with custom authorizer`() = runTest {
        val client = createClient(StandardTestDispatcher(testScheduler))
        val channel = "default/channel"
        var receivedSubscriptionResult = false
        val customAuthorizer = TestAuthorizer("c1")

        setupSendResult(authKey = "c1") { sendType, id ->
            when (sendType) {
                "subscribe" -> {
                    receivedSubscriptionResult = true
                    subscribeSuccessResult(id)
                }
                "publish" -> {
                    if (!receivedSubscriptionResult) {
                        fail("Subscription result not received before publish")
                    }
                    successPublishResult(id)
                }
            }
        }

        client.subscribe(channel, customAuthorizer).test {
            webSocketLogCapture.messages.filter {
                it == "Successfully subscribed to: $channel"
            }.testIn(backgroundScope).apply {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            val result = client.publish(
                channel,
                JsonPrimitive("test"),
                customAuthorizer
            )

            val response = result.shouldBeInstanceOf<PublishResult.Response>()
            response.successfulEvents.shouldHaveSize(1)
            response.failedEvents.shouldBeEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `failed subscribe bad format`() = runTest {
        turbineScope {
            val client = createClient(StandardTestDispatcher(testScheduler))
            val channel = "default/channel"

            setupSendResult { _, id -> subscribeErrorResult(id) }

            val error = client.subscribe(channel).testIn(backgroundScope, timeout = Duration.INFINITE).awaitError()

            error shouldBe BadRequestException("Invalid Channel Format")
        }
    }

    @Test
    fun `failed subscribe connection closed`() = runTest {
        turbineScope {
            val client = createClient(StandardTestDispatcher(testScheduler))
            val channel = "default/channel"

            setupSendResult { _, id ->
                launch {
                    delay(1)
                    websocketListenerSlot.captured.onClosed(websocket, 1000, "User initiated disconnect")
                }
            }

            val error = client.subscribe(channel).testIn(backgroundScope).awaitError()
            error shouldBe ConnectionClosedException()
        }
    }

    @Repeat(1000)
    @Test
    fun `disconnect with flush`() = runTest {
        val client = createClient(StandardTestDispatcher(testScheduler))
        val channel = "default/channel"
        setupSendResult { _, id ->
            println("SuccessResult")
            subscribeSuccessResult(id)
        }
        every { websocket.close(any(), any()) } answers {
            backgroundScope.launch {
                delay(1)
                println("onClosed being triggered")
                websocketListenerSlot.captured.onClosed(websocket, 1000, "User initiated disconnect")
            }
            true
        }

        client.subscribe(channel).test {
            webSocketLogCapture.messages.filter {
                it == "Successfully subscribed to: $channel"
            }.testIn(backgroundScope).apply {
                println("Awaiting Item")
                awaitItem()
                println("Cancel and Ignore Remaining")
                cancelAndIgnoreRemainingEvents()
            }
            println("Start disconnect")
            client.disconnect(true)
            println("End disconnect")
            awaitComplete()
            println("Await completed finished")
        }
    }

    @Test
    fun `disconnect without flush`() = runTest {
        val client = createClient(StandardTestDispatcher(testScheduler))
        val channel = "default/channel"
        setupSendResult { _, id -> subscribeSuccessResult(id) }
        every { websocket.cancel() } answers {
            launch {
                delay(1)
                println("on Falure being triggered")
                websocketListenerSlot.captured.onFailure(websocket, Throwable("Cancelled"), null)
            }
        }

        client.subscribe(channel).test {
            webSocketLogCapture.messages.filter {
                it == "Successfully subscribed to: $channel"
            }.testIn(backgroundScope).apply {
                println("Awaiting Item")
                awaitItem()
                println("Cancel and Ignore Remaining")
                cancelAndIgnoreRemainingEvents()
            }
            println("Start disconnect")
            client.disconnect(false)
            println("End disconnect")
            awaitComplete()
            println("Await completed finished")
        }
    }

    private suspend fun TestScope.createClient(testDispatcher: CoroutineDispatcher) = coroutineScope {
        every { constructedWith<OkHttpClient.Builder>().build() } returns mockk<OkHttpClient>(relaxed = true) {
            every { newWebSocket(any(), capture(websocketListenerSlot)) } answers {
                val ack = """ { "type": "connection_ack", "connectionTimeoutMs": 10000 } """
                backgroundScope.launch(testDispatcher) {
                    delay(1)
                    websocketListenerSlot.captured.onMessage(websocket, ack)
                }
                websocket
            }
        }

        EventsWebSocketClient(
            connectAuthorizer,
            subscribeAuthorizer,
            publishAuthorizer,
            options,
            eventsEndpoints,
            testDispatcher
        )
    }

    private fun setupSendResult(
        authKey: String = "default",
        channel: String = "default/channel",
        func: (sendType: String, id: String) -> Unit
    ) {
        val expectedSubscribeData = Json.parseToJsonElement(
            """
                {
                    "channel":"$channel",
                    "type":"subscribe",
                    "authorization":{
                        "host":"11111111111111111111111111.appsync-api.us-east-1.amazonaws.com",
                        "testKey":"$authKey"
                    }
                }
            """.trimIndent()
        ).jsonObject

        val expectedSendData = Json.parseToJsonElement(
            """
                {
                    "channel":"$channel",
                    "events":["\"test\""],
                    "type":"publish",
                    "authorization":{
                        "host":"11111111111111111111111111.appsync-api.us-east-1.amazonaws.com",
                        "testKey":"$authKey"
                    }
                }
            """.trimIndent()
        ).jsonObject

        println("Setup send")
        every { websocket.send(any<String>()) } answers {
            val json = firstArg<String>()
            val sendObject = Json.parseToJsonElement(json).jsonObject
            val compareSendObject = JsonObject(sendObject.filterKeys { it != "id" })
            val id = sendObject["id"]
            println("Send called with ${compareSendObject["type"]}")

            when (compareSendObject["type"]) {
                JsonPrimitive("subscribe") -> {
                    compareSendObject shouldBe expectedSubscribeData
                    func("subscribe", id.toString())
                }
                JsonPrimitive("publish") -> {
                    compareSendObject shouldBe expectedSendData
                    func("publish", id.toString())
                }
                else -> throw IllegalArgumentException("Unexpected send type")
            }
            true
        }
    }

    private fun TestScope.successPublishResult(id: String) {
        val result = """
            {
                "id": $id,
                "type": "publish_success",
                "successful": [
                    {
                      "identifier": "cc696343-9349-4211-b38e-dac22c1d64f8",
                      "index": 0
                    }
                ],
                "failed": []
            }
        """.trimIndent()
        backgroundScope.launch {
            delay(1)
            websocketListenerSlot.captured.onMessage(websocket, result)
        }
    }

    private fun TestScope.subscribeSuccessResult(id: String) {
        val result = """
            {
                "id": $id,
                "type": "subscribe_success"
            }
        """.trimIndent()
        backgroundScope.launch {
            delay(1)
            println("Subscribe Success onMessage")
            websocketListenerSlot.captured.onMessage(websocket, result)
        }
    }

    private fun TestScope.subscribeErrorResult(id: String) {
        val result = """
            {
                "id": $id,
                "type": "subscribe_error",
                "errors": [
                    {
                      "errorType": "BadRequestException",
                      "message": "Invalid Channel Format"
                    }
                ]
            }
        """.trimIndent()
        backgroundScope.launch {
            delay(1)
            websocketListenerSlot.captured.onMessage(websocket, result)
        }
    }
}
