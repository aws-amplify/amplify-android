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
import com.amplifyframework.api.aws.AppSyncGraphQLRequest
import com.amplifyframework.api.aws.GsonVariablesSerializer
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.SimpleGraphQLRequest
import com.amplifyframework.core.model.AuthRule
import com.amplifyframework.core.model.AuthStrategy
import com.amplifyframework.core.model.ModelOperation
import com.amplifyframework.core.model.ModelSchema
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.encodeUtf8
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

    /** The socket's settled terminal state. Completing it is how these tests simulate the socket dying. */
    private val terminated = CompletableDeferred<AppSyncException?>()

    private val socket: AppSyncWebSocket = mockk(relaxed = true) {
        every { this@mockk.messages } returns this@AppSyncSubscriberTest.messages
        every { isClosed } returns false
        every { send(capture(sentSlot)) } returns true
        every { closure } returns terminated
    }

    private fun subscriber(
        authorization: AppSyncAuthorization = AppSyncAuthorization.Single(
            AppSyncClientAuthorizer.ApiKey("da2-fakekey")
        ),
        // Deliberately NOT linked to runTest's scheduler: leaving it unadvanced keeps the subscriber's
        // disconnect watcher from running, which is what lets the tests below assert expectNoEvents().
        // The disconnect test opts into a linked dispatcher precisely to make the watcher run.
        ioDispatcher: CoroutineDispatcher = StandardTestDispatcher()
    ) = AppSyncSubscriber(
        provider = AppSyncWebSocketProvider(UnconfinedTestDispatcher()) { socket },
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

    /** Auth rules are irrelevant here — a Multi with two authorizers is enough to have a fallback. */
    private fun multiAuth() = AppSyncAuthorization.Multi(
        defaultAuthMode = AppSyncAuthMode.USER_POOLS,
        authorizers = listOf(
            AppSyncClientAuthorizer.UserPools { OWNER_JWT },
            AppSyncClientAuthorizer.ApiKey("da2-fakekey")
        )
    )

    /**
     * A request carrying model metadata, so auth-rule resolution yields more than one candidate mode.
     * A raw request has no schema and therefore resolves to the default alone, which is why retry does
     * not engage for one.
     *
     * Mocked rather than built because a real [AppSyncGraphQLRequest] also runs selection-set
     * generation, which needs a code-generated model class.
     */
    private fun modelRequest(): AppSyncGraphQLRequest<String> {
        val ownerRule = AuthRule.builder()
            .authStrategy(AuthStrategy.OWNER)
            .authProvider(AuthStrategy.OWNER.defaultAuthProvider)
            .identityClaim("cognito:username")
            .ownerField("owner")
            .operations(listOf(ModelOperation.READ))
            .build()
        val publicRule = AuthRule.builder()
            .authStrategy(AuthStrategy.PUBLIC)
            .authProvider(AuthStrategy.Provider.API_KEY)
            .operations(listOf(ModelOperation.READ))
            .build()

        return mockk {
            every { content } returns """{"query":"subscription { onCreateTodo { id name } }"}"""
            every { responseType } returns String::class.java
            every { modelSchema } returns ModelSchema.builder()
                .name("Todo")
                .authRules(listOf(ownerRule, publicRule))
                .build()
            every { authRuleOperation } returns ModelOperation.READ
            every { authorizationType } returns null
            // The owner rule means claim injection rebuilds the request; the rebuilt one only needs to
            // answer content.
            every { newBuilder() } returns mockk(relaxed = true) {
                every { variable(any(), any(), any()) } returns this
                every { build<String>() } returns mockk(relaxed = true) {
                    every { content } returns
                        """{"query":"subscription { onCreateTodo { id name } }","variables":{"owner":"alice"}}"""
                }
            }
        }
    }

    private fun wireError(message: String, errorType: String? = null) =
        AppSyncWebSocketMessage.WireError(message, errorType)

    private fun unauthorized() = wireError("Not Authorized to access onCreateTodo", "Unauthorized")

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

            messages.emit(AppSyncWebSocketMessage.Error(id, listOf(wireError("Validation failed"))))

            val error = awaitError().shouldBeInstanceOf<AppSyncGraphQLErrorException>()
            error.message shouldContain "Validation failed"
            // The parsed errors are carried on the exception, so its own recovery suggestion —
            // "inspect the errors list" — is actually actionable.
            error.errors.single().message shouldBe "Validation failed"
        }
    }

    @Test
    fun `a connection-level error with no id is terminal for this subscription`() = runTest {
        subscriber().subscribe(request()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting

            messages.emit(AppSyncWebSocketMessage.Error(null, listOf(wireError("connection died"))))

            awaitError().shouldBeInstanceOf<AppSyncGraphQLErrorException>()
        }
    }

    @Test
    fun `a connection_error is terminal`() = runTest {
        subscriber().subscribe(request()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting

            messages.emit(AppSyncWebSocketMessage.ConnectionError(listOf(wireError("Unauthorized"))))

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

    // ── Multi-auth retry ────────────────────────────────────────────────

    @Test
    fun `an unauthorized error frame re-registers under the next auth mode`() = runTest {
        val subscriber = subscriber(multiAuth())

        subscriber.subscribe(modelRequest()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting
            val firstId = startedId
            (sentSlot.captured as AppSyncWebSocketMessage.Start)
                .authorizationHeaders["authorization"] shouldBe OWNER_JWT

            messages.emit(AppSyncWebSocketMessage.Error(firstId, listOf(unauthorized())))

            // Re-registered under a new id and a different identity, without ending the stream.
            val retry = sentSlot.captured.shouldBeInstanceOf<AppSyncWebSocketMessage.Start>()
            retry.id shouldNotBe firstId
            retry.authorizationHeaders["x-api-key"] shouldBe "da2-fakekey"
            expectNoEvents()

            messages.emit(AppSyncWebSocketMessage.StartAck(retry.id))
            awaitItem() shouldBe SubscriptionEvent.Connected

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an unauthorized data message also triggers a retry`() = runTest {
        // AppSync can reject the identity in a data message rather than an error frame.
        val subscriber = subscriber(multiAuth())

        subscriber.subscribe(modelRequest()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting
            val firstId = startedId

            messages.emit(
                AppSyncWebSocketMessage.Data(
                    firstId,
                    """{"errors":[{"message":"Not Authorized","extensions":{"errorType":"Unauthorized"}}]}"""
                )
            )

            // Retried rather than handed to the consumer.
            expectNoEvents()
            (sentSlot.captured as AppSyncWebSocketMessage.Start).id shouldNotBe firstId

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exhausting every auth mode reports which were attempted`() = runTest {
        val subscriber = subscriber(multiAuth())

        subscriber.subscribe(modelRequest()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting

            messages.emit(AppSyncWebSocketMessage.Error(startedId, listOf(unauthorized())))
            messages.emit(AppSyncWebSocketMessage.Error(startedId, listOf(unauthorized())))

            val error = awaitError().shouldBeInstanceOf<AppSyncAuthExhaustedException>()
            error.attemptedAuthModes shouldBe listOf(AppSyncAuthMode.USER_POOLS, AppSyncAuthMode.API_KEY)
        }
    }

    @Test
    fun `a non-auth error is terminal even when other auth modes remain`() = runTest {
        // Retrying a validation failure would fail identically under a different identity and hide the
        // real reason behind an exhaustion error.
        val subscriber = subscriber(multiAuth())

        subscriber.subscribe(modelRequest()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting

            messages.emit(
                AppSyncWebSocketMessage.Error(startedId, listOf(wireError("Validation failed")))
            )

            awaitError().shouldBeInstanceOf<AppSyncGraphQLErrorException>()
        }
    }

    @Test
    fun `a mode whose credentials cannot be obtained is skipped`() = runTest {
        val subscriber = subscriber(
            AppSyncAuthorization.Multi(
                defaultAuthMode = AppSyncAuthMode.API_KEY,
                authorizers = listOf(
                    AppSyncClientAuthorizer.UserPools { error("session expired") },
                    AppSyncClientAuthorizer.ApiKey("da2-fakekey")
                )
            )
        )

        subscriber.subscribe(modelRequest()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting

            // The first mode never produced a start message; the second did.
            (sentSlot.captured as AppSyncWebSocketMessage.Start)
                .authorizationHeaders["x-api-key"] shouldBe "da2-fakekey"

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `single auth does not retry, and surfaces the rejection reason directly`() = runTest {
        // With one candidate there is nothing to exhaust, so reporting "auth exhausted" would hide what
        // the service actually said.
        subscriber().subscribe(modelRequest()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting

            messages.emit(AppSyncWebSocketMessage.Error(startedId, listOf(unauthorized())))

            awaitError().shouldBeInstanceOf<AppSyncGraphQLErrorException>()
                .message shouldContain "Not Authorized"
        }
    }

    @Test
    fun `a subscription limit error is terminal and is not retried`() = runTest {
        // The limit belongs to the API, not the identity, so another auth mode would fail the same way.
        val subscriber = subscriber(multiAuth())

        subscriber.subscribe(modelRequest()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting
            val firstId = startedId

            messages.emit(
                AppSyncWebSocketMessage.Error(
                    firstId,
                    listOf(wireError("Max number of 100 subscriptions reached", "MaxSubscriptionsReachedError"))
                )
            )

            awaitError().shouldBeInstanceOf<AppSyncSubscriptionLimitExceededException>()
                .message shouldContain "100 subscriptions"
        }
    }

    @Test
    fun `a rate limit error wins over an unauthorized error in the same frame`() = runTest {
        // Carries both so the precedence is actually exercised: with a rate-limit error alone the
        // auth-retry branch is not a candidate at all, and this would pass wherever the check sat.
        // A rate limit belongs to the API, so retrying under another identity only wastes an attempt.
        val subscriber = subscriber(multiAuth())

        subscriber.subscribe(modelRequest()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting

            messages.emit(
                AppSyncWebSocketMessage.Error(
                    startedId,
                    listOf(unauthorized(), wireError("Rate exceeded", "LimitExceededError"))
                )
            )

            awaitError().shouldBeInstanceOf<AppSyncRateLimitExceededException>()
                .message shouldContain "Rate exceeded"
        }
    }

    @Test
    fun `the subscription limit wins over a rate limit in the same frame`() = runTest {
        // The two are separate types precisely because the remedies differ, so which one a frame
        // carrying both resolves to is a decision worth pinning rather than leaving to branch order.
        val subscriber = subscriber(multiAuth())

        subscriber.subscribe(modelRequest()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting

            messages.emit(
                AppSyncWebSocketMessage.Error(
                    startedId,
                    listOf(
                        wireError("Rate exceeded", "LimitExceededError"),
                        wireError("limit", "MaxSubscriptionsReachedError")
                    )
                )
            )

            awaitError().shouldBeInstanceOf<AppSyncSubscriptionLimitExceededException>()
        }
    }

    @Test
    fun `a subscription limit error wins over an unauthorized error in the same frame`() = runTest {
        val subscriber = subscriber(multiAuth())

        subscriber.subscribe(modelRequest()).test {
            awaitItem() shouldBe SubscriptionEvent.Connecting

            messages.emit(
                AppSyncWebSocketMessage.Error(
                    startedId,
                    listOf(unauthorized(), wireError("limit", "MaxSubscriptionsReachedError"))
                )
            )

            awaitError().shouldBeInstanceOf<AppSyncSubscriptionLimitExceededException>()
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
            provider = AppSyncWebSocketProvider(UnconfinedTestDispatcher()) {
                throw AppSyncConnectionException("refused")
            },
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
                terminated.complete(AppSyncTimeoutException("idle"))
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

    private companion object {
        /**
         * Unsigned JWT carrying `username`, which is what an owner rule's identity claim resolves to.
         * The model request below carries an owner rule, so claim injection reads this token; a token
         * that is not a JWT would make the mode be skipped rather than tried.
         */
        val OWNER_JWT = "header." +
            """{"username":"alice"}""".encodeUtf8().base64Url().trimEnd('=') + ".signature"
    }
}
