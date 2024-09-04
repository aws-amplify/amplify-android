/*
 *  Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.apollo.appsync

import com.amplifyframework.apollo.appsync.SubscriptionMessageType.ConnectionAck
import com.amplifyframework.apollo.appsync.SubscriptionMessageType.ConnectionError
import com.amplifyframework.apollo.appsync.SubscriptionMessageType.ConnectionInit
import com.amplifyframework.apollo.appsync.SubscriptionMessageType.ConnectionKeepAlive
import com.amplifyframework.apollo.appsync.SubscriptionMessageType.SubscriptionAck
import com.amplifyframework.apollo.appsync.SubscriptionMessageType.SubscriptionComplete
import com.amplifyframework.apollo.appsync.SubscriptionMessageType.SubscriptionData
import com.amplifyframework.apollo.appsync.SubscriptionMessageType.SubscriptionError
import com.amplifyframework.apollo.appsync.SubscriptionMessageType.SubscriptionStart
import com.amplifyframework.apollo.appsync.SubscriptionMessageType.SubscriptionStop
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.exception.NullOrMissingField
import com.apollographql.apollo.exception.SubscriptionConnectionException
import com.apollographql.apollo.network.ws.WebSocketConnection
import com.apollographql.apollo.network.ws.WsProtocol
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Implementation of [WsProtocol] that implements the subscription protocol for AppSync.
 *
 * Pass an instance of the [Factory] class to WebSocketNetworkTransport to use this protocol when subscribing to
 * AppSync.
 */
class AppSyncProtocol internal constructor(
    private val endpoint: AppSyncEndpoint,
    private val authorizer: AppSyncAuthorizer,
    webSocketConnection: WebSocketConnection,
    listener: Listener,
    private val scope: CoroutineScope
) : WsProtocol(webSocketConnection, listener) {
    private val connectionAckTimeout = 10.seconds
    private val subscriptionAckTimeout = 10.seconds

    // Subscriptions for which we are awaiting an acknowledgement from AppSync
    private val pendingSubscriptions = mutableMapOf<String, CompletableJob>()

    override suspend fun connectionInit() {
        // See SubscriptionEndpoint.onOpen
        sendMessageMapText(mapOf("type" to ConnectionInit.value))
        waitForConnectionAck()
    }

    override fun handleServerMessage(messageMap: Map<String, Any?>) {
        val message = decodeServerMessage(messageMap)

        if (message == null) {
            // No type specified
            listener.generalError(messageMap)
            return
        }

        // See AmplifyWebsocketListener.processJsonMessage
        when (message.type) {
            SubscriptionAck -> pendingSubscriptions[message.id]?.complete()
            SubscriptionData -> when {
                message.id == null -> listener.generalError(message.payload)
                message.payload == null -> listener.operationError(message.id, null)
                else -> listener.operationResponse(message.id, message.payload)
            }
            SubscriptionError -> {
                if (message.id != null) {
                    listener.operationError(message.id, message.payload)
                } else {
                    listener.generalError(message.payload)
                }
            }
            SubscriptionComplete -> listener.operationComplete(message.id!!)
            ConnectionKeepAlive -> Unit // Connection keep-alive, nothing to do
            else -> Unit // other message, nothing to do
        }
    }

    override fun <D : Operation.Data> startOperation(request: ApolloRequest<D>) {
        scope.launch {
            val subscriptionId = request.subscriptionId

            // Prepare to wait for the subscription acknowledgement
            val subscriptionAck = Job()
            pendingSubscriptions[subscriptionId] = subscriptionAck

            // Get the request content as a string
            val data = request.toJson()

            // All authorization types require specifying the host. Append the authorizer-specific headers
            val authorization = mapOf(
                "host" to endpoint.serverUrl.host
            ) + authorizer.getWebSocketSubscriptionPayload(endpoint, request)

            // See SubscriptionEndpoint.startSubscription
            sendMessageMapText(
                mapOf(
                    "id" to subscriptionId,
                    "type" to SubscriptionStart.value,
                    "payload" to
                        mapOf(
                            "data" to data,
                            "extensions" to
                                mapOf(
                                    "authorization" to authorization
                                )
                        )
                )
            )

            // Wait for the subscription to be acknowledged. Close with an error if the ack is not received.
            try {
                withTimeout(subscriptionAckTimeout.inWholeMilliseconds) {
                    subscriptionAck.join()
                }
            } catch (e: Exception) {
                println("Error waiting for subscription to be acknowledged: $e")
                listener.operationError(subscriptionId, null)
            } finally {
                pendingSubscriptions.remove(subscriptionId)
            }
        }
    }

    override fun <D : Operation.Data> stopOperation(request: ApolloRequest<D>) {
        // See SubscriptionEndpoint.releaseSubscription
        sendMessageMapText(
            mapOf(
                "id" to request.subscriptionId,
                "type" to SubscriptionStop.value
            )
        )
    }

    private suspend fun waitForConnectionAck() = withTimeout(connectionAckTimeout.inWholeMilliseconds) {
        while (true) {
            val map = receiveMessageMap()
            val type = map["type"] as? String ?: throw NullOrMissingField("No AppSync message type specified: $map")
            when (type.messageType) {
                ConnectionAck -> return@withTimeout
                ConnectionError -> throw SubscriptionConnectionException(map)
                else -> continue
            }
        }
    }

    private fun decodeServerMessage(message: Map<String, Any?>): ServerMessage? {
        val type = (message["type"] as? String)?.messageType ?: return null
        val id = message["id"] as? String

        @Suppress("UNCHECKED_CAST")
        val payload = message["payload"] as? Map<String, Any?>
        return ServerMessage(type, id, payload)
    }

    /**
     * The [WsProtocol.Factory] class for instantiating [AppSyncProtocol] instances. This should be passed to
     * the WebSocketNetworkTransport builder to allow the websocket to connect to AppSync.
     * @param endpoint The [AppSyncEndpoint] to connect to
     * @param authorizer The [AppSyncAuthorizer] that determines the authorization mode to use when connecting to AppSync
     */
    class Factory(private val endpoint: AppSyncEndpoint, private val authorizer: AppSyncAuthorizer) :
        WsProtocol.Factory {
        override val name = "graphql-ws"

        override fun create(
            webSocketConnection: WebSocketConnection,
            listener: Listener,
            scope: CoroutineScope
        ): WsProtocol = AppSyncProtocol(
            endpoint = endpoint,
            authorizer = authorizer,
            webSocketConnection = webSocketConnection,
            listener = listener,
            scope = scope
        )
    }

    private data class ServerMessage(
        val type: SubscriptionMessageType,
        val id: String?,
        val payload: Map<String, Any?>?
    )
}
