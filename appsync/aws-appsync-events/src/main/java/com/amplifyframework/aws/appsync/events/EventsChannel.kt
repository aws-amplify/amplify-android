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
import com.amplifyframework.aws.appsync.events.data.ChannelAuthorizers
import com.amplifyframework.aws.appsync.events.data.ConnectionClosedException
import com.amplifyframework.aws.appsync.events.data.EventsException
import com.amplifyframework.aws.appsync.events.data.EventsMessage
import com.amplifyframework.aws.appsync.events.data.PublishResult
import com.amplifyframework.aws.appsync.events.data.UserClosedConnectionException
import com.amplifyframework.aws.appsync.events.data.WebSocketMessage
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
import kotlinx.serialization.json.JsonElement

/**
 * A class to manage channel subscriptions and publishes
 *
 * @property name of the channel
 * @property authorizers used for channel subscriptions and publishes
 */
class EventsChannel internal constructor(
    val name: String,
    val authorizers: ChannelAuthorizers,
    private val endpoints: EventsEndpoints,
    private val eventsWebSocket: EventsWebSocket
) {

    /**
     * Subscribe to a channel.
     *
     * @param authorizer for the subscribe call. If not provided, the EventChannel subscribe authorizer will be used.
     * @return flow of event messages. Collect flow to receive messages.
     */
    fun subscribe(authorizer: AppSyncAuthorizer = this.authorizers.subscribeAuthorizer): Flow<EventsMessage> {
        val subscriptionId = UUID.randomUUID().toString() // create unique subscriptionId
        var isSubscribed = false
        return createSubscriptionEventDataFlow(subscriptionId) // create flow of data messages for given subscription
            .onStart { // block completes complete before event messages begin emitting through flow
                // open websocket (if not already) + send subscription. Returns true if successfully subscribed
                isSubscribed = initiateSubscription(subscriptionId, authorizer)
            }.flowOn(Dispatchers.IO) // io used for authorizers to pull headers asynchronously
            .onCompletion {
                // only unsubscribe if already subscribed
                if (isSubscribed) {
                    completeSubscription(subscriptionId, it) // unsubscribe if websocket is still open
                }
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
    @Throws(EventsException::class)
    suspend fun publish(
        event: JsonElement,
        authorizer: AppSyncAuthorizer = this.authorizers.publishAuthorizer
    ): PublishResult {
        TODO("Need to implement")
    }

    /**
     * Publish a multiple events (up to 5) to a channel.
     *
     * @param events list of formatted json events.
     * @param authorizer for the publish call. If not provided, the EventChannel publish authorizer will be used.
     * @return result of publish.
     */
    @Throws(EventsException::class)
    suspend fun publish(
        events: List<JsonElement>,
        authorizer: AppSyncAuthorizer = this.authorizers.publishAuthorizer
    ): PublishResult {
        TODO("Need to implement")
    }

    private fun createSubscriptionEventDataFlow(subscriptionId: String): Flow<EventsMessage> {
        return flow {
            eventsWebSocket.events.collect {
                // First part of chained flow which not only receives Data message, but also detects
                // ConnectionClosed. This allows us to complete the flow when the websocket closes
                when {
                    it is WebSocketMessage.Received.Subscription.Data && it.id == subscriptionId -> {
                        emit(EventsMessage(it.event))
                    }
                    it is WebSocketMessage.Closed -> throw ConnectionClosedException()
                    else -> Unit
                }
            }
        }
    }

    private suspend fun initiateSubscription(
        subscriptionId: String,
        authorizer: AppSyncAuthorizer
    ): Boolean = coroutineScope {
        eventsWebSocket.connect()

        val deferredSubscriptionResponse = async { getSubscriptionResponse(subscriptionId) }

        // Publish subscription to websocket
        publishSubscription(subscriptionId, authorizer)

        // Wait for subscription result to return
        when (val response = deferredSubscriptionResponse.await()) {
            is WebSocketMessage.Received.Subscription.SubscribeSuccess -> {
                return@coroutineScope true
            }
            is WebSocketMessage.Received.Subscription.SubscribeError -> {
                val exceptionMessage = "Subscribe failed for channel: $name"
                throw response.errors.firstOrNull()
                    ?.toEventsException(exceptionMessage)
                    ?: EventsException(exceptionMessage)
            }
            is WebSocketMessage.Received.ConnectionClosed -> {
                throw ConnectionClosedException()
            }
            else -> throw EventsException("Received unexpected subscription response of type: ${response::class}")
        }
    }

    private suspend fun getSubscriptionResponse(subscriptionId: String): WebSocketMessage {
        return eventsWebSocket.events.first {
            when {
                it is WebSocketMessage.Received.Subscription && it.id == subscriptionId -> true
                it is WebSocketMessage.Received.ConnectionClosed -> true
                else -> false
            }
        }
    }

    private suspend fun publishSubscription(subscriptionId: String, authorizer: AppSyncAuthorizer) {
        val subscribeMessage = WebSocketMessage.Send.Subscription.Subscribe(
            id = subscriptionId,
            channel = name,
            authorization = authorizer.getAuthorizationHeaders(
                object : AppSyncRequest {
                    override val url: String
                        get() = endpoints.restEndpoint.toString()
                    override val body: String?
                        get() = null
                    override val headers: Map<String, String>
                        get() = emptyMap()
                    override val method: AppSyncRequest.HttpMethod
                        get() = AppSyncRequest.HttpMethod.GET
                }
            )
        )
        eventsWebSocket.send(subscribeMessage)
    }

    private fun completeSubscription(subscriptionId: String, throwable: Throwable?) {
        if (throwable !is ConnectionClosedException) {
            // Unsubscribe from channel when flow is completed
            eventsWebSocket.send(WebSocketMessage.Send.Subscription.Unsubscribe(subscriptionId))
        }
    }
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
