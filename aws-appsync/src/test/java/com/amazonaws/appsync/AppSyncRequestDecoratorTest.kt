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

import com.amplifyframework.foundation.credentials.AwsCredentials
import com.amplifyframework.foundation.credentials.AwsCredentialsProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Test

/**
 * Tests [AppSyncRequestDecorator] — that each auth mode attaches the credentials AppSync expects, and
 * that a failing credential supplier becomes a typed exception rather than escaping as whatever the
 * customer's lambda threw.
 */
@OptIn(ExperimentalTime::class)
class AppSyncRequestDecoratorTest {

    private val decorator = AppSyncRequestDecorator(region = "us-east-1")

    private fun request() = Request.Builder()
        .url("https://abc123.appsync-api.us-east-1.amazonaws.com/graphql")
        .post("""{"query":"query { listTodos { items { id } } }"}""".toRequestBody(JSON))
        .build()

    // ── Header-based modes ──────────────────────────────────────────────

    @Test
    fun `api key goes in the x-api-key header`() = runTest {
        val decorated = decorator.decorate(request(), AppSyncClientAuthorizer.ApiKey("da2-fakekey"))

        decorated.header("x-api-key") shouldBe "da2-fakekey"
        decorated.header("authorization").shouldBeNull()
    }

    @Test
    fun `user pools token goes in the authorization header`() = runTest {
        val decorated = decorator.decorate(
            request(),
            AppSyncClientAuthorizer.UserPools { "user-pools-token" }
        )

        decorated.header("authorization") shouldBe "user-pools-token"
        decorated.header("x-api-key").shouldBeNull()
    }

    @Test
    fun `oidc token goes in the authorization header`() = runTest {
        decorator.decorate(request(), AppSyncClientAuthorizer.Oidc { "oidc-token" })
            .header("authorization") shouldBe "oidc-token"
    }

    @Test
    fun `lambda token goes in the authorization header`() = runTest {
        decorator.decorate(request(), AppSyncClientAuthorizer.Lambda { "lambda-token" })
            .header("authorization") shouldBe "lambda-token"
    }

    @Test
    fun `the api key supplier is invoked per request, not cached`() = runTest {
        // A rotating key must be picked up on the next call, so the supplier cannot be memoised.
        var calls = 0
        val authorizer = AppSyncClientAuthorizer.ApiKey { "key-${++calls}" }

        decorator.decorate(request(), authorizer).header("x-api-key") shouldBe "key-1"
        decorator.decorate(request(), authorizer).header("x-api-key") shouldBe "key-2"
    }

    @Test
    fun `the request body and url are left untouched`() = runTest {
        val original = request()

        val decorated = decorator.decorate(original, AppSyncClientAuthorizer.ApiKey("da2-fakekey"))

        decorated.url shouldBe original.url
        decorated.method shouldBe "POST"
        decorated.body?.contentLength() shouldBe original.body?.contentLength()
    }

    // ── SigV4 ───────────────────────────────────────────────────────────

    @Test
    fun `iam signing attaches the sigv4 authorization and content sha headers`() = runTest {
        val decorated = decorator.decorate(request(), iamAuthorizer())

        decorated.signature() shouldStartWith "AWS4-HMAC-SHA256"
        // AppSync requires this body header on a signed request; without it the signature is rejected.
        decorated.header("x-amz-content-sha256").shouldNotBeNull()
        decorated.header("X-Amz-Date").shouldNotBeNull()
    }

    @Test
    fun `iam signing scopes the credential to the configured region and the appsync service`() = runTest {
        AppSyncRequestDecorator(region = "eu-west-2")
            .decorate(request(), iamAuthorizer())
            .signature() shouldContain "/eu-west-2/appsync/aws4_request"
    }

