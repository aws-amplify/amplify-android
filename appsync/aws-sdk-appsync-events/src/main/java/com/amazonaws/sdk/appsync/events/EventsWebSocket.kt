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

import com.amazonaws.sdk.appsync.core.AppSyncAuthorizer
import com.amazonaws.sdk.appsync.core.AppSyncRequest
import com.amazonaws.sdk.appsync.core.LoggerProvider
import com.amazonaws.sdk.appsync.events.data.ConnectException
import com.amazonaws.sdk.appsync.events.data.EventsException
import com.amazonaws.sdk.appsync.events.data.WebSocketMessage
import com.amazonaws.sdk.appsync.events.data.toEventsException
import com.amazonaws.sdk.appsync.events.utils.ConnectionTimeoutTimer
import com.amazonaws.sdk.appsync.events.utils.HeaderKeys
import com.amazonaws.sdk.appsync.events.utils.HeaderValues
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.jetbrains.annotations.VisibleForTesting

internal class EventsWebSocket(
    private val eventsEndpoints: EventsEndpoints,
    private val authorizer: AppSyncAuthorizer,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    loggerProvider: LoggerProvider?
) : WebSocketListener() {

    private val _events = MutableSharedFlow<WebSocketMessage>(extraBufferCapacity = Int.MAX_VALUE)
    val events = _events.asSharedFlow() // publicly exposed as read-only shared flow

    private lateinit var webSocket: WebSocket

    @Volatile internal var isClosed = false
    internal var disconnectReason: WebSocketDisconnectReason? = null
    private val connectionTimeoutTimer = ConnectionTimeoutTimer(
        scope = CoroutineScope(Dispatchers.IO),
        onTimeout = ::onTimeout
    )
    val preAuthPublishHeaders: Map<String, String> by lazy { mapOf(HeaderKeys.HOST to eventsEndpoints.host) }
    private val logger = loggerProvider?.getLogger(TAG)

    @Throws(ConnectException::class)
    suspend fun connect() = coroutineScope {
        logger?.debug("Opening Websocket Connection")
        // Get deferred connect response. We need to listen before opening connection, but not block
        val deferredConnectResponse = async { getConnectResponse() }
        // Create initial request without auth headers
        val preAuthRequest = createPreAuthConnectRequest(eventsEndpoints)
        // Fetch auth headers from authorizer
        val authorizerHeaders = authorizer.getAuthorizationHeaders(
            ConnectAppSyncRequest(eventsEndpoints, preAuthRequest)
        )
        // Attach auth headers to request
        val authRequest = createAuthConnectRequest(preAuthRequest, authorizerHeaders)
        // Open websocket
        webSocket = okHttpClient.newWebSocket(authRequest, this@EventsWebSocket)
        // Handle connection response and update status
        when (val connectionResponse = deferredConnectResponse.await()) {
            is WebSocketMessage.Closed -> {
                webSocket.cancel()
                throw ConnectException(connectionResponse.reason.throwable)
            }
            is WebSocketMessage.ErrorContainer -> {
                webSocket.cancel()
                throw ConnectException(
                    connectionResponse.errors.firstOrNull()?.toEventsException()
                        ?: EventsException.unknown()
                )
            }
            is WebSocketMessage.Received.ConnectionAck -> {
                connectionTimeoutTimer.resetTimeoutTimer(connectionResponse.connectionTimeoutMs)
            }
            else -> Unit // Not obvious here but this block should never run
        }
        logger?.debug("Websocket Connection Open")
    }

    suspend fun disconnect(flushEvents: Boolean) = withContext(Dispatchers.IO) {
        disconnectReason = WebSocketDisconnectReason.UserInitiated
        val deferredClosedResponse = async { getClosedResponse() }
        when (flushEvents) {
            true -> webSocket.close(NORMAL_CLOSE_CODE, "User initiated disconnect")
            false -> webSocket.cancel()
        }
        deferredClosedResponse.await()
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        logger?.debug("onOpen: sending connection init")
        try {
            send(WebSocketMessage.Send.ConnectionInit())
        } catch (e: Exception) {
            logger?.error("onOpen: exception encountered", e) // do nothing. closure will handle error
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        connectionTimeoutTimer.resetTimeoutTimer()
        logger?.debug("Websocket onMessage received")
        try {
            val eventMessage = json.decodeFromString<WebSocketMessage.Received>(text)
            emitEvent(eventMessage)
        } catch (e: Exception) {
            logger?.error(e) { "Websocket onMessage: exception encountered" }
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        logger?.error(t) { "onFailure" }
        handleClosed() // onClosed doesn't get called in failure. Treat this block the same as onClosed
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        logger?.debug("onClosing")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        // Events api sends normal close code even in failure
        // so inspecting code/reason isn't helpful as it should be
        logger?.debug { "onClosed: reason = $disconnectReason" }
        handleClosed()
    }

    private fun onTimeout() {
        disconnectReason = WebSocketDisconnectReason.Timeout
        webSocket.cancel()
    }

    private fun handleClosed() {
        connectionTimeoutTimer.stop()
        emitEvent(WebSocketMessage.Closed(reason = disconnectReason ?: WebSocketDisconnectReason.Service()))
        isClosed = true
    }

    // returns true if websocket queued up event. false if failed
    internal suspend inline fun <reified T : WebSocketMessage.Send> sendWithAuthorizer(
        webSocketMessage: T,
        authorizer: AppSyncAuthorizer
    ): Boolean {
        // The base message will not include id, type, or authorization fields
        val baseMessageJson = json.encodeToJsonElement(webSocketMessage).jsonObject

        // Create the authorization headers first
        val authHeaders = authorizer.getAuthorizationHeaders(object : AppSyncRequest {
            override val method = AppSyncRequest.HttpMethod.POST
            override val url = eventsEndpoints.restEndpoint.toString()
            override val headers = preAuthPublishHeaders
            override val body = json.encodeToString(baseMessageJson)
        })

        // We reconstruct the message, adding in the id, type, and authorization fields
        val message = json.encodeToString(
            JsonObject(
                buildMap {
                    putAll(baseMessageJson)
                    put("id", JsonPrimitive(webSocketMessage.id))
                    put("type", JsonPrimitive(webSocketMessage.type))
                    put(
                        "authorization",
                        JsonObject((preAuthPublishHeaders + authHeaders).mapValues { JsonPrimitive(it.value) })
                    )
                }
            )
        )

        return send(message)
    }

    // returns true if websocket queued up event. false if failed
    inline fun <reified T : WebSocketMessage> send(webSocketMessage: T): Boolean =
        send(json.encodeToString(webSocketMessage))

    // returns true if websocket queued up event. false if failed
    private fun send(eventJson: String): Boolean {
        logger?.debug("sending event over websocket")
        return webSocket.send(eventJson)
    }

    private fun emitEvent(event: WebSocketMessage) {
        logger?.debug { "emit ${event::class.java}" }
        _events.tryEmit(event)
    }

    companion object {
        const val TAG = "EventsWebSocket"
        const val NORMAL_CLOSE_CODE = 1000

        private fun createPreAuthConnectRequest(eventsEndpoints: EventsEndpoints): Request = Request.Builder().apply {
            url(eventsEndpoints.websocketRealtimeEndpoint)
            addHeader(HeaderKeys.SEC_WEBSOCKET_PROTOCOL, HeaderValues.SEC_WEBSOCKET_PROTOCOL_APPSYNC_EVENTS)
            addHeader(HeaderKeys.HOST, eventsEndpoints.restEndpoint.host)
            addHeader(HeaderKeys.X_AMZ_USER_AGENT, HeaderValues.USER_AGENT)
        }.build()

        private fun createAuthConnectRequest(preAuthRequest: Request, authorizerHeaders: Map<String, String>): Request =
            preAuthRequest.newBuilder().apply {
                authorizerHeaders.forEach {
                    header(it.key, it.value)
                }
            }.build()
    }

    private suspend fun getConnectResponse(): WebSocketMessage = events.first {
        when (it) {
            is WebSocketMessage.Received.ConnectionAck -> true
            is WebSocketMessage.Received.ConnectionError -> true
            is WebSocketMessage.Closed -> true
            else -> false
        }
    }

    private suspend fun getClosedResponse(): WebSocketMessage = events.first {
        when (it) {
            is WebSocketMessage.Closed -> true
            else -> false
        }
    }
}

@VisibleForTesting
internal class ConnectAppSyncRequest(
    private val eventsEndpoints: EventsEndpoints,
    private val preAuthRequest: Request
) : AppSyncRequest {
    override val method: AppSyncRequest.HttpMethod
        get() = AppSyncRequest.HttpMethod.POST
    override val url: String
        get() = eventsEndpoints.websocketBaseEndpoint.toString()
    override val headers: Map<String, String>
        get() = preAuthRequest.headers.toMap()
    override val body: String
        get() = "{}"
}
