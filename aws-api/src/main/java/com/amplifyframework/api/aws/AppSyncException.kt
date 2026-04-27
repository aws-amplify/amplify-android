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

// ─── Auth exceptions ─── extend ApiAuthException for backward compatibility ───

/**
 * Sealed auth exception hierarchy for the AppSync client.
 * Extends [ApiAuthException] so existing `catch (ApiAuthException)` and
 * `throws ApiAuthException` declarations work without changes.
 */
sealed class AppSyncAuthException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : ApiAuthException(message, cause, recoverySuggestion) {

    class TokenFetchException(
        message: String,
        cause: Throwable?,
        recoverySuggestion: String
    ) : AppSyncAuthException(message, cause, recoverySuggestion)

    class TokenExpiredException(
        message: String,
        cause: Throwable?,
        recoverySuggestion: String
    ) : AppSyncAuthException(message, cause, recoverySuggestion)

    class ProviderNotConfiguredException(
        message: String,
        cause: Throwable?,
        recoverySuggestion: String
    ) : AppSyncAuthException(message, cause, recoverySuggestion)

    class SigningException(
        cause: Throwable?,
        recoverySuggestion: String
    ) : AppSyncAuthException("Failed to sign the request.", cause, recoverySuggestion)

    class TokenParsingException(
        cause: Throwable?,
        recoverySuggestion: String
    ) : AppSyncAuthException("Failed to parse auth token.", cause, recoverySuggestion)

    class AuthorizationClaimException(
        message: String,
        cause: Throwable?,
        recoverySuggestion: String
    ) : AppSyncAuthException(message, cause, recoverySuggestion)
}

// ─── Non-auth exceptions ─── extend ApiException directly ─────────────────────

/**
 * Sealed non-auth exception hierarchy for the AppSync client.
 * Extends [ApiException] for backward compatibility.
 */
sealed class AppSyncException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : ApiException(message, cause, recoverySuggestion) {

    sealed class ConfigurationException(message: String, cause: Throwable?, recoverySuggestion: String) :
        AppSyncException(message, cause, recoverySuggestion) {

        class InvalidConfigException(
            message: String,
            cause: Throwable?,
            recoverySuggestion: String
        ) : ConfigurationException(message, cause, recoverySuggestion)

        class EndpointResolutionException(
            message: String,
            cause: Throwable?,
            recoverySuggestion: String
        ) : ConfigurationException(message, cause, recoverySuggestion)
    }

    sealed class ResponseException(message: String, cause: Throwable?, recoverySuggestion: String) :
        AppSyncException(message, cause, recoverySuggestion) {

        class DeserializationException(
            cause: Throwable?,
            recoverySuggestion: String
        ) : ResponseException("Failed to deserialize the GraphQL response.", cause, recoverySuggestion)

        class GraphQLErrorException(
            val errors: List<GraphQLResponse.Error>,
            recoverySuggestion: String
        ) : ResponseException(
            "GraphQL response contained errors: ${errors.joinToString { it.message }}", null, recoverySuggestion
        )
    }

    sealed class SubscriptionException(message: String, cause: Throwable?, recoverySuggestion: String) :
        AppSyncException(message, cause, recoverySuggestion) {

        class ConnectionException(
            message: String,
            cause: Throwable?,
            recoverySuggestion: String
        ) : SubscriptionException(message, cause, recoverySuggestion)

        class TimeoutException(
            cause: Throwable?,
            recoverySuggestion: String
        ) : SubscriptionException("Subscription connection timed out.", cause, recoverySuggestion)

        class LimitExceededException(
            message: String,
            recoverySuggestion: String
        ) : SubscriptionException(message, null, recoverySuggestion)
    }

    sealed class RequestException(message: String, cause: Throwable?, recoverySuggestion: String) :
        AppSyncException(message, cause, recoverySuggestion) {

        class SchemaException(
            message: String,
            cause: Throwable?,
            recoverySuggestion: String
        ) : RequestException(message, cause, recoverySuggestion)

        class ValidationException(
            message: String,
            cause: Throwable?,
            recoverySuggestion: String
        ) : RequestException(message, cause, recoverySuggestion)
    }

    class NetworkException(
        cause: Throwable,
        recoverySuggestion: String
    ) : AppSyncException("A network error occurred.", cause, recoverySuggestion)

    class UnknownException(
        message: String,
        cause: Throwable?,
        recoverySuggestion: String
    ) : AppSyncException(message, cause, recoverySuggestion)
}
