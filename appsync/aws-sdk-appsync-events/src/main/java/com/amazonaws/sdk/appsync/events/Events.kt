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
import com.amazonaws.sdk.appsync.core.LoggerProvider

/**
 * The main class for interacting with AWS AppSync Events
 *
 * @property endpoint AWS AppSync Events endpoint.
 */
class Events(
    val endpoint: String
) {
    private val endpoints = EventsEndpoints(endpoint)

    /**
     * Create a REST client to publish to channels over REST
     *
     * @param publishAuthorizer sets the default AppSyncAuthorizer for REST publish calls
     * @param options for optional customizations to the REST client
     */
    fun createRestClient(publishAuthorizer: AppSyncAuthorizer, options: Options.Rest = Options.Rest()) =
        EventsRestClient(publishAuthorizer, options, endpoints.restEndpoint)

    /**
     * Create a WebSocket client to subscribe and publish to channels over WebSocket
     *
     * @param connectAuthorizer sets the default AppSyncAuthorizer for the websocket connection
     * @param subscribeAuthorizer sets the default AppSyncAuthorizer for subscriptions over the websocket
     * @param publishAuthorizer sets the default AppSyncAuthorizer for publishes over the websocket
     * @param options for optional customizations to the EventsRestClient
     */
    fun createWebSocketClient(
        connectAuthorizer: AppSyncAuthorizer,
        subscribeAuthorizer: AppSyncAuthorizer,
        publishAuthorizer: AppSyncAuthorizer,
        options: Options.WebSocket = Options.WebSocket()
    ) = EventsWebSocketClient(connectAuthorizer, subscribeAuthorizer, publishAuthorizer, options, endpoints)

    /**
     * The base Options class for all events clients, allowing optional customizations
     *
     * @param loggerProvider allows the client to emit logs to a provided logger
     * @param okHttpConfigurationProvider provides the OkHttp Builder the client will use
     */
    sealed class Options(
        val loggerProvider: LoggerProvider? = null,
        val okHttpConfigurationProvider: OkHttpConfigurationProvider? = null
    ) {
        /**
         * Configurable Options for the EventsRestClient
         *
         * @param loggerProvider allows the EventsRestClient to emit logs to a provided logger
         * @param okHttpConfigurationProvider provides the OkHttp.Builder used by the EventsRestClient,
         * enabling further networking customizations
         */
        class Rest(
            loggerProvider: LoggerProvider? = null,
            okHttpConfigurationProvider: OkHttpConfigurationProvider? = null
        ) : Options(loggerProvider, okHttpConfigurationProvider)

        /**
         * Configurable Options for the EventsWebSocketClient
         *
         * @param loggerProvider allows the EventsWebSocketClient to emit logs to a provided logger
         * @param okHttpConfigurationProvider provides the OkHttp.Builder used by the EventsWebSocketClient,
         * enabling further networking customizations
         */
        class WebSocket(
            loggerProvider: LoggerProvider? = null,
            okHttpConfigurationProvider: OkHttpConfigurationProvider? = null
        ) : Options(loggerProvider, okHttpConfigurationProvider)
    }
}
