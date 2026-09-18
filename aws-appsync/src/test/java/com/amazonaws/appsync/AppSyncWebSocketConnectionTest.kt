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
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests [AppSyncWebSocket] over a **real** WebSocket connection to a local server.
 *
 * The distinction from [AppSyncWebSocketTest] is what is exercised, not what is asserted.
 * [AppSyncWebSocketTest] invokes the [WebSocketListener] callbacks itself, so no socket is ever
 * opened and no HTTP upgrade is ever performed — which means it cannot see the upgrade request the
 * client actually sends. These tests complete a genuine HTTP upgrade and receive genuine
 * server-pushed frames, so they cover the two things that mocking hides: the contents of the
 * upgrade request, and whether an unprompted server frame is received at all.
 *
 * `AppSyncWebSocket` builds a `wss://` URL in production. The local server speaks plain HTTP, so
 * these tests construct the socket directly with a `ws://` URL rather than going through
 * [AmplifyAppSyncClient].
 *
 * Robolectric is required because the upgrade request sets a User-Agent, and building one reads
 * `android.os.Build`.
 */
@RunWith(RobolectricTestRunner::class)
class AppSyncWebSocketConnectionTest {

    private lateinit var server: MockWebServer

    /** The server's end of the connection, available once the upgrade completes. */
    private val serverSocket = CompletableDeferred<WebSocket>()

