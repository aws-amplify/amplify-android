/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

@file:JvmName("ApolloExtensions")

package com.amplifyframework.apollo.appsync

import com.amplifyframework.apollo.appsync.util.UserAgentHeader
import com.amplifyframework.apollo.appsync.util.WebSocketConnectionInterceptor
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.NullableAnyAdapter
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo.api.toJsonString
import com.apollographql.apollo.network.ws.DefaultWebSocketEngine
import com.apollographql.apollo.network.ws.WebSocketNetworkTransport
import okhttp3.OkHttpClient

// Use the requestUuid as the subscriptionId
internal val <D : Operation.Data> ApolloRequest<D>.subscriptionId: String
    get() = requestUuid.toString()

// Returns the request payload as a JSON string
internal fun ApolloRequest<*>.toJson() =
    NullableAnyAdapter.toJsonString(DefaultHttpRequestComposer.composePayload(this))

/**
 * Convenience function that configures the [WebSocketNetworkTransport] to connect to AppSync. This function:
 * 1. Sets the serverUrl
 * 2. Sets up an [AppSyncProtocol] using the given endpoint and authorizer
 * 3. Adds an interceptor to append the authorization payload to the connection request
 * @param endpoint The [AppSyncEndpoint] to connect to
 * @param authorizer The [AppSyncAuthorizer] that determines the authorization mode to use when connecting to AppSync
 * @return The builder instance for chaining
 */
fun WebSocketNetworkTransport.Builder.appSync(endpoint: AppSyncEndpoint, authorizer: AppSyncAuthorizer) = apply {
    // Set the connection URL
    serverUrl(endpoint.realtime.toString())

    // Add User-agent header
    addHeader(UserAgentHeader.NAME, UserAgentHeader.value)

    // Add an interceptor that appends the authorization headers
    val client = OkHttpClient.Builder()
        .addInterceptor(WebSocketConnectionInterceptor(endpoint, authorizer))
        .build()
    webSocketEngine(DefaultWebSocketEngine(client))

    // Set the WebSocket protocol
    protocol(
        AppSyncProtocol.Factory(
            endpoint = endpoint,
            authorizer = authorizer
        )
    )
}

/**
 * Convenience function that configures the [ApolloClient] to connect to AppSync. This function:
 * 1. Sets the serverUrl
 * 2. Adds an HttpInterceptor that adds authorization headers provided by the given [AppSyncAuthorizer]
 * 3. Sets up the [WebSocketNetworkTransport] to use an [AppSyncProtocol] instance.
 * @param endpoint The [AppSyncEndpoint] to connect to
 * @param authorizer The [AppSyncAuthorizer] that determines the authorization mode to use when connecting to AppSync
 * @return The builder instance for chaining
 */
fun ApolloClient.Builder.appSync(endpoint: AppSyncEndpoint, authorizer: AppSyncAuthorizer) =
    appSync(endpoint, authorizer, WebSocketNetworkTransport.Builder())

internal fun ApolloClient.Builder.appSync(
    endpoint: AppSyncEndpoint,
    authorizer: AppSyncAuthorizer,
    transportBuilder: WebSocketNetworkTransport.Builder
) = apply {
    serverUrl(endpoint.serverUrl.toString())

    subscriptionNetworkTransport(transportBuilder.appSync(endpoint, authorizer).build())

    addHttpInterceptor(AppSyncInterceptor(authorizer))
}
