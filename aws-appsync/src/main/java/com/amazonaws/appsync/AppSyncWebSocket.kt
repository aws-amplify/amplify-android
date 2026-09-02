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

import com.amplifyframework.util.UserAgent
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * One AppSync realtime WebSocket connection, shared by every subscription on a client.
 *
 * A private reimplementation of the plugin's `SubscriptionEndpoint`, which is 720 lines of Java built
 * around locks, latches and callbacks. This exposes a single [messages] flow instead and lets callers
 * filter it, which is what makes per-subscription multiplexing a `filter` rather than a registry.
 *
 * Deliberately has no reconnection logic: a dead connection surfaces as a terminal error and the
 * consumer re-subscribes.
 *
 * @param realtimeUrl The `wss://` URL, from [AppSyncEndpointParser.realtimeUrl].
 * @param httpEndpoint The HTTP endpoint, needed because SigV4 signs against it rather than the socket.
 * @param authorizer Supplies the connection's credentials.
 * @param decorator Produces the authorization headers.
 * @param client The OkHttp client to open the socket with.
 */
internal class AppSyncWebSocket(
    private val realtimeUrl: String,
    private val httpEndpoint: String,
    private val authorizer: AppSyncClientAuthorizer,
    private val decorator: AppSyncRequestDecorator,
    private val client: OkHttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : WebSocketListener() {

    // extraBufferCapacity keeps tryEmit from ever dropping a frame: emission happens on OkHttp's
    // callback thread, which cannot suspend to wait for a slow collector.
    private val messageFlow = MutableSharedFlow<AppSyncWebSocketMessage.Inbound>(
        extraBufferCapacity = Int.MAX_VALUE
    )

    /** Every inbound message, including the synthetic [AppSyncWebSocketMessage.Closed]. */
    val messages = messageFlow.asSharedFlow()

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
    private var socket: WebSocket? = null
    private var watchdog: Job? = null

    @Volatile
    var isClosed = false
        private set

    /** Set before the socket is torn down so the emitted [AppSyncWebSocketMessage.Closed] can explain why. */
    @Volatile
    private var pendingCloseCause: AppSyncException? = null

    /**
     * Opens the socket and completes the AppSync handshake.
     *
     * @throws AppSyncConnectionException if the connection is refused or closes during the handshake.
     * @throws AppSyncTimeoutException if no acknowledgement arrives.
     */
    suspend fun connect(): Unit = coroutineScope {
        // Collection has to be running before the socket opens, or a fast connection_ack lands before
        // anyone is listening and the await below waits forever. The shared flow has no replay.
        val listening = CompletableDeferred<Unit>()
        val handshake = async { awaitHandshake { listening.complete(Unit) } }
        listening.await()

        val request = try {
            buildConnectRequest()
        } catch (error: AppSyncException) {
            handshake.cancel()
            throw error
        }

        socket = client.newWebSocket(request, this@AppSyncWebSocket)

        when (val result = handshake.await()) {
            is AppSyncWebSocketMessage.ConnectionAck -> resetWatchdog(result.connectionTimeoutMs)
            is AppSyncWebSocketMessage.ConnectionError -> {
                teardown()
                throw AppSyncConnectionException(
                    message = "AppSync refused the subscription connection: " +
                        result.errors.joinToString("; ").ifEmpty { "no reason given" }
                )
            }
            is AppSyncWebSocketMessage.Closed -> {
                teardown()
                throw result.cause ?: AppSyncConnectionException(
                    message = "The subscription connection closed during the handshake."
                )
            }
            else -> {
                teardown()
                throw AppSyncConnectionException(message = "Unexpected handshake reply: $result.")
            }
        }
    }

    /**
     * Sends a message.
     *
     * @return false if the socket is not open or its send queue rejected the message.
     */
    fun send(message: AppSyncWebSocketMessage.Outbound): Boolean = socket?.send(message.toJson()) ?: false

    /**
     * Closes the socket and waits for closure to be observed, so a caller can rely on the connection
     * being gone once this returns. Idempotent.
     *
     * @param cause Why it is being closed, or null for a clean shutdown.
     */
    suspend fun disconnect(cause: AppSyncException? = null): Unit = withContext(ioDispatcher) {
        if (isClosed) return@withContext
        pendingCloseCause = cause

        val listening = CompletableDeferred<Unit>()
        val closed = async { awaitClosed { listening.complete(Unit) } }
        listening.await()

        socket?.close(NORMAL_CLOSURE, "Client closed the connection") ?: run {
            // Never opened, so no close frame will come back — synthesize one.
            handleClosed()
        }
        closed.await()
        scope.cancel()
    }

    // ── WebSocketListener ───────────────────────────────────────────────

    override fun onOpen(webSocket: WebSocket, response: Response) {
        // AppSync expects connection_init immediately; the ack is what makes the socket usable.
        webSocket.send(AppSyncWebSocketMessage.ConnectionInit.toJson())
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        // Any frame proves the connection is alive, including a keep-alive.
        restartWatchdog()
        messageFlow.tryEmit(AppSyncWebSocketMessage.parse(text))
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        // onClosed is not called after a failure, so this has to close things out itself.
        if (pendingCloseCause == null) {
            pendingCloseCause = when (t) {
                is IOException -> AppSyncConnectionException(
                    message = t.message ?: "The subscription connection failed.",
                    cause = t
                )
                else -> AppSyncException.from(t)
            }
        }
        handleClosed()
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = handleClosed()

    // ── Internals ───────────────────────────────────────────────────────

    private suspend fun buildConnectRequest(): Request {
        // body = null signs the handshake against {endpoint}/connect, which is what AppSync expects
        // for the connection as opposed to an individual subscription.
        val authHeaders = decorator.authorizationHeaders(authorizer, httpEndpoint, body = null)

        return Request.Builder()
            .url(realtimeUrl)
            .addHeader(SUBPROTOCOL_HEADER, GRAPHQL_WS_SUBPROTOCOL)
            .header(USER_AGENT_HEADER, UserAgent.string())
            .apply { authHeaders.forEach { (name, value) -> header(name, value) } }
            .build()
    }

    private suspend fun awaitHandshake(onListening: () -> Unit) = messages
        .onStart { onListening() }
        .first {
            it is AppSyncWebSocketMessage.ConnectionAck ||
                it is AppSyncWebSocketMessage.ConnectionError ||
                it is AppSyncWebSocketMessage.Closed
        }

    private suspend fun awaitClosed(onListening: () -> Unit) = messages
        .onStart { onListening() }
        .first { it is AppSyncWebSocketMessage.Closed }

    /**
     * AppSync closes an idle connection without warning, so the absence of traffic for longer than the
     * timeout it advertised is treated as a dead connection rather than waiting on a socket that will
     * never answer.
     */
    private fun resetWatchdog(timeoutMs: Long) {
        watchdogTimeoutMs = timeoutMs
        restartWatchdog()
    }

    private fun restartWatchdog() {
        val timeout = watchdogTimeoutMs
        if (timeout <= 0) return
        watchdog?.cancel()
        watchdog = scope.launch {
            delay(timeout)
            pendingCloseCause = AppSyncTimeoutException(
                message = "No traffic on the subscription connection for ${timeout}ms."
            )
            // cancel(), not close(): a timed-out socket is not expected to complete a closing handshake.
            socket?.cancel()
            handleClosed()
        }
    }

    @Volatile
    private var watchdogTimeoutMs: Long = 0

    private fun handleClosed() {
        if (isClosed) return
        isClosed = true
        watchdog?.cancel()
        messageFlow.tryEmit(AppSyncWebSocketMessage.Closed(pendingCloseCause))
    }

    private fun teardown() {
        socket?.cancel()
        handleClosed()
        scope.cancel()
    }

    private companion object {
        const val SUBPROTOCOL_HEADER = "Sec-WebSocket-Protocol"
        const val GRAPHQL_WS_SUBPROTOCOL = "graphql-ws"
        const val USER_AGENT_HEADER = "User-Agent"
        const val NORMAL_CLOSURE = 1000
    }
}