    @Before
    fun setUp() {
        server = MockWebServer()
        // start() is explicit: url() no longer starts the server implicitly.
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    /**
     * A server that answers `connection_init` with [initReply], mimicking what AppSync does.
     *
     * @param initReply The frame to push in response to `connection_init`, or null to accept the
     *   upgrade and then stay silent.
     */
    private fun enqueueServer(initReply: String? = ACK) {
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                serverSocket.complete(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (initReply != null && text.contains(AppSyncWebSocketMessage.TYPE_CONNECTION_INIT)) {
                    webSocket.send(initReply)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                // Completes the closing handshake. Without this the server's end stays half-open and
                // MockWebServer.close() fails, having waited out its shutdown timeout.
                webSocket.close(code, null)
            }
        }
        server.enqueue(MockResponse.Builder().webSocketUpgrade(listener).build())
    }

    /**
     * Runs a test body on real threads.
     *
     * `runTest` is wrong here: its virtual clock advances as soon as every coroutine is idle, and a
     * coroutine awaiting real network I/O looks idle — so the handshake timeout would fire while the
     * socket was still connecting. Typed to Unit so a test body cannot accidentally return a value,
     * which JUnit rejects.
     */
    private fun runSocketTest(body: suspend () -> Unit) = runBlocking { body() }

    private fun webSocket(authorizer: AppSyncClientAuthorizer = AppSyncClientAuthorizer.ApiKey(API_KEY)) =
        AppSyncWebSocket(
            // The server speaks ws://, so the scheme is swapped rather than taken from the parser.
            realtimeUrl = server.url("/graphql").toString().replaceFirst("http://", "ws://"),
            httpEndpoint = HTTP_ENDPOINT,
            authorizer = authorizer,
            decorator = AppSyncRequestDecorator("us-east-1"),
            client = OkHttpClient()
        )

    // ── Handshake ───────────────────────────────────────────────────────

    @Test
    fun `the handshake completes over a real socket`() = runSocketTest {
        // The ack here is genuinely pushed by the server over the wire, unprompted by any read on the
        // client's side. Nothing but a real connection demonstrates that path works.
        enqueueServer()

        val socket = webSocket()
        socket.connect(handshakeTimeout = HANDSHAKE_TIMEOUT)

        socket.isClosed shouldBe false

        socket.disconnect()
    }

    @Test
    fun `the upgrade request carries the host entry the service authorizes against`() = runSocketTest {
        // The regression this exists for: a missing `host` entry made every non-IAM auth mode fail at
        // connection time. It is invisible to a mocked client, because nothing there ever inspects the
        // request that would have gone out.
        enqueueServer()

        val socket = webSocket()
        socket.connect(handshakeTimeout = HANDSHAKE_TIMEOUT)

        val request = server.takeRequest(TAKE_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS).shouldNotBeNull()
        // Signed against the HTTP endpoint, not the socket's own host, which is why it has to be sent
        // explicitly rather than left to OkHttp.
        request.headers["host"] shouldBe "abc123.appsync-api.us-east-1.amazonaws.com"
        request.headers["x-api-key"] shouldBe API_KEY
        request.headers["Sec-WebSocket-Protocol"] shouldBe "graphql-ws"
        request.headers["User-Agent"].isNullOrBlank() shouldBe false

        socket.disconnect()
    }

    @Test
    fun `a server that accepts the upgrade and goes silent times the handshake out`() = runSocketTest {
        // An upgraded socket carries no read timeout of its own, so this bound is the only thing
        // between a silent service and a permanently suspended connect().
        enqueueServer(initReply = null)

        val error = shouldThrow<AppSyncTimeoutException> {
            webSocket().connect(handshakeTimeout = 2.seconds)
        }

        error.message shouldContain "2s"
    }

    @Test
    fun `a connection_error from the service fails the connect`() = runSocketTest {
        enqueueServer(initReply = """{"type":"connection_error","payload":{"errors":[{"message":"Unauthorized"}]}}""")

        val error = shouldThrow<AppSyncConnectionException> {
            webSocket().connect(handshakeTimeout = HANDSHAKE_TIMEOUT)
        }

        error.message shouldContain "Unauthorized"
    }

    // ── Server-pushed frames ────────────────────────────────────────────

    @Test
    fun `a keep-alive pushed by the server reaches the messages flow`() = runSocketTest {
        // The keep-alive is what the watchdog is driven by, and it arrives entirely at the service's
        // initiative — there is no client message it answers.
        enqueueServer()

        val socket = webSocket()
        socket.connect(handshakeTimeout = HANDSHAKE_TIMEOUT)

        // Turbine subscribes before the block body runs, which matters: the flow has no replay, so a
        // frame pushed before collection began would be lost.
        socket.messages.test(timeout = FLOW_TIMEOUT) {
            serverSocket.await().send(KEEP_ALIVE)

            awaitItem().shouldBeInstanceOf<AppSyncWebSocketMessage.KeepAlive>()
            cancelAndIgnoreRemainingEvents()
        }

        socket.isClosed shouldBe false

        socket.disconnect()
    }

    @Test
    fun `a data frame pushed by the server reaches the messages flow`() = runSocketTest {
        enqueueServer()

        val socket = webSocket()
        socket.connect(handshakeTimeout = HANDSHAKE_TIMEOUT)

        socket.messages.test(timeout = FLOW_TIMEOUT) {
            serverSocket.await().send("""{"type":"data","id":"sub-1","payload":{"data":{"x":1}}}""")

            val data = awaitItem().shouldBeInstanceOf<AppSyncWebSocketMessage.Data>()
            data.id shouldBe "sub-1"
            data.payload shouldContain "\"x\""
            cancelAndIgnoreRemainingEvents()
        }

        socket.disconnect()
    }

    // ── Closure ─────────────────────────────────────────────────────────

    @Test
    fun `a close initiated by the server settles terminal state`() = runSocketTest {
        enqueueServer()

        val socket = webSocket()
        socket.connect(handshakeTimeout = HANDSHAKE_TIMEOUT)

        serverSocket.await().close(1000, "server is done")

        // A clean close carries no cause. Awaiting settled state rather than a flow message is what
        // makes this safe to observe however late it happens.
        socket.closure.await() shouldBe null
        socket.isClosed shouldBe true
    }

    @Test
    fun `disconnect closes a real socket and settles terminal state`() = runSocketTest {
        enqueueServer()

        val socket = webSocket()
        socket.connect(handshakeTimeout = HANDSHAKE_TIMEOUT)

        socket.disconnect()

        socket.isClosed shouldBe true
        socket.closure.await() shouldBe null
    }

    private companion object {
        const val API_KEY = "da2-fakekey"
        const val HTTP_ENDPOINT = "https://abc123.appsync-api.us-east-1.amazonaws.com/graphql"
        const val ACK = """{"type":"connection_ack","payload":{"connectionTimeoutMs":300000}}"""
        const val KEEP_ALIVE = """{"type":"ka"}"""
        const val TAKE_REQUEST_TIMEOUT_SECONDS = 5L

        val HANDSHAKE_TIMEOUT = 10.seconds
        val FLOW_TIMEOUT = 10.seconds
    }
}
