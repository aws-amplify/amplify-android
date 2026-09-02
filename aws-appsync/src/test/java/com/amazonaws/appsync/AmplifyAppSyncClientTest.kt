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

import com.amplifyframework.api.aws.GsonVariablesSerializer
import com.amplifyframework.api.graphql.SimpleGraphQLRequest
import com.amplifyframework.testutils.assertions.shouldBeFailure
import com.amplifyframework.testutils.assertions.shouldBeSuccess
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
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
    fun `a 401 with no parseable body becomes a validation exception`() = runTest {
        server.enqueue(jsonResponse("Unauthorized", code = HttpURLConnection.HTTP_UNAUTHORIZED))

        val error = client().query(request()).shouldBeFailure().error

        error.shouldBeInstanceOf<AppSyncValidationException>()
        error.message shouldContain "401"
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
