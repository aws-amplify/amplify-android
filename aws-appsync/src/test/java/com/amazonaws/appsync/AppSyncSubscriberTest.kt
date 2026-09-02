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
package com.amazonaws.appsync

import app.cash.turbine.test
import com.amplifyframework.api.aws.GsonVariablesSerializer
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.SimpleGraphQLRequest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests [AppSyncSubscriber] — the subscription lifecycle and, mostly, its terminality rules.
 *
 * The distinction between terminal and non-terminal is easy to implement backwards, so each side has a
 * test: a GraphQL error carried *inside* a data message is **not** terminal (the service considered the
 * message deliverable), while a subscription `error` frame **is**. Likewise a socket close with a cause
 * throws but a clean close completes normally.
 *
 * The socket is mocked and its message flow driven directly, which is how the rest of the repo tests
 * WebSocket behaviour.
 *
 * Robolectric is required because `GraphQLRequest.getContent()` uses `android.text.TextUtils`.
 */
@RunWith(RobolectricTestRunner::class)
class AppSyncSubscriberTest {

    private val messages = MutableSharedFlow<AppSyncWebSocketMessage.Inbound>(extraBufferCapacity = 64)
    private val sentSlot = slot<AppSyncWebSocketMessage.Outbound>()

    private val socket: AppSyncWebSocket = mockk(relaxed = true) {
        every { this@mockk.messages } returns this@AppSyncSubscriberTest.messages
        every { isClosed } returns false
        every { send(capture(sentSlot)) } returns true
    }

    private fun subscriber(
        authorization: AppSyncAuthorization = AppSyncAuthorization.Single(
            AppSyncClientAuthorizer.ApiKey("da2-fakekey")
        ),
        ioDispatcher: CoroutineDispatcher = StandardTestDispatcher()
    ) = AppSyncSubscriber(
        provider = AppSyncWebSocketProvider { socket },
        authorization = authorization,
        decorator = AppSyncRequestDecorator("us-east-1"),
        httpEndpoint = "https://abc123.appsync-api.us-east-1.amazonaws.com/graphql",
        ioDispatcher = ioDispatcher
    )

    private fun request(): GraphQLRequest<String> = SimpleGraphQLRequest(
        "subscription { onCreateTodo { id name } }",
        emptyMap(),
        String::class.java,
        GsonVariablesSerializer()
    )

    private val startedId: String
        get() = (sentSlot.captured as AppSyncWebSocketMessage.Start).id

    // ── Lifecycle ───────────────────────────────────────────────────────

