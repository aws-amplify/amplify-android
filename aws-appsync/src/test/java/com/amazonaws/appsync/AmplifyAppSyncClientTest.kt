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

import com.amplifyframework.api.aws.AppSyncGraphQLRequest
import com.amplifyframework.api.aws.AuthorizationType
import com.amplifyframework.api.aws.GsonVariablesSerializer
import com.amplifyframework.api.graphql.SimpleGraphQLRequest
import com.amplifyframework.core.model.AuthRule
import com.amplifyframework.core.model.AuthStrategy
import com.amplifyframework.core.model.ModelOperation
import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.testutils.assertions.shouldBeFailure
import com.amplifyframework.testutils.assertions.shouldBeSuccess
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * End-to-end tests for [AmplifyAppSyncClient.query] and [AmplifyAppSyncClient.mutate] over a
 * [MockWebServer]. These cover the whole HTTP path — request construction, auth decoration, transport
 * and deserialization — and are the only place the error mapping is exercised the way a caller sees
 * it, as a `Result.Failure` carrying a specific [AppSyncException] subtype.
 *
 * Robolectric is required because the transport sets a User-Agent, and building it reads
 * `android.os.Build`, which is a stub on a plain JVM.
 */
@RunWith(RobolectricTestRunner::class)
class AmplifyAppSyncClientTest {

    private val server = MockWebServer()

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ── Success ─────────────────────────────────────────────────────────

    @Test
    fun `query returns the deserialized data`() = runTest {
        server.enqueue(jsonResponse("""{"data":"hello"}"""))

        val response = client().query(request()).shouldBeSuccess().data

        response.data shouldBe "hello"
        response.hasErrors() shouldBe false
    }

    @Test
    fun `mutate returns the deserialized data`() = runTest {
        server.enqueue(jsonResponse("""{"data":"created"}"""))

        client().mutate(request()).shouldBeSuccess().data.data shouldBe "created"
    }

    @Test
    fun `the request is a POST of the request content with the auth and content-type headers`() = runTest {
        server.enqueue(jsonResponse("""{"data":"hello"}"""))

        client().query(request()).shouldBeSuccess()

        val recorded = server.awaitRequest()
        recorded.method shouldBe "POST"
        recorded.path shouldBe "/graphql"
        recorded.getHeader("x-api-key") shouldBe "da2-fakekey"
        recorded.getHeader("accept") shouldBe "application/json"
        recorded.getHeader("content-type") shouldContain "application/json"
        recorded.body.readUtf8() shouldContain "getTodo"
    }

    @Test
    fun `a user agent is sent`() = runTest {
        server.enqueue(jsonResponse("""{"data":"hello"}"""))

        client().query(request()).shouldBeSuccess()

        server.awaitRequest().getHeader("User-Agent").isNullOrBlank() shouldBe false
    }

    @Test
    fun `the http client configurator is applied`() = runTest {
        server.enqueue(jsonResponse("""{"data":"hello"}"""))
        var configured = false

        client { httpClientConfigurator = { configured = true } }.query(request())

        configured shouldBe true
    }

    // ── GraphQL errors ──────────────────────────────────────────────────

    @Test
    fun `errors alongside data are a success with errors populated, not a failure`() = runTest {
        // Partial success: AppSync returns 200 with both. Reporting this as a Failure would discard
        // the data the service did return.
        server.enqueue(
            jsonResponse("""{"data":"partial","errors":[{"message":"Unauthorized on field secret"}]}""")
        )

        val response = client().query(request()).shouldBeSuccess().data

        response.data shouldBe "partial"
        response.errors shouldHaveSize 1
        response.errors[0].message shouldBe "Unauthorized on field secret"
    }

    @Test
    fun `a 4xx with a graphql error body becomes a GraphQLError exception carrying the errors`() = runTest {
        server.enqueue(
            jsonResponse(
                """{"errors":[{"message":"Validation error: unknown field 'nope'"}]}""",
                code = HttpURLConnection.HTTP_BAD_REQUEST
            )
        )

        val error = client().query(request()).shouldBeFailure().error
            .shouldBeInstanceOf<AppSyncGraphQLErrorException>()

        error.errors shouldHaveSize 1
        error.errors[0].message shouldContain "unknown field"
        // The message surfaces the service's own reason, not merely the status code.
        error.message shouldContain "unknown field"
    }

