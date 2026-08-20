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
import com.amplifyframework.api.ApiException.ApiAuthException
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

/**
 * Verifies the sealed exception hierarchy relationships and backward compatibility.
 */
class AppSyncExceptionHierarchyTest {

    // ─── 1. Hierarchy relationships ───

    @Test
    fun `AppSyncAuthException subtypes are ApiAuthException`() {
        val exceptions: List<AppSyncAuthException> = listOf(
            AppSyncTokenFetchException("msg", RuntimeException(), "recovery"),
            AppSyncProviderNotConfiguredException("msg", null, "recovery"),
            AppSyncSigningException("msg", RuntimeException(), "recovery"),
            AppSyncTokenParsingException("msg", RuntimeException(), "recovery"),
            AppSyncAuthorizationClaimException("msg", null, "recovery"),
            AppSyncAuthExhaustedException("msg", null, "recovery"),
            AppSyncAuthUnknownException("msg", null, "recovery")
        )
        exceptions.forEach { ex ->
            ex.shouldBeInstanceOf<ApiAuthException>()
            ex.shouldBeInstanceOf<ApiException>()
        }
    }

    @Test
    fun `AppSyncException subtypes are ApiException`() {
        val cause = RuntimeException("test")
        val exceptions: List<AppSyncException> = listOf(
            AppSyncInvalidConfigException("msg", null, "recovery"),
            AppSyncEndpointResolutionException("msg", cause, "recovery"),
            AppSyncDeserializationException("msg", cause, "recovery"),
            AppSyncSubscriptionConnectionException("msg", cause, "recovery"),
            AppSyncSubscriptionTimeoutException("msg", cause, "recovery"),
            AppSyncRequestValidationException("msg", cause, "recovery"),
            AppSyncInvalidStateException("msg", cause, "recovery"),
            AppSyncNetworkException("msg", cause, "recovery"),
            AppSyncUnknownException("msg", cause, "recovery")
        )
        exceptions.forEach { ex ->
            ex.shouldBeInstanceOf<ApiException>()
        }
    }

    @Test
    fun `AppSyncAuthException is not AppSyncException`() {
        val auth = AppSyncTokenFetchException("msg", null, "recovery")
        (auth is AppSyncException) shouldBe false
    }

    @Test
    fun `AppSyncException is not ApiAuthException`() {
        val ex = AppSyncNetworkException("msg", RuntimeException(), "recovery")
        (ex is ApiAuthException) shouldBe false
    }

    @Test
    fun `AppSyncAuthException subtypes are not caught by catch AppSyncException`() {
        val auth = AppSyncTokenFetchException("msg", null, "recovery")
        val caughtAsAppSync = try {
            throw auth
        } catch (_: AppSyncException) {
            true
        } catch (_: ApiException) {
            false
        }
        caughtAsAppSync shouldBe false
    }

    // ─── 2. Sealed exhaustiveness ───

    @Test
    fun `when expression over AppSyncAuthException is exhaustive`() {
        val exceptions: List<AppSyncAuthException> = listOf(
            AppSyncTokenFetchException("msg", null, "r"),
            AppSyncProviderNotConfiguredException("msg", null, "r"),
            AppSyncSigningException("msg", null, "r"),
            AppSyncTokenParsingException("msg", null, "r"),
            AppSyncAuthorizationClaimException("msg", null, "r"),
            AppSyncAuthExhaustedException("msg", null, "r"),
            AppSyncAuthUnknownException("msg", null, "r")
        )
        exceptions.forEach { ex ->
            val label = when (ex) {
                is AppSyncTokenFetchException -> "tokenFetch"
                is AppSyncProviderNotConfiguredException -> "providerNotConfigured"
                is AppSyncSigningException -> "signing"
                is AppSyncTokenParsingException -> "tokenParsing"
                is AppSyncAuthorizationClaimException -> "authClaim"
                is AppSyncAuthExhaustedException -> "authExhausted"
                is AppSyncAuthUnknownException -> "unknown"
            }
            label.isNotEmpty() shouldBe true
        }
    }

    @Test
    fun `when expression over AppSyncException is exhaustive`() {
        val cause = RuntimeException()
        val exceptions: List<AppSyncException> = listOf(
            AppSyncInvalidConfigException("msg", null, "r"),
            AppSyncEndpointResolutionException("msg", null, "r"),
            AppSyncDeserializationException("msg", null, "r"),
            AppSyncSubscriptionConnectionException("msg", null, "r"),
            AppSyncSubscriptionTimeoutException("msg", null, "r"),
            AppSyncRequestValidationException("msg", null, "r"),
            AppSyncInvalidStateException("msg", null, "r"),
            AppSyncNetworkException("msg", cause, "r"),
            AppSyncUnknownException("msg", null, "r")
        )
        exceptions.forEach { ex ->
            val label = when (ex) {
                is AppSyncInvalidConfigException -> "invalidConfig"
                is AppSyncEndpointResolutionException -> "endpointResolution"
                is AppSyncDeserializationException -> "deserialization"
                is AppSyncSubscriptionConnectionException -> "subscriptionConnection"
                is AppSyncSubscriptionTimeoutException -> "subscriptionTimeout"
                is AppSyncRequestValidationException -> "requestValidation"
                is AppSyncInvalidStateException -> "invalidState"
                is AppSyncNetworkException -> "network"
                is AppSyncUnknownException -> "unknown"
            }
            label.isNotEmpty() shouldBe true
        }
    }

    // ─── Property preservation ───

    @Test
    fun `exception preserves message, cause, and recoverySuggestion`() {
        val cause = IllegalStateException("root cause")
        val ex = AppSyncInvalidConfigException(
            "bad config",
            cause,
            "fix your config"
        )
        ex.message shouldBe "bad config"
        ex.cause shouldBe cause
        ex.recoverySuggestion shouldBe "fix your config"
    }
}
