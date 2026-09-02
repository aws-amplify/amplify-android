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

import com.amplifyframework.api.graphql.GraphQLRequest
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch

/**
 * Turns a GraphQL subscription request into a [Flow] of [SubscriptionEvent], multiplexed over the
 * client's shared WebSocket.
 *
 * A private reimplementation of the plugin's `SubscriptionOperation` and the registry inside
 * `SubscriptionEndpoint`. Because the socket exposes one flow of messages, a subscription is a
 * *filter* over it keyed by subscription id rather than an entry in a callback registry.
 *
 * @param provider Supplies the shared connection, opening it on first subscribe.
 * @param authorization The client's authorizer configuration.
 * @param decorator Produces the per-subscription authorization headers.
 * @param httpEndpoint The HTTP endpoint. SigV4 signs the `start` message against it.
 * @param authModeResolver Chooses the auth mode, honouring `@auth` rules and per-request overrides.
 */
internal class AppSyncSubscriber(
    private val provider: AppSyncWebSocketProvider,
    private val authorization: AppSyncAuthorization,
    private val decorator: AppSyncRequestDecorator,
    private val httpEndpoint: String,
    private val authModeResolver: AppSyncAuthModeResolver = AppSyncAuthModeResolver(authorization),
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val connectionEvents = MutableSharedFlow<ConnectionState>(replay = 1)
    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())

    /** Connection state for the shared socket. Replays the latest so a late observer is not blind. */
    val events: SharedFlow<ConnectionState> = connectionEvents.asSharedFlow()

    /**
     * A cold flow for one subscription. Nothing happens until it is collected; cancelling the
     * collector sends `stop` and releases the subscription.
     *
     * Lifecycle and terminality:
     * - `Connecting` → `Connected` → `Data`*, then normal completion or a terminal throw.
     * - Registration failures and connection failures are **terminal**.
     * - GraphQL errors carried *inside* a data message are **not** terminal — they are delivered on
     *   the response, because the service considered the message deliverable.
     * - A data message that fails to deserialize is **not** terminal either; it is dropped and the
     *   stream continues. Losing one message is better than tearing down a working subscription.
     * - `complete` from the service, and a clean socket close, both end the flow normally.
     */
    fun <T> subscribe(request: GraphQLRequest<T>): Flow<SubscriptionEvent<T>> = flow {
        emit(SubscriptionEvent.Connecting)

        val socket = connectedSocket()
        val id = UUID.randomUUID().toString()
        val start = startMessage(id, request)

        emitAll(
            socket.messages
                // onStart runs once this collector is subscribed. Sending `start` any earlier would
                // race the reply: the socket's flow has no replay, so a fast start_ack would be lost
                // and the subscription would appear to hang.
                .onStart { socket.send(start) }
                .onCompletion { socket.send(AppSyncWebSocketMessage.Stop(id)) }
                .transformWhile { message -> handle(message, id, request) }
        )
    }

    private suspend fun <T> startMessage(id: String, request: GraphQLRequest<T>): AppSyncWebSocketMessage.Start {
        val authMode = authModeResolver.resolve(request).first()
        val authorizer = authorization.authorizerFor(authMode)
            ?: throw AppSyncProviderNotConfiguredException(
                message = "No authorizer is configured for auth mode $authMode."
            )

        return AppSyncWebSocketMessage.Start(
            id = id,
            query = request.content,
            // A non-null body signs against the plain endpoint, which is what AppSync expects for an
            // individual subscription as opposed to the connection.
            authorizationHeaders = decorator.authorizationHeaders(authorizer, httpEndpoint, request.content)
        )
    }

    /**
     * Decides what one inbound message means for this subscription.
     *
     * @return true to keep the flow open, false to complete it normally. Throws to terminate it.
     */
    private suspend fun <T> kotlinx.coroutines.flow.FlowCollector<SubscriptionEvent<T>>.handle(
        message: AppSyncWebSocketMessage.Inbound,
        id: String,
        request: GraphQLRequest<T>
    ): Boolean = when (message) {
        is AppSyncWebSocketMessage.StartAck -> {
            if (message.id == id) emit(SubscriptionEvent.Connected)
            true
        }

        is AppSyncWebSocketMessage.Data -> {
            if (message.id == id) {
                // Deserialization failure is deliberately swallowed: it is non-terminal, so one
                // unreadable message must not end a healthy subscription.
                runCatching { AppSyncResponseDeserializer.deserialize(request, message.payload) }
                    .getOrNull()
                    ?.let { emit(SubscriptionEvent.Data(it)) }
            }
            true
        }

        is AppSyncWebSocketMessage.Complete -> message.id != id

        is AppSyncWebSocketMessage.Error -> {
            // A null id is a connection-level error, which is terminal for every subscription on it.
            if (message.id == id || message.id == null) {
                throw AppSyncGraphQLErrorException(
                    message = "The subscription failed: " +
                        message.errors.joinToString("; ").ifEmpty { "no reason given" },
                    errors = emptyList()
                )
            }
            true
        }

        is AppSyncWebSocketMessage.ConnectionError -> throw AppSyncConnectionException(
            message = "The subscription connection failed: " +
                message.errors.joinToString("; ").ifEmpty { "no reason given" }
        )

        // A cause means the connection died; no cause means close() was called, which ends
        // subscription streams normally rather than throwing.
        is AppSyncWebSocketMessage.Closed -> message.cause?.let { throw it } ?: false

        // Keep-alives, unknown frames, and anything addressed to another subscription.
        is AppSyncWebSocketMessage.ConnectionAck,
        is AppSyncWebSocketMessage.KeepAlive,
        is AppSyncWebSocketMessage.Unknown -> true
    }

    /**
     * Returns the shared socket, emitting connection state around the attempt.
     */
    private suspend fun connectedSocket(): AppSyncWebSocket {
        provider.existing?.let { return it }

        connectionEvents.emit(ConnectionState.Connecting)
        val socket = try {
            provider.connection()
        } catch (error: Throwable) {
            connectionEvents.emit(ConnectionState.Disconnected(AppSyncException.from(error)))
            throw error
        }
        connectionEvents.emit(ConnectionState.Connected)
        watchForDisconnect(socket)
        return socket
    }

    /**
     * Reports an unexpected disconnect on [events].
     *
     * One watcher per connection, not per subscription: every subscriber sees the same `Closed`
     * message, so emitting from the per-subscription path would report the same disconnect N times.
     */
    private fun watchForDisconnect(socket: AppSyncWebSocket) {
        scope.launch {
            // Already gone — the Closed message has been and passed, so there is nothing left to await.
            if (socket.isClosed) {
                connectionEvents.emit(ConnectionState.Disconnected(null))
                return@launch
            }
            val closed = socket.messages.first { it is AppSyncWebSocketMessage.Closed }
            connectionEvents.emit(
                ConnectionState.Disconnected((closed as AppSyncWebSocketMessage.Closed).cause)
            )
        }
    }

    /** Closes the shared connection and reports a clean disconnect. */
    suspend fun close() {
        provider.close()
        connectionEvents.emit(ConnectionState.Disconnected(null))
        scope.cancel()
    }
}