    @Test
    fun `a 401 with no parseable body is an auth failure`() = runTest {
        // Reported in the auth category rather than as a request-shape problem, so a caller can catch
        // AppSyncAuthException to re-authenticate without inspecting status codes.
        server.enqueue(jsonResponse("Unauthorized", code = HttpURLConnection.HTTP_UNAUTHORIZED))

        val error = client().query(request()).shouldBeFailure().error

        error.shouldBeInstanceOf<AppSyncUnauthorizedException>()
        error.shouldBeInstanceOf<AppSyncAuthException>()
        error.message shouldContain "401"
        error.errors.shouldBeEmpty()
    }

    @Test
    fun `a 401 carrying errors keeps them on the exception`() = runTest {
        server.enqueue(
            jsonResponse(
                """{"errors":[{"message":"Valid authorization header not provided"}]}""",
                code = HttpURLConnection.HTTP_UNAUTHORIZED
            )
        )

        val error = client().query(request()).shouldBeFailure().error
            .shouldBeInstanceOf<AppSyncUnauthorizedException>()

        error.errors.single().message shouldBe "Valid authorization header not provided"
        error.message shouldContain "Valid authorization header not provided"
    }

    @Test
    fun `a 429 is a rate limit, not a request problem`() = runTest {
        // A throttle used to arrive as a validation failure, whose advice is to correct the request —
        // misleading, because the request is fine and only its timing is not.
        server.enqueue(jsonResponse("Too Many Requests", code = 429))

        val error = client().query(request()).shouldBeFailure().error

        error.shouldBeInstanceOf<AppSyncRateLimitExceededException>()
        error.recoverySuggestion shouldContain "backoff"
    }

    @Test
    fun `a LimitExceededError type under another status is still a rate limit`() = runTest {
        server.enqueue(
            jsonResponse(
                """{"errors":[{"message":"Rate exceeded","extensions":{"errorType":"LimitExceededError"}}]}""",
                code = HttpURLConnection.HTTP_BAD_REQUEST
            )
        )

        client().query(request()).shouldBeFailure().error
            .shouldBeInstanceOf<AppSyncRateLimitExceededException>()
            // The service's own reason reaches the message; the 429 case above has no body to carry one.
            .message shouldContain "Rate exceeded"
    }

    @Test
    fun `an Unauthorized error type under a non-401 status is still an auth failure`() = runTest {
        // The error type is the authoritative signal; AppSync does not always pair it with a 401.
        server.enqueue(
            jsonResponse(
                """{"errors":[{"message":"Not Authorized to access getTodo","extensions":{"errorType":"Unauthorized"}}]}""",
                code = HttpURLConnection.HTTP_BAD_REQUEST
            )
        )

        client().query(request()).shouldBeFailure().error
            .shouldBeInstanceOf<AppSyncUnauthorizedException>()
    }

    // ── Transport failures ──────────────────────────────────────────────

    @Test
    fun `a 5xx becomes a network exception, since it is usually transient`() = runTest {
        server.enqueue(jsonResponse("boom", code = HttpURLConnection.HTTP_INTERNAL_ERROR))

        val error = client().query(request()).shouldBeFailure().error

        error.shouldBeInstanceOf<AppSyncNetworkException>()
        error.message shouldContain "500"
    }

    @Test
    fun `an unfollowed redirect is not reported as transient`() = runTest {
        server.enqueue(
            jsonResponse("", code = HttpURLConnection.HTTP_MOVED_PERM)
                .setHeader("Location", "https://example.invalid/graphql")
        )

        val error = client { httpClientConfigurator = { it.followRedirects(false) } }
            .query(request()).shouldBeFailure().error

        error.shouldBeInstanceOf<AppSyncNetworkException>()
        error.message shouldContain "301"
        // Retrying a redirect the client will not follow produces the same response every time.
        error.recoverySuggestion shouldNotContain "Retry"
    }

    @Test
    fun `an unreachable endpoint becomes a network exception`() = runTest {
        // Port 1 is reserved, so nothing is listening.
        val unreachable = AmplifyAppSyncClient(
            AmplifyAppSyncClient.Configuration {
                endpoint = "http://localhost:1/graphql"
                authorization = AppSyncAuthorization.Single(AppSyncClientAuthorizer.ApiKey("da2-fakekey"))
                region = "us-east-1"
            }
        )

        unreachable.query(request()).shouldBeFailure().error
            .shouldBeInstanceOf<AppSyncNetworkException>()
    }

    @Test
    fun `an empty body becomes a deserialization exception`() = runTest {
        server.enqueue(jsonResponse(""))

        client().query(request()).shouldBeFailure().error
            .shouldBeInstanceOf<AppSyncDeserializationException>()
    }

