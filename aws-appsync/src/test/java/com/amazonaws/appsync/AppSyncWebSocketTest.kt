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

import app.cash.turbine.test
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests [AppSyncWebSocket] by driving its [WebSocketListener] callbacks directly against a mocked
 * OkHttp socket.
 *
 * This is the pattern the rest of the repo uses for WebSocket unit tests — `EventsWebSocketTest` mocks
 * `OkHttpClient` and `WebSocket` outright, and `LivenessWebSocketTest`'s server-side listener is
 * entirely empty because it invokes the client listener itself. Neither relies on MockWebServer
 * delivering server-to-client frames; I tried that first and the frames never arrive (the upgrade
 * succeeds with a 101, then the connection resets). Real-socket behaviour is covered by instrumented
 * tests instead.
 *
 * The handshake ordering is what these mostly pin. `connection_ack` is delivered *synchronously* from
 * inside `newWebSocket`, i.e. before the caller reaches its `await` — a harsher ordering than a real
 * server produces, and exactly the race that loses the ack if collection has not already started.
 *
 * Robolectric is required because the handshake sets a User-Agent, and building one reads
 * `android.os.Build`.
 */
@RunWith(RobolectricTestRunner::class)
class AppSyncWebSocketTest {

    private val client = mockk<OkHttpClient>()
    private val rawSocket = mockk<WebSocket>(relaxed = true)
    private val requestSlot = slot<Request>()

    /**
     * Makes `newWebSocket` play a scripted exchange the moment the socket is created: `onOpen`,
     * then each frame in [frames].
     */
    private fun scriptServer(vararg frames: String) {
        every { client.newWebSocket(capture(requestSlot), any()) } answers {
            val listener = secondArg<WebSocketListener>()
            listener.onOpen(rawSocket, mockk(relaxed = true))
            frames.forEach { listener.onMessage(rawSocket, it) }
            rawSocket
        }
    }

    private fun webSocket(authorizer: AppSyncClientAuthorizer = AppSyncClientAuthorizer.ApiKey("da2-fakekey")) =
        AppSyncWebSocket(
            realtimeUrl = "wss://abc123.appsync-realtime-api.us-east-1.amazonaws.com/graphql",
            httpEndpoint = "https://abc123.appsync-api.us-east-1.amazonaws.com/graphql",
            authorizer = authorizer,
            decorator = AppSyncRequestDecorator("us-east-1"),
            client = client
        )

    // ── Handshake ───────────────────────────────────────────────────────

    @Test
    fun `connect completes once the service acknowledges`() = runTest {
        scriptServer(ACK)

        val socket = webSocket()
        socket.connect()

        socket.isClosed shouldBe false
    }

    @Test
    fun `an ack delivered before the caller awaits is not missed`() = runTest {
        // The ordering bug this guards: the shared flow has no replay, so if collection has not begun
        // before the socket opens, a synchronous ack is lost and connect() hangs forever.
        scriptServer(ACK)

        webSocket().connect()
    }

    @Test
    fun `connection_init is sent unprompted on open`() = runTest {
        scriptServer(ACK)

        webSocket().connect()

        verify { rawSocket.send(match<String> { it.contains("connection_init") }) }
    }

    @Test
    fun `the upgrade request carries the subprotocol, user agent and auth headers`() = runTest {
        scriptServer(ACK)

        webSocket().connect()

        val request = requestSlot.captured
        request.header("Sec-WebSocket-Protocol") shouldBe "graphql-ws"
        request.header("x-api-key") shouldBe "da2-fakekey"
        request.header("User-Agent").isNullOrBlank() shouldBe false
        request.url.toString() shouldContain "appsync-realtime-api"
    }

    @Test
    fun `a keep-alive before the ack does not satisfy the handshake`() = runTest {
        // A ka proves liveness but not readiness; only the ack means the connection can carry
        // subscriptions.
        scriptServer(KEEP_ALIVE, ACK)

        webSocket().connect()
    }

    @Test
    fun `a silent server times the handshake out rather than hanging`() = runTest {
        // OkHttp leaves an upgraded WebSocket with no read timeout, so without this bound a server that
        // completes the 101 and then says nothing would suspend connect() forever — and every later
        // subscriber would join the same hung attempt.
        every { client.newWebSocket(any(), any()) } answers {
            secondArg<WebSocketListener>().onOpen(rawSocket, mockk(relaxed = true))
            rawSocket
        }

        val error = shouldThrow<AppSyncTimeoutException> {
            webSocket().connect(handshakeTimeout = 5.seconds)
        }

        error.message shouldContain "5s"
    }

    // ── Handshake failures ──────────────────────────────────────────────

    @Test
    fun `a connection_error fails the connect with the service reason`() = runTest {
        scriptServer("""{"type":"connection_error","payload":{"errors":[{"message":"Unauthorized"}]}}""")

        val error = shouldThrow<AppSyncConnectionException> { webSocket().connect() }

        error.message shouldContain "Unauthorized"
    }

    @Test
    fun `a connection_error with no reason still fails cleanly`() = runTest {
        scriptServer("""{"type":"connection_error"}""")

        shouldThrow<AppSyncConnectionException> { webSocket().connect() }
            .message shouldContain "no reason given"
    }

