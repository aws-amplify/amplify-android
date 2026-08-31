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

package com.amplifyframework.api.aws

import com.amplifyframework.api.ApiException
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.EOFException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies the fast-fail behavior of [SubscriptionEndpoint] so that pending subscriptions do not
 * block for the full acknowledgement timeouts when the underlying connection fails or a message
 * cannot be sent. Without these behaviors, each of these tests would block for the 10s (start_ack)
 * or 30s (connection) timeout and the [Callbacks.done] latch would not count down in time.
 */
@RunWith(RobolectricTestRunner::class)
class SubscriptionEndpointFastFailTest {
    private val executor = Executors.newCachedThreadPool()
    private val listenerSlot = slot<WebSocketListener>()
    private val listenerReady = CountDownLatch(1)
    private val webSocket = mockk<WebSocket>(relaxed = true)
    private val okHttpClient = mockk<OkHttpClient>()
    private val authorizer = mockk<SubscriptionAuthorizer>()
    private val apiConfiguration = mockk<ApiConfiguration>()
    private val responseFactory = mockk<GraphQLResponse.Factory>(relaxed = true)
    private val request = mockk<GraphQLRequest<String>>(relaxed = true)

    private lateinit var endpoint: SubscriptionEndpoint

    private fun setup(sendBehavior: (String) -> Boolean = { true }) {
        every { apiConfiguration.endpoint } returns
            "https://abcdefghijklmnopqrstuvwxyz.appsync-api.us-east-1.amazonaws.com/graphql"
        every { authorizer.createHeadersForConnection(any()) } returns JSONObject()
        every { authorizer.createHeadersForSubscription(any(), any()) } returns JSONObject()
        every { request.content } returns "{}"
        every { request.responseType } returns String::class.java
        every { webSocket.send(any<String>()) } answers { sendBehavior(firstArg()) }
        every { okHttpClient.newWebSocket(any<Request>(), capture(listenerSlot)) } answers {
            listenerReady.countDown()
            webSocket
        }
        endpoint = SubscriptionEndpoint(apiConfiguration, responseFactory, authorizer, null, okHttpClient)
    }

    private class Callbacks {
        val startedRef = AtomicReference<String?>(null)
        val errorRef = AtomicReference<ApiException?>(null)
        val done = CountDownLatch(1)
    }

    private fun requestSubscriptionAsync(): Callbacks {
        val cb = Callbacks()
        executor.execute {
            try {
                endpoint.requestSubscription(
                    request,
                    AuthorizationType.API_KEY,
                    Consumer { id -> cb.startedRef.set(id) },
                    Consumer { /* onNextItem */ },
                    Consumer { error -> cb.errorRef.set(error) },
                    Action { /* onComplete */ }
                )
            } finally {
                cb.done.countDown()
            }
        }
        return cb
    }

    private fun awaitListener(): WebSocketListener {
        listenerReady.await(5, TimeUnit.SECONDS) shouldBe true
        return listenerSlot.captured
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
    fun `pending subscription fails fast when the connection fails`() {
        setup()
        val callbacks = requestSubscriptionAsync()
        val listener = awaitListener()

        driveConnected(listener)
        // Give the request thread a moment to send "start" and begin awaiting the start_ack.
        Thread.sleep(300)
        // A transport failure (e.g. EOFException on a network change) arrives while awaiting start_ack.
        listener.onFailure(webSocket, EOFException("connection reset"), null)

        // Without the fix, the request thread would block for the full 10s start_ack timeout.
        callbacks.done.await(5, TimeUnit.SECONDS) shouldBe true
        callbacks.startedRef.get().shouldBeNull()
        callbacks.errorRef.get().shouldNotBeNull()
    }

    @Test
    fun `subscription fails fast when the start message send is rejected`() {
        // The WebSocket rejects the "start" frame (socket closed/closing), but accepts connection_init.
        setup(sendBehavior = { message -> !message.contains("\"type\":\"start\"") })
        val callbacks = requestSubscriptionAsync()
        val listener = awaitListener()

        driveConnected(listener)

        // Without the fix, the request thread would block for the full 10s start_ack timeout.
        callbacks.done.await(5, TimeUnit.SECONDS) shouldBe true
        callbacks.startedRef.get().shouldBeNull()
        val error = callbacks.errorRef.get()
        error.shouldNotBeNull()
        error.message shouldContain "closed or closing"
    }

    @Test
    fun `connection fails fast when the connection_init send is rejected`() {
        // The WebSocket rejects the connection_init frame outright.
        setup(sendBehavior = { message -> !message.contains("connection_init") })
        val callbacks = requestSubscriptionAsync()
        val listener = awaitListener()

        // onOpen triggers the connection_init send, which is rejected.
        listener.onOpen(webSocket, mockk(relaxed = true))

        // Without the fix, the request thread would block for the full 30s connection timeout.
        callbacks.done.await(5, TimeUnit.SECONDS) shouldBe true
        callbacks.startedRef.get().shouldBeNull()
        val error = callbacks.errorRef.get()
        error.shouldNotBeNull()
        error.cause?.message shouldContain "connection_init"
    }
}
