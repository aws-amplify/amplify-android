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
import com.amplifyframework.aws.appsync.events.data.ChannelAuthorizers
import com.amplifyframework.aws.appsync.events.data.ConnectionClosedException
import com.amplifyframework.aws.appsync.events.data.EventsException
import com.amplifyframework.aws.appsync.events.data.EventsMessage
import com.amplifyframework.aws.appsync.events.data.PublishResult
import com.amplifyframework.aws.appsync.events.data.UserClosedConnectionException
import com.amplifyframework.aws.appsync.events.data.WebSocketMessage
import com.amplifyframework.aws.appsync.events.data.toEventsException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * A class to manage channel subscriptions and publishes
 *
 * @property name of the channel
 * @property authorizers used for channel subscriptions and publishes
 */
class EventsChannel internal constructor(
    val name: String,
    val authorizers: ChannelAuthorizers,
    private val eventsWebSocketProvider: EventsWebSocketProvider
) {

    /**
     * Subscribe to a channel.
     *
     * @param authorizer for the subscribe call. If not provided, the EventChannel subscribe authorizer will be used.
     * @return flow of event messages. Collect flow to receive messages.
     */
    fun subscribe(authorizer: AppSyncAuthorizer = this.authorizers.subscribeAuthorizer): Flow<EventsMessage> {
        val subscriptionHolder = SubscriptionHolder()
        return createSubscriptionEventDataFlow(subscriptionHolder)
            .onStart { // block completes complete before event messages begin emitting through flow
                // get a connected websocket
                val newWebSocket = eventsWebSocketProvider.getConnectedWebSocket()
                subscriptionHolder.webSocket = newWebSocket
                // + send subscription. Returns true if successfully subscribed
                subscriptionHolder.isSubscribed = initiateSubscription(newWebSocket, subscriptionHolder.id, authorizer)
            }.flowOn(Dispatchers.IO) // io used for authorizers to pull headers asynchronously
            .onCompletion {
                // only unsubscribe if already subscribed and websocket is still open
                val currentWebSocket = subscriptionHolder.webSocket
                if (subscriptionHolder.isSubscribed && currentWebSocket != null) {
                    completeSubscription(subscriptionHolder, it)
                }
                subscriptionHolder.webSocket = null
            }
            .catchUserClosedException() // allow emitting all exceptions but user initiated close
    }

    /**
     * Publish a single event to a channel.
     *
     * @param event formatted in json.
     * @param authorizer for the publish call. If not provided, the EventChannel publish authorizer will be used.
     * @return result of publish.
     */
    suspend fun publish(
        event: JsonElement,
        authorizer: AppSyncAuthorizer = this.authorizers.publishAuthorizer
    ): PublishResult {
        return publish(listOf(event), authorizer)
    }

    /**
     * Publish a multiple events (up to 5) to a channel.
     *
     * @param events list of formatted json events.
     * @param authorizer for the publish call. If not provided, the EventChannel publish authorizer will be used.
     * @return result of publish.
     */
    suspend fun publish(
        events: List<JsonElement>,
        authorizer: AppSyncAuthorizer = this.authorizers.publishAuthorizer
    ): PublishResult {
        return try {
            publishToWebSocket(events, authorizer).let {
                PublishResult.Response(
                    successfulEvents = it.successfulEvents,
                    failedEvents = it.failedEvents
                )
            }
        } catch (exception: Exception) {
            PublishResult.Failure(exception.toEventsException())
        }
    }

    @Throws(Exception::class)
    private suspend fun publishToWebSocket(
        events: List<JsonElement>,
        authorizer: AppSyncAuthorizer
    ): WebSocketMessage.Received.PublishSuccess = coroutineScope {
        val publishId = UUID.randomUUID().toString()
        val publishMessage = WebSocketMessage.Send.Publish(
            id = publishId,
            channel = name,
            events = JsonArray(events.map { JsonPrimitive(it.toString()) }),
        )

        val webSocket = eventsWebSocketProvider.getConnectedWebSocket()
        val deferredResponse = async { getPublishResponse(webSocket, publishId) }

        val queued = webSocket.sendWithAuthorizer(publishMessage, authorizer)
        if (!queued) {
            throw webSocket.disconnectReason?.toCloseException() ?: ConnectionClosedException()
        }

        return@coroutineScope when (val response = deferredResponse.await()) {
            is WebSocketMessage.Received.PublishSuccess -> {
                response
            }
            is WebSocketMessage.ErrorContainer -> {
                val fallbackMessage = "Failed to publish event(s)"
                throw response.errors.firstOrNull()?.toEventsException(fallbackMessage)
                    ?: EventsException(fallbackMessage)
            }
            is WebSocketMessage.Closed -> {
                throw response.reason.toCloseException()
            }
            else -> throw EventsException("Received unexpected publish response of type: ${response::class}")
        }
    }

    private fun createSubscriptionEventDataFlow(subscriptionHolder: SubscriptionHolder): Flow<EventsMessage> {
        return flow {
            subscriptionHolder.webSocket?.events?.collect {
                // First part of chained flow which not only receives Data message, but also detects
                // ConnectionClosed. This allows us to complete the flow when the websocket closes
                when {
                    it is WebSocketMessage.Received.Subscription.Data && it.id == subscriptionHolder.id -> {
                        emit(EventsMessage(it.event))
                    }
                    it is WebSocketMessage.Closed -> {
                        throw it.reason.toCloseException()
                    }
                    it is WebSocketMessage.ErrorContainer && it.id == subscriptionHolder.id -> {
                        val exceptionMessage = "Received error for subscription"
                        throw it.errors.firstOrNull()?.toEventsException(exceptionMessage)
                            ?: EventsException(exceptionMessage)
                    }
                    else -> Unit
                }
            } ?: throw EventsException.unknown("EventsWebSocket was null when attempting to collect.")
        }
    }

    private suspend fun initiateSubscription(
        webSocket: EventsWebSocket,
        subscriptionId: String,
        authorizer: AppSyncAuthorizer
    ): Boolean = coroutineScope {
        // create a deferred holder for subscription response
        val deferredSubscriptionResponse = async { getSubscriptionResponse(webSocket, subscriptionId) }

        // Publish subscription to websocket
        val queued = webSocket.sendWithAuthorizer(
            webSocketMessage = WebSocketMessage.Send.Subscription.Subscribe(id = subscriptionId, channel = name),
            authorizer = authorizer
        )
        if (!queued) {
            throw webSocket.disconnectReason?.toCloseException() ?: ConnectionClosedException()
        }

        // Wait for subscription result to return
        when (val response = deferredSubscriptionResponse.await()) {
            is WebSocketMessage.Received.Subscription.SubscribeSuccess -> {
                return@coroutineScope true
            }
            is WebSocketMessage.ErrorContainer -> {
                val exceptionMessage = "Subscribe failed for channel: $name"
                throw response.errors.firstOrNull()
                    ?.toEventsException(exceptionMessage)
                    ?: EventsException(exceptionMessage)
            }
            is WebSocketMessage.Closed -> {
                throw response.reason.toCloseException()
            }
            else -> throw EventsException("Received unexpected subscription response of type: ${response::class}")
        }
    }

    private suspend fun getSubscriptionResponse(webSocket: EventsWebSocket, subscriptionId: String): WebSocketMessage {
        return webSocket.events.first {
            when {
                it is WebSocketMessage.Received.Subscription && it.id == subscriptionId -> true
                it is WebSocketMessage.ErrorContainer && it.id == subscriptionId -> true
                it is WebSocketMessage.Closed -> true
                else -> false
            }
        }
    }

    private suspend fun getPublishResponse(webSocket: EventsWebSocket, publishId: String): WebSocketMessage {
        return webSocket.events.first {
            when {
                it is WebSocketMessage.Received.PublishSuccess && it.id == publishId -> true
                it is WebSocketMessage.ErrorContainer && it.id == publishId -> true
                it is WebSocketMessage.Closed -> true
                else -> false
            }
        }
    }

    private fun completeSubscription(subscriptionHolder: SubscriptionHolder, throwable: Throwable?) {
        // only unsubscribe if already subscribed and websocket is still open
        val currentWebSocket = subscriptionHolder.webSocket
        val isSubscribed = subscriptionHolder.isSubscribed
        val isDisconnected = throwable is ConnectionClosedException || throwable is UserClosedConnectionException

        if (currentWebSocket != null && isSubscribed && !isDisconnected) {
            // Unsubscribe from channel when flow is completed
            try {
                currentWebSocket.send(WebSocketMessage.Send.Subscription.Unsubscribe(subscriptionHolder.id))
            } catch (e: Exception) {
                // do nothing with a failed unsubscribe post
            }
        }
    }
}

/**
 * Holds mutable details about a subscription including the websocket and whether or not subscribe was successful
 */
internal data class SubscriptionHolder(
    var webSocket: EventsWebSocket? = null,
    var isSubscribed: Boolean = false
) {
    val id = UUID.randomUUID().toString()
}

/**
 * Extension function that catches only UserClosedConnectionException and re-throws all other exceptions.
 *
 * @return Flow that ignores UserClosedConnectionException but propagates all other exceptions
 */
private fun <T> Flow<T>.catchUserClosedException(): Flow<T> = catch { throwable ->
    // Only swallow UserClosedConnectionException, propagate all others
    if (throwable !is UserClosedConnectionException) {
        throw throwable
    }
}
