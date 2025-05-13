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

import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.amazonaws.sdk.appsync.core.authorizers.ApiKeyAuthorizer
import com.amazonaws.sdk.appsync.events.data.InvalidInputException
import com.amazonaws.sdk.appsync.events.data.PublishResult
import com.amazonaws.sdk.appsync.events.data.UnauthorizedException
import com.amazonaws.sdk.appsync.events.data.WebSocketMessage
import com.amazonaws.sdk.appsync.events.testmodels.TestMessage
import com.amazonaws.sdk.appsync.events.utils.EventsLibraryLogCapture
import com.amazonaws.sdk.appsync.events.utils.JsonUtils
import com.amazonaws.sdk.appsync.events.utils.getEventsConfig
import com.amplifyframework.testutils.coroutines.runBlockingWithTimeout
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.After
import org.junit.Test

internal class EventsWebSocketClientTests {
    private val eventsConfig = getEventsConfig(InstrumentationRegistry.getInstrumentation().targetContext)
    private val apiKeyAuthorizer = ApiKeyAuthorizer(eventsConfig.apiKey)
    private val webSocketLogCapture = EventsLibraryLogCapture()
    private val defaultChannel = "default/${UUID.randomUUID()}"
    private val customChannel = "custom/${UUID.randomUUID()}"
    private val json = JsonUtils.createJsonForLibrary()
    private val events = Events(eventsConfig.url)
    private val webSocketClient = events.createWebSocketClient(
        apiKeyAuthorizer,
        apiKeyAuthorizer,
        apiKeyAuthorizer,
        options = Events.Options.WebSocket(
            loggerProvider = { _ -> webSocketLogCapture }
        )
    )
    private val backgroundScope = CoroutineScope(Dispatchers.IO)

    @After
    fun tearDown() {
        runBlockingWithTimeout {
            webSocketClient.disconnect(flushEvents = false)
        }
    }

    @Test
    fun testSinglePrimitivePublish() = runBlockingWithTimeout {
        testSinglePublish(JsonPrimitive(true))
    }

    @Test
    fun testSingleArrayPublish() = runBlockingWithTimeout {
        testSinglePublish(JsonArray(listOf(JsonPrimitive(true), JsonPrimitive(false))))
    }

    @Test
    fun testSingleObjectPublish() = runBlockingWithTimeout {
        testSinglePublish(json.encodeToJsonElement(TestMessage()))
    }

    @Test
    fun testMultiplePrimitivePublish() = runBlockingWithTimeout {
        testMultiplePublish(listOf(JsonPrimitive(true), JsonPrimitive(false)))
    }

    @Test
    fun testMultipleArrayPublish() = runBlockingWithTimeout {
        testMultiplePublish(
            listOf(
                JsonArray(listOf(JsonPrimitive(true), JsonPrimitive(false))),
                JsonArray(listOf(JsonPrimitive(true), JsonPrimitive(false)))
            )
        )
    }

    @Test
    fun testMultipleObjectPublish() = runBlockingWithTimeout {
        testMultiplePublish(
            listOf(
                json.encodeToJsonElement(TestMessage(messageId = "1", content = "hi")),
                json.encodeToJsonElement(TestMessage(messageId = "2", content = "hello"))
            )
        )
    }

    @Test
    fun testPublishWithBadAuth(): Unit = runBlockingWithTimeout {
        // Publish the message
        val webSocketClient = events.createWebSocketClient(apiKeyAuthorizer, apiKeyAuthorizer, apiKeyAuthorizer)
        val result = webSocketClient.publish(
            channelName = defaultChannel,
            event = JsonPrimitive(true),
            authorizer = ApiKeyAuthorizer("bad-api-key")
        )

        // Assert expected response
        (result is PublishResult.Failure) shouldBe true
        (result as PublishResult.Failure).apply {
            error shouldBe UnauthorizedException("You are not authorized to make this call.")
        }
        val response = result.shouldBeInstanceOf<PublishResult.Failure>()
        response.error shouldBe UnauthorizedException("You are not authorized to make this call.")
    }

    @Test
    fun testPublishWithTooManyEvents(): Unit = runBlockingWithTimeout {
        val sendEvents = (0 until 6).map { JsonPrimitive(true) }
        // Publish the message
        val webSocketClient = events.createWebSocketClient(apiKeyAuthorizer, apiKeyAuthorizer, apiKeyAuthorizer)
        val result = webSocketClient.publish(
            channelName = defaultChannel,
            events = sendEvents
        )

        // Assert expected response
        (result is PublishResult.Failure) shouldBe true
        (result as PublishResult.Failure).apply {
            error shouldBe InvalidInputException("input exceeded 5 event limit")
        }
    }

    @Test
    fun testPublishToNonConfiguredChannel(): Unit = runBlockingWithTimeout {
        // Publish the message
        val webSocketClient = events.createWebSocketClient(apiKeyAuthorizer, apiKeyAuthorizer, apiKeyAuthorizer)
        val result = webSocketClient.publish(
            channelName = "bad/channel",
            event = JsonPrimitive(true)
        )

        // Assert expected response
        (result is PublishResult.Failure) shouldBe true
        (result as PublishResult.Failure).apply {
            error shouldBe UnauthorizedException("Failed to publish event(s)")
        }
    }

