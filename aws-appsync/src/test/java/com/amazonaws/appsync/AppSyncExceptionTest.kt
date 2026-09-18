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

import com.amplifyframework.foundation.exceptions.AmplifyException
import com.amplifyframework.foundation.exceptions.DEFAULT_RECOVERY_SUGGESTION
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.reflect.KClass
import org.junit.Test

/**
 * Tests [AppSyncException]: that the hierarchy has the intended shape, and that
 * [AppSyncException.from] maps throwables into it rather than letting anything escape untyped.
 */
class AppSyncExceptionTest {

    // ── Hierarchy shape ─────────────────────────────────────────────────

    @Test
    fun `extends the foundation AmplifyException, not core's and not ApiException`() {
        val exception = AppSyncNetworkException("network down")

        exception.shouldBeInstanceOf<AmplifyException>()
        exception.shouldBeInstanceOf<AppSyncException>()
    }

    @Test
    fun `auth leaves are catchable as one group`() {
        val leaves = listOf(
            AppSyncTokenFetchException("a"),
            AppSyncProviderNotConfiguredException("c"),
            AppSyncSigningException("d"),
            AppSyncTokenParsingException("e"),
            AppSyncAuthorizationClaimException("f"),
            AppSyncAuthExhaustedException("g", emptyList())
        )

        leaves.forEach { it.shouldBeInstanceOf<AppSyncAuthException>() }
    }

    @Test
    fun `the exhaustion exception reports the modes that were attempted`() {
        val attempted = listOf(AppSyncAuthMode.USER_POOLS, AppSyncAuthMode.API_KEY)

        val exception = AppSyncAuthExhaustedException("all failed", attempted)

        exception.attemptedAuthModes shouldBe attempted
    }

    @Test
    fun `configuration leaves are catchable as one group`() {
        AppSyncInvalidConfigException("a").shouldBeInstanceOf<AppSyncConfigurationException>()
        AppSyncEndpointResolutionException("b").shouldBeInstanceOf<AppSyncConfigurationException>()
    }

    @Test
    fun `response leaves are catchable as one group`() {
        AppSyncDeserializationException("a").shouldBeInstanceOf<AppSyncResponseException>()
        AppSyncGraphQLErrorException("b", emptyList()).shouldBeInstanceOf<AppSyncResponseException>()
    }

    @Test
    fun `subscription leaves are catchable as one group`() {
        AppSyncConnectionException("a").shouldBeInstanceOf<AppSyncSubscriptionException>()
        AppSyncTimeoutException("b").shouldBeInstanceOf<AppSyncSubscriptionException>()
        AppSyncSubscriptionLimitExceededException("c").shouldBeInstanceOf<AppSyncSubscriptionException>()
    }

    @Test
    fun `request leaves are catchable as one group`() {
        AppSyncValidationException("b").shouldBeInstanceOf<AppSyncRequestException>()
    }

    @Test
    fun `network, rate limit and unknown are leaves directly under the base, in no group`() {
        // Guards the hierarchy shape: these deliberately have no intermediate group, so a `when` over
        // the groups must still handle them.
        val network: AppSyncException = AppSyncNetworkException("a")
        val rateLimit: AppSyncException = AppSyncRateLimitExceededException("b")
        val unknown: AppSyncException = AppSyncUnknownException("c")

        listOf(network, rateLimit, unknown).forEach {
            (it is AppSyncAuthException) shouldBe false
            (it is AppSyncConfigurationException) shouldBe false
            (it is AppSyncResponseException) shouldBe false
            (it is AppSyncSubscriptionException) shouldBe false
            (it is AppSyncRequestException) shouldBe false
        }
    }

    // ── Payload ─────────────────────────────────────────────────────────

    @Test
    fun `every leaf carries a non-blank default recovery suggestion`() {
        val leaves = listOf(
            AppSyncTokenFetchException("a"),
            AppSyncProviderNotConfiguredException("a"),
            AppSyncSigningException("a"),
            AppSyncTokenParsingException("a"),
            AppSyncAuthorizationClaimException("a"),
            AppSyncUnauthorizedException("a"),
            AppSyncAuthExhaustedException("a", emptyList()),
            AppSyncInvalidConfigException("a"),
            AppSyncEndpointResolutionException("a"),
            AppSyncDeserializationException("a"),
            AppSyncGraphQLErrorException("a", emptyList()),
            AppSyncConnectionException("a"),
            AppSyncTimeoutException("a"),
            AppSyncSubscriptionLimitExceededException("a"),
            AppSyncValidationException("a"),
            AppSyncNetworkException("a"),
            AppSyncRateLimitExceededException("a"),
            AppSyncUnknownException("a")
        )

        // Compared against the hierarchy rather than against a count. A `size shouldBe n` assertion
        // counts the list directly below it, so it can never fail when a leaf is added — which is the
        // one thing it exists to catch, and it did not: AppSyncUnauthorizedException was absent from
        // this list while the count still passed.
        leaves.map { it::class.simpleName }.toSet() shouldBe AppSyncException::class.leafNames()
        leaves.forEach { it.recoverySuggestion.shouldNotBeBlank() }
    }

    /** Every concrete type reachable through the sealed hierarchy, by simple name. */
    private fun KClass<*>.leafNames(): Set<String> = when {
        sealedSubclasses.isEmpty() -> setOfNotNull(simpleName)
        else -> sealedSubclasses.flatMap { it.leafNames() }.toSet()
    }

    @Test
    fun `message, recovery suggestion and cause are all preserved`() {
        val cause = IllegalStateException("underlying")
        val exception = AppSyncTokenFetchException(
            message = "token supplier threw",
            recoverySuggestion = "fix the supplier",
            cause = cause
        )

        exception.message shouldBe "token supplier threw"
        exception.recoverySuggestion shouldBe "fix the supplier"
        exception.cause shouldBe cause
    }

    @Test
    fun `GraphQLError exception carries the error list`() {
        val errors = listOf(com.amplifyframework.api.graphql.GraphQLResponse.Error("boom", null, null, null))

        val exception = AppSyncGraphQLErrorException("service returned errors", errors)

        exception.errors shouldBe errors
    }

    // ── from() mapping ──────────────────────────────────────────────────

    @Test
    fun `from returns an AppSyncException unchanged rather than re-wrapping it`() {
        val original = AppSyncTimeoutException("timed out")

        AppSyncException.from(original) shouldBe original
    }

    @Test
    fun `from maps an IOException to a network exception, preserving the cause`() {
        val cause = IOException("connection reset")

        val mapped = AppSyncException.from(cause)

        mapped.shouldBeInstanceOf<AppSyncNetworkException>()
        mapped.message shouldBe "connection reset"
        mapped.cause shouldBe cause
    }

    @Test
    fun `from maps an IOException subclass to a network exception`() {
        // SocketTimeoutException is an IOException, so it must not fall through to Unknown.
        AppSyncException.from(SocketTimeoutException("read timed out"))
            .shouldBeInstanceOf<AppSyncNetworkException>()
    }

    @Test
    fun `from maps anything unrecognised to unknown with the default recovery suggestion`() {
        val cause = IllegalArgumentException("bad state")

        val mapped = AppSyncException.from(cause)

        mapped.shouldBeInstanceOf<AppSyncUnknownException>()
        mapped.recoverySuggestion shouldBe DEFAULT_RECOVERY_SUGGESTION
        mapped.cause shouldBe cause
    }

    @Test
    fun `from substitutes a message when the throwable has none`() {
        val mapped = AppSyncException.from(RuntimeException())

        mapped.message shouldBe "An unknown error occurred."
    }
}
