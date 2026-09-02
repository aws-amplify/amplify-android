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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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

    @Volatile
    private var socket: WebSocket? = null

    @Volatile
    private var watchdog: Job? = null

    @Volatile
    private var watchdogTimeoutMs: Long = 0

    @Volatile
    var isClosed = false
        private set

    /** Set before the socket is torn down so the emitted [AppSyncWebSocketMessage.Closed] can explain why. */
    @Volatile
    private var pendingCloseCause: AppSyncException? = null

    /**
     * Completes when the socket dies, carrying the reason or null for a clean close.
     *
     * Waiting on this rather than on a [AppSyncWebSocketMessage.Closed] message removes a race: the
     * message flow has no replay, so anyone who subscribes after closure has already been emitted would
     * wait forever. This is settled state, so it can be awaited at any point and still be observed.
     */
    private val terminated = CompletableDeferred<AppSyncException?>()
    private val disconnectMutex = Mutex()

    /** Completes when the socket dies. See [terminated]. */
    val closure: Deferred<AppSyncException?> get() = terminated

    /**
     * Opens the socket and completes the AppSync handshake.
     *
     * @param handshakeTimeout How long to wait for the service to acknowledge. Bounding this matters
     *   because OkHttp leaves an upgraded WebSocket with no read timeout, so a server that completes
     *   the upgrade and then goes silent would otherwise suspend this call indefinitely.
     * @throws AppSyncConnectionException if the connection is refused or closes during the handshake.
     * @throws AppSyncTimeoutException if the service does not acknowledge within [handshakeTimeout].
     */
    suspend fun connect(handshakeTimeout: Duration = DEFAULT_HANDSHAKE_TIMEOUT): Unit = coroutineScope {
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

        val result = try {
            withTimeout(handshakeTimeout) { handshake.await() }
        } catch (timeout: TimeoutCancellationException) {
            teardown()
            throw AppSyncTimeoutException(
                message = "AppSync did not acknowledge the subscription connection within " +
                    "${handshakeTimeout.inWholeSeconds}s.",
                cause = timeout
            )
        }

        when (result) {
            is AppSyncWebSocketMessage.ConnectionAck -> resetWatchdog(result.connectionTimeoutMs)
            is AppSyncWebSocketMessage.ConnectionError -> {
                teardown()
                throw AppSyncConnectionException(
                    message = "AppSync refused the subscription connection: " +
                        result.errors.joinToString("; ") { it.message }.ifEmpty { "no reason given" }
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
     * being gone once this returns. Idempotent, including under concurrent callers.
     *
     * @param cause Why it is being closed, or null for a clean shutdown.
     */
    suspend fun disconnect(cause: AppSyncException? = null): Unit = withContext(ioDispatcher) {
        // Serialized so two concurrent callers cannot both pass the isClosed check, leaving the second
        // awaiting a closure notification that has already been delivered.
        disconnectMutex.withLock {
            if (!isClosed) {
                pendingCloseCause = cause
                socket?.close(NORMAL_CLOSURE, "Client closed the connection")
                    // Never opened, so no close frame will come back — synthesize one.
                    ?: handleClosed()
            }
        }
        // Settled state rather than a message, so this is safe however late it is awaited.
        terminated.await()
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

    /**
     * Every closure path funnels through here, so this is where the socket's resources are released and
     * its terminal state is published.
     */
    private fun handleClosed() {
        if (isClosed) return
        isClosed = true
        watchdog?.cancel()
        val cause = pendingCloseCause
        messageFlow.tryEmit(AppSyncWebSocketMessage.Closed(cause))
        terminated.complete(cause)
        // The scope's lifetime is the socket's. Cancelling here rather than only in disconnect() covers
        // a socket that dies on its own, which is the common case.
        scope.cancel()
    }

    private fun teardown() {
        socket?.cancel()
        handleClosed()
    }

    private companion object {
        const val SUBPROTOCOL_HEADER = "Sec-WebSocket-Protocol"
        const val GRAPHQL_WS_SUBPROTOCOL = "graphql-ws"
        const val USER_AGENT_HEADER = "User-Agent"
        const val NORMAL_CLOSURE = 1000

        // Matches the plugin's connection acknowledgement bound.
        val DEFAULT_HANDSHAKE_TIMEOUT = 30.seconds
    }
}
