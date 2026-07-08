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
package com.amplifyframework.connect

import com.amplifyframework.foundation.exceptions.AmplifyException

/**
 * Base exception for all Amplify Connect client errors.
 */
sealed class AmplifyConnectException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyException(message, recoverySuggestion, cause)

/** Neither a Cognito access token nor guest credentials could be resolved. */
class ConnectNotSignedInException(
    cause: Throwable? = null
) : AmplifyConnectException(
    message = "No Cognito access token or guest credentials were found.",
    recoverySuggestion = "Ensure Amplify Auth is configured with a Cognito Identity Pool " +
        "(guest access) or sign the user in before calling identifyUser.",
    cause = cause
)

/** Request failed due to connectivity or transport errors. */
class ConnectNetworkException(
    cause: Throwable? = null
) : AmplifyConnectException(
    message = "The request to the Customer Profiles endpoint failed to complete.",
    recoverySuggestion = "Check the device connectivity and that the configured endpoint " +
        "is reachable, then retry.",
    cause = cause
)

/** The endpoint rate limit was exceeded. */
class ConnectThrottlingException(
    cause: Throwable? = null
) : AmplifyConnectException(
    message = "The request was throttled by the endpoint.",
    recoverySuggestion = "Retry the request with exponential backoff.",
    cause = cause
)

/** The request is not authorized (bad token or missing guest permissions). */
class ConnectAccessDeniedException(
    cause: Throwable? = null
) : AmplifyConnectException(
    message = "Access was denied by the Customer Profiles endpoint.",
    recoverySuggestion = "Ensure the caller is signed in (valid access token) or that the " +
        "guest role can invoke execute-api on the identify-user-guest route.",
    cause = cause
)

/** The endpoint rejected a request as malformed. */
class ConnectValidationException(
    detail: String? = null,
    cause: Throwable? = null
) : AmplifyConnectException(
    message = detail ?: "The request was rejected as invalid.",
    recoverySuggestion = "This is likely a developer error. Verify the request inputs.",
    cause = cause
)

/** The client configuration is missing or malformed. */
class ConnectConfigurationException(
    detail: String
) : AmplifyConnectException(
    message = detail,
    recoverySuggestion = "Provide a valid ConnectClientConfiguration, or add an " +
        "\"analytics.amazon_connect_customer_profiles\" section with " +
        "\"endpoint\" and \"aws_region\" to amplify_outputs."
)

/** Unclassified endpoint error (e.g., 5xx responses). */
class ConnectServiceException(
    detail: String,
    cause: Throwable? = null
) : AmplifyConnectException(
    message = detail,
    recoverySuggestion = "Retry the request. If it persists, verify the backend identify " +
        "Lambda and endpoint health.",
    cause = cause
)
