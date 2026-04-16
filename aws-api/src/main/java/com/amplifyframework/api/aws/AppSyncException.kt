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
    cause: Throwable? = null,
    recoverySuggestion: String = "See the underlying exception for more details."
) : ApiAuthException(message, cause, recoverySuggestion) {

    class TokenFetchException @JvmOverloads constructor(cause: Throwable? = null) : AppSyncAuthException(
        "Failed to fetch auth token.", cause,
        "Check your auth provider configuration and network connectivity."
    )

    class TokenExpiredException @JvmOverloads constructor(cause: Throwable? = null) : AppSyncAuthException(
        "Auth token has expired.", cause,
        "Ensure your token refresh mechanism is working correctly."
    )

    class ProviderNotConfiguredException @JvmOverloads constructor(
        message: String = "Auth provider is not configured."
    ) : AppSyncAuthException(
        message, null,
        "Ensure the required authorizer is provided when constructing the client."
    )

    class SigningException @JvmOverloads constructor(cause: Throwable? = null) : AppSyncAuthException(
        "Failed to sign the request.", cause,
        "Check your IAM credentials and signing configuration."
    )

    class TokenParsingException @JvmOverloads constructor(cause: Throwable? = null) : AppSyncAuthException(
        "Failed to parse auth token.", cause,
        "Ensure the token is a valid JWT."
    )

    class AuthorizationClaimException(
        message: String,
        cause: Throwable? = null
    ) : AppSyncAuthException(
        message, cause,
        "Check the owner/group claims in your auth token."
    )
}

// ─── Non-auth exceptions ─── extend ApiException directly ─────────────────────

/**
 * Sealed non-auth exception hierarchy for the AppSync client.
 * Extends [ApiException] for backward compatibility.
 */
sealed class AppSyncException(
    message: String,
    cause: Throwable? = null,
    recoverySuggestion: String = "See the underlying exception for more details."
) : ApiException(message, cause, recoverySuggestion) {

    sealed class ConfigurationException(message: String, cause: Throwable?, recoverySuggestion: String) :
        AppSyncException(message, cause, recoverySuggestion) {

        class InvalidConfigException @JvmOverloads constructor(
            message: String, cause: Throwable? = null
        ) : ConfigurationException(message, cause, "Check your AppSync client configuration parameters.")

        class EndpointResolutionException @JvmOverloads constructor(
            message: String, cause: Throwable? = null
        ) : ConfigurationException(message, cause, "Verify the endpoint URL is a valid AppSync GraphQL endpoint.")
    }

    sealed class ResponseException(message: String, cause: Throwable?, recoverySuggestion: String) :
        AppSyncException(message, cause, recoverySuggestion) {

        class DeserializationException @JvmOverloads constructor(cause: Throwable? = null) : ResponseException(
            "Failed to deserialize the GraphQL response.", cause,
            "Check that the response type matches the expected schema."
        )

        class GraphQLErrorException(val errors: List<GraphQLResponse.Error>) : ResponseException(
            "GraphQL response contained errors: ${errors.joinToString { it.message }}", null,
            "Check the GraphQL errors for details."
        )
    }

    sealed class SubscriptionException(message: String, cause: Throwable?, recoverySuggestion: String) :
        AppSyncException(message, cause, recoverySuggestion) {

        class ConnectionException @JvmOverloads constructor(
            message: String = "Failed to establish subscription connection.",
            cause: Throwable? = null
        ) : SubscriptionException(message, cause, "Check your network connection and endpoint configuration.")

        class TimeoutException @JvmOverloads constructor(cause: Throwable? = null) : SubscriptionException(
            "Subscription connection timed out.", cause,
            "Check your network connection. Consider increasing the connection timeout."
        )

        class LimitExceededException @JvmOverloads constructor(
            message: String = "Maximum number of subscriptions reached."
        ) : SubscriptionException(message, null, "Close existing subscriptions before creating new ones.")
    }

    sealed class RequestException(message: String, cause: Throwable?, recoverySuggestion: String) :
        AppSyncException(message, cause, recoverySuggestion) {

        class SchemaException @JvmOverloads constructor(
            message: String, cause: Throwable? = null
        ) : RequestException(message, cause, "Check the model schema and codegen output.")

        class ValidationException @JvmOverloads constructor(
            message: String, cause: Throwable? = null
        ) : RequestException(message, cause, "Check the request parameters.")
    }

    class NetworkException(cause: Throwable) : AppSyncException(
        "A network error occurred.", cause,
        "Check your internet connection and try again."
    )

    class UnknownException @JvmOverloads constructor(
        message: String? = null, cause: Throwable? = null
    ) : AppSyncException(
        message ?: "An unknown error occurred.", cause,
        "This is not expected to occur. Please report this issue."
    )
}
