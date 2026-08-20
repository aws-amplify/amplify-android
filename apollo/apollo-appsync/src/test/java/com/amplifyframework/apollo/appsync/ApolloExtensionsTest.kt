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

import com.amplifyframework.apollo.appsync.authorizers.ApiKeyAuthorizer
import com.amplifyframework.apollo.appsync.util.UserAgentHeader
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.AnyAdapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.apollographql.apollo.network.ws.WebSocketNetworkTransport
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.ByteString
import org.junit.Test

class ApolloExtensionsTest {

    private val builder = spyk(ApolloClient.Builder())
    private val endpoint =
        AppSyncEndpoint("https://example1234567890123456789.appsync-api.us-east-1.amazonaws.com/graphql")
    private val authorizer = ApiKeyAuthorizer("apiKey")

    @Test
    fun `sets serverUrl`() {
        builder.appSync(endpoint, authorizer)

        verify {
            builder.serverUrl("https://example1234567890123456789.appsync-api.us-east-1.amazonaws.com/graphql")
        }
    }

    @Test
    fun `sets http interceptor`() {
        builder.appSync(endpoint, authorizer)

        verify {
            builder.addHttpInterceptor(any<AppSyncInterceptor>())
        }
    }

    @Test
    fun `sets websocket protocol`() {
        val transportBuilder = mockk<WebSocketNetworkTransport.Builder>(relaxed = true)
        builder.appSync(endpoint, authorizer, transportBuilder)

        verify {
            transportBuilder.protocol(any<AppSyncProtocol.Factory>())
        }
    }

    @Test
    fun `appends the user agent header`() {
        val transportBuilder = mockk<WebSocketNetworkTransport.Builder>(relaxed = true)
        builder.appSync(endpoint, authorizer, transportBuilder)

        verify {
            transportBuilder.addHeader(UserAgentHeader.NAME, UserAgentHeader.value)
        }
    }

    @Test
    fun `sets websocket serverUrl`() = runTest {
        val transportBuilder = mockk<WebSocketNetworkTransport.Builder>(relaxed = true)
        builder.appSync(endpoint, authorizer, transportBuilder)

        verify {
            transportBuilder.serverUrl(
                "https://example1234567890123456789.appsync-realtime-api.us-east-1.amazonaws.com/graphql"
            )
        }
    }

    @Test
    fun `sets websocket engine`() {
        val transportBuilder = mockk<WebSocketNetworkTransport.Builder>(relaxed = true)
        builder.appSync(endpoint, authorizer, transportBuilder)

        verify {
            transportBuilder.webSocketEngine(any())
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun ByteString.toJsonMap(): Map<String, Any?> = AnyAdapter.fromJson(
        BufferedSourceJsonReader(Buffer().write(this)),
        CustomScalarAdapters.Empty
    ) as Map<String, Any?>
}
