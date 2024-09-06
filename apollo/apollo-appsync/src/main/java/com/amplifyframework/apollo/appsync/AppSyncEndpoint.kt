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

package com.amplifyframework.apollo.appsync

import com.apollographql.apollo.api.json.buildJsonByteString
import com.apollographql.apollo.api.json.writeAny
import java.net.URL
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private val standardEndpointRegex =
    "^https://\\w{26}\\.appsync-api\\.\\w{2}(?:-\\w{2,})+-\\d\\.amazonaws.com/graphql$".toRegex()

/**
 * Class representing the AppSync endpoint. There are multiple URLs associated with each AppSync endpoint: the
 * appsync server URL, for sending HTTP requests, and the realtime URL, for establishing websocket connections. This
 * class derives the realtime URL from the server URL.
 */
class AppSyncEndpoint(serverUrl: String) {
    private val urlString = serverUrl

    /**
     * The URL to use for HTTP requests. Set this value as the serverUrl for the ApolloClient.
     */
    val serverUrl = URL(urlString)

    init {
        if (this.serverUrl.protocol != "https") {
            throw IllegalArgumentException("AppSync URL must be using TLS")
        }
    }

    // See SubscriptionEndpoint.buildConnectionRequestUrl
    private val realtime by lazy {
        if (standardEndpointRegex.matches(urlString)) {
            // For standard URLs we insert "realtime" into the domain
            URL(urlString.replace("appsync-api", "appsync-realtime-api"))
        } else {
            // For custom URLs we append "realtime" to the path
            URL("$urlString/realtime")
        }
    }

    internal val websocketConnection by lazy {
        // See SubscriptionEndpoint.buildConnectionRequestUrl
        URL("$realtime/connect")
    }

    /**
     * Creates the serverUrl to be used for the WebSocketTransport's serverUrl. For AppSync, this URL has authorization
     * information appended in query parameters. Set this value as the serverUrl for the WebSocketTransport.
     */
    suspend fun createWebsocketServerUrl(authorizer: AppSyncAuthorizer): String {
        val headers = mapOf("host" to serverUrl.host) + authorizer.getWebsocketConnectionHeaders(this)
        val authorization = headers.base64()

        val url = websocketConnection.toHttpUrlOrNull() ?: error("Invalid endpoint url")

        return url.newBuilder()
            .addQueryParameter("header", authorization)
            .addQueryParameter("payload", "e30=")
            .build()
            .toString()
    }

    private fun Map<String, String>.base64() = buildJsonByteString { writeAny(this@base64) }.base64()
}
