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
import com.amazonaws.sdk.appsync.events.data.ConnectionClosedException
import com.amazonaws.sdk.appsync.events.data.EventsException
import com.amazonaws.sdk.appsync.events.data.EventsMessage
import com.amazonaws.sdk.appsync.events.data.PublishResult
import com.amazonaws.sdk.appsync.events.data.UserClosedConnectionException
import com.amazonaws.sdk.appsync.events.data.WebSocketMessage
import com.amazonaws.sdk.appsync.events.data.toEventsException
import com.amazonaws.sdk.appsync.events.utils.JsonUtils
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient

class EventsWebSocketClient internal constructor(
    val connectAuthorizer: AppSyncAuthorizer,
    val subscribeAuthorizer: AppSyncAuthorizer,
    val publishAuthorizer: AppSyncAuthorizer,
    val options: Events.Options.WebSocket,
    endpoints: EventsEndpoints,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val okHttpClient = OkHttpClient.Builder().apply {
        options.okHttpConfigurationProvider?.applyConfiguration(this)
    }.build()

    private val json = JsonUtils.createJsonForLibrary()

    private val eventsWebSocketProvider = EventsWebSocketProvider(
        endpoints,
        connectAuthorizer,
        okHttpClient,
        json,
        options.loggerProvider
    )

    /**
     * Subscribe to a channel.
     *
     * @param channelName of the channel to subscribe to.
     * @param authorizer for the subscribe call. The subscribeAuthorizer passed to the client will be used as default.
     * @return flow of event messages. Collect flow to receive messages.
     */
    fun subscribe(channelName: String, authorizer: AppSyncAuthorizer = this.subscribeAuthorizer): Flow<EventsMessage> {
        val subscriptionHolder = SubscriptionHolder()
        return createSubscriptionEventDataFlow(subscriptionHolder)
            .onStart {
                // block completes complete before event messages begin emitting through flow
                // get a connected websocket
                val newWebSocket = eventsWebSocketProvider.getConnectedWebSocket()
                subscriptionHolder.webSocket = newWebSocket
                // + send subscription. Returns true if successfully subscribed
                subscriptionHolder.isSubscribed = initiateSubscription(
                    channelName,
                    newWebSocket,
                    subscriptionHolder.id,
                    authorizer
                )
            }.flowOn(ioDispatcher) // io used for authorizers to pull headers asynchronously
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
     * Publish a single event to a channel over WebSocket.
     *
     * @param channelName of the channel to publish to.
     * @param event formatted in json.
     * @param authorizer for the publish call. The publishAuthorizer passed to the client is the default value.
     * @return result of publish.
     */
    suspend fun publish(
        channelName: String,
        event: JsonElement,
        authorizer: AppSyncAuthorizer = publishAuthorizer
    ): PublishResult = publish(channelName, listOf(event), authorizer)

    /**
     * Publish a multiple events (up to 5) to a channel over WebSocket.
     *
     * @param channelName of the channel to publish to.
     * @param events list of formatted json events.
     * @param authorizer for the publish call. The publishAuthorizer passed to the client is the default value.
     * @return result of publish.
     */
    suspend fun publish(
        channelName: String,
        events: List<JsonElement>,
        authorizer: AppSyncAuthorizer = publishAuthorizer
    ): PublishResult = try {
        publishToWebSocket(channelName, events, authorizer).let {
            PublishResult.Response(
                successfulEvents = it.successfulEvents,
                failedEvents = it.failedEvents
            )
        }
    } catch (exception: Exception) {
        PublishResult.Failure(exception.toEventsException())
    }

    /**
     * Method to disconnect from the websocket. This will result in all subscriptions completing.
     *
     * @param flushEvents set to true (default) to allow all pending publish calls to succeed before disconnecting.
     * Setting to false will immediately disconnect, cancelling any in-progress or queued event publishes.
     */
    suspend fun disconnect(flushEvents: Boolean = true) {
        eventsWebSocketProvider.existingWebSocket?.disconnect(flushEvents)
    }

    @Throws(Exception::class)
    private suspend fun publishToWebSocket(
        channelName: String,
        events: List<JsonElement>,
        authorizer: AppSyncAuthorizer
    ): WebSocketMessage.Received.PublishSuccess = withContext(ioDispatcher) {
        val publishId = UUID.randomUUID().toString()
        val publishMessage = WebSocketMessage.Send.Publish(
            id = publishId,
            channel = channelName,
            events = JsonArray(events.map { JsonPrimitive(it.toString()) })
        )

        val webSocket = eventsWebSocketProvider.getConnectedWebSocket()
        val deferredResponse = async { getPublishResponse(webSocket, publishId) }

        val queued = webSocket.sendWithAuthorizer(publishMessage, authorizer)
        if (!queued) {
            throw webSocket.disconnectReason?.toCloseException() ?: ConnectionClosedException()
        }

        return@withContext when (val response = deferredResponse.await()) {
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

    private fun createSubscriptionEventDataFlow(subscriptionHolder: SubscriptionHolder): Flow<EventsMessage> = flow {
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
        }
            ?: throw EventsException.unknown("EventsWebSocket was null when attempting to collect.")
    }

    private suspend fun initiateSubscription(
        channelName: String,
        webSocket: EventsWebSocket,
        subscriptionId: String,
        authorizer: AppSyncAuthorizer
    ): Boolean = coroutineScope {
        // create a deferred holder for subscription response
        val deferredSubscriptionResponse =
            async { getSubscriptionResponse(webSocket, subscriptionId) }

        // Publish subscription to websocket
        val queued = webSocket.sendWithAuthorizer(
            webSocketMessage = WebSocketMessage.Send.Subscription.Subscribe(
                id = subscriptionId,
                channel = channelName
            ),
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
                val exceptionMessage = "Subscribe failed for channel: $channelName"
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

    private suspend fun getSubscriptionResponse(webSocket: EventsWebSocket, subscriptionId: String): WebSocketMessage =
        webSocket.events.first {
            when {
                it is WebSocketMessage.Received.Subscription && it.id == subscriptionId -> true
                it is WebSocketMessage.ErrorContainer && it.id == subscriptionId -> true
                it is WebSocketMessage.Closed -> true
                else -> false
            }
        }

    private suspend fun getPublishResponse(webSocket: EventsWebSocket, publishId: String): WebSocketMessage =
        webSocket.events.first {
            when {
                it is WebSocketMessage.Received.PublishSuccess && it.id == publishId -> true
                it is WebSocketMessage.ErrorContainer && it.id == publishId -> true
                it is WebSocketMessage.Closed -> true
                else -> false
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
                currentWebSocket.send(
                    WebSocketMessage.Send.Subscription.Unsubscribe(
                        subscriptionHolder.id
                    )
                )
            } catch (e: Exception) {
                // do nothing with a failed unsubscribe post
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
}
