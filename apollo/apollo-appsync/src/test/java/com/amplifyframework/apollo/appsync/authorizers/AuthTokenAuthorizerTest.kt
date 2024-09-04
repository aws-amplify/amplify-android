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
import io.kotest.matchers.maps.shouldContainAll
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AuthTokenAuthorizerTest {

    @Test
    fun `returns authorization header for HTTP requests`() = runTest {
        val delegate: () -> String = { "test" }

        val authorizer = AuthTokenAuthorizer(delegate)
        val result = authorizer.getHttpAuthorizationHeaders(mockk())

        result shouldContainAll mapOf(HeaderKeys.Http.AUTHORIZATION to "test")
    }

    @Test
    fun `returns authorization header for websocket requests`() = runTest {
        val delegate: () -> String = { "test" }

        val authorizer = AuthTokenAuthorizer(delegate)
        val result = authorizer.getWebsocketConnectionHeaders(mockk())

        result shouldContainAll mapOf(HeaderKeys.WebSocket.AUTHORIZATION to "test")
    }

    @Test
    fun `returns authorization payload for websocket requests`() = runTest {
        val delegate: () -> String = { "test" }

        val authorizer = AuthTokenAuthorizer(delegate)
        val result = authorizer.getWebSocketSubscriptionPayload(mockk(), mockk())

        result shouldContainAll mapOf(HeaderKeys.WebSocket.AUTHORIZATION to "test")
    }

    @Test
    fun `returns authorization header when using callback API`() = runTest {
        val authorizer = AuthTokenAuthorizer { onSuccess, _ -> onSuccess.accept("test") }
        val result = authorizer.getHttpAuthorizationHeaders(mockk())

        result shouldContainAll mapOf(HeaderKeys.Http.AUTHORIZATION to "test")
    }
}
