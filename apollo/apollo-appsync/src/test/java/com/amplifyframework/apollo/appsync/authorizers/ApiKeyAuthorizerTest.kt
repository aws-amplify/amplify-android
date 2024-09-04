/*
 *  Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.apollo.appsync.authorizers

import com.amplifyframework.apollo.appsync.util.HeaderKeys
import com.amplifyframework.apollo.appsync.util.Iso8601Timestamp
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import io.kotest.matchers.maps.shouldContainAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ApiKeyAuthorizerTest {
    private val httpRequest = HttpRequest.Builder(HttpMethod.Post, "url").build()

    @Test
    fun `returns static apiKey for HTTP headers`() = runTest {
        val apiKey = "my_test_api_key"

        val authorizer = ApiKeyAuthorizer(apiKey)
        val result = authorizer.getHttpAuthorizationHeaders(httpRequest)

        result shouldContainAll mapOf(HeaderKeys.Http.API_KEY to apiKey)
    }

    @Test
    fun `returns provided apiKey for HTTP headers`() = runTest {
        val apiKey = "my_test_api_key"

        val authorizer = ApiKeyAuthorizer { apiKey }
        val result = authorizer.getHttpAuthorizationHeaders(httpRequest)

        result shouldContainAll mapOf(HeaderKeys.Http.API_KEY to apiKey)
    }

    @Test
    fun `returns static apiKey for websocket headers`() = runTest {
        val apiKey = "my_test_api_key"

        mockkObject(Iso8601Timestamp) {
            every { Iso8601Timestamp.now() } returns "now"

            val authorizer = ApiKeyAuthorizer(apiKey)
            val result = authorizer.getWebsocketConnectionHeaders(mockk())

            result shouldContainAll mapOf(
                HeaderKeys.WebSocket.API_KEY to apiKey,
                HeaderKeys.WebSocket.AMAZON_DATE to "now"
            )
        }
    }

    @Test
    fun `returns provided apiKey for websocket headers`() = runTest {
        val apiKey = "my_test_api_key"

        mockkObject(Iso8601Timestamp) {
            every { Iso8601Timestamp.now() } returns "now"

            val authorizer = ApiKeyAuthorizer { apiKey }
            val result = authorizer.getWebsocketConnectionHeaders(mockk())

            result shouldContainAll mapOf(
                HeaderKeys.WebSocket.API_KEY to apiKey,
                HeaderKeys.WebSocket.AMAZON_DATE to "now"
            )
        }
    }

    @Test
    fun `returns static apiKey for websocket payload`() = runTest {
        val apiKey = "my_test_api_key"

        mockkObject(Iso8601Timestamp) {
            every { Iso8601Timestamp.now() } returns "now"

            val authorizer = ApiKeyAuthorizer(apiKey)
            val result = authorizer.getWebSocketSubscriptionPayload(mockk(), mockk())

            result shouldContainAll mapOf(
                HeaderKeys.WebSocket.API_KEY to apiKey,
                HeaderKeys.WebSocket.AMAZON_DATE to "now"
            )
        }
    }

    @Test
    fun `returns provided apiKey for websocket payload`() = runTest {
        val apiKey = "my_test_api_key"

        mockkObject(Iso8601Timestamp) {
            every { Iso8601Timestamp.now() } returns "now"

            val authorizer = ApiKeyAuthorizer { apiKey }
            val result = authorizer.getWebSocketSubscriptionPayload(mockk(), mockk())

            result shouldContainAll mapOf(
                HeaderKeys.WebSocket.API_KEY to apiKey,
                HeaderKeys.WebSocket.AMAZON_DATE to "now"
            )
        }
    }

    @Test
    fun `returns provided API key when using callback API`() = runTest {
        val apiKey = "my_test_api_key"

        val authorizer = ApiKeyAuthorizer { onSuccess, _ -> onSuccess.accept(apiKey) }
        val result = authorizer.getHttpAuthorizationHeaders(httpRequest)

        result shouldContainAll mapOf(HeaderKeys.Http.API_KEY to apiKey)
    }
}
