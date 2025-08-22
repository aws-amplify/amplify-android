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

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.network.ws.WebSocketConnection
import com.apollographql.apollo.network.ws.WsProtocol
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.mockk.Awaits
import io.mockk.Called
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppSyncProtocolTest {

    private val testScope = TestScope()

    private val endpoint =
        AppSyncEndpoint("https://example1234567890123456789.appsync-api.us-east-1.amazonaws.com/graphql")
    private val authorizer = mockk<AppSyncAuthorizer>()
    private val webSocket = mockk<WebSocketConnection> {
        coEvery { send(any<String>()) } just Runs
    }
    private val listener = mockk<WsProtocol.Listener>(relaxed = true)

    private val request = mockk<ApolloRequest<Operation.Data>> {
        every { requestUuid.toString() } returns "subscriptionId"
    }

    private val protocol = AppSyncProtocol(
        endpoint = endpoint,
        authorizer = authorizer,
        webSocketConnection = webSocket,
        listener = listener,
        scope = testScope
    )

    //region connectionInit tests

    @Test
    fun `sends connection init message and waits for ack on connection init`() = testScope.runTest {
        coEvery { webSocket.receive() } returns """{"type":"connection_ack"}"""
        protocol.connectionInit()

        coVerify {
            webSocket.send("""{"type":"connection_init"}""")
            webSocket.receive()
        }
    }

    @Test
    fun `throws if connection ack not received within timeout window`() = testScope.runTest {
        coEvery { webSocket.receive() } just Awaits

        shouldThrow<TimeoutCancellationException> {
            protocol.connectionInit()
            advanceTimeBy(10_000) // 10 seconds
        }
    }

    @Test
    fun `throws if connection error is received while waiting for ack`() = testScope.runTest {
        coEvery { webSocket.receive() } returns """{"type":"connection_error"}"""

        shouldThrowWithMessage<ApolloException>("""Subscription connection error""") {
            protocol.connectionInit()
        }
    }

    @Test
    fun `throws if message is received without a type while waiting for ack`() = testScope.runTest {
        coEvery { webSocket.receive() } returns """{"foo":"bar"}"""

        shouldThrowWithMessage<ApolloException>("""No AppSync message type specified: {foo=bar}""") {
            protocol.connectionInit()
        }
    }

    //endregion
    //region handleServerMessage tests

    @Test
    fun `emits response for subscription data`() = testScope.runTest {
        val payload = mapOf("foo" to "bar")
        val message = mapOf(
            "type" to "data",
            "id" to "subscriptionId",
            "payload" to payload
        )

        protocol.handleServerMessage(message)

        coVerify {
            listener.operationResponse("subscriptionId", payload)
        }
    }

    @Test
    fun `ignores keep-alive message`() = testScope.runTest {
        val message = mapOf("type" to "ka")
        protocol.handleServerMessage(message)
        verify {
            listener wasNot Called
        }
    }

    @Test
    fun `ignores unknown message type`() = testScope.runTest {
        val message = mapOf("type" to "start_ack")
        protocol.handleServerMessage(message)
        verify {
            listener wasNot Called
        }
    }

    @Test
    fun `emits general error if message does not have a type`() = testScope.runTest {
        val message = mapOf("foo" to "bar")
        protocol.handleServerMessage(message)
        verify {
            listener.generalError(message)
        }
    }

    @Test
    fun `emits operation complete for subscription complete message`() = testScope.runTest {
        val message = mapOf("type" to "complete", "id" to "some_id")
        protocol.handleServerMessage(message)
        verify {
            listener.operationComplete("some_id")
        }
    }

    @Test
    fun `emits operation error for subscription error with id`() = testScope.runTest {
        val message = mapOf("type" to "error", "id" to "some_id")
        protocol.handleServerMessage(message)
        verify {
            listener.operationError("some_id", null)
        }
    }

    @Test
    fun `emits general error for subscription error without id`() = testScope.runTest {
        val message = mapOf("type" to "error")
        protocol.handleServerMessage(message)
        verify {
            listener.generalError(null)
        }
    }

    //endregion
    //region startOperation tests

    @Test
    fun `sends expected subscription payload`() = testScope.runTest {
        mockkObject(DefaultHttpRequestComposer) {
            every { DefaultHttpRequestComposer.composePayload<Operation.Data>(any()) } returns mapOf("test" to "value")

            coEvery { authorizer.getWebSocketSubscriptionPayload(any(), any()) } returns mapOf("token" to "abc")
            coEvery { webSocket.receive() } returns """{"type":"start_ack"}"""

            protocol.startOperation(request)
            advanceUntilIdle()

            verify {
                webSocket.send(
                    """{"id":"subscriptionId","type":"start","payload":{"data":"{\"test\":\"value\"}",
                        |"extensions":{"authorization":{
                        |"host":"example1234567890123456789.appsync-api.us-east-1.amazonaws.com",
                        |"token":"abc"}}}}
                    """.trimMargin().replace("\n", "")
                )
            }
        }
    }

    @Test
    fun `emits operation error if subscription is not acknowledged`() = testScope.runTest {
        mockkObject(DefaultHttpRequestComposer) {
            every { DefaultHttpRequestComposer.composePayload<Operation.Data>(any()) } returns mapOf("test" to "value")

            coEvery { authorizer.getWebSocketSubscriptionPayload(any(), any()) } returns mapOf("token" to "abc")
            coEvery { webSocket.receive() } just Awaits

            protocol.startOperation(request)
            advanceUntilIdle()

            verify {
                listener.operationError("subscriptionId", null)
            }
        }
    }

    @Test
    fun `emits operation error if subscription error is received`() = testScope.runTest {
        mockkObject(DefaultHttpRequestComposer) {
            every { DefaultHttpRequestComposer.composePayload<Operation.Data>(any()) } returns mapOf("test" to "value")

            coEvery { authorizer.getWebSocketSubscriptionPayload(any(), any()) } returns mapOf("token" to "abc")
            coEvery { webSocket.receive() } returns """{"type":"error"}"""

            protocol.startOperation(request)
            advanceUntilIdle()

            verify {
                listener.operationError("subscriptionId", null)
            }
        }
    }

    @Test
    fun `emits operation error if message with no type is received`() = testScope.runTest {
        mockkObject(DefaultHttpRequestComposer) {
            every { DefaultHttpRequestComposer.composePayload<Operation.Data>(any()) } returns mapOf("test" to "value")

            coEvery { authorizer.getWebSocketSubscriptionPayload(any(), any()) } returns mapOf("token" to "abc")
            coEvery { webSocket.receive() } returns """{}"""

            protocol.startOperation(request)
            advanceUntilIdle()

            verify {
                listener.operationError("subscriptionId", null)
            }
        }
    }

    //endregion
    //region stopOperation tests

    @Test
    fun `sends subscription stop message `() = testScope.runTest {
        protocol.stopOperation(request)

        verify {
            webSocket.send("""{"id":"subscriptionId","type":"stop"}""")
        }
    }

    //endregion
}
