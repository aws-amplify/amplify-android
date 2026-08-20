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
) : ApiAuthException(message, cause, recoverySuggestion)

@ExperimentalAmplifyApi
class AppSyncTokenFetchException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : AppSyncAuthException(message, cause, recoverySuggestion)

@ExperimentalAmplifyApi
class AppSyncProviderNotConfiguredException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : AppSyncAuthException(message, cause, recoverySuggestion)

@ExperimentalAmplifyApi
class AppSyncSigningException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : AppSyncAuthException(message, cause, recoverySuggestion)

@ExperimentalAmplifyApi
class AppSyncTokenParsingException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : AppSyncAuthException(message, cause, recoverySuggestion)

@ExperimentalAmplifyApi
class AppSyncAuthorizationClaimException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : AppSyncAuthException(message, cause, recoverySuggestion)

@ExperimentalAmplifyApi
class AppSyncAuthExhaustedException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : AppSyncAuthException(message, cause, recoverySuggestion)

@ExperimentalAmplifyApi
class AppSyncAuthUnknownException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : AppSyncAuthException(message, cause, recoverySuggestion)

/**
 * Sealed non-auth exception hierarchy for the AppSync client.
 * Extends [ApiException] for backward compatibility.
 */
@ExperimentalAmplifyApi
sealed class AppSyncException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : ApiException(message, cause, recoverySuggestion)

@ExperimentalAmplifyApi
class AppSyncInvalidConfigException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : AppSyncException(message, cause, recoverySuggestion)

@ExperimentalAmplifyApi
class AppSyncEndpointResolutionException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : AppSyncException(message, cause, recoverySuggestion)

@ExperimentalAmplifyApi
class AppSyncDeserializationException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : AppSyncException(message, cause, recoverySuggestion)

@ExperimentalAmplifyApi
class AppSyncSubscriptionConnectionException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : AppSyncException(message, cause, recoverySuggestion)

@ExperimentalAmplifyApi
class AppSyncSubscriptionTimeoutException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : AppSyncException(message, cause, recoverySuggestion)

@ExperimentalAmplifyApi
class AppSyncRequestValidationException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : AppSyncException(message, cause, recoverySuggestion)

@ExperimentalAmplifyApi
class AppSyncInvalidStateException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : AppSyncException(message, cause, recoverySuggestion)

@ExperimentalAmplifyApi
class AppSyncNetworkException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : AppSyncException(message, cause, recoverySuggestion)

@ExperimentalAmplifyApi
class AppSyncUnknownException(
    message: String,
    cause: Throwable?,
    recoverySuggestion: String
) : AppSyncException(message, cause, recoverySuggestion)