    @Test
    fun `a malformed body becomes a deserialization exception`() = runTest {
        server.enqueue(jsonResponse("{not json at all"))

        client().query(request()).shouldBeFailure().error
            .shouldBeInstanceOf<AppSyncDeserializationException>()
    }

    // ── Auth failures reach the caller typed ────────────────────────────

    @Test
    fun `a failing token supplier surfaces as a token fetch failure without an http call`() = runTest {
        val failing = AmplifyAppSyncClient(
            AmplifyAppSyncClient.Configuration {
                endpoint = server.url("/graphql").toString()
                authorization = AppSyncAuthorization.Single(
                    AppSyncClientAuthorizer.UserPools { error("session expired") }
                )
                region = "us-east-1"
            }
        )

        failing.query(request()).shouldBeFailure().error
            .shouldBeInstanceOf<AppSyncTokenFetchException>()

        // Nothing was sent — the request never got past decoration.
        server.requestCount shouldBe 0
    }

    // ── close() ─────────────────────────────────────────────────────────

    @Test
    fun `a closed client fails subsequent requests instead of sending them`() = runTest {
        val client = client()
        client.close()

        val error = client.query(request()).shouldBeFailure().error

        error.shouldBeInstanceOf<AppSyncInvalidConfigException>()
        error.message shouldContain "closed"
        server.requestCount shouldBe 0
    }

    @Test
    fun `close on a client that never issued a request does not build the http client`() = runTest {
        var configured = false
        val client = client { httpClientConfigurator = { configured = true } }

        client.close()

        configured shouldBe false
    }

    @Test
    fun `close is idempotent`() = runTest {
        val client = client()

        client.close()
        client.close()

        client.query(request()).shouldBeFailure()
    }

    // ── Cancellation ────────────────────────────────────────────────────

    @Test
    fun `cancelling a request in flight stops it without a retry`() = runBlocking<Unit> {
        // Real socket I/O, so this cannot use runTest: its virtual clock would skip straight past the
        // body delay the cancellation has to land inside.
        //
        // Asserts only what is deterministic. Issuing a second request here to show the client still
        // works reads well but is not sound: whether it reuses the connection the cancelled call left
        // mid-response is a timing question, and the assertion fails intermittently. Nor can this force
        // the narrower ordering the resume callback in AppSyncHttpTransport guards, where a response
        // arrives after the continuation is already cancelled — that race cannot be provoked on demand.
        server.enqueue(jsonResponse("""{"data":"slow"}""").setBodyDelay(5, TimeUnit.SECONDS))

        val inFlight = launch(Dispatchers.IO) { client().query(request()) }
        server.awaitRequest()
        inFlight.cancelAndJoin()

        inFlight.isCancelled shouldBe true
        // Cancellation must not be mistaken for a failure worth attempting under another auth mode.
        server.requestCount shouldBe 1
    }

    // ── Subscriptions ───────────────────────────────────────────────────

    @Test
    fun `subscribe on a closed client fails the collector`() = runTest {
        // Deliberately a different mechanism from query/mutate: subscribe returns a cold flow, so the
        // check belongs at collection rather than at the call that merely builds it.
        val client = client()
        client.close()

        val error = runCatching { client.subscribe(request()).first() }.exceptionOrNull()

        error.shouldBeInstanceOf<AppSyncValidationException>()
        error.message shouldContain "closed"
    }

    @Test
    fun `the websocket client configurator is applied`() = runTest {
        // This PR makes the WebSocket configurator live for the first time; without a test, passing
        // httpClientConfigurator here by mistake would go unnoticed.
        var configured = false
        val client = client { webSocketClientConfigurator = { configured = true } }

        // The connection attempt fails against a server that will not upgrade; touching it is enough.
        runCatching { client.subscribe(request()).first() }

        configured shouldBe true
    }

    // ── Configuration ───────────────────────────────────────────────────

    @Test
    fun `an explicit region overrides inference`() {
        configuration("https://abc123.appsync-api.us-east-1.amazonaws.com/graphql") {
            region = "eu-central-1"
        }.region shouldBe "eu-central-1"
    }

    @Test
    fun `the region is inferred from a standard endpoint`() {
        val config = configuration("https://abc123.appsync-api.ap-south-1.amazonaws.com/graphql")

        config.region shouldBe "ap-south-1"
        config.httpClientConfigurator.shouldBeNull()
    }

    @Test
    fun `a china endpoint infers its region`() {
        configuration("https://abc123.appsync-api.cn-north-1.amazonaws.com.cn/graphql")
            .region shouldBe "cn-north-1"
    }

    // ── Multi-auth retry ────────────────────────────────────────────────