    @Test
    fun `a socket closing during the handshake fails the connect rather than hanging`() = runTest {
        every { client.newWebSocket(any(), any()) } answers {
            val listener = secondArg<WebSocketListener>()
            listener.onOpen(rawSocket, mockk(relaxed = true))
            listener.onClosed(rawSocket, 1000, "server went away")
            rawSocket
        }

        shouldThrow<AppSyncConnectionException> { webSocket().connect() }
    }

    @Test
    fun `a socket failure during the handshake surfaces the cause`() = runTest {
        val cause = java.io.IOException("connection reset")
        every { client.newWebSocket(any(), any()) } answers {
            val listener = secondArg<WebSocketListener>()
            listener.onFailure(rawSocket, cause, null)
            rawSocket
        }

        val error = shouldThrow<AppSyncConnectionException> { webSocket().connect() }

        error.cause shouldBe cause
    }

    @Test
    fun `a failing authorizer fails the connect before any socket is opened`() = runTest {
        shouldThrow<AppSyncTokenFetchException> {
            webSocket(AppSyncClientAuthorizer.UserPools { error("session expired") }).connect()
        }

        verify(exactly = 0) { client.newWebSocket(any(), any()) }
    }

    // ── Message routing ─────────────────────────────────────────────────

    @Test
    fun `inbound frames reach the messages flow`() = runTest {
        scriptServer(ACK)
        val socket = webSocket()
        socket.connect()

        // Turbine subscribes before the block runs. A plain `async { first { } }` does not start until
        // a suspension point, so the frame below would be emitted into a flow with no subscriber and
        // lost — the same no-replay race the handshake has to defend against.
        socket.messages.test {
            socket.onMessage(rawSocket, """{"type":"data","id":"sub-1","payload":{"data":{"x":1}}}""")

            val data = awaitItem().shouldBeInstanceOf<AppSyncWebSocketMessage.Data>()
            data.id shouldBe "sub-1"
            data.payload shouldContain "\"x\""
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a malformed frame becomes Unknown and leaves the connection open`() = runTest {
        scriptServer(ACK)
        val socket = webSocket()
        socket.connect()

        socket.messages.test {
            socket.onMessage(rawSocket, "{not json")

            awaitItem().shouldBeInstanceOf<AppSyncWebSocketMessage.Unknown>()
            cancelAndIgnoreRemainingEvents()
        }

        // The point: one bad frame must not take down the subscriptions sharing this socket.
        socket.isClosed shouldBe false
    }

    @Test
    fun `send serializes the message onto the socket`() = runTest {
        scriptServer(ACK)
        val socket = webSocket()
        socket.connect()

        socket.send(AppSyncWebSocketMessage.Stop("sub-9"))

        verify { rawSocket.send(match<String> { it.contains("\"stop\"") && it.contains("sub-9") }) }
    }

    @Test
    fun `send returns false before the socket is open`() {
        webSocket().send(AppSyncWebSocketMessage.Stop("sub-1")) shouldBe false
    }

    // ── Closure ─────────────────────────────────────────────────────────

    @Test
    fun `a failure after connect closes the socket and emits Closed with the cause`() = runTest {
        scriptServer(ACK)
        val socket = webSocket()
        socket.connect()

        socket.messages.test {
            socket.onFailure(rawSocket, java.io.IOException("dropped"), null)

            val message = awaitItem().shouldBeInstanceOf<AppSyncWebSocketMessage.Closed>()
            message.cause.shouldBeInstanceOf<AppSyncConnectionException>()
            cancelAndIgnoreRemainingEvents()
        }

        socket.isClosed shouldBe true
    }

    @Test
    fun `a clean close emits Closed with no cause`() = runTest {
        scriptServer(ACK)
        val socket = webSocket()
        socket.connect()

        socket.messages.test {
            socket.onClosed(rawSocket, 1000, "done")

            awaitItem().shouldBeInstanceOf<AppSyncWebSocketMessage.Closed>().cause shouldBe null
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `closure is reported once, not per callback`() = runTest {
        // OkHttp can deliver onFailure and onClosed for the same death; subscribers must not see two.
        scriptServer(ACK)
        val socket = webSocket()
        socket.connect()

        socket.onClosed(rawSocket, 1000, "first")
        socket.onClosed(rawSocket, 1000, "second")
        socket.onFailure(rawSocket, java.io.IOException("also this"), null)

        socket.isClosed shouldBe true
    }

    @Test
    fun `disconnect closes the socket`() = runTest {
        scriptServer(ACK)
        // close() on the mock does not call back, so the listener is driven directly, mirroring how
        // LivenessWebSocketTest does it.
        val socket = webSocket()
        socket.connect()
        every { rawSocket.close(any(), any()) } answers {
            socket.onClosed(rawSocket, firstArg(), secondArg() ?: "")
            true
        }

        socket.disconnect()

        socket.isClosed shouldBe true
        verify { rawSocket.close(1000, any()) }
    }

    @Test
    fun `disconnect on a socket that never connected returns rather than hanging`() = runTest {
        webSocket().disconnect()
    }

    private companion object {
        const val ACK = """{"type":"connection_ack","payload":{"connectionTimeoutMs":300000}}"""
        const val KEEP_ALIVE = """{"type":"ka"}"""
    }
}
