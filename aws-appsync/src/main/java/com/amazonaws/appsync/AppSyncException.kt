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

import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.foundation.exceptions.AmplifyException
import com.amplifyframework.foundation.exceptions.DEFAULT_RECOVERY_SUGGESTION
import java.io.IOException

/**
 * Base exception for all AppSync client operations.
 *
 * This is a sealed hierarchy with two levels. The intermediate groups are themselves sealed, so a
 * caller can catch a whole category:
 *
 * ```kotlin
 * when (val result = client.query(request)) {
 *     is Result.Success -> use(result.data)
 *     is Result.Failure -> when (val e = result.error) {
 *         is AppSyncAuthException -> reauthenticate()
 *         is AppSyncNetworkException -> retryLater()
 *         else -> report(e)
 *     }
 * }
 * ```
 *
 * The groups are [AppSyncAuthException], [AppSyncConfigurationException],
 * [AppSyncResponseException], [AppSyncSubscriptionException] and [AppSyncRequestException].
 * [AppSyncNetworkException] and [AppSyncUnknownException] are leaves directly under this base.
 *
 * @param message Error message describing what went wrong
 * @param recoverySuggestion Suggested action to resolve the error
 * @param cause Underlying cause of the exception
 */
@ExperimentalAmplifyApi
sealed class AppSyncException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyException(message, recoverySuggestion, cause) {
    companion object {
        /**
         * Maps a [Throwable] raised anywhere inside the client into the appropriate
         * [AppSyncException] subtype. Anything unrecognised becomes [AppSyncUnknownException] rather
         * than escaping untyped.
         */
        internal fun from(error: Throwable): AppSyncException = when (error) {
            is AppSyncException -> error
            is IOException -> AppSyncNetworkException(
                message = error.message ?: "A network error occurred.",
                cause = error
            )
            else -> AppSyncUnknownException(
                message = error.message ?: "An unknown error occurred.",
                cause = error
            )
        }
    }
}

// ── Auth ────────────────────────────────────────────────────────────────

/**
 * Authorization failures. Catch this to handle every auth problem as one category.
 */
@ExperimentalAmplifyApi
sealed class AppSyncAuthException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AppSyncException(message, recoverySuggestion, cause)

/** An authorizer's token or API key supplier failed. */
@ExperimentalAmplifyApi
class AppSyncTokenFetchException(
    message: String,
    recoverySuggestion: String = "Verify that the token supplier passed to the authorizer succeeds.",
    cause: Throwable? = null
) : AppSyncAuthException(message, recoverySuggestion, cause)

/** No authorizer was configured for the auth mode a request requires. */
@ExperimentalAmplifyApi
class AppSyncProviderNotConfiguredException(
    message: String,
    recoverySuggestion: String = "Add an authorizer for this auth mode to the client's AppSyncAuthorization.",
    cause: Throwable? = null
) : AppSyncAuthException(message, recoverySuggestion, cause)

/** SigV4 request signing failed. */
@ExperimentalAmplifyApi
class AppSyncSigningException(
    message: String,
    recoverySuggestion: String = "Verify that the credentials provider returns valid IAM credentials.",
    cause: Throwable? = null
) : AppSyncAuthException(message, recoverySuggestion, cause)

/** A token could not be parsed, so its claims could not be read. */
@ExperimentalAmplifyApi
class AppSyncTokenParsingException(
    message: String,
    recoverySuggestion: String = "Verify that the token supplier returns a well-formed JWT.",
    cause: Throwable? = null
) : AppSyncAuthException(message, recoverySuggestion, cause)

/** A claim required to authorize the request was missing from the token. */
@ExperimentalAmplifyApi
class AppSyncAuthorizationClaimException(
    message: String,
    recoverySuggestion: String = "Verify that the token contains the claim the model's @auth rule requires.",
    cause: Throwable? = null
) : AppSyncAuthException(message, recoverySuggestion, cause)

/**
 * Every auth mode the request was eligible for was tried and all of them failed.
 *
 * Only raised in multi-auth: with a single candidate mode the underlying failure is surfaced
 * directly, since wrapping one failure in an "exhausted" exception hides it for no benefit.
 *
 * @param attemptedAuthModes The modes that were tried, in the order they were tried.
 */
@ExperimentalAmplifyApi
class AppSyncAuthExhaustedException(
    message: String,
    val attemptedAuthModes: List<AppSyncAuthMode>,
    recoverySuggestion: String = "Check that one of the model's @auth rules matches a configured authorizer, " +
        "and inspect the cause for the last failure.",
    cause: Throwable? = null
) : AppSyncAuthException(message, recoverySuggestion, cause)