    @Test
    fun `an unauthorized response retries with the next auth mode and succeeds`() = runTest {
        server.enqueue(unauthorizedResponse())
        server.enqueue(jsonResponse("""{"data":"hello"}"""))

        val response = multiAuthClient().query(modelRequest()).shouldBeSuccess().data

        response.data shouldBe "hello"
        server.requestCount shouldBe 2
    }

    @Test
    fun `the retry actually switches identity, it does not resend the same credentials`() = runTest {
        server.enqueue(unauthorizedResponse())
        server.enqueue(jsonResponse("""{"data":"hello"}"""))

        multiAuthClient().query(modelRequest()).shouldBeSuccess()

        // Owner rules order User Pools first, then the public API key rule.
        val first = server.awaitRequest()
        val second = server.awaitRequest()
        first.getHeader("authorization") shouldBe "user-pools-token"
        first.getHeader("x-api-key").shouldBeNull()
        second.getHeader("x-api-key") shouldBe "da2-fakekey"
        second.getHeader("authorization").shouldBeNull()
    }

    @Test
    fun `a 401 retries with the next auth mode and succeeds`() = runTest {
        // The retryable signal used to be recognised only on a 200 carrying Unauthorized errors, so a
        // plain 401 — the response AppSync gives when it refuses the credentials outright — threw
        // straight out of the attempt and no fallback was tried.
        server.enqueue(jsonResponse("Unauthorized", code = HttpURLConnection.HTTP_UNAUTHORIZED))
        server.enqueue(jsonResponse("""{"data":"hello"}"""))

        val response = multiAuthClient().query(modelRequest()).shouldBeSuccess().data

        response.data shouldBe "hello"
        server.requestCount shouldBe 2
    }

    @Test
    fun `a 401 on every mode reports exhaustion`() = runTest {
        server.enqueue(jsonResponse("Unauthorized", code = HttpURLConnection.HTTP_UNAUTHORIZED))
        server.enqueue(jsonResponse("Unauthorized", code = HttpURLConnection.HTTP_UNAUTHORIZED))

        val error = multiAuthClient().query(modelRequest()).shouldBeFailure().error
            .shouldBeInstanceOf<AppSyncAuthExhaustedException>()

        error.attemptedAuthModes shouldBe listOf(AppSyncAuthMode.USER_POOLS, AppSyncAuthMode.API_KEY)
        server.requestCount shouldBe 2
    }

    @Test
    fun `single auth does not retry a 401, and reports the rejection directly`() = runTest {
        // The single-candidate contract: with nothing to fall back to the caller must see the service's
        // own reason, never an exhaustion failure that implies modes were tried and ruled out.
        server.enqueue(jsonResponse("Unauthorized", code = HttpURLConnection.HTTP_UNAUTHORIZED))

        val error = client().query(modelRequest()).shouldBeFailure().error

        error.shouldBeInstanceOf<AppSyncUnauthorizedException>()
        server.requestCount shouldBe 1
    }

    @Test
    fun `exhausting every auth mode reports which were attempted`() = runTest {
        server.enqueue(unauthorizedResponse())
        server.enqueue(unauthorizedResponse())

        val error = multiAuthClient().query(modelRequest()).shouldBeFailure().error
            .shouldBeInstanceOf<AppSyncAuthExhaustedException>()

        error.attemptedAuthModes shouldBe listOf(AppSyncAuthMode.USER_POOLS, AppSyncAuthMode.API_KEY)
        server.requestCount shouldBe 2
    }

    @Test
    fun `a failing token supplier retries with the next auth mode instead of failing`() = runTest {
        server.enqueue(jsonResponse("""{"data":"hello"}"""))

        val client = AmplifyAppSyncClient(
            configuration(server.url("/graphql").toString()) {
                region = "us-east-1"
                authorization = AppSyncAuthorization.Multi(
                    defaultAuthMode = AppSyncAuthMode.API_KEY,
                    authorizers = listOf(
                        AppSyncClientAuthorizer.UserPools { error("session expired") },
                        AppSyncClientAuthorizer.ApiKey("da2-fakekey")
                    )
                )
            }
        )

        client.query(modelRequest()).shouldBeSuccess().data.data shouldBe "hello"
        // Only one HTTP call: the first mode failed before anything was sent.
        server.requestCount shouldBe 1
        server.awaitRequest().getHeader("x-api-key") shouldBe "da2-fakekey"
    }