    @Test
    fun `iam signing carries the session token for temporary credentials`() = runTest {
        val decorated = decorator.decorate(
            request(),
            AppSyncClientAuthorizer.Iam(
                AwsCredentialsProvider {
                    AwsCredentials.Temporary(
                        accessKeyId = "AKIAIOSFODNN7EXAMPLE",
                        secretAccessKey = "secret",
                        sessionToken = "session-token-value",
                        expiration = Instant.DISTANT_FUTURE
                    )
                }
            )
        )

        decorated.header("X-Amz-Security-Token") shouldBe "session-token-value"
    }

    @Test
    fun `iam signing preserves every value of a repeated header`() = runTest {
        val repeated = Request.Builder()
            .url("https://abc123.appsync-api.us-east-1.amazonaws.com/graphql")
            .addHeader("x-trace", "first")
            .addHeader("x-trace", "second")
            .post("{}".toRequestBody(JSON))
            .build()

        val decorated = decorator.decorate(repeated, iamAuthorizer())

        // Reading one value per name on the way in or out would silently drop the other.
        decorated.headers("x-trace") shouldBe listOf("first", "second")
    }

    @Test
    fun `iam signing preserves the request body`() = runTest {
        val original = request()

        val decorated = decorator.decorate(original, iamAuthorizer())

        // Signing rebuilds the OkHttp request from scratch, so this guards against dropping the body.
        decorated.body.shouldNotBeNull()
        decorated.body?.contentLength() shouldBe original.body?.contentLength()
        decorated.url shouldBe original.url
        decorated.method shouldBe "POST"
    }

    @Test
    fun `iam signing keeps the china dns suffix in the signed host`() = runTest {
        val chinaRequest = Request.Builder()
            .url("https://abc123.appsync-api.cn-north-1.amazonaws.com.cn/graphql")
            .post("{}".toRequestBody(JSON))
            .build()

        val decorated = AppSyncRequestDecorator(region = "cn-north-1")
            .decorate(chinaRequest, iamAuthorizer())

        decorated.url.host shouldBe "abc123.appsync-api.cn-north-1.amazonaws.com.cn"
        decorated.signature() shouldContain "/cn-north-1/appsync/aws4_request"
    }

    // ── Connection authorization object ─────────────────────────────────
    //
    // These key sets are what AppSync validates the realtime connection against. They are JSON keys,
    // not HTTP header names, so casing is significant and each mode's set is asserted exactly — a
    // missing `host` or a lowercased `Authorization` is accepted by the client and rejected by the
    // service as a routing error, with nothing in the failure naming the field at fault.

    @Test
    fun `api key connection authorization carries host, date and the key`() = runTest {
        val auth = decorator.authorizationHeaders(
            AppSyncClientAuthorizer.ApiKey("da2-fakekey"),
            HTTP_ENDPOINT
        )

        auth.keys shouldContainExactly setOf("host", "x-amz-date", "x-api-key")
        auth["host"] shouldBe ENDPOINT_HOST
        auth["x-api-key"] shouldBe "da2-fakekey"
    }

    @Test
    fun `user pools connection authorization carries host and a capitalized Authorization`() = runTest {
        val auth = decorator.authorizationHeaders(
            AppSyncClientAuthorizer.UserPools { "user-pools-token" },
            HTTP_ENDPOINT
        )

        auth.keys shouldContainExactly setOf("host", "Authorization")
        auth["host"] shouldBe ENDPOINT_HOST
        auth["Authorization"] shouldBe "user-pools-token"
    }

    @Test
    fun `oidc connection authorization carries host and a capitalized Authorization`() = runTest {
        val auth = decorator.authorizationHeaders(
            AppSyncClientAuthorizer.Oidc { "oidc-token" },
            HTTP_ENDPOINT
        )

        auth.keys shouldContainExactly setOf("host", "Authorization")
        auth["host"] shouldBe ENDPOINT_HOST
        auth["Authorization"] shouldBe "oidc-token"
    }

