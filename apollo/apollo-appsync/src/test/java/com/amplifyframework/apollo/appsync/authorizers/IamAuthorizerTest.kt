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

package com.amplifyframework.apollo.appsync.authorizers

import com.amplifyframework.apollo.appsync.AppSyncEndpoint
import com.amplifyframework.apollo.appsync.toJson
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.http.HttpRequest
import io.kotest.matchers.maps.shouldContainAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Test

class IamAuthorizerTest {

    private val endpoint =
        AppSyncEndpoint("https://example1234567890123456789.appsync-api.us-east-1.amazonaws.com/graphql")
    private val delegate: (
        HttpRequest
    ) -> Map<String, String> = { mapOf("header1" to "header1Value", "header2" to "header2Value") }

    @Test
    fun `returns authorization header for HTTP requests`() = runTest {
        val authorizer = IamAuthorizer(delegate)
        val result = authorizer.getHttpAuthorizationHeaders(mockk())
        result shouldContainAll mapOf("header1" to "header1Value", "header2" to "header2Value")
    }

    @Test
    fun `returns authorization header for websocket requests`() = runTest {
        val authorizer = IamAuthorizer(delegate)
        val result = authorizer.getWebsocketConnectionHeaders(endpoint)
        result shouldContainAll mapOf("header1" to "header1Value", "header2" to "header2Value")
    }

    @Test
    fun `returns authorization payload for websocket requests`() = runTest {
        val apolloRequest = mockk<ApolloRequest<*>>()
        mockkStatic(ApolloRequest<*>::toJson) {
            every { apolloRequest.toJson() } returns "{}"
            val authorizer = IamAuthorizer(delegate)
            val result = authorizer.getWebSocketSubscriptionPayload(endpoint, apolloRequest)
            result shouldContainAll mapOf("header1" to "header1Value", "header2" to "header2Value")
        }
    }

    @Test
    fun `returns authorization header when using callback API`() = runTest {
        val authorizer = IamAuthorizer { request, onSuccess, _ -> onSuccess.accept(delegate(request)) }
        val result = authorizer.getHttpAuthorizationHeaders(mockk())
        result shouldContainAll mapOf("header1" to "header1Value", "header2" to "header2Value")
    }
}
