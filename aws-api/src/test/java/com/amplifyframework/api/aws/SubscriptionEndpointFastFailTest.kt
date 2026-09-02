/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws

import com.amplifyframework.api.ApiException
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import java.io.EOFException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies the fast-fail behavior of [SubscriptionEndpoint] so that pending subscriptions do not
 * block for the full acknowledgement timeouts when the underlying connection fails or a message
 * cannot be sent.
 *
 * Each test asserts the failure is reported within 5s. Without the fixes the request thread would
 * block for the full 10s (start_ack) or 30s (connection) timeout — or, for the `connection_error`
 * path, never report at all — so the 5s [withTimeout] would fail the test.
 */
@RunWith(RobolectricTestRunner::class)
class SubscriptionEndpointFastFailTest {
    private val webSocket = mockk<WebSocket>(relaxed = true)
    private val okHttpClient = mockk<OkHttpClient>()
    private val authorizer = mockk<SubscriptionAuthorizer>()
    private val apiConfiguration = mockk<ApiConfiguration>()
    private val responseFactory = mockk<GraphQLResponse.Factory>(relaxed = true)
    private val request = mockk<GraphQLRequest<String>>(relaxed = true)

    // Explicit ordering signals (no sleeps): the listener is captured when newWebSocket is called,
    // and startSent fires the moment the request thread sends the "start" frame.
    private val listenerReady = CompletableDeferred<WebSocketListener>()
    private val startSent = CompletableDeferred<Unit>()

    private lateinit var endpoint: SubscriptionEndpoint

    private fun setup(sendBehavior: (String) -> Boolean = { true }) {
        every { apiConfiguration.endpoint } returns
            "https://abcdefghijklmnopqrstuvwxyz.appsync-api.us-east-1.amazonaws.com/graphql"
        every { authorizer.createHeadersForConnection(any()) } returns JSONObject()
        every { authorizer.createHeadersForSubscription(any(), any()) } returns JSONObject()
        every { request.content } returns "{}"
        every { request.responseType } returns String::class.java
        every { webSocket.send(any<String>()) } answers {
            val message = firstArg<String>()
            if (message.contains("\"type\":\"start\"")) {
                startSent.complete(Unit)
            }
            sendBehavior(message)
        }
        every { okHttpClient.newWebSocket(any<Request>(), any()) } answers {
            listenerReady.complete(secondArg())
            webSocket
        }
        endpoint = SubscriptionEndpoint(apiConfiguration, responseFactory, authorizer, null, okHttpClient)
    }

    private class Callbacks {
        val started = CompletableDeferred<String>()
        val errored = CompletableDeferred<ApiException>()
    }

    /** Launches the blocking [SubscriptionEndpoint.requestSubscription] on a background dispatcher. */
    private fun CoroutineScope.launchRequest(): Callbacks {
        val callbacks = Callbacks()
        launch(Dispatchers.IO) {
            endpoint.requestSubscription(
                request,
                AuthorizationType.API_KEY,
                Consumer { id -> callbacks.started.complete(id) },
                Consumer { /* onNextItem */ },
                Consumer { error -> callbacks.errored.complete(error) },
                Action { /* onComplete */ }
            )
        }
        return callbacks
    }

    private fun driveConnected(listener: WebSocketListener) {
        // onOpen triggers connection_init, then a connection_ack transitions the endpoint to CONNECTED.
        listener.onOpen(webSocket, mockk(relaxed = true))
        listener.onMessage(
            webSocket,
            JSONObject()
                .put("type", "connection_ack")
                .put("payload", JSONObject().put("connectionTimeoutMs", "300000"))
                .toString()
        )
    }

    @Test
    fun `pending subscription fails fast when the connection fails`() = runTest {
        setup()
        val callbacks = launchRequest()

        withContext(Dispatchers.IO) {
            val listener = withTimeout(5.seconds) { listenerReady.await() }
            driveConnected(listener)
            // Wait until the request thread has actually sent "start" and is awaiting the start_ack.
            withTimeout(5.seconds) { startSent.await() }
            // A transport failure (e.g. EOFException on a network change) arrives while awaiting start_ack.
            listener.onFailure(webSocket, EOFException("connection reset"), null)

            withTimeout(5.seconds) { callbacks.errored.await() }.shouldNotBeNull()
        }
        callbacks.started.isCompleted.shouldBeFalse()
    }

    @Test
    fun `subscription fails fast when the start message send is rejected`() = runTest {
        // The WebSocket rejects the "start" frame (socket closed/closing), but accepts connection_init.
        setup(sendBehavior = { message -> !message.contains("\"type\":\"start\"") })
        val callbacks = launchRequest()

        withContext(Dispatchers.IO) {
            val listener = withTimeout(5.seconds) { listenerReady.await() }
            driveConnected(listener)

            val error = withTimeout(5.seconds) { callbacks.errored.await() }
            error.message shouldContain "closed or closing"
        }
        callbacks.started.isCompleted.shouldBeFalse()
    }

    @Test
    fun `connection fails fast when the connection_init send is rejected`() = runTest {
        // The WebSocket rejects the connection_init frame outright.
        setup(sendBehavior = { message -> !message.contains("connection_init") })
        val callbacks = launchRequest()

        withContext(Dispatchers.IO) {
            val listener = withTimeout(5.seconds) { listenerReady.await() }
            // onOpen triggers the connection_init send, which is rejected.
            listener.onOpen(webSocket, mockk(relaxed = true))

            val error = withTimeout(5.seconds) { callbacks.errored.await() }
            error.cause?.message shouldContain "connection_init"
        }
        callbacks.started.isCompleted.shouldBeFalse()
    }

    @Test
    fun `pending subscription is reported when a connection_error frame arrives`() = runTest {
        setup()
        val callbacks = launchRequest()

        withContext(Dispatchers.IO) {
            val listener = withTimeout(5.seconds) { listenerReady.await() }
            driveConnected(listener)
            withTimeout(5.seconds) { startSent.await() }
            // A connection_error frame arrives while the subscription is awaiting start_ack.
            listener.onMessage(
                webSocket,
                JSONObject()
                    .put("type", "connection_error")
                    .put("payload", JSONObject().put("errors", JSONArray().put(JSONObject().put("message", "boom"))))
                    .toString()
            )

            // Without the error dispatch on this path, the subscription would be silently released.
            withTimeout(5.seconds) { callbacks.errored.await() }.shouldNotBeNull()
        }
        callbacks.started.isCompleted.shouldBeFalse()
    }
}