    @Test
    fun `emits Connecting then Connected then Data`() = runTest {
        subscriber().subscribe(request()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting

            val id = startedId
            messages.emit(AppSyncWebSocketMessage.StartAck(id))
            awaitItem() shouldBe SubscriptionEvent.Connected

            messages.emit(AppSyncWebSocketMessage.Data(id, """{"data":"hello"}"""))
            val data = awaitItem().shouldBeInstanceOf<SubscriptionEvent.Data<String>>()
            data.response.data shouldBe "hello"

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `nothing is sent until the flow is collected`() {
        subscriber().subscribe(request())

        verify(exactly = 0) { socket.send(any()) }
    }

    @Test
    fun `the start message carries the document and its auth headers`() = runTest {
        subscriber().subscribe(request()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting

            val start = sentSlot.captured.shouldBeInstanceOf<AppSyncWebSocketMessage.Start>()
            start.query shouldContain "onCreateTodo"
            start.authorizationHeaders["x-api-key"] shouldBe "da2-fakekey"

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancelling the collector sends stop`() = runTest {
        subscriber().subscribe(request()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting
            cancelAndIgnoreRemainingEvents()
        }

        verify { socket.send(match { it is AppSyncWebSocketMessage.Stop }) }
    }

    @Test
    fun `messages addressed to another subscription are ignored`() = runTest {
        subscriber().subscribe(request()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting

            messages.emit(AppSyncWebSocketMessage.StartAck("someone-else"))
            messages.emit(AppSyncWebSocketMessage.Data("someone-else", """{"data":"theirs"}"""))
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keep-alives and unknown frames do not disturb the stream`() = runTest {
        subscriber().subscribe(request()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting
            val id = startedId

            messages.emit(AppSyncWebSocketMessage.KeepAlive)
            messages.emit(AppSyncWebSocketMessage.Unknown("something_new", "{}"))
            expectNoEvents()

            messages.emit(AppSyncWebSocketMessage.StartAck(id))
            awaitItem() shouldBe SubscriptionEvent.Connected

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Non-terminal cases ──────────────────────────────────────────────

    @Test
    fun `graphql errors inside a data message are delivered, not terminal`() = runTest {
        // The service considered the message deliverable, so the consumer gets the data and the errors
        // and the subscription stays open.
        subscriber().subscribe(request()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting
            val id = startedId

            messages.emit(
                AppSyncWebSocketMessage.Data(
                    id,
                    """{"data":"partial","errors":[{"message":"Unauthorized on field secret"}]}"""
                )
            )

            val data = awaitItem().shouldBeInstanceOf<SubscriptionEvent.Data<String>>()
            data.response.data shouldBe "partial"
            data.response.errors.size shouldBe 1

            // Still open: another message arrives afterwards.
            messages.emit(AppSyncWebSocketMessage.Data(id, """{"data":"next"}"""))
            awaitItem().shouldBeInstanceOf<SubscriptionEvent.Data<String>>()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an undeserializable data message is dropped and the stream continues`() = runTest {
        // Losing one message is better than tearing down a working subscription.
        subscriber().subscribe(request()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting
            val id = startedId

            messages.emit(AppSyncWebSocketMessage.Data(id, "{not json"))
            expectNoEvents()

            messages.emit(AppSyncWebSocketMessage.Data(id, """{"data":"recovered"}"""))
            awaitItem().shouldBeInstanceOf<SubscriptionEvent.Data<String>>()

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Terminal cases ──────────────────────────────────────────────────

    @Test
    fun `an error frame for this subscription is terminal`() = runTest {
        subscriber().subscribe(request()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting
            val id = startedId

            messages.emit(AppSyncWebSocketMessage.Error(id, listOf("Validation failed")))

            awaitError().shouldBeInstanceOf<AppSyncGraphQLErrorException>()
                .message shouldContain "Validation failed"
        }
    }

    @Test
    fun `a connection-level error with no id is terminal for this subscription`() = runTest {
        subscriber().subscribe(request()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting

            messages.emit(AppSyncWebSocketMessage.Error(null, listOf("connection died")))

            awaitError().shouldBeInstanceOf<AppSyncGraphQLErrorException>()
        }
    }

    @Test
    fun `a connection_error is terminal`() = runTest {
        subscriber().subscribe(request()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting

            messages.emit(AppSyncWebSocketMessage.ConnectionError(listOf("Unauthorized")))

            awaitError().shouldBeInstanceOf<AppSyncConnectionException>()
                .message shouldContain "Unauthorized"
        }
    }

    @Test
    fun `a socket close with a cause is terminal`() = runTest {
        subscriber().subscribe(request()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting

            messages.emit(AppSyncWebSocketMessage.Closed(AppSyncTimeoutException("idle")))

            awaitError().shouldBeInstanceOf<AppSyncTimeoutException>()
        }
    }

    @Test
    fun `a failing authorizer makes the subscription terminal`() = runTest {
        val failing = subscriber(
            AppSyncAuthorization.Single(AppSyncClientAuthorizer.UserPools { error("expired") })
        )

        failing.subscribe(request()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting
            awaitError().shouldBeInstanceOf<AppSyncTokenFetchException>()
        }
    }

    // ── Normal completion ───────────────────────────────────────────────

    @Test
    fun `a complete frame ends the flow normally`() = runTest {
        subscriber().subscribe(request()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting
            val id = startedId

            messages.emit(AppSyncWebSocketMessage.Complete(id))

            awaitComplete()
        }
    }

    @Test
    fun `a clean socket close ends the flow normally rather than throwing`() = runTest {
        // This is what makes close() a graceful shutdown: a null cause means the client asked for it.
        subscriber().subscribe(request()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting

            messages.emit(AppSyncWebSocketMessage.Closed(cause = null))

            awaitComplete()
        }
    }

    @Test
    fun `a complete for another subscription does not end this one`() = runTest {
        subscriber().subscribe(request()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting
            val id = startedId

            messages.emit(AppSyncWebSocketMessage.Complete("someone-else"))
            expectNoEvents()

            messages.emit(AppSyncWebSocketMessage.StartAck(id))
            awaitItem() shouldBe SubscriptionEvent.Connected

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Connection state ────────────────────────────────────────────────

    @Test
    fun `events reports Connecting then Connected around the first subscribe`() = runTest {
        val subscriber = subscriber()

        subscriber.events.test {
            subscriber.subscribe(request()).test {
                awaitItem() shouldBe SubscriptionEvent.Connecting
                cancelAndIgnoreRemainingEvents()
            }

            awaitItem() shouldBe ConnectionState.Connecting
            awaitItem() shouldBe ConnectionState.Connected
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `events reports a Disconnected with the cause when connecting fails`() = runTest {
        val failing = AppSyncSubscriber(
            provider = AppSyncWebSocketProvider { throw AppSyncConnectionException("refused") },
            authorization = AppSyncAuthorization.Single(AppSyncClientAuthorizer.ApiKey("da2-fakekey")),
            decorator = AppSyncRequestDecorator("us-east-1"),
            httpEndpoint = "https://abc123.appsync-api.us-east-1.amazonaws.com/graphql"
        )

        failing.events.test {
            failing.subscribe(request()).test {
                awaitItem() shouldBe SubscriptionEvent.Connecting
                awaitError()
            }

            awaitItem() shouldBe ConnectionState.Connecting
            val disconnected = awaitItem().shouldBeInstanceOf<ConnectionState.Disconnected>()
            disconnected.cause.shouldBeInstanceOf<AppSyncConnectionException>()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `events reports a Disconnected with the cause when the connection dies unexpectedly`() = runTest {
        val subscriber = subscriber(ioDispatcher = UnconfinedTestDispatcher(testScheduler))

        subscriber.events.test {
            subscriber.subscribe(request()).test {
                awaitItem() shouldBe SubscriptionEvent.Connecting
                messages.emit(AppSyncWebSocketMessage.Closed(AppSyncTimeoutException("idle")))
                awaitError()
            }

            awaitItem() shouldBe ConnectionState.Connecting
            awaitItem() shouldBe ConnectionState.Connected
            val disconnected = awaitItem().shouldBeInstanceOf<ConnectionState.Disconnected>()
            disconnected.cause.shouldBeInstanceOf<AppSyncTimeoutException>()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `close reports a clean Disconnected with no cause`() = runTest {
        val subscriber = subscriber()

        subscriber.events.test {
            subscriber.close()

            awaitItem() shouldBe ConnectionState.Disconnected(null)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `events replays the latest state to a late observer`() = runTest {
        val subscriber = subscriber()
        subscriber.close()

        // A consumer that starts observing after the fact should still learn the current state.
        subscriber.events.test {
            awaitItem() shouldBe ConnectionState.Disconnected(null)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
