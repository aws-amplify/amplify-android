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
        val registration = Registration(request, authModeResolver.resolve(request))

        emitAll(
            socket.messages
                // onStart runs once this collector is subscribed. Sending `start` any earlier would
                // race the reply: the socket's flow has no replay, so a fast start_ack would be lost
                // and the subscription would appear to hang.
                .onStart { registration.register(socket) }
                .onCompletion { socket.send(AppSyncWebSocketMessage.Stop(registration.id)) }
                .transformWhile { message -> handle(message, registration, socket, request) }
        )
    }

    /**
     * Tracks which auth mode a subscription is currently registered under, and moves to the next one
     * when the service rejects the identity.
     *
     * Registration is retried rather than the whole subscription because the connection is shared and
     * already established — only the `start` message needs replaying, under a fresh id.
     */
    private inner class Registration(
        private val request: GraphQLRequest<*>,
        private val authModes: List<AppSyncAuthMode>
    ) {
        private var index = -1
        private val failures = mutableListOf<AppSyncException>()

        /** The id of the current attempt. Changes on every retry. */
        var id: String = UUID.randomUUID().toString()
            private set

        /** The mode the current attempt used, for reporting. */
        val attemptedModes: List<AppSyncAuthMode>
            get() = authModes.take(index + 1)

        val canRetry: Boolean
            get() = index < authModes.lastIndex

        /** Whether there was ever more than one mode to try. */
        val hasFallback: Boolean
            get() = authModes.size > 1

        /**
         * Sends `start` under the next auth mode, skipping modes whose credentials cannot be obtained.
         *
         * @throws AppSyncException if no remaining mode can produce a usable `start` message.
         */
        suspend fun register(socket: AppSyncWebSocket) {
            while (index < authModes.lastIndex) {
                index++
                val mode = authModes[index]
                val authorizer = authorization.authorizerFor(mode)
                if (authorizer == null) {
                    failures += AppSyncProviderNotConfiguredException(
                        message = "No authorizer is configured for auth mode $mode."
                    )
                    continue
                }
                // A fresh id per attempt: the previous one was rejected, so the service considers it
                // spent, and reusing it would conflate the two attempts' replies.
                id = UUID.randomUUID().toString()
                try {
                    socket.send(
                        AppSyncWebSocketMessage.Start(
                            id = id,
                            query = request.content,
                            authorizationHeaders = decorator.authorizationHeaders(
                                authorizer,
                                httpEndpoint,
                                request.content
                            )
                        )
                    )
                    return
                } catch (error: AppSyncAuthException) {
                    // Credentials for this mode could not be obtained; another may still work.
                    failures += error
                }
            }
            throw exhausted()
        }

        /** The failure to report once no mode is left to try. */
        fun exhausted(): AppSyncException {
            // With a single candidate there is nothing to exhaust, so the real failure is more useful.
            if (authModes.size <= 1) {
                return failures.lastOrNull() ?: AppSyncProviderNotConfiguredException(
                    message = "No auth mode was available to authorize the subscription."
                )
            }
            return AppSyncAuthExhaustedException(
                message = "The subscription failed with every eligible auth mode: " +
                    authModes.joinToString() + ".",
                attemptedAuthModes = authModes,
                cause = failures.lastOrNull()
            )
        }
    }

    /**
     * Decides what one inbound message means for this subscription.
     *
     * @return true to keep the flow open, false to complete it normally. Throws to terminate it.
     */
    private suspend fun <T> kotlinx.coroutines.flow.FlowCollector<SubscriptionEvent<T>>.handle(
        message: AppSyncWebSocketMessage.Inbound,
        registration: Registration,
        socket: AppSyncWebSocket,
        request: GraphQLRequest<T>
    ): Boolean = when (message) {
        is AppSyncWebSocketMessage.StartAck -> {
            if (message.id == registration.id) emit(SubscriptionEvent.Connected)
            true
        }

        is AppSyncWebSocketMessage.Data -> {
            if (message.id == registration.id) {
                // Deserialization failure is deliberately swallowed: it is non-terminal, so one
                // unreadable message must not end a healthy subscription.
                val response = runCatching {
                    AppSyncResponseDeserializer.deserialize(request, message.payload)
                }.getOrNull()

                when {
                    response == null -> true
                    // AppSync can reject the identity in a data message rather than an error frame, so
                    // this is a retry trigger too, not just something to hand to the consumer.
                    response.hasUnauthorizedError() && registration.canRetry -> {
                        registration.register(socket)
                        true
                    }
                    else -> {
                        emit(SubscriptionEvent.Data(response))
                        true
                    }
                }
            } else {
                true
            }
        }

        is AppSyncWebSocketMessage.Complete -> message.id != registration.id

        is AppSyncWebSocketMessage.Error -> {
            // A null id is a connection-level error, which applies to every subscription on it.
            if (message.id == registration.id || message.id == null) {
                val rejectedIdentity = message.errors.hasUnauthorizedError()
                when {
                    // The identity was rejected; a different auth mode may be accepted.
                    rejectedIdentity && registration.canRetry -> {
                        registration.register(socket)
                        true
                    }
                    // Every eligible mode has now been rejected.
                    rejectedIdentity && registration.hasFallback -> throw registration.exhausted()
                    // Either there was never a fallback, or the failure is not about identity at all —
                    // retrying would fail the same way and hide the reason. Report what the service said.
                    else -> throw AppSyncGraphQLErrorException(
                        message = "The subscription failed: " +
                            message.errors.joinToString("; ") { it.message }.ifEmpty { "no reason given" },
                        errors = emptyList()
                    )
                }
            } else {
                true
            }
        }

        is AppSyncWebSocketMessage.ConnectionError -> throw AppSyncConnectionException(
            message = "The subscription connection failed: " +
                message.errors.joinToString("; ") { it.message }.ifEmpty { "no reason given" }
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