// ── Configuration ───────────────────────────────────────────────────────

/**
 * The client was configured in a way it cannot use. These are programming errors, surfaced at
 * construction or on first use rather than silently.
 */
@ExperimentalAmplifyApi
sealed class AppSyncConfigurationException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AppSyncException(message, recoverySuggestion, cause)

/** The configuration was rejected. */
@ExperimentalAmplifyApi
class AppSyncInvalidConfigException(
    message: String,
    recoverySuggestion: String = "Correct the AmplifyAppSyncClient.Configuration and recreate the client.",
    cause: Throwable? = null
) : AppSyncConfigurationException(message, recoverySuggestion, cause)

/** The endpoint could not be parsed, or a region could not be resolved from it. */
@ExperimentalAmplifyApi
class AppSyncEndpointResolutionException(
    message: String,
    recoverySuggestion: String = "Provide a standard AppSync endpoint URL, or set region explicitly.",
    cause: Throwable? = null
) : AppSyncConfigurationException(message, recoverySuggestion, cause)

// ── Response ────────────────────────────────────────────────────────────

/** A response arrived but could not be turned into the expected result. */
@ExperimentalAmplifyApi
sealed class AppSyncResponseException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AppSyncException(message, recoverySuggestion, cause)

/** The response body did not deserialize into the request's response type. */
@ExperimentalAmplifyApi
class AppSyncDeserializationException(
    message: String,
    recoverySuggestion: String = "Verify that the request's response type matches the shape the API returns.",
    cause: Throwable? = null
) : AppSyncResponseException(message, recoverySuggestion, cause)

/**
 * The service returned GraphQL errors.
 *
 * Note this is for the case where errors are terminal. Errors that accompany data in a successful
 * response are delivered on [GraphQLResponse.errors] instead, not thrown.
 *
 * @param errors The GraphQL errors the service returned.
 */
@ExperimentalAmplifyApi
class AppSyncGraphQLErrorException(
    message: String,
    val errors: List<GraphQLResponse.Error>,
    recoverySuggestion: String = "Inspect the errors list for the failures the service reported.",
    cause: Throwable? = null
) : AppSyncResponseException(message, recoverySuggestion, cause)

// ── Subscription ────────────────────────────────────────────────────────

/**
 * A subscription could not be established or was terminated. All subscription errors are terminal:
 * the stream ends and the consumer must re-subscribe.
 */
@ExperimentalAmplifyApi
sealed class AppSyncSubscriptionException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AppSyncException(message, recoverySuggestion, cause)

/** The WebSocket connection failed or was lost. */
@ExperimentalAmplifyApi
class AppSyncConnectionException(
    message: String,
    recoverySuggestion: String = "Check network connectivity and re-subscribe.",
    cause: Throwable? = null
) : AppSyncSubscriptionException(message, recoverySuggestion, cause)

/** Establishing the subscription timed out. */
@ExperimentalAmplifyApi
class AppSyncTimeoutException(
    message: String,
    recoverySuggestion: String = "Re-subscribe, or raise the timeout via the WebSocket client configurator.",
    cause: Throwable? = null
) : AppSyncSubscriptionException(message, recoverySuggestion, cause)

// ── Request ─────────────────────────────────────────────────────────────

/** The request could not be built or was rejected before being sent. */
@ExperimentalAmplifyApi
sealed class AppSyncRequestException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AppSyncException(message, recoverySuggestion, cause)

/** The request was structurally invalid. */
@ExperimentalAmplifyApi
class AppSyncValidationException(
    message: String,
    recoverySuggestion: String = "Correct the request and retry.",
    cause: Throwable? = null
) : AppSyncRequestException(message, recoverySuggestion, cause)

// ── Ungrouped leaves ────────────────────────────────────────────────────

/** The request could not reach the service. */
@ExperimentalAmplifyApi
class AppSyncNetworkException(
    message: String,
    recoverySuggestion: String = "Check network connectivity and retry the request.",
    cause: Throwable? = null
) : AppSyncException(message, recoverySuggestion, cause)

/** An unexpected or uncategorized error. */
@ExperimentalAmplifyApi
class AppSyncUnknownException(
    message: String,
    recoverySuggestion: String = DEFAULT_RECOVERY_SUGGESTION,
    cause: Throwable? = null
) : AppSyncException(message, recoverySuggestion, cause)
