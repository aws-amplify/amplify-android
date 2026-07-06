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

import aws.sdk.kotlin.services.customerprofiles.model.AccessDeniedException
import aws.sdk.kotlin.services.customerprofiles.model.BadRequestException
import aws.sdk.kotlin.services.customerprofiles.model.InternalServerException
import aws.sdk.kotlin.services.customerprofiles.model.ResourceNotFoundException
import aws.sdk.kotlin.services.customerprofiles.model.ThrottlingException
import com.amplifyframework.foundation.exceptions.AmplifyException
import com.amplifyframework.foundation.exceptions.DEFAULT_RECOVERY_SUGGESTION

/**
 * Base exception for all Connect client operations.
 *
 * This is a sealed hierarchy. Callers can exhaustively match on the subtype:
 * - [ConnectObjectTypeNotConfiguredException] — AmplifyDevice object type not provisioned
 * - [ConnectNotSignedInException] — operation requires an authenticated user
 * - [ConnectDeviceNotRegisteredException] — removeDevice called before registerDevice
 * - [ConnectAccessDeniedException] — IAM policy missing required permissions
 * - [ConnectNetworkException] — network connectivity failure
 * - [ConnectServiceException] — Customer Profiles service error
 * - [ConnectValidationException] — invalid input parameters
 * - [ConnectUnknownException] — unexpected or uncategorized error
 */
sealed class AmplifyConnectException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyException(message, recoverySuggestion, cause) {
    companion object {
        /**
         * Maps a [Throwable] into the appropriate [AmplifyConnectException] subtype.
         */
        internal fun from(error: Throwable): AmplifyConnectException = when (error) {
            is AmplifyConnectException -> error
            is AccessDeniedException -> ConnectAccessDeniedException(
                message = error.message ?: "Access denied",
                recoverySuggestion = "Verify the Cognito authenticated role has the required " +
                    "Customer Profiles IAM permissions (profile:SearchProfiles, profile:CreateProfile, " +
                    "profile:UpdateProfile, profile:PutProfileObject, profile:DeleteProfileObject, " +
                    "profile:GetProfileObjectType).",
                cause = error
            )
            is ThrottlingException -> ConnectServiceException(
                message = error.message ?: "Request throttled",
                recoverySuggestion = "Reduce request frequency or implement backoff.",
                cause = error
            )
            is ResourceNotFoundException -> ConnectServiceException(
                message = error.message ?: "Resource not found",
                recoverySuggestion = "Verify the Customer Profiles domain name and region are correct.",
                cause = error
            )
            is BadRequestException -> ConnectValidationException(
                message = error.message ?: "Invalid request",
                recoverySuggestion = "Check the input parameters.",
                cause = error
            )
            is InternalServerException -> ConnectServiceException(
                message = error.message ?: "Internal service error",
                recoverySuggestion = "Retry the request. If the issue persists, contact AWS support.",
                cause = error
            )
            is java.io.IOException -> ConnectNetworkException(
                message = error.message ?: "Network error",
                recoverySuggestion = "Check network connectivity and retry.",
                cause = error
            )
            else -> ConnectUnknownException(
                message = error.message ?: "An unknown error occurred",
                recoverySuggestion = DEFAULT_RECOVERY_SUGGESTION,
                cause = error
            )
        }
    }
}

/** AmplifyDevice ProfileObjectType is not provisioned in the Customer Profiles domain. */
class ConnectObjectTypeNotConfiguredException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyConnectException(message, recoverySuggestion, cause)

/** Operation requires an authenticated user but no auth session is available. */
class ConnectNotSignedInException(
    message: String,
    recoverySuggestion: String = "Ensure the user is signed in via Amplify Auth before calling Connect client methods.",
    cause: Throwable? = null
) : AmplifyConnectException(message, recoverySuggestion, cause)

/** removeDevice was called but no device is registered. */
class ConnectDeviceNotRegisteredException(
    message: String = "No device is registered. Call registerDevice before removeDevice.",
    recoverySuggestion: String = "Call registerDevice with a valid device token first.",
    cause: Throwable? = null
) : AmplifyConnectException(message, recoverySuggestion, cause)

/** IAM policy is missing required permissions. */
class ConnectAccessDeniedException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyConnectException(message, recoverySuggestion, cause)

/** Network connectivity failure. */
class ConnectNetworkException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyConnectException(message, recoverySuggestion, cause)

/** Customer Profiles service error (throttling, internal error, resource not found). */
class ConnectServiceException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyConnectException(message, recoverySuggestion, cause)

/** Invalid input parameters. */
class ConnectValidationException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyConnectException(message, recoverySuggestion, cause)

/** Unexpected or uncategorized error. */
class ConnectUnknownException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyConnectException(message, recoverySuggestion, cause)
