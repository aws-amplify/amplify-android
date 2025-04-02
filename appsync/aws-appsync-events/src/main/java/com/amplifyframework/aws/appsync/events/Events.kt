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
import kotlinx.serialization.json.JsonElement

/**
 * Publish a single event to a channel.
 *
 * @property endpoint AWS AppSync Events endpoint.
 * @param connectAuthorizer for AWS AppSync Websocket Pub/Sub connection.
 * @param defaultChannelAuthorizers passed to created channels if not overridden.
 */
class Events(
    val endpoint: String,
    val connectAuthorizer: AppSyncAuthorizer,
    val defaultChannelAuthorizers: ChannelAuthorizers
) {

    /**
     * Publish a single event to a channel.
     *
     * @param channelName of the channel to publish to.
     * @param event formatted in json.
     * @param authorizer for the publish call. If not provided, the EventChannel publish authorizer will be used.
     * @return result of publish.
     */
    @Throws(EventsException::class)
    suspend fun publish(
        channelName: String,
        event: JsonElement,
        authorizer: AppSyncAuthorizer = this.defaultChannelAuthorizers.publishAuthorizer
    ) : PublishResult {
        TODO("Need to implement")
    }

    /**
     * Publish a multiple events (up to 5) to a channel.
     *
     * @param channelName of the channel to publish to.
     * @param events list of formatted json events.
     * @param authorizer for the publish call. If not provided, the EventChannel publish authorizer will be used.
     * @return result of publish.
     */
    @Throws(EventsException::class)
    suspend fun publish(
        channelName: String,
        events: List<JsonElement>,
        authorizer: AppSyncAuthorizer = this.defaultChannelAuthorizers.publishAuthorizer
    ) : PublishResult {
        TODO("Need to implement")
    }

    /**
     * Create a channel.
     *
     * @param channelName of the channel to use.
     * @param authorizers for the channel to use for subscriptions and publishes.
     * @return a channel to manage subscriptions and publishes.
     */
    @Throws(EventsException::class)
    fun channel(
        channelName: String,
        authorizers: ChannelAuthorizers = this.defaultChannelAuthorizers,
    ) : EventsChannel {
        TODO("Need to implement")
    }

    /**
     * Method to disconnect from all channels.
     *
     * @param flushEvents set to true (default) to allow all pending publish calls to succeed before disconnecting.
     * Setting to false will immediately disconnect, cancelling any in-progress or queued event publishes.
     * @param authorizers for the channel to use for subscriptions and publishes.
     * @return a channel to manage subscriptions and publishes.
     */
    @Throws(EventsException::class)
    suspend fun disconnect(flushEvents: Boolean = true) {
        TODO("Need to implement")
    }
}