    @Test
    fun testWebSocketFlowLifecycle(): Unit = runBlockingWithTimeout(15.seconds) {
        val expectedLogs = listOf(
            "Opening Websocket Connection",
            "onOpen: sending connection init",
            "onMessage: processed ${WebSocketMessage.Received.ConnectionAck::class.java}",
            "onMessage: processed ${WebSocketMessage.Received.Subscription.SubscribeSuccess::class.java}",
            "Successfully subscribed to: $defaultChannel",
            "onMessage: processed ${WebSocketMessage.Received.Subscription.UnsubscribeSuccess::class.java}",
            "emit ${WebSocketMessage.Closed::class.java}"
        )

        turbineScope(timeout = 10.seconds) {
            webSocketClient.subscribe(defaultChannel).test(timeout = 5.seconds) {
                // Wait for subscription to return success
                webSocketLogCapture.messages.filter {
                    it == "Successfully subscribed to: $defaultChannel"
                }.testIn(backgroundScope, timeout = 5.seconds).apply {
                    awaitItem()
                    cancelAndIgnoreRemainingEvents()
                }
                // Cleanup
                cancelAndIgnoreRemainingEvents()
            }

            // Wait for websocket to unsubscribe
            webSocketLogCapture.messages.filter {
                it == "onMessage: processed ${WebSocketMessage.Received.Subscription.UnsubscribeSuccess::class.java}"
            }.testIn(backgroundScope, timeout = 5.seconds).apply {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            webSocketClient.disconnect()

            // Wait for channel to unsubscribe
            webSocketLogCapture.messages.filter {
                it == "emit ${WebSocketMessage.Closed::class.java}"
            }.testIn(backgroundScope, timeout = 5.seconds).apply {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
        }

        val receivedLogs = webSocketLogCapture.messages.replayCache.filter { expectedLogs.contains(it) }
        receivedLogs shouldBe expectedLogs
    }

    @Test
    fun channelsOnlyReceiveEventsFromTheirChannel(): Unit = runBlockingWithTimeout {
        val expectedCustomMessage = JsonPrimitive(false)
        val expectedDefaultMessage = JsonPrimitive(true)

        turbineScope {
            webSocketClient.subscribe(defaultChannel).test {
                webSocketClient.subscribe(customChannel).test {
                    // Wait for subscription to return success
                    webSocketLogCapture.messages.filter {
                        it == "Successfully subscribed to: $defaultChannel" ||
                            it == "Successfully subscribed to: $customChannel"
                    }.testIn(backgroundScope).apply {
                        awaitItem() // subscription 1
                        awaitItem() // subscription 2
                        cancelAndIgnoreRemainingEvents()
                    }

                    // Publish the messages
                    webSocketClient.publish(defaultChannel, expectedDefaultMessage)
                    webSocketClient.publish(customChannel, expectedCustomMessage)

                    awaitItem().data shouldBe expectedCustomMessage
                    cancelAndIgnoreRemainingEvents()
                }

                awaitItem().data shouldBe expectedDefaultMessage

                // Cleanup
                webSocketClient.disconnect(flushEvents = false)
                cancelAndIgnoreRemainingEvents()
            }
        }

        // Ensure only 1 WS Connection was opened
        webSocketLogCapture.messages.replayCache.filter {
            it == "Opening Websocket Connection"
        }.size shouldBe 1
    }

    @Test
    fun testWebSocketRecreateScenario(): Unit = runBlockingWithTimeout {
        testSinglePublish(JsonPrimitive(true))

        // websocket has disconnected at this point
        // ensure connect is tracked, then reset cache
        webSocketLogCapture.messages.replayCache.filter {
            it == "Opening Websocket Connection"
        }.size shouldBe 1
        webSocketLogCapture.resetCache() // reset cache
        webSocketLogCapture.messages.replayCache.size shouldBe 0 // ensure cache is reset

        testSinglePublish(JsonPrimitive(false))

        webSocketLogCapture.messages.replayCache.filter {
            it == "Opening Websocket Connection"
        }.size shouldBe 1
    }

    private suspend fun testSinglePublish(jsonItem: JsonElement) {
        turbineScope {
            webSocketClient.subscribe(defaultChannel).test {
                // Wait for subscription to return success
                webSocketLogCapture.messages.filter {
                    it == "Successfully subscribed to: $defaultChannel"
                }.testIn(backgroundScope).apply {
                    awaitItem()
                    cancelAndIgnoreRemainingEvents()
                }

                // Publish the message
                val result = webSocketClient.publish(defaultChannel, jsonItem)

                // Assert expected response
                val response = result.shouldBeInstanceOf<PublishResult.Response>()
                response.failedEvents.shouldBeEmpty()
                response.successfulEvents.shouldHaveSize(1)
                response.successfulEvents.first().index shouldBe 0

                // Wait for message to be provided in subscription
                val receivedMessage = awaitItem()

                // Assert expected subscription event message
                receivedMessage.data shouldBe jsonItem

                // Cleanup
                webSocketClient.disconnect(flushEvents = false)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    private suspend fun testMultiplePublish(jsonItems: List<JsonElement>) {
        turbineScope {
            webSocketClient.subscribe(defaultChannel).test {
                // Wait for subscription to return success
                webSocketLogCapture.messages.filter {
                    it == "Successfully subscribed to: $defaultChannel"
                }.testIn(backgroundScope).apply {
                    awaitItem()
                    cancelAndIgnoreRemainingEvents()
                }

                // Publish the message
                val result = webSocketClient.publish(defaultChannel, jsonItems)

                // Assert expected response
                val response = result.shouldBeInstanceOf<PublishResult.Response>()
                response.failedEvents.shouldBeEmpty()
                response.successfulEvents.shouldHaveSize(jsonItems.size)

                // Wait for message to be provided in subscription
                val receivedMessages = jsonItems.indices.map {
                    awaitItem()
                }

                // Assert expected subscription event messages
                receivedMessages.map { it.data }.toSet() shouldBe jsonItems.toSet()

                // Cleanup
                webSocketClient.disconnect(flushEvents = false)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
