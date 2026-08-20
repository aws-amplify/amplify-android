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

package com.amplifyframework.auth.cognito.exceptions.session

import com.amplifyframework.auth.exceptions.SessionExpiredException

/**
 * Thrown when Cognito rejects a refresh token. Subclasses [SessionExpiredException] so existing
 * handling keeps working, and adds [reason] plus [serviceMessage] so an app can tell a recoverable
 * device-binding failure apart from a genuine expiry or a revocation instead of treating every
 * rejection as an expired session.
 */
class AWSCognitoSessionExpiredException internal constructor(
    /** Best-effort classification of the rejection. */
    val reason: RefreshFailureReason,
    /** The message returned by Cognito, unmodified. Null if the service supplied none. */
    val serviceMessage: String?,
    cause: Throwable? = null
) : SessionExpiredException(cause = cause) {

    internal companion object {
        /**
         * Cognito signals the cause of a refresh rejection only through the NotAuthorizedException
         * message, so classification is by text. Matching is lenient because these strings are
         * service-controlled and have varied (for example "Invalid Refresh Token." is returned with
         * a trailing period for a device-binding failure and without one when the token itself
         * cannot be validated).
         */
        fun classify(serviceMessage: String?): RefreshFailureReason {
            val message = serviceMessage?.trim() ?: return RefreshFailureReason.Unknown
            return when {
                message.contains("has expired", ignoreCase = true) -> RefreshFailureReason.Expired
                message.contains("has been revoked", ignoreCase = true) -> RefreshFailureReason.Revoked
                message.contains("been deleted", ignoreCase = true) -> RefreshFailureReason.UserDeleted
                message.contains("is disabled", ignoreCase = true) -> RefreshFailureReason.UserDisabled
                // The trailing period distinguishes a device-binding failure from a token that
                // could not be validated at all.
                message.equals("Invalid Refresh Token.", ignoreCase = true) ->
                    RefreshFailureReason.DeviceBindingFailed
                message.contains("Invalid Refresh Token", ignoreCase = true) -> RefreshFailureReason.Invalid
                else -> RefreshFailureReason.Unknown
            }
        }
    }
}