    @Test
    fun `a non-auth graphql error does not trigger a retry`() = runTest {
        // Only Unauthorized error types are retryable. Retrying a validation error would just fail
        // again with a different identity and hide the real problem behind an exhaustion error.
        server.enqueue(jsonResponse("""{"errors":[{"message":"Validation failed"}]}"""))

        val response = multiAuthClient().query(modelRequest()).shouldBeSuccess().data

        response.errors shouldHaveSize 1
        server.requestCount shouldBe 1
    }

    @Test
    fun `a per-request override is not retried against other modes`() = runTest {
        server.enqueue(unauthorizedResponse())

        val response = multiAuthClient()
            .query(modelRequest(authorizationType = AuthorizationType.API_KEY))
            .shouldBeSuccess().data

        // The caller named a mode. Falling back would authorize as a different identity than asked.
        response.errors shouldHaveSize 1
        server.requestCount shouldBe 1
        server.awaitRequest().getHeader("x-api-key") shouldBe "da2-fakekey"
    }

    @Test
    fun `single auth does not retry, and surfaces the unauthorized response unchanged`() = runTest {
        // Regression guard for the single-auth contract: with one candidate mode there is nothing to
        // exhaust, so a 200-with-errors stays a Success rather than becoming an exhaustion failure.
        server.enqueue(unauthorizedResponse())

        val response = client().query(modelRequest()).shouldBeSuccess().data

        response.errors shouldHaveSize 1
        server.requestCount shouldBe 1
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun client(configure: AmplifyAppSyncClient.Configuration.Builder.() -> Unit = {}) = AmplifyAppSyncClient(
        // The mock server URL carries no region, so it is always set explicitly here.
        configuration(server.url("/graphql").toString()) {
            region = "us-east-1"
            configure()
        }
    )

    private fun configuration(url: String, configure: AmplifyAppSyncClient.Configuration.Builder.() -> Unit = {}) =
        AmplifyAppSyncClient.Configuration {
            endpoint = url
            authorization = AppSyncAuthorization.Single(AppSyncClientAuthorizer.ApiKey("da2-fakekey"))
            configure()
        }

    private fun request() = SimpleGraphQLRequest<String>(
        """query { getTodo(id: "1") { id name } }""",
        emptyMap(),
        String::class.java,
        GsonVariablesSerializer()
    )

    private fun jsonResponse(body: String, code: Int = HttpURLConnection.HTTP_OK) = MockResponse()
        .setResponseCode(code)
        .setHeader("content-type", "application/json")
        .setBody(body)

    /** A 200 carrying AppSync's Unauthorized error type — the signal that triggers an auth retry. */
    private fun unauthorizedResponse() = jsonResponse(
        """{"errors":[{"message":"Not Authorized to access getTodo on type Query",
           "extensions":{"errorType":"Unauthorized"}}]}
        """.trimIndent()
    )

    /** A client whose auth rules make User Pools the first candidate and the API key the second. */
    private fun multiAuthClient() = AmplifyAppSyncClient(
        configuration(server.url("/graphql").toString()) {
            region = "us-east-1"
            authorization = AppSyncAuthorization.Multi(
                defaultAuthMode = AppSyncAuthMode.API_KEY,
                authorizers = listOf(
                    AppSyncClientAuthorizer.ApiKey("da2-fakekey"),
                    AppSyncClientAuthorizer.UserPools { "user-pools-token" }
                )
            )
        }
    )

    /**
     * A request carrying model metadata, so auth-rule resolution has something to inspect. Mocked
     * rather than built, because a real [AppSyncGraphQLRequest] also runs selection-set generation,
     * which needs a code-generated model class and is irrelevant here.
     */
    private fun modelRequest(authorizationType: AuthorizationType? = null): AppSyncGraphQLRequest<String> {
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
            every { content } returns """{"query":"query { getTodo(id: \"1\") { id name } }"}"""
            every { responseType } returns String::class.java
            every { modelSchema } returns ModelSchema.builder()
                .name("Todo")
                .authRules(listOf(ownerRule, publicRule))
                .build()
            every { authRuleOperation } returns ModelOperation.READ
            every { this@mockk.authorizationType } returns authorizationType
        }
    }

    /**
     * Takes the next recorded request, failing if none arrives. The bare `takeRequest()` blocks the
     * test thread forever when the request was never sent, and because it blocks rather than suspends
     * `runTest`'s timeout cannot interrupt it — so a bug that stops a request being sent would hang
     * the build instead of failing it.
     */
    private fun MockWebServer.awaitRequest(): RecordedRequest = requireNotNull(
        takeRequest(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    ) { "Expected a request to reach the server, but none arrived within ${REQUEST_TIMEOUT_SECONDS}s." }

    private companion object {
        const val REQUEST_TIMEOUT_SECONDS = 5L
    }
}
