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
import com.amplifyframework.api.graphql.GraphQLResponse
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
            AppSyncAuthException.TokenFetchException("msg", RuntimeException(), "recovery"),
            AppSyncAuthException.TokenExpiredException("msg", null, "recovery"),
            AppSyncAuthException.ProviderNotConfiguredException("msg", null, "recovery"),
            AppSyncAuthException.SigningException("msg", RuntimeException(), "recovery"),
            AppSyncAuthException.TokenParsingException("msg", RuntimeException(), "recovery"),
            AppSyncAuthException.AuthorizationClaimException("msg", null, "recovery"),
            AppSyncAuthException.AuthExhaustedException("msg", null, "recovery")
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
            AppSyncException.ConfigurationException.InvalidConfigException("msg", null, "recovery"),
            AppSyncException.ConfigurationException.EndpointResolutionException("msg", cause, "recovery"),
            AppSyncException.ResponseException.DeserializationException("msg", cause, "recovery"),
            AppSyncException.ResponseException.GraphQLErrorException(emptyList(), "recovery"),
            AppSyncException.SubscriptionException.ConnectionException("msg", cause, "recovery"),
            AppSyncException.SubscriptionException.TimeoutException("msg", cause, "recovery"),
            AppSyncException.SubscriptionException.LimitExceededException("msg", "recovery"),
            AppSyncException.RequestException.SchemaException("msg", cause, "recovery"),
            AppSyncException.RequestException.ValidationException("msg", cause, "recovery"),
            AppSyncException.NetworkException("msg", cause, "recovery"),
            AppSyncException.UnknownException("msg", cause, "recovery")
        )
        exceptions.forEach { ex ->
            ex.shouldBeInstanceOf<ApiException>()
        }
    }

    @Test
    fun `AppSyncAuthException is not AppSyncException`() {
        val auth = AppSyncAuthException.TokenFetchException("msg", null, "recovery")
        (auth is AppSyncException) shouldBe false
    }

    @Test
    fun `AppSyncException is not ApiAuthException`() {
        val ex = AppSyncException.NetworkException("msg", RuntimeException(), "recovery")
        (ex is ApiAuthException) shouldBe false
    }

    // ─── 2. Sealed exhaustiveness ───

    @Test
    fun `when expression over AppSyncAuthException is exhaustive`() {
        val exceptions: List<AppSyncAuthException> = listOf(
            AppSyncAuthException.TokenFetchException("msg", null, "r"),
            AppSyncAuthException.TokenExpiredException("msg", null, "r"),
            AppSyncAuthException.ProviderNotConfiguredException("msg", null, "r"),
            AppSyncAuthException.SigningException("msg", null, "r"),
            AppSyncAuthException.TokenParsingException("msg", null, "r"),
            AppSyncAuthException.AuthorizationClaimException("msg", null, "r"),
            AppSyncAuthException.AuthExhaustedException("msg", null, "r")
        )
        exceptions.forEach { ex ->
            val label = when (ex) {
                is AppSyncAuthException.TokenFetchException -> "tokenFetch"
                is AppSyncAuthException.TokenExpiredException -> "tokenExpired"
                is AppSyncAuthException.ProviderNotConfiguredException -> "providerNotConfigured"
                is AppSyncAuthException.SigningException -> "signing"
                is AppSyncAuthException.TokenParsingException -> "tokenParsing"
                is AppSyncAuthException.AuthorizationClaimException -> "authClaim"
                is AppSyncAuthException.AuthExhaustedException -> "authExhausted"
            }
            label.isNotEmpty() shouldBe true
        }
    }

    @Test
    fun `when expression over AppSyncException is exhaustive`() {
        val cause = RuntimeException()
        val exceptions: List<AppSyncException> = listOf(
            AppSyncException.ConfigurationException.InvalidConfigException("msg", null, "r"),
            AppSyncException.ResponseException.DeserializationException("msg", null, "r"),
            AppSyncException.SubscriptionException.ConnectionException("msg", null, "r"),
            AppSyncException.RequestException.ValidationException("msg", null, "r"),
            AppSyncException.NetworkException("msg", cause, "r"),
            AppSyncException.UnknownException("msg", null, "r")
        )
        exceptions.forEach { ex ->
            val label = when (ex) {
                is AppSyncException.ConfigurationException -> "config"
                is AppSyncException.ResponseException -> "response"
                is AppSyncException.SubscriptionException -> "subscription"
                is AppSyncException.RequestException -> "request"
                is AppSyncException.NetworkException -> "network"
                is AppSyncException.UnknownException -> "unknown"
            }
            label.isNotEmpty() shouldBe true
        }
    }

    // ─── Property preservation ───

    @Test
    fun `exception preserves message, cause, and recoverySuggestion`() {
        val cause = IllegalStateException("root cause")
        val ex = AppSyncException.ConfigurationException.InvalidConfigException(
            "bad config",
            cause,
            "fix your config"
        )
        ex.message shouldBe "bad config"
        ex.cause shouldBe cause
        ex.recoverySuggestion shouldBe "fix your config"
    }

    @Test
    fun `GraphQLErrorException preserves errors list`() {
        val errors = listOf(
            GraphQLResponse.Error("error1", null, null, null),
            GraphQLResponse.Error("error2", null, null, null)
        )
        val ex = AppSyncException.ResponseException.GraphQLErrorException(errors, "check errors")
        ex.errors shouldBe errors
        ex.message shouldBe "GraphQL response contained errors: error1, error2"
    }
}