    @Test
    fun `lambda connection authorization carries host and a capitalized Authorization`() = runTest {
        val auth = decorator.authorizationHeaders(
            AppSyncClientAuthorizer.Lambda { "lambda-token" },
            HTTP_ENDPOINT
        )

        auth.keys shouldContainExactly setOf("host", "Authorization")
        auth["host"] shouldBe ENDPOINT_HOST
        auth["Authorization"] shouldBe "lambda-token"
    }

    @Test
    fun `iam connection authorization carries host among the signed headers`() = runTest {
        val auth = decorator.authorizationHeaders(iamAuthorizer(), HTTP_ENDPOINT)

        // Signing produces the key set, so it is asserted by containment rather than exactly.
        auth["host"] shouldBe ENDPOINT_HOST
        auth["authorization"].shouldNotBeNull() shouldStartWith "AWS4-HMAC-SHA256"
        auth["x-amz-date"].shouldNotBeNull()
    }

    @Test
    fun `only api key carries a date in the connection authorization`() = runTest {
        // x-amz-date is part of what API key authorization is validated against. The token modes are
        // not dated, and sending one would be rejected.
        decorator.authorizationHeaders(AppSyncClientAuthorizer.ApiKey("da2-fakekey"), HTTP_ENDPOINT)
            .shouldContainKey("x-amz-date")

        decorator.authorizationHeaders(AppSyncClientAuthorizer.UserPools { "t" }, HTTP_ENDPOINT)
            .shouldNotContainKey("x-amz-date")
        decorator.authorizationHeaders(AppSyncClientAuthorizer.Oidc { "t" }, HTTP_ENDPOINT)
            .shouldNotContainKey("x-amz-date")
        decorator.authorizationHeaders(AppSyncClientAuthorizer.Lambda { "t" }, HTTP_ENDPOINT)
            .shouldNotContainKey("x-amz-date")
    }

