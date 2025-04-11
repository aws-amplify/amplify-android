/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.aws.appsync.events

import com.amplifyframework.aws.appsync.core.AppSyncAuthorizer
import com.amplifyframework.aws.appsync.core.AppSyncRequest
import com.amplifyframework.aws.appsync.core.util.Logger
import com.amplifyframework.aws.appsync.events.data.ConnectException
import com.amplifyframework.aws.appsync.events.data.EventsException
import com.amplifyframework.aws.appsync.events.data.WebSocketMessage
import com.amplifyframework.aws.appsync.events.utils.HeaderKeys
import com.amplifyframework.aws.appsync.events.utils.HeaderValues
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicBoolean

internal class EventsWebSocket(
    private val eventsEndpoints: EventsEndpoints,
    private val authorizer: AppSyncAuthorizer,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val logger: Logger?
) : WebSocketListener() {

    private val _events = MutableSharedFlow<WebSocketMessage>(extraBufferCapacity = Int.MAX_VALUE)
    val events = _events.asSharedFlow() // publicly exposed as read-only shared flow

    private lateinit var webSocket: WebSocket
    internal val isClosed = AtomicBoolean(false)
    private var userInitiatedDisconnect = false

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
                throw ConnectException(connectionResponse.throwable)
            }
            is WebSocketMessage.Received.ConnectionError -> {
                webSocket.cancel()
                throw ConnectException(
                    connectionResponse.errors.firstOrNull()?.toEventsException()
                        ?: EventsException.unknown()
                )
            }
            else -> Unit // It isn't obvious here, but only other connect response type is ConnectionAck
        }
        logger?.debug("Websocket Connection Open")
    }

    suspend fun disconnect(flushEvents: Boolean) = coroutineScope {
        userInitiatedDisconnect = true
        val deferredClosedResponse = async { getClosedResponse() }
        when (flushEvents) {
            true -> webSocket.close(NORMAL_CLOSE_CODE, "User initiated disconnect")
            false -> webSocket.cancel()
        }
        deferredClosedResponse.await()
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        val connectionInitMessage = json.encodeToString(WebSocketMessage.Send.ConnectionInit())
        logger?.debug { "$TAG onOpen: sending connection init" }
        webSocket.send(connectionInitMessage)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        logger?.debug { "Websocket onMessage: $text" }
        try {
            val eventMessage = json.decodeFromString<WebSocketMessage.Received>(text)
            _events.tryEmit(eventMessage)
        } catch (e: Exception) {
            logger?.error(e) { "Websocket onMessage: exception encountered" }
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        logger?.error(t) { "$TAG onFailure" }
        notifyClosed() // onClosed doesn't get called in failure. Treat this block the same as onClosed
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        logger?.debug("$TAG onClosing")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        // Events api sends normal close code even in failure
        // so inspecting code/reason isn't helpful as it should be
        logger?.debug("$TAG onClosed: userInitiated = $userInitiatedDisconnect")
        notifyClosed()
    }

    private fun notifyClosed() {
        _events.tryEmit(WebSocketMessage.Closed(userInitiated = userInitiatedDisconnect))
        isClosed.set(true)
    }

    inline fun <reified T : WebSocketMessage> send(webSocketMessage: T) {
        val message = json.encodeToString(webSocketMessage)
        logger?.debug("$TAG send: $message")
        webSocket.send(message)
    }

    companion object {
        const val TAG = "EventsWebSocket"
        const val NORMAL_CLOSE_CODE = 1000

        private fun createPreAuthConnectRequest(eventsEndpoints: EventsEndpoints): Request {
            return Request.Builder().apply {
                url(eventsEndpoints.websocketRealtimeEndpoint)
                addHeader(HeaderKeys.SEC_WEBSOCKET_PROTOCOL, HeaderValues.SEC_WEBSOCKET_PROTOCOL_APPSYNC_EVENTS)
                addHeader(HeaderKeys.HOST, eventsEndpoints.restEndpoint.host)
            }.build()
        }

        private fun createAuthConnectRequest(preAuthRequest: Request, authorizerHeaders: Map<String, String>): Request {
            return preAuthRequest.newBuilder().apply {
                authorizerHeaders.forEach {
                    header(it.key, it.value)
                }
            }.build()
        }
    }

    private suspend fun getConnectResponse(): WebSocketMessage {
        return events.first {
            when (it) {
                is WebSocketMessage.Received.ConnectionAck -> true
                is WebSocketMessage.Received.ConnectionError -> true
                is WebSocketMessage.Closed -> true
                else -> false
            }
        }
    }

    private suspend fun getClosedResponse(): WebSocketMessage {
        return events.first {
            when (it) {
                is WebSocketMessage.Closed -> true
                else -> false
            }
        }
    }
}

private class ConnectAppSyncRequest(
    val eventsEndpoints: EventsEndpoints,
    val preAuthRequest: Request
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
