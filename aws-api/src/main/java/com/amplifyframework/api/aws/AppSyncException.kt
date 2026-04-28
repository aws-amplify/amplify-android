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

import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.ApiException.ApiAuthException

// ─── Auth exceptions ─── extend ApiAuthException for backward compatibility ───

/**
 * Sealed auth exception hierarchy for the AppSync client.
 * Extends [ApiAuthException] so existing `catch (ApiAuthException)` and
 * `throws ApiAuthException` declarations work without changes.
 */
@ExperimentalAmplifyApi
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

    class ProviderNotConfiguredException(
        message: String,
        cause: Throwable?,
        recoverySuggestion: String
    ) : AppSyncAuthException(message, cause, recoverySuggestion)

    class SigningException(
        message: String,
        cause: Throwable?,
        recoverySuggestion: String
    ) : AppSyncAuthException(message, cause, recoverySuggestion)

    class TokenParsingException(
        message: String,
        cause: Throwable?,
        recoverySuggestion: String
    ) : AppSyncAuthException(message, cause, recoverySuggestion)

    class AuthorizationClaimException(
        message: String,
        cause: Throwable?,
        recoverySuggestion: String
    ) : AppSyncAuthException(message, cause, recoverySuggestion)

    class AuthExhaustedException(
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
@ExperimentalAmplifyApi
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
            message: String,
            cause: Throwable?,
            recoverySuggestion: String
        ) : ResponseException(message, cause, recoverySuggestion)
    }

    sealed class SubscriptionException(message: String, cause: Throwable?, recoverySuggestion: String) :
        AppSyncException(message, cause, recoverySuggestion) {

        class ConnectionException(
            message: String,
            cause: Throwable?,
            recoverySuggestion: String
        ) : SubscriptionException(message, cause, recoverySuggestion)

        class TimeoutException(
            message: String,
            cause: Throwable?,
            recoverySuggestion: String
        ) : SubscriptionException(message, cause, recoverySuggestion)
    }

    sealed class RequestException(message: String, cause: Throwable?, recoverySuggestion: String) :
        AppSyncException(message, cause, recoverySuggestion) {

        class ValidationException(
            message: String,
            cause: Throwable?,
            recoverySuggestion: String
        ) : RequestException(message, cause, recoverySuggestion)

        class InvalidStateException(
            message: String,
            cause: Throwable?,
            recoverySuggestion: String
        ) : RequestException(message, cause, recoverySuggestion)
    }

    class NetworkException(
        message: String,
        cause: Throwable?,
        recoverySuggestion: String
    ) : AppSyncException(message, cause, recoverySuggestion)

    class UnknownException(
        message: String,
        cause: Throwable?,
        recoverySuggestion: String
    ) : AppSyncException(message, cause, recoverySuggestion)
}