    @Test
    fun `the api key date is an iso8601 basic format utc timestamp`() = runTest {
        val date = decorator.authorizationHeaders(
            AppSyncClientAuthorizer.ApiKey("da2-fakekey"),
            HTTP_ENDPOINT
        )["x-amz-date"].shouldNotBeNull()

        date shouldMatch Regex("""\d{8}T\d{6}Z""")
        // Parses back as UTC, so a non-UTC default zone cannot make the timestamp claim a Z it is not.
        val parsed = LocalDateTime
            .parse(date, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US))
            .toInstant(ZoneOffset.UTC)
        abs(parsed.toEpochMilli() - System.currentTimeMillis()) shouldBeLessThan CLOCK_SKEW_MILLIS
    }

    @Test
    fun `host is the http endpoint's host, not the realtime host`() = runTest {
        val auth = decorator.authorizationHeaders(
            AppSyncClientAuthorizer.UserPools { "token" },
            HTTP_ENDPOINT
        )

        auth["host"] shouldBe "abc123.appsync-api.us-east-1.amazonaws.com"
        auth["host"]!! shouldNotContain "realtime"
    }

    @Test
    fun `host keeps the china dns suffix`() = runTest {
        decorator.authorizationHeaders(
            AppSyncClientAuthorizer.ApiKey("da2-fakekey"),
            "https://abc123.appsync-api.cn-north-1.amazonaws.com.cn/graphql"
        )["host"] shouldBe "abc123.appsync-api.cn-north-1.amazonaws.com.cn"
    }

    @Test
    fun `an endpoint with no host is reported as a configuration failure`() = runTest {
        // Reported here rather than sent on, because the service's rejection of a hostless
        // authorization object names no field and reads as a routing error.
        shouldThrow<AppSyncEndpointResolutionException> {
            decorator.authorizationHeaders(AppSyncClientAuthorizer.ApiKey("da2-fakekey"), "")
        }
    }

    @Test
    fun `a subscription body does not change the non-iam key sets`() = runTest {
        // Only the SigV4 path varies with the body; the token modes are the same either way.
        val body = """{"query":"subscription { onCreateTodo { id } }"}"""

        decorator.authorizationHeaders(AppSyncClientAuthorizer.ApiKey("da2-fakekey"), HTTP_ENDPOINT, body)
            .keys shouldContainExactly setOf("host", "x-amz-date", "x-api-key")
        decorator.authorizationHeaders(AppSyncClientAuthorizer.UserPools { "t" }, HTTP_ENDPOINT, body)
            .keys shouldContainExactly setOf("host", "Authorization")
    }

    // ── Supplier failures ───────────────────────────────────────────────

    @Test
    fun `a throwing api key supplier becomes a token fetch exception`() = runTest {
        val boom = IllegalStateException("no key available")

        val error = shouldThrow<AppSyncTokenFetchException> {
            decorator.decorate(request(), AppSyncClientAuthorizer.ApiKey { throw boom })
        }

        error.cause shouldBe boom
        error.message shouldContain "API key"
    }

    @Test
    fun `a throwing token supplier becomes a token fetch exception naming the mode`() = runTest {
        shouldThrow<AppSyncTokenFetchException> {
            decorator.decorate(request(), AppSyncClientAuthorizer.UserPools { error("expired") })
        }.message shouldContain "User Pools"

        shouldThrow<AppSyncTokenFetchException> {
            decorator.decorate(request(), AppSyncClientAuthorizer.Oidc { error("expired") })
        }.message shouldContain "OIDC"

        shouldThrow<AppSyncTokenFetchException> {
            decorator.decorate(request(), AppSyncClientAuthorizer.Lambda { error("expired") })
        }.message shouldContain "Lambda"
    }

    @Test
    fun `a throwing credentials provider becomes a signing exception`() = runTest {
        val error = shouldThrow<AppSyncSigningException> {
            decorator.decorate(
                request(),
                AppSyncClientAuthorizer.Iam(
                    AwsCredentialsProvider<AwsCredentials> { error("no credentials") }
                )
            )
        }

        error.message shouldContain "SigV4"
    }

    // ── Cancellation is not mistaken for a credential failure ───────────
    @Test
    fun `cancelling while a token supplier suspends propagates the cancellation`() = runTest {
        // A supplier that never completes, so the only way out is cancellation. If the decorator
        // translated it the way it translates a supplier failure, this would be a token fetch
        // exception instead.
        shouldThrow<TimeoutCancellationException> {
            withTimeout(CANCELLATION_TIMEOUT_MILLIS) {
                decorator.decorate(
                    request(),
                    AppSyncClientAuthorizer.UserPools { suspendCancellableCoroutine { } }
                )
            }
        }
    }

    @Test
    fun `cancelling while credentials resolve propagates the cancellation`() = runTest {
        shouldThrow<TimeoutCancellationException> {
            withTimeout(CANCELLATION_TIMEOUT_MILLIS) {
                decorator.decorate(
                    request(),
                    AppSyncClientAuthorizer.Iam(
                        AwsCredentialsProvider<AwsCredentials> { suspendCancellableCoroutine { } }
                    )
                )
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun iamAuthorizer() = AppSyncClientAuthorizer.Iam(
        AwsCredentialsProvider {
            AwsCredentials.Static(
                accessKeyId = "AKIAIOSFODNN7EXAMPLE",
                secretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
            )
        }
    )

    /** The SigV4 Authorization header. Header lookup is case-insensitive in OkHttp. */
    private fun Request.signature(): String = header("Authorization").shouldNotBeNull()

    private companion object {
        val JSON = "application/json".toMediaType()
        const val CANCELLATION_TIMEOUT_MILLIS = 1_000L
        const val HTTP_ENDPOINT = "https://abc123.appsync-api.us-east-1.amazonaws.com/graphql"
        const val ENDPOINT_HOST = "abc123.appsync-api.us-east-1.amazonaws.com"
        const val CLOCK_SKEW_MILLIS = 60_000L
    }
}
