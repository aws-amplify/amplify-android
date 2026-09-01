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
            AppSyncTokenExpiredException("b"),
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
        AppSyncLimitExceededException("c").shouldBeInstanceOf<AppSyncSubscriptionException>()
    }

    @Test
    fun `request leaves are catchable as one group`() {
        AppSyncSchemaException("a").shouldBeInstanceOf<AppSyncRequestException>()
        AppSyncValidationException("b").shouldBeInstanceOf<AppSyncRequestException>()
    }

    @Test
    fun `network and unknown are leaves directly under the base, in no group`() {
        // Guards the hierarchy shape: these two deliberately have no intermediate group, so a `when`
        // over
        // the groups must still handle them.
        val network: AppSyncException = AppSyncNetworkException("a")
        val unknown: AppSyncException = AppSyncUnknownException("b")

        listOf(network, unknown).forEach {
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
            AppSyncTokenExpiredException("a"),
            AppSyncProviderNotConfiguredException("a"),
            AppSyncSigningException("a"),
            AppSyncTokenParsingException("a"),
            AppSyncAuthorizationClaimException("a"),
            AppSyncAuthExhaustedException("a", emptyList()),
            AppSyncInvalidConfigException("a"),
            AppSyncEndpointResolutionException("a"),
            AppSyncDeserializationException("a"),
            AppSyncGraphQLErrorException("a", emptyList()),
            AppSyncConnectionException("a"),
            AppSyncTimeoutException("a"),
            AppSyncLimitExceededException("a"),
            AppSyncSchemaException("a"),
            AppSyncValidationException("a"),
            AppSyncNetworkException("a"),
            AppSyncUnknownException("a")
        )

        leaves.size shouldBe 18
        leaves.forEach { it.recoverySuggestion.shouldNotBeBlank() }
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
