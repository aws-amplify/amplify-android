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
import com.amplifyframework.aws.appsync.events.data.EventsException
import com.amplifyframework.aws.appsync.events.data.EventsMessage
import com.amplifyframework.aws.appsync.events.data.PublishResult
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement

/**
 * A class to manage channel subscriptions and publishes
 *
 * @property name of the channel
 * @property authorizers used for channel subscriptions and publishes
 */
class EventsChannel internal constructor(
    val name: String,
    val authorizers: ChannelAuthorizers
) {

    /**
     * Subscribe to a channel.
     *
     * @param authorizer for the subscribe call. If not provided, the EventChannel subscribe authorizer will be used.
     * @return flow of event messages. Collect flow to receive messages.
     */
    @Throws(EventsException::class)
    fun subscribe(
        authorizer: AppSyncAuthorizer = this.authorizers.subscribeAuthorizer
    ): Flow<EventsMessage> {
        TODO("Need to implement")
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
}
