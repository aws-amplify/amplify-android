package com.amazonaws.sdk.appsync.events

import com.amazonaws.sdk.appsync.events.data.BadRequestException
import com.amazonaws.sdk.appsync.events.data.ConnectionClosedException
import com.amazonaws.sdk.appsync.events.data.PublishResult
import com.amazonaws.sdk.appsync.events.mocks.TestAuthorizer
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkConstructor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class EventsWebSocketClientTest {
    private val eventsEndpoints = EventsEndpoints(
        "https://11111111111111111111111111.appsync-api.us-east-1.amazonaws.com/event"
    )

    private val connectAuthorizer = TestAuthorizer()
    private val subscribeAuthorizer = TestAuthorizer()
    private val publishAuthorizer = TestAuthorizer()
    private val websocket = mockk<WebSocket>(relaxed = true)
    private val websocketListenerSlot = slot<WebSocketListener>()
    private val options = Events.Options.WebSocket()
    private lateinit var client: EventsWebSocketClient

    @Before
    fun setUp() {
        mockkConstructor(OkHttpClient.Builder::class)
        val mockClient = mockk<OkHttpClient>(relaxed = true) {
            every { newWebSocket(any(), capture(websocketListenerSlot)) } answers {
                val ack = """
                    {
                        "type": "connection_ack",
                        "connectionTimeoutMs": 10000
                    }
                """
                websocket.also {
                    CoroutineScope(Dispatchers.IO).launch {
                        websocketListenerSlot.captured.onMessage(websocket, ack)
                    }
                }
            }
        }
        every { constructedWith<OkHttpClient.Builder>().build() } returns mockClient

        client = EventsWebSocketClient(
            connectAuthorizer,
            subscribeAuthorizer,
            publishAuthorizer,
            options,
            eventsEndpoints
        )
    }

    @After
    fun tearDown() {
        unmockkConstructor(OkHttpClient.Builder::class)
    }

    @Test
    fun `successful publish with default authorizer`(): Unit = runBlocking(Dispatchers.IO) {
        // assertion must remove id since that is not known
        val expectedSendData = Json.parseToJsonElement(
            """
                {
                    "channel":"default/channel",
                    "events":["\"test\""],
                    "type":"publish",
                    "authorization":{
                        "host":"11111111111111111111111111.appsync-api.us-east-1.amazonaws.com",
                        "testKey":"default"
                    }
                }
            """.trimIndent()
        ).jsonObject

        val capturedSend = slot<String>()
        every { websocket.send(capture(capturedSend)) } answers {
            true.also {
                val sendObject = Json.parseToJsonElement(capturedSend.captured).jsonObject

                val compareSendObject = JsonObject(sendObject.filterKeys { it != "id" })
                compareSendObject shouldBe expectedSendData
                val id = sendObject["id"]
                val successResult = """
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

                launch(Dispatchers.IO) {
                    websocketListenerSlot.captured.onMessage(websocket, successResult)
                }
            }
        }

        val result = client.publish(
            "default/channel",
            JsonPrimitive("test")
        )

        (result is PublishResult.Response) shouldBe true
        (result as PublishResult.Response).let {
            it.successfulEvents.size shouldBe 1
            it.failedEvents.size shouldBe 0
        }
    }

    @Test
    fun `successful publish with custom authorizer`(): Unit = runBlocking(Dispatchers.IO) {
        val customAuthorizer = TestAuthorizer("c1")
        // assertion must remove id since that is not known
        val expectedSendData = Json.parseToJsonElement(
            """
                {
                    "channel":"default/channel",
                    "events":["\"test\""],
                    "type":"publish",
                    "authorization":{
                        "host":"11111111111111111111111111.appsync-api.us-east-1.amazonaws.com",
                        "testKey":"c1"
                    }
                }
            """.trimIndent()
        ).jsonObject

        val capturedSend = slot<String>()
        every { websocket.send(capture(capturedSend)) } answers {
            true.also {
                val sendObject = Json.parseToJsonElement(capturedSend.captured).jsonObject

                val compareSendObject = JsonObject(sendObject.filterKeys { it != "id" })
                compareSendObject shouldBe expectedSendData
                val id = sendObject["id"]
                val successResult = """
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
                launch(Dispatchers.IO) {
                    websocketListenerSlot.captured.onMessage(websocket, successResult)
                }
            }
        }

        val result = client.publish(
            "default/channel",
            JsonPrimitive("test"),
            customAuthorizer
        )

        (result is PublishResult.Response) shouldBe true
        (result as PublishResult.Response).let {
            it.successfulEvents.size shouldBe 1
            it.failedEvents.size shouldBe 0
        }
    }

    @Test
    fun `failed publish with connection closed`(): Unit = runBlocking(Dispatchers.IO) {
        // assertion must remove id since that is not known
        val expectedSendData = Json.parseToJsonElement(
            """
                {
                    "channel":"default/channel",
                    "events":["\"test\""],
                    "type":"publish",
                    "authorization":{
                        "host":"11111111111111111111111111.appsync-api.us-east-1.amazonaws.com",
                        "testKey":"default"
                    }
                }
            """.trimIndent()
        ).jsonObject

        val capturedSend = slot<String>()
        every { websocket.send(capture(capturedSend)) } answers {
            true.also {
                val sendObject = Json.parseToJsonElement(capturedSend.captured).jsonObject

                val compareSendObject = JsonObject(sendObject.filterKeys { it != "id" })
                compareSendObject shouldBe expectedSendData
                websocketListenerSlot.captured.onClosed(websocket, 1000, "User initiated disconnect")
            }
        }

        val result = client.publish(
            "default/channel",
            JsonPrimitive("test")
        )

        (result is PublishResult.Failure) shouldBe true
        (result as PublishResult.Failure).let {
            (it.error is ConnectionClosedException) shouldBe true
        }
    }

    @Test
    fun `failed publish with bad request error`(): Unit = runBlocking(Dispatchers.IO) {
        // assertion must remove id since that is not known
        val expectedSendData = Json.parseToJsonElement(
            """
                {
                    "channel":"default/*",
                    "events":["\"test\""],
                    "type":"publish",
                    "authorization":{
                        "host":"11111111111111111111111111.appsync-api.us-east-1.amazonaws.com",
                        "testKey":"default"
                    }
                }
            """.trimIndent()
        ).jsonObject

        val capturedSend = slot<String>()
        every { websocket.send(capture(capturedSend)) } answers {
            true.also {
                val sendObject = Json.parseToJsonElement(capturedSend.captured).jsonObject
                val compareSendObject = JsonObject(sendObject.filterKeys { it != "id" })
                compareSendObject shouldBe expectedSendData
                val id = sendObject["id"]
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
                launch(Dispatchers.IO) {
                    websocketListenerSlot.captured.onMessage(websocket, failedResult)
                }
            }
        }

        val result = client.publish(
            "default/*",
            JsonPrimitive("test")
        )

        (result is PublishResult.Failure) shouldBe true
        (result as PublishResult.Failure).let {
            (it.error is BadRequestException) shouldBe true
        }
    }
}